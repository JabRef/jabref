package org.jabref.migrations;

import java.util.prefs.Preferences;

import org.jabref.Globals;
import org.jabref.JabRefMain;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreferencesMigrationsTest {

    private JabRefPreferences prefs;

    private final String[] oldStylePatterns = new String[] {"\\bibtexkey",
            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
    private final String[] newStylePatterns = new String[] {"[bibtexkey]",
            "[bibtexkey] - [fulltitle]"};

    @BeforeEach
    public void setUp() {
        prefs = JabRefPreferences.getInstance();
        Globals.prefs = prefs;
    }

    @Test
    public void testOldStyleBibtexkeyPattern0() {
        String previousFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);

        PreferencesMigrations.upgradeImportFileAndDirePatterns();

        String updatedPrefsFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        String updatedMainNodeFileNamePattern = mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);

        assertEquals(newStylePatterns[0], updatedPrefsFileNamePattern);
        assertEquals(newStylePatterns[0], updatedMainNodeFileNamePattern);
    }

    @Test
    public void testOldStyleBibtexkeyPattern1() {
        String previousFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[1]);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[1]);

        PreferencesMigrations.upgradeImportFileAndDirePatterns();

        String updatedPrefsFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        String updatedMainNodeFileNamePattern = mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);

        assertEquals(newStylePatterns[1], updatedPrefsFileNamePattern);
        assertEquals(newStylePatterns[1], updatedMainNodeFileNamePattern);
    }

    @Test
    public void testArbitraryBibtexkeyPattern() {
        String previousFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);
        String arbitraryPattern = "[anyUserPrividedString]";

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);

        PreferencesMigrations.upgradeImportFileAndDirePatterns();

        String updatedPrefsFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        String updatedMainNodeFileNamePattern = mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);

        assertEquals(arbitraryPattern, updatedPrefsFileNamePattern);
        assertEquals(arbitraryPattern, updatedMainNodeFileNamePattern);
    }
}
