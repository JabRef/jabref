package org.jabref.migrations;

import java.util.Objects;
import java.util.prefs.Preferences;

import org.jabref.Globals;
import org.jabref.JabRefMain;
import org.jabref.preferences.JabRefPreferences;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PreferencesMigrationsTest {

    private final String[] oldStylePatterns = new String[] {"\\bibtexkey",
            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
    private final String[] newStylePatterns = new String[] {"[bibtexkey]",
            "[bibtexkey] - [fulltitle]"};

    @Test
    public void testOldStyleBibtexkeyPattern() {
        //final Preferences mainPrefsNode = mock(Preferences.class);
        final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);
        Objects.requireNonNull(mainPrefsNode);

        final JabRefPreferences prefs = JabRefPreferences.getInstance();
        Objects.requireNonNull(prefs);

        Globals.prefs = prefs;

        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);
        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);

        PreferencesMigrations.upgradeImportFileAndDirePatterns();

        assertEquals(newStylePatterns[0], mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, "kva"));
        assertEquals(newStylePatterns[0], prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));
    }

}
