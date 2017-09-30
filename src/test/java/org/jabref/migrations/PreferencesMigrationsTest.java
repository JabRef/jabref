package org.jabref.migrations;

import java.util.prefs.Preferences;

import org.jabref.Globals;
import org.jabref.JabRefMain;
import org.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PreferencesMigrationsTest {

    private JabRefPreferences prefs;

    private final String[] oldStylePatterns = new String[] {"\\bibtexkey",
            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
    private final String[] newStylePatterns = new String[] {"[bibtexkey]",
            "[bibtexkey] - [fulltitle]"};

    @Before
    public void setUp() {
        prefs = JabRefPreferences.getInstance();
        Globals.prefs = prefs;
    }

    @Test
    public void testOldStyleBibtexkeyPattern() {
        String previousFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);

        //when(mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[0]);
        //when(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[0]);
        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);

        PreferencesMigrations.upgradeImportFileAndDirePatterns();

        String updatedPrefsFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        String updatedMainNodeFileNamePattern = mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);

        //verify(prefs).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
        //verify(mainPrefsNode).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
        assertEquals(newStylePatterns[0], updatedPrefsFileNamePattern);
        assertEquals(newStylePatterns[0], updatedMainNodeFileNamePattern);
        //assertTrue(true);
    }
}
