package org.jabref.preferences;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LastFocusedTabPreferencesTest {

    private static String previousValue;

    @BeforeAll
    static void savePreferenceLastFocus() {
        previousValue = JabRefPreferences.getInstance().get(JabRefPreferences.LAST_FOCUSED);
    }

    @AfterAll
    static void restorePreferenceLastFocus() {
        if (previousValue != null) {
            JabRefPreferences.getInstance().put(JabRefPreferences.LAST_FOCUSED, previousValue);
        }
    }

    @Test
    void testLastFocusedTab() {
        LastFocusedTabPreferences prefs = new LastFocusedTabPreferences(JabRefPreferences.getInstance());
        File whatever = new File("whatever");
        prefs.setLastFocusedTab(whatever);
        assertTrue(prefs.hadLastFocus(whatever));
    }

    @Test
    void testLastFocusedTabNull() {
        LastFocusedTabPreferences prefs = new LastFocusedTabPreferences(JabRefPreferences.getInstance());
        File whatever = new File("whatever");
        prefs.setLastFocusedTab(whatever);
        assertTrue(prefs.hadLastFocus(whatever));

        prefs.setLastFocusedTab(null);
        assertTrue(prefs.hadLastFocus(whatever));
    }
}
