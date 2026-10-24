/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.logic.journals;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.conferences.ConferenceAbbreviationPreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class AbbreviationPreferencesTest {
    @Test
    void conferenceListsDoNotChangeJournalLists() {
        JournalAbbreviationPreferences journals = new JournalAbbreviationPreferences(List.of("journals.csv"), true, false);
        ConferenceAbbreviationPreferences conferences = new ConferenceAbbreviationPreferences(List.of("conferences.csv"));

        conferences.setExternalLists(List.of("other-conferences.csv"));

        assertEquals(List.of("journals.csv"), journals.getExternalLists());
        assertEquals(List.of("other-conferences.csv"), conferences.getExternalLists());
    }

    @Test
    void replacingListsPreservesObservableListForBindings() {
        ConferenceAbbreviationPreferences preferences = new ConferenceAbbreviationPreferences(List.of("old.csv"));
        var observedLists = preferences.getExternalLists();

        preferences.setExternalLists(List.of("new.csv"));

        assertSame(observedLists, preferences.getExternalLists());
        assertEquals(List.of("new.csv"), observedLists);
    }

    @Test
    void constructorCopiesExternalLists() {
        List<String> externalLists = new ArrayList<>(List.of("conferences.csv"));
        ConferenceAbbreviationPreferences preferences = new ConferenceAbbreviationPreferences(externalLists);

        externalLists.clear();

        assertEquals(List.of("conferences.csv"), preferences.getExternalLists());
    }

    @Test
    void defaultsPreserveJournalSettingsAndKeepConferenceListsEmpty() {
        JournalAbbreviationPreferences journals = JournalAbbreviationPreferences.getDefault();

        assertEquals(List.of(), journals.getExternalLists());
        assertTrue(journals.shouldUseFJournalField());
        assertFalse(journals.shouldEnableMscKeywordDescriptions());
        assertEquals(List.of(), ConferenceAbbreviationPreferences.getDefault().getExternalLists());
    }
}
