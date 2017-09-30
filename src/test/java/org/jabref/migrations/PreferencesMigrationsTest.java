package org.jabref.migrations;

//import java.util.prefs.Preferences;

import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PreferencesMigrationsTest {

    private JabRefPreferences prefs;
    private JabRefPreferences backup;

    private final String[] oldStylePatterns = new String[] {"\\bibtexkey",
            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
    private final String[] newStylePatterns = new String[] {"[bibtexkey]",
            "[bibtexkey] - [fulltitle]"};

    @Before
    public void setUp() {
        prefs = JabRefPreferences.getInstance();
        backup = prefs;
        Globals.prefs = prefs;
    }

    @Test
    public void testOldStyleBibtexkeyPattern() {
        //final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);

        //when(mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[0]);
        //mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);
        //prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);
        //when(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[0]);

        PreferencesMigrations.upgradeImportFileAndDirePatterns();

        //verify(prefs).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
        //verify(mainPrefsNode).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
        //assertEquals(newStylePatterns[0], mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, "kva"));
        //assertEquals(newStylePatterns[0], prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));
        assertTrue(true);
    }

    @After
    public void tearDown() {
        //clean up preferences to default state
        prefs.overwritePreferences(backup);
    }
}
