/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.logic.preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@ResourceLock("Preferences")
class AbbreviationPreferencesStorageTest {
    private static final String JOURNAL_LISTS = "externalJournalLists";
    private static final String CONFERENCE_LISTS = "externalConferenceLists";
    private final Map<String, Optional<String>> savedValues = new HashMap<>();

    @BeforeEach
    void savePreferences() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        for (String key : List.of(JOURNAL_LISTS, CONFERENCE_LISTS)) {
            savedValues.put(key, preferences.hasKey(key) ? Optional.of(preferences.get(key, "")) : Optional.empty());
            preferences.remove(key);
        }
    }

    @AfterEach
    void restorePreferences() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        savedValues.forEach((key, value) -> value.ifPresentOrElse(
                stored -> preferences.put(key, stored),
                () -> preferences.remove(key)));
    }

    @Test
    void readsExistingJournalListKey() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.put(JOURNAL_LISTS, JabRefCliPreferences.convertListToString(List.of("existing-journals.csv")));

        assertEquals(List.of("existing-journals.csv"), preferences.getJournalAbbreviationPreferences().getExternalLists());
        assertEquals(List.of(), preferences.getConferenceAbbreviationPreferences().getExternalLists());
    }

    @Test
    void persistsJournalAndConferenceListsIndependently() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.getJournalAbbreviationPreferences().setExternalLists(List.of("journals.csv"));
        preferences.getConferenceAbbreviationPreferences().setExternalLists(List.of("conferences.csv"));

        JabRefCliPreferences reloaded = new JabRefCliPreferences();
        assertEquals(List.of("journals.csv"), reloaded.getJournalAbbreviationPreferences().getExternalLists());
        assertEquals(List.of("conferences.csv"), reloaded.getConferenceAbbreviationPreferences().getExternalLists());
    }
}
