package org.jabref.preferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalApplicationsPreferencesTest {


    @Test
    public void testGetCiteCommandWhenValid() {
        ExternalApplicationsPreferences prefs = new ExternalApplicationsPreferences(
                "emailSubject",
                true,
                "\\cite{key1,key2}",
                "defaultCiteCommand",
                false,
                "customTerminalCommand",
                false,
                "customFileBrowserCommand",
                "kindleEmail"
        );

        CitationCommandStringPreferences result = prefs.getCiteCommand();
        assertEquals("\\cite{", result.prefix());
        assertEquals(",", result.delimiter());
        assertEquals("}", result.suffix());
    }

    @Test
    void testGetCiteCommandWhenInValid() {
        ExternalApplicationsPreferences prefs = new ExternalApplicationsPreferences(
                "emailSubject",
                true,
                "\\cite",
                String defaultCiteCommand,
                false,
                "customTerminalCommand",
                false,
                "customFileBrowserCommand",
                "kindleEmail");

        CitationCommandStringPreferences result = prefs.getCiteCommand();
        assertEquals("{", result.prefix()); // default prefix
        assertEquals(",", result.delimiter()); // default delimiter
        assertEquals("}", result.suffix()); // default suffix
    }
}
