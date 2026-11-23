package org.jabref.logic.preferences;

import java.util.HashMap;
import java.util.List;
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
    private final Map<String, String> stringValues = new HashMap<>();

    private Preferences isolatedStore() {
        Preferences store = mock(Preferences.class);
        when(store.get(anyString(), anyString())).thenAnswer(invocation ->
                stringValues.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
        doAnswer(invocation -> {
            stringValues.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(store).put(anyString(), anyString());
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

    @Test
    void missingDisabledListsPreferenceEnablesExistingExternalLists() {
        Preferences store = isolatedStore();
        new JabRefCliPreferences(store).getAbbreviationPreferences().setExternalJournalLists(List.of("external.csv"));

        var reloaded = new JabRefCliPreferences(store).getAbbreviationPreferences();
        assertEquals(List.of("external.csv"), reloaded.getExternalJournalLists());
        assertEquals(List.of(), reloaded.getDisabledExternalJournalLists());
    }

    @Test
    void disabledExternalListsSurviveReloadWithoutChangingListOrder() {
        // [utest->req~journals.disable-external~1]
        Preferences store = isolatedStore();
        var preferences = new JabRefCliPreferences(store).getAbbreviationPreferences();
        preferences.setExternalJournalLists(List.of("first.csv", "second.csv"));
        preferences.setDisabledExternalJournalLists(List.of("first.csv"));

        var reloaded = new JabRefCliPreferences(store).getAbbreviationPreferences();
        assertEquals(List.of("first.csv", "second.csv"), reloaded.getExternalJournalLists());
        assertEquals(List.of("first.csv"), reloaded.getDisabledExternalJournalLists());

        reloaded.setDisabledExternalJournalLists(List.of());
        assertEquals(List.of(), new JabRefCliPreferences(store).getAbbreviationPreferences().getDisabledExternalJournalLists());
    }
}
