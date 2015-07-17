package net.sf.jabref;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        String tmpFile = "src/test/resources/net/sf/jabref/preferencesTest.pref";
        prefs.exportPreferences(tmpFile);

        List<String> actual = Files.readAllLines(Paths.get(tmpFile));
        List<String> expected = Files.readAllLines(Paths.get("src/test/resources/net/sf/jabref/defaultPreferences.pref"));
      
        Files.delete(Paths.get(tmpFile));

        assertEquals(expected, actual);
    }

}
