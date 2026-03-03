package org.jabref.logic.journals;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbbreviationPreferencesTest {

    @Test
    void bothPreferencesAcceptSameListType() {
        List<String> externalLists = List.of("/path/to/list1.csv", "/path/to/list2.csv");

        JournalAbbreviationPreferences journalPrefs = new JournalAbbreviationPreferences(externalLists, true);
        ConferenceAbbreviationPreferences conferencePrefs = new ConferenceAbbreviationPreferences(externalLists);

        assertEquals(externalLists, journalPrefs.getExternalLists());
        assertEquals(externalLists, conferencePrefs.getExternalLists());
    }

    @Test
    void externalListsInBaseClass() {
        List<String> externalLists = List.of("/path/to/list.csv");

        AbbreviationPreferences journalPrefs = new JournalAbbreviationPreferences(externalLists, false);
        AbbreviationPreferences conferencePrefs = new ConferenceAbbreviationPreferences(externalLists);

        assertNotNull(journalPrefs.getExternalLists());
        assertNotNull(conferencePrefs.getExternalLists());
    }

    @Test
    void useFJournalFieldOnlyInJournalPreferences() {
        JournalAbbreviationPreferences journalPrefs = new JournalAbbreviationPreferences(List.of(), true);

        assertTrue(journalPrefs.shouldUseFJournalField());
    }

    @Test
    void setExternalListsWorksOnBoth() {
        JournalAbbreviationPreferences journalPrefs = new JournalAbbreviationPreferences(List.of(), true);
        ConferenceAbbreviationPreferences conferencePrefs = new ConferenceAbbreviationPreferences(List.of());

        List<String> newLists = List.of("/new/path.csv");
        journalPrefs.setExternalLists(newLists);
        conferencePrefs.setExternalLists(newLists);

        assertEquals(1, journalPrefs.getExternalLists().size());
        assertEquals(1, conferencePrefs.getExternalLists().size());
    }
}
