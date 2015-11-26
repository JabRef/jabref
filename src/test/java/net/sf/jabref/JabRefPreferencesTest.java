package net.sf.jabref;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JabRefPreferencesTest {

    private JabRefPreferences prefs;

    private JabRefPreferences backup;


    @Before
    public void setUp() {
        prefs = JabRefPreferences.getInstance();
        backup = prefs;
    }

    @Test
    public void testPreferencesImport() throws JabRefException {
        // the primary sort field has been changed to "editor" in this case
        File importFile = new File("src/test/resources/net/sf/jabref/customPreferences.xml");

        prefs.importPreferences(importFile.getAbsolutePath());

        String expected = "editor";
        String actual = prefs.get(JabRefPreferences.SAVE_PRIMARY_SORT_FIELD);

        assertEquals(expected, actual);
    }

    @After
    public void tearDown() {
        //clean up preferences to default state
        prefs.overwritePreferences(backup);
    }

}
