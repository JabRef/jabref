package org.jabref.preferences;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class JabRefPreferencesFilterTest {

    @Test
    public void givenNothingWhenCreatingThenNothingThrown() {
        boolean nothingThrown = true;
        try {
            JabRefPreferences preferences = JabRefPreferences.getInstance();
            new JabRefPreferencesFilter(preferences);
        } catch (Exception e) {
            nothingThrown = false;
        }
        assertTrue("Simple creation should not throw exceptions.", nothingThrown);
    }

    @Test
    public void givenNothingWhenCreatingPreferenceOptionThenNothingThrown() {
        boolean nothingThrown = true;
        try {
            String key = "";
            Object value = new Object();
            Object defaultValue = new Object();
            new JabRefPreferencesFilter.PreferenceOption(key, value, defaultValue);
        } catch (Exception e) {
            nothingThrown = false;
        }
        assertTrue("Simple creation should not throw exceptions.", nothingThrown);
    }
}
