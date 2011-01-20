package com.yahoo.platform.yui.compressor;

import static junitx.framework.FileAssert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

public class CssCompressorTest {

    private static final String TESTS_DIRECTORY =
            "c:\\projects\\yuicompressor\\src\\test\\resources\\";
    
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
                     getCompressedFile(filename));
    }

    private File getCompressedFile(String testFileName)
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
        File expected = new File(TESTS_DIRECTORY + testFileName + ".min");
        return expected;
    }

    private URL getFileUrl(final String name) {
        return ClassLoader.getSystemResource(name);
    }

}
