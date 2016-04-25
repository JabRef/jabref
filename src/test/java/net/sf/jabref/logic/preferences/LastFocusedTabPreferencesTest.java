package net.sf.jabref.logic.preferences;

import java.io.File;

import net.sf.jabref.JabRefPreferences;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LastFocusedTabPreferencesTest {

    private static String previousValue;

    @BeforeClass
    public static void savePreferenceLastFocus() {
        previousValue = JabRefPreferences.getInstance().get(JabRefPreferences.LAST_FOCUSED);
    }

    @AfterClass
    public static void restorePreferenceLastFocus() {
        if (previousValue != null) {
            JabRefPreferences.getInstance().put(JabRefPreferences.LAST_FOCUSED, previousValue);
        }
    }

    @Test
    public void testLastFocusedTab() {
        LastFocusedTabPreferences prefs = new LastFocusedTabPreferences(JabRefPreferences.getInstance());
        File whatever = new File("whatever");
        prefs.setLastFocusedTab(whatever);
        assertTrue(prefs.hadLastFocus(whatever));
    }

    @Test
    public void testLastFocusedTabNull() {
        LastFocusedTabPreferences prefs = new LastFocusedTabPreferences(JabRefPreferences.getInstance());
        File whatever = new File("whatever");
        prefs.setLastFocusedTab(whatever);
        assertTrue(prefs.hadLastFocus(whatever));

        prefs.setLastFocusedTab(null);
        assertTrue(prefs.hadLastFocus(whatever));
    }
}