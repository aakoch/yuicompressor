/*
 * YUI Compressor Author: Julien Lecomte - http://www.julienlecomte.net/ Author:
 * Isaac Schlueter - http://foohack.com/ Author: Stoyan Stefanov -
 * http://phpied.com/ Copyright (c) 2009 Yahoo! Inc. All rights reserved. The
 * copyrights embodied in the content of this file are licensed by Yahoo! Inc.
 * under the BSD (revised) open source license.
 */

package com.yahoo.platform.yui.compressor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CssCompressor {

    private String css;

    public CssCompressor(Reader in) throws IOException {
        StringBuffer srcsb = new StringBuffer();
        // Read the stream...
        int c;
        while ((c = in.read()) != -1) {
            srcsb.append((char) c);
        }
        css = srcsb.toString();
    }

    public CssCompressor(String css) {
        this.css = css;
    }

    public void compress(Writer out, int linebreakpos) throws IOException {
        Pattern p;
        Matcher m;
        StringBuffer sb = new StringBuffer(css);

        int startIndex = 0;
        int endIndex = 0;
        int i = 0;
        int max = 0;
        ArrayList preservedTokens = new ArrayList(0);
        ArrayList comments = new ArrayList(0);
        String token;
        int totallen = css.length();
        String placeholder;

        // collect all comment blocks...
        while ((startIndex = sb.indexOf("/*", startIndex)) >= 0) {
            endIndex = sb.indexOf("*/", startIndex + 2);
            if (endIndex < 0) {
                endIndex = totallen;
            }

            token = sb.substring(startIndex + 2, endIndex);
            comments.add(token);
            sb.replace(startIndex + 2, endIndex,
                       "___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_" +
                               (comments.size() - 1) + "___");
            startIndex += 2;
        }
        css = sb.toString();

        // preserve strings so their content doesn't get accidentally minified
        sb = new StringBuffer();
        p = Pattern.compile("(\"([^\\\\\"]|\\\\.|\\\\)*\")|(\'([^\\\\\']|\\\\.|\\\\)*\')");
        m = p.matcher(css);
        while (m.find()) {
            token = m.group();
            char quote = token.charAt(0);
            token = token.substring(1, token.length() - 1);

            // maybe the string contains a comment-like substring?
            // one, maybe more? put'em back then
            if (token.indexOf("___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_") >= 0) {
                for (i = 0, max = comments.size(); i < max; i += 1) {
                    token = token.replace("___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_" +
                                                     i + "___", comments.get(i)
                                                     .toString());
                }
            }

            // minify alpha opacity in filter strings
            token = token.replaceAll("(?i)progid:DXImageTransform.Microsoft.Alpha\\(Opacity=",
                            "alpha(opacity=");

            preservedTokens.add(token);
            String preserver =
                    quote + "___YUICSSMIN_PRESERVED_TOKEN_" +
                            (preservedTokens.size() - 1) + "___" + quote;
            m.appendReplacement(sb, preserver);
        }
        m.appendTail(sb);
        css = sb.toString();

        // strings are safe, now wrestle the comments
        for (i = 0, max = comments.size(); i < max; i += 1) {

            token = comments.get(i).toString();
            placeholder =
                    "___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_" + i + "___";

            // ! in the first position of the comment means preserve
            // so push to the preserved tokens while stripping the !
            if (token.startsWith("!")) {
                preservedTokens.add(token);
                css = css.replace(placeholder,
                            "___YUICSSMIN_PRESERVED_TOKEN_" +
                                    (preservedTokens.size() - 1) +
                                    "___");
                continue;
            }

            // \ in the last position looks like hack for Mac/IE5
            // shorten that to /*\*/ and the next one to /**/
            if (token.endsWith("\\")) {
                preservedTokens.add("\\");
                css = css.replace(placeholder,
                            "___YUICSSMIN_PRESERVED_TOKEN_" +
                                    (preservedTokens.size() - 1) +
                                    "___");
                i = i + 1; // attn: advancing the loop
                preservedTokens.add("");
                css = css.replace("___YUICSSMIN_PRESERVE_CANDIDATE_COMMENT_" +
                        i + "___", "___YUICSSMIN_PRESERVED_TOKEN_" +
                        (preservedTokens.size() - 1) + "___");
                continue;
            }

            // keep empty comments after child selectors (IE7 hack)
            // e.g. html >/**/ body
            if (token.length() == 0) {
                startIndex = css.indexOf(placeholder);
                if (startIndex > 2) {
                    if (css.charAt(startIndex - 3) == '>') {
                        preservedTokens.add("");
                        css = css.replace(placeholder,
                                 "___YUICSSMIN_PRESERVED_TOKEN_" +
                                         (preservedTokens.size() - 1) +
                                         "___");
                    }
                }
            }

            // in all other cases kill the comment
            css = css.replace("/*" + placeholder + "*/", "");
        }

        // Normalize all whitespace strings to single spaces. Easier to work with that way.
        css = css.replaceAll("\\s+", " ");

        // Remove the spaces before the things that should not have spaces before them.
        // But, be careful not to turn "p :link {...}" into "p:link{...}"
        // Swap out any pseudo-class colons with the token, and then swap back.
        sb = new StringBuffer();
        p = Pattern.compile("(^|\\})(([^\\{:])+:)+([^\\{]*\\{)");
        m = p.matcher(css);
        while (m.find()) {
            String s = m.group();
            s = s.replaceAll(":", "___YUICSSMIN_PSEUDOCLASSCOLON___");
            s = s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$");
            m.appendReplacement(sb, s);
        }
        m.appendTail(sb);
        css = sb.toString();
        // Remove spaces before the things that should not have spaces before them.
        css = css.replaceAll("\\s+([!{};:>+\\(\\)\\],])", "$1");
        // bring back the colon
        css = css.replaceAll("___YUICSSMIN_PSEUDOCLASSCOLON___", ":");

        // retain space for special IE6 cases
        css = css.replaceAll(":first\\-(line|letter)(\\{|,)", ":first-$1 $2");

        // no space after the end of a preserved comment
        css = css.replaceAll("\\*/ ", "*/");

        // If there is a @charset, then only allow one, and push to the top of the file.
        css = css.replaceAll("^(.*)(@charset \"[^\"]*\";)", "$2$1");
        css = css.replaceAll("^(\\s*@charset [^;]+;\\s*)+", "$1");

        // Put the space back in some cases, to support stuff like
        // @media screen and (-webkit-min-device-pixel-ratio:0){
        css = css.replaceAll("\\band\\(", "and (");

        // Remove the spaces after the things that should not have spaces after them.
        css = css.replaceAll("([!{}:;>+\\(\\[,])\\s+", "$1");

        // remove unnecessary semicolons
        css = css.replaceAll(";+}", "}");

        // Replace 0(px,em,%) with 0.
        css = css.replaceAll("([\\s:])(0)(px|em|%|in|cm|mm|pc|pt|ex)", "$1$2");

        // Replace 0 0 0 0; with 0.
        css = css.replaceAll(":0 0 0 0(;|})", ":0$1");
        css = css.replaceAll(":0 0 0(;|})", ":0$1");
        css = css.replaceAll(":0 0(;|})", ":0$1");

        // Replace background-position:0; with background-position:0 0;
        // same for transform-origin
        sb = new StringBuffer();
        p = Pattern.compile("(?i)(background-position|transform-origin|webkit-transform-origin|moz-transform-origin|o-transform-origin|ms-transform-origin):0(;|})");
        m = p.matcher(css);
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toLowerCase() + ":0 0" +
                    m.group(2));
        }
        m.appendTail(sb);
        css = sb.toString();

        // Replace 0.6 to .6, but only when preceded by : or a white-space
        css = css.replaceAll("(:|\\s)0+\\.(\\d+)", "$1.$2");

        // Shorten colors from rgb(51,102,153) to #336699
        // This makes it more likely that it'll get further compressed in the next step.
        p = Pattern.compile("rgb\\s*\\(\\s*([0-9,\\s]+)\\s*\\)");
        m = p.matcher(css);
        sb = new StringBuffer();
        while (m.find()) {
            String[] rgbcolors = m.group(1).split(",");
            StringBuffer hexcolor = new StringBuffer("#");
            for (i = 0; i < rgbcolors.length; i++) {
                int val = Integer.parseInt(rgbcolors[i]);
                if (val < 16) {
                    hexcolor.append("0");
                }
                hexcolor.append(Integer.toHexString(val));
            }
            m.appendReplacement(sb, hexcolor.toString());
        }
        m.appendTail(sb);
        css = sb.toString();

        // Replace color keywords with hex value
        css = replaceColors(css);
        
        // Shorten colors from #AABBCC to #ABC. Note that we want to make sure
        // the color is not preceded by either ", " or =. Indeed, the property
        //     filter: chroma(color="#FFFFFF");
        // would become
        //     filter: chroma(color="#FFF");
        // which makes the filter break in IE.
        p = Pattern.compile("([^\"'=\\s])(\\s*)#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");
        
        // ([0-9a-fA-F])\1([0-9a-fA-F])\2([0-9a-fA-F])\3
        
        m = p.matcher(css);
        sb = new StringBuffer();
        while (m.find()) {
            // Test for AABBCC pattern
            if (m.group(3).equalsIgnoreCase(m.group(4)) &&
                    m.group(5).equalsIgnoreCase(m.group(6)) &&
                    m.group(7).equalsIgnoreCase(m.group(8))) {
                m.appendReplacement(sb, (m.group(1) + m.group(2) + "#" +
                        m.group(3) + m.group(5) + m.group(7)).toLowerCase());
            }
            else {
                m.appendReplacement(sb, m.group().toLowerCase());
            }
        }
        m.appendTail(sb);
        css = sb.toString();
        
        css = convertUppercaseColorsToLowercase(css);

        // border: none -> border:0
        sb = new StringBuffer();
        p = Pattern.compile("(?i)(border|border-top|border-right|border-bottom|border-right|outline|background):none(;|})");
        m = p.matcher(css);
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toLowerCase() + ":0" + m.group(2));
        }
        m.appendTail(sb);
        css = sb.toString();

        // shorter opacity IE filter
        css = css.replaceAll("(?i)progid:DXImageTransform.Microsoft.Alpha\\(Opacity=",
                            "alpha(opacity=");

        // Remove empty rules.
        css = css.replaceAll("[^\\}\\{/;]+\\{\\}", "");

        if (linebreakpos >= 0) {
            // Some source control tools don't like it when files containing lines longer
            // than, say 8000 characters, are checked in. The linebreak option is used in
            // that case to split long lines after a specific column.
            i = 0;
            int linestartpos = 0;
            sb = new StringBuffer(css);
            while (i < sb.length()) {
                char c = sb.charAt(i++);
                if (c == '}' && i - linestartpos > linebreakpos) {
                    sb.insert(i, '\n');
                    linestartpos = i;
                }
            }

            css = sb.toString();
        }


        // Replace #f00 with red
        css = css.replaceAll("#[fF]00", "red");

        // Replace multiple semi-colons in a row by a single one
        // See SF bug #1980989
        css = css.replaceAll(";;+", ";");

        // restore preserved comments and strings
        for (i = 0, max = preservedTokens.size(); i < max; i++) {
            css = css.replace("___YUICSSMIN_PRESERVED_TOKEN_" + i + "___",
                        preservedTokens.get(i).toString());
        }
        
        css = convertPreservedColorsTokensFromUppercaseToLowercase(css);

        // Trim the final string (for any leading or trailing white spaces)
        css = css.trim();

        // Write the output...
        out.write(css);
    }

    final MessageFormat regexFormat =
            new MessageFormat("(?i)(\\s+|:){0}(\\b|;)");
    final MessageFormat replacementFormat = new MessageFormat("$1#{0}$2");
    
    String[][] colorsAndHexes =
            new String[][] { {"aliceblue","f0f8ff"}, 
            {"antiquewhite","faebd7"}, 
//            {"aqua",""},
            {"aquamarine","7fffd4"}, 
//            {"azure","azure"}, 
//            {"beige","beige"}, 
//            {"bisque","bisque"}, 
            {"black","000"}, 
            {"blanchedalmond","ffebcd"},
//            {"blue","blue"}, 
            {"blueviolet","8a2be2"},
//            {"brown","brown"}, 
            {"burlywood","deb887"}, 
            {"cadetblue","5f9ea0"}, 
            {"chartreuse","7fff00"}, 
            {"chocolate","d2691e"}, 
//            {"coral","coral"}, 
            {"cornflowerblue","6495ed"}, 
            {"cornsilk","fff8dc"},
//            {"crimson","crimson"}, 
//            {"cyan","cyan"}, 
            {"darkblue","00008b"}, 
            {"darkcyan","008b8b"}, 
            {"darkgoldenrod","b8860b"}, 
            {"darkgray","a9a9a9"}, 
            {"darkgreen","006400"}, 
            {"darkkhaki","bdb76b"}, 
            {"darkmagenta","8b008b"}, 
            {"darkolivegreen","556b2f"}, 
            {"darkorange","ff8c00"}, 
            {"darkorchid","9932cc"},
//            {"darkred","darkred"}, 
            {"darksalmon","e9967a"}, 
            {"darkseagreen","8fbc8f"}, 
            {"darkslateblue","483d8b"}, 
            {"darkslategray","2f4f4f"}, 
            {"darkturquoise","00ced1"}, 
            {"darkviolet","9400d3"}, 
            {"deeppink","ff1493"}, 
            {"deepskyblue","00bfff"},
//            {"dimgray","dimgray"}, 
            {"dodgerblue","1e90ff"}, 
            {"firebrick","b22222"}, 
            {"floralwhite","fffaf0"}, 
            {"forestgreen","228b22"}, 
            {"fuchsia","f0f"}, 
            {"gainsboro","dcdcdc"}, 
            {"ghostwhite","f8f8ff"},
//            {"gold","gold"}, 
            {"goldenrod","daa520"},
//            {"gray","gray"}, 
//            {"green","green"}, 
            {"greenyellow","adff2f"}, 
            {"honeydew","f0fff0"}, 
//            {"hotpink","hotpink"}, 
            {"indianred","cd5c5c"}, 
//            {"indigo","indigo"}, 
//            {"ivory","ivory"}, 
//            {"khaki","khaki"}, 
            {"lavender","e6e6fa"}, 
            {"lavenderblush","fff0f5"}, 
            {"lawngreen","7cfc00"}, 
            {"lemonchiffon","fffacd"}, 
            {"lightblue","add8e6"}, 
            {"lightcoral","f08080"}, 
            {"lightcyan","e0ffff"}, 
            {"lightgoldenrodyellow","fafad2"}, 
            {"lightgreen","90ee90"}, 
            {"lightgrey","d3d3d3"}, 
            {"lightpink","ffb6c1"}, 
            {"lightsalmon","ffa07a"}, 
            {"lightseagreen","20b2aa"}, 
            {"lightskyblue","87cefa"}, 
            {"lightslategray","778899"}, 
            {"lightsteelblue","b0c4de"}, 
            {"lightyellow","ffffe0"}, 
            {"lime","0f0"}, 
            {"limegreen","32cd32"}, 
//            {"linen","linen"}, 
            {"magenta","f0f"}, 
//            {"maroon","maroon"}, 
            {"mediumaquamarine","66cdaa"}, 
            {"mediumblue","0000cd"}, 
            {"mediumorchid","ba55d3"}, 
            {"mediumpurple","9370d8"}, 
            {"mediumseagreen","3cb371"}, 
            {"mediumslateblue","7b68ee"}, 
            {"mediumspringgreen","00fa9a"}, 
            {"mediumturquoise","48d1cc"}, 
            {"mediumvioletred","c71585"}, 
            {"midnightblue","191970"}, 
            {"mintcream","f5fffa"}, 
            {"mistyrose","ffe4e1"}, 
            {"moccasin","ffe4b5"}, 
            {"navajowhite","ffdead"},
//            {"navy","navy"}, 
//            {"oldlace","oldlace"}, 
//            {"olive","olive"}, 
            {"olivedrab","688e23"}, 
            {"orangered","ff4500"}, 
            {"palegoldenrod","eee8aa"}, 
            {"palegreen","98fb98"}, 
            {"paleturquoise","afeeee"}, 
            {"palevioletred","d87093"}, 
            {"papayawhip","ffefd5"}, 
            {"peachpuff","ffdab9"},
            {"powderblue","b0e0e6"}, 
            {"rosybrown","bc8f8f"}, 
            {"royalblue","4169e1"}, 
            {"saddlebrown","8b4513"}, 
            {"sandybrown","f4a460"}, 
            {"seagreen","2e8b57"}, 
            {"seashell","fff5ee"}, 
            {"silver","ccc"}, 
//            {"skyblue","87ceeb"}, 
            {"slateblue","6a5acd"}, 
            {"slategray","708090"}, 
            {"springgreen","00ff7f"}, 
            {"steelblue","4682b4"}, 
            {"thistle","d8bfd8"}, 
            {"turquoise","40e0d0"}, 
            {"white","fff"},
            {"whitesmoke","f5f5f5"},
            {"yellow","ff0"},
            {"yellowgreen","9acd32"} };
    
    private String replaceColors(String css) {
        for (String[] colorAndHex : colorsAndHexes) {
            css = replaceColor(css, colorAndHex[0], colorAndHex[1]);
        }
        return css;
    }

    private String replaceColor(String css, String color, String hex) {
        String regex = regexFormat.format(new String[] { color });
        String replacement = replacementFormat.format(new String[] { hex });
        css = css.replaceAll(regex, replacement);
        return css;
    }

    private String convertUppercaseColorsToLowercase(final String css) {
        String newCss = css;
        Pattern p = Pattern.compile("#([0-9a-fA-F]){3}");
        Matcher m = p.matcher(newCss);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(0).toLowerCase());
        }
        m.appendTail(sb);
        newCss = sb.toString();

        p = Pattern.compile("#([0-9a-fA-F]){6}");
        m = p.matcher(newCss);

        sb.setLength(0);
        sb.ensureCapacity(newCss.length());

        while (m.find()) {
            m.appendReplacement(sb, m.group(0).toLowerCase());
        }
        m.appendTail(sb);
        newCss = sb.toString();

        return newCss;
    }

    private String convertPreservedColorsTokensFromUppercaseToLowercase(final String css) {
        Pattern p = Pattern.compile("#([0-9a-fA-F]){6}");;
        Matcher m = p.matcher(css);
        StringBuffer sb = new StringBuffer(css.length());

        while (m.find()) {
            m.appendReplacement(sb, m.group(0).toLowerCase());
        }
        m.appendTail(sb);

        return sb.toString();
    }
}
