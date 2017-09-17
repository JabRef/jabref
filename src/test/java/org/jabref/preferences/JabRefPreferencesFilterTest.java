package org.jabref.preferences;

import org.junit.Test;

public class JabRefPreferencesFilterTest {

    @Test
    public void givenNothingWhenCreatingThenNothingThrown() {
        JabRefPreferences preferences = JabRefPreferences.getInstance();
        new JabRefPreferencesFilter(preferences);
    }

    @Test
    public void givenNothingWhenCreatingPreferenceOptionThenNothingThrown() {
        String key = "";
        Object value = new Object();
        Object defaultValue = new Object();
        new JabRefPreferencesFilter.PreferenceOption(key, value, defaultValue);
    }
}
