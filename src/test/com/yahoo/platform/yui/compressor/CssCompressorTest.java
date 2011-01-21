package com.yahoo.platform.yui.compressor;

import static junitx.framework.FileAssert.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

public class CssCompressorTest {

    private static final String TESTS_DIRECTORY =
            "c:\\projects\\yuicompressor\\src\\test\\resources\\";
    
    @Test
    public void testRed() {
        assertEquals("body{color:red}", compress("body{color:#f00}"));
    }
    
    @Test
    public void testBlack() {
        assertEquals("body{color:#000}", compress("body { color: black; }"));
    }
    
    @Test
    public void testColorNames() {
        for (String[] colorHex : colorsAndHexes) {
            if (colorHex[1].length() >= colorHex[0].length() && 
                    !colorHex[1].equalsIgnoreCase(colorHex[0]))
                System.out.println("Hex is larger or equal to " + colorHex[0]);
            if (colorHex[1].matches("#([0-9a-fA-F])\1([0-9a-fA-F])\2([0-9a-fA-F])\3"))
                System.out.println("Hex is larger or equal to " + colorHex[0]);
            assertEquals("body{color:" + colorHex[1].toLowerCase() + "}", compress("body { color: " + colorHex[0] + "; }"));
        }
    }
    

    private String[][] colorsAndHexes =
            new String[][] {{"aliceblue","#f0f8ff"}, 
            {"antiquewhite","#faebd7"}, 
            {"aqua","aqua"},
            {"aquamarine","#7fffd4"}, 
            {"azure","azure"}, 
            {"beige","beige"}, 
            {"bisque","bisque"}, 
            {"black","#000"}, 
            {"blanchedalmond","#ffebcd"}, 
            {"blue","blue"}, 
            {"blueviolet","#8a2be2"}, 
            {"brown","brown"}, 
            {"burlywood","#deb887"}, 
            {"cadetblue","#5f9ea0"}, 
            {"chartreuse","#7fff00"}, 
            {"chocolate","#d2691e"}, 
            {"coral","coral"}, 
            {"cornflowerblue","#6495ed"}, 
            {"cornsilk","#fff8dc"}, 
            {"crimson","crimson"}, 
            {"cyan","cyan"}, 
            {"darkblue","#00008b"}, 
            {"darkcyan","#008b8b"}, 
            {"darkgoldenrod","#b8860b"}, 
            {"darkgray","#a9a9a9"}, 
            {"darkgreen","#006400"}, 
            {"darkkhaki","#bdb76b"}, 
            {"darkmagenta","#8b008b"}, 
            {"darkolivegreen","#556b2f"}, 
            {"darkorange","#ff8c00"}, 
            {"darkorchid","#9932cc"}, 
            {"darkred","darkred"}, 
            {"darksalmon","#e9967a"}, 
            {"darkseagreen","#8fbc8f"}, 
            {"darkslateblue","#483d8b"}, 
            {"darkslategray","#2f4f4f"}, 
            {"darkturquoise","#00ced1"}, 
            {"darkviolet","#9400d3"}, 
            {"deeppink","#ff1493"}, 
            {"deepskyblue","#00bfff"}, 
            {"dimgray","dimgray"}, 
            {"dodgerblue","#1e90ff"}, 
            {"firebrick","#b22222"}, 
            {"floralwhite","#fffaf0"}, 
            {"forestgreen","#228b22"}, 
            {"fuchsia","#f0f"}, 
            {"gainsboro","#dcdcdc"}, 
            {"ghostwhite","#f8f8ff"}, 
            {"gold","gold"}, 
            {"goldenrod","#daa520"}, 
            {"gray","gray"}, 
            {"green","green"}, 
            {"greenyellow","#adff2f"}, 
            {"honeydew","#f0fff0"}, 
            {"hotpink","hotpink"}, 
            {"indianred","#cd5c5c"}, 
            {"indigo","indigo"}, 
            {"ivory","ivory"}, 
            {"khaki","khaki"}, 
            {"lavender","#e6e6fa"}, 
            {"lavenderblush","#fff0f5"}, 
            {"lawngreen","#7cfc00"}, 
            {"lemonchiffon","#fffacd"}, 
            {"lightblue","#add8e6"}, 
            {"lightcoral","#f08080"}, 
            {"lightcyan","#e0ffff"}, 
            {"lightgoldenrodyellow","#fafad2"}, 
            {"lightgreen","#90ee90"}, 
            {"lightgrey","#d3d3d3"}, 
            {"lightpink","#ffb6c1"}, 
            {"lightsalmon","#ffa07a"}, 
            {"lightseagreen","#20b2aa"}, 
            {"lightskyblue","#87cefa"}, 
            {"lightslategray","#789"}, 
            {"lightsteelblue","#b0c4de"}, 
            {"lightyellow","#ffffe0"}, 
            {"lime","#0f0"}, 
            {"limegreen","#32cd32"}, 
            {"linen","linen"}, 
            {"magenta","#f0f"}, 
            {"maroon","maroon"}, 
            {"mediumaquamarine","#66cdaa"}, 
            {"mediumblue","#0000cd"}, 
            {"mediumorchid","#ba55d3"}, 
            {"mediumpurple","#9370d8"}, 
            {"mediumseagreen","#3cb371"}, 
            {"mediumslateblue","#7b68ee"}, 
            {"mediumspringgreen","#00fa9a"}, 
            {"mediumturquoise","#48d1cc"}, 
            {"mediumvioletred","#c71585"}, 
            {"midnightblue","#191970"}, 
            {"mintcream","#f5fffa"}, 
            {"mistyrose","#ffe4e1"}, 
            {"moccasin","#ffe4b5"}, 
            {"navajowhite","#ffdead"}, 
            {"navy","navy"}, 
            {"oldlace","oldlace"}, 
            {"olive","olive"}, 
            {"olivedrab","#688e23"}, 
            {"orange","orange"}, 
            {"orangered","#ff4500"}, 
            {"orchid","orchid"}, 
            {"palegoldenrod","#eee8aa"}, 
            {"palegreen","#98fb98"}, 
            {"paleturquoise","#afeeee"}, 
            {"palevioletred","#d87093"}, 
            {"papayawhip","#ffefd5"}, 
            {"peachpuff","#ffdab9"}, 
            {"peru","peru"}, 
            {"pink","pink"}, 
            {"plum","plum"}, 
            {"powderblue","#b0e0e6"}, 
            {"purple","purple"}, 
            {"rosybrown","#bc8f8f"}, 
            {"royalblue","#4169e1"}, 
            {"saddlebrown","#8b4513"}, 
            {"salmon","salmon"}, 
            {"sandybrown","#f4a460"}, 
            {"seagreen","#2e8b57"}, 
            {"seashell","#fff5ee"}, 
            {"sienna","sienna"}, 
            {"silver","#ccc"}, 
            {"skyblue","skyblue"}, 
            {"slateblue","#6a5acd"}, 
            {"slategray","#708090"}, 
            {"snow","snow"}, 
            {"springgreen","#00ff7f"}, 
            {"steelblue","#4682b4"}, 
            {"tan","tan"}, 
            {"teal","teal"}, 
            {"thistle","#d8bfd8"}, 
            {"tomato","tomato"}, 
            {"turquoise","#40e0d0"}, 
            {"violet","violet"}, 
            {"wheat","wheat"}, 
            {"white","#fff"}, 
            {"whitesmoke","#f5f5f5"}, 
            {"yellow","#ff0"},
            {"yellowgreen","#9acd32"}};
    
    
    @Test
    public void testMagenta() {
        assertEquals("body{color:#f0f}", compress("body { color: magenta; }"));
    }
    
    @Test
    public void testSilver() {
        assertEquals("body{color:#ccc}", compress("body { color: silver; }"));
    }
    
    @Test
    public void testWhite() {
        assertEquals("body{color:#fff}", compress("body { color: white; }"));
    }
    
    @Test
    public void testYellow() {
        assertEquals("body{color:#ff0}", compress("body { color: yellow; }"));
    }
    
    @Test
    public void testFuchsia() {
        assertEquals("body{color:#f0f}", compress("body { color: fuchsia; }"));
    }
    
    @Test
    public void testLightSlateGray() {
        assertEquals("body{color:#789}", compress("body { color: lightslategray; }"));
    }
    
    @Test
    public void testRedUppercase() {
        assertEquals("body{color:red}", compress("body{color:#F00}"));
    }
    
    @Test
    public void testBlueLowercase() {
        assertEquals("body{color:#0f0}", compress("body{color:#0f0}"));
    }

    @Test
    public void testBlueUppercaseToLowercase() {
        assertEquals("body{color:#0f0}", compress("body{color:#0F0}"));
    }

    @Test
    public void testUppercaseToLowercase() {
        assertEquals("body{color:#0ff}", compress("body{color:#00FFFF}"));
    }

    @Test
    public void test6CharacterUppercaseToLowercase() {
        assertEquals(".color{filter:chroma(color=\"#ffffff\")}", compress(".color{filter: chroma(color=\"#FFFFFF\");}"));
    }
    
    private String compress(String css) {
        CssCompressor compressor = new CssCompressor(css);
        Writer out = new StringWriter();
        int linebreakpos = -1;
        try {
            compressor.compress(out, linebreakpos);
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return out.toString();
    }

    @Test
    public void testCompress() throws URISyntaxException,
            FileNotFoundException, IOException {

        File dir = new File(TESTS_DIRECTORY);

        FilenameFilter cssFileFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".css");
            }
        };

        String[] children = dir.list(cssFileFilter);
        assertTrue("No files found in " + TESTS_DIRECTORY, children.length > 0);
        
        for (String filename : children) {
            checkFile(filename);
        }

    }

    private void checkFile(String filename) throws IOException,
            FileNotFoundException {
        System.out.println(filename);

        assertEquals(getExpectedFile(filename),
                     compressFile(filename));
    }

    private File compressFile(String testFileName)
            throws FileNotFoundException, IOException {
        final FileReader in =
                new FileReader(getFileUrl(testFileName).getFile());

        CssCompressor cssCompressor = new CssCompressor(in);

        File actual = File.createTempFile("out", null);
        Writer out = new FileWriter(actual);
        cssCompressor.compress(out, -1);
        out.close();

        return actual;
    }

    private File getExpectedFile(String testFileName) throws IOException {
        return new File(TESTS_DIRECTORY + testFileName + ".min");
    }

    private URL getFileUrl(final String name) {
        return ClassLoader.getSystemResource(name);
    }

}
