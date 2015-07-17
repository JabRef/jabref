package net.sf.jabref;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

public class JabRefPreferencesTest {

    private JabRefPreferences prefs;


    @Before
    public void setUp() {
        prefs = JabRefPreferences.getInstance();
    }

    @After
    public void tearDown() {
        prefs.resetToDefaultPreferences();
    }

    @Test
    public void testPreferencesExport() throws IOException {
        File tmpFile = new File("src/test/resources/net/sf/jabref/preferencesTest.xml");
        File expectedFile = new File("src/test/resources/net/sf/jabref/defaultPreferences.xml");
        
        prefs.exportPreferences(tmpFile.getAbsolutePath());

        List<String> actual = Files.readLines(tmpFile, Charset.defaultCharset());
        List<String> expected = Files.readLines(expectedFile, Charset.defaultCharset());
      
        tmpFile.delete();

        assertEquals(expected, actual);
    }

}
