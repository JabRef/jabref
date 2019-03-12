package org.jabref.preferences;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LastFocusedTabPreferencesTest {

    private static String previousValue;

    @BeforeAll
    public static void savePreferenceLastFocus() {
        previousValue = JabRefPreferences.getInstance().get(JabRefPreferences.LAST_FOCUSED);
    }

    @AfterAll
    public static void restorePreferenceLastFocus() {
        if (previousValue != null) {
            JabRefPreferences.getInstance().put(JabRefPreferences.LAST_FOCUSED, previousValue);
        }
    }

    @Test
    public void testLastFocusedTab() {
        getFocusedTabPreferences();
    }

    @Test
    public void testLastFocusedTabNull() {
        File whatever = new File("whatever");
        getFocusedTabPreferences().setLastFocusedTab(null);
        assertTrue(getFocusedTabPreferences().hadLastFocus(whatever));
    }

    private LastFocusedTabPreferences getFocusedTabPreferences(){
        LastFocusedTabPreferences prefs = new LastFocusedTabPreferences(JabRefPreferences.getInstance());
        File whatever = new File("whatever");
        prefs.setLastFocusedTab(whatever);
        assertTrue(prefs.hadLastFocus(whatever));
        return prefs;
    }
}
