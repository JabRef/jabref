package org.jabref.logic.preferences;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Preferences")
class BuiltInJournalListPreferencesTest {
    private static final String KEY = "useBuiltInJournalList";
    private Optional<String> savedValue = Optional.empty();

    @BeforeEach
    void savePreference() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        savedValue = preferences.hasKey(KEY) ? Optional.of(preferences.get(KEY, "")) : Optional.empty();
        preferences.remove(KEY);
    }

    @AfterEach
    void restorePreference() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        savedValue.ifPresentOrElse(value -> preferences.put(KEY, value), () -> preferences.remove(KEY));
    }

    @Test
    void missingPreferenceEnablesBuiltInList() {
        assertTrue(new JabRefCliPreferences().getAbbreviationPreferences().shouldUseBuiltInJournalList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void savedChoiceSurvivesReload(boolean enabled) {
        new JabRefCliPreferences().getAbbreviationPreferences().setUseBuiltInJournalList(enabled);
        assertEquals(enabled, new JabRefCliPreferences().getAbbreviationPreferences().shouldUseBuiltInJournalList());
    }
}
