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

    @Test
    public void testPreferencesExport() throws IOException {
        File tmpFile = new File("src/test/resources/net/sf/jabref/preferencesTest.xml");
        File expectedFile = new File("src/test/resources/net/sf/jabref/defaultPreferences.xml");

        prefs.exportPreferences(tmpFile.getAbsolutePath());

        List<String> actual = Files.readLines(tmpFile, Charset.defaultCharset());
        List<String> expected = Files.readLines(expectedFile, Charset.defaultCharset());

        tmpFile.delete();

        for (int i = 0; i < actual.size(); i++) {
            assertEquals(expected.get(i), actual.get(i));
        }
    }

    @Test
    public void testPreferencesImport() throws IOException {
        // the primary sort field has been changed to "editor" in this case
        File importFile = new File("src/test/resources/net/sf/jabref/customPreferences.xml");

        prefs.importPreferences(importFile.getAbsolutePath());

        String expected = "editor";
        String actual = prefs.get(JabRefPreferences.SAVE_PRIMARY_SORT_FIELD);
        
        //clean up preferences to default state
        prefs.put(JabRefPreferences.SAVE_PRIMARY_SORT_FIELD, "author");

        assertEquals(expected, actual);
    }

}
