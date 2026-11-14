package org.jabref.logic.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class BuiltInJournalListPreferencesTest {
    private final Map<String, Boolean> values = new HashMap<>();

    private Preferences isolatedStore() {
        Preferences store = mock(Preferences.class);
        when(store.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(store.getBoolean(anyString(), anyBoolean())).thenAnswer(invocation ->
                values.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(store).putBoolean(anyString(), anyBoolean());
        return store;
    }

    @Test
    void missingPreferenceEnablesBuiltInList() {
        assertTrue(new JabRefCliPreferences(isolatedStore()).getAbbreviationPreferences().shouldUseBuiltInJournalList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void savedChoiceSurvivesReload(boolean enabled) {
        Preferences store = isolatedStore();
        var preferences = new JabRefCliPreferences(store).getAbbreviationPreferences();
        preferences.setUseBuiltInJournalList(!enabled);
        preferences.setUseBuiltInJournalList(enabled);
        assertEquals(enabled, new JabRefCliPreferences(store).getAbbreviationPreferences().shouldUseBuiltInJournalList());
    }
}
