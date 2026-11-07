package org.jabref.logic.journals_gui;

import java.util.Optional;

import org.jabref.logic.cleanup.AbbreviateCleanupFactory;
import org.jabref.logic.cleanup.AbbreviateJournalCleanup;
import org.jabref.logic.cleanup.CleanupJob;
import org.jabref.logic.cleanup.UnabbreviateJournalCleanup;
import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.journals.JournalMode;
import org.jabref.model.database.BibDatabase;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// [utest->feat~fetchers.journal-title-automatic-abbreviation~1]
@NullMarked
class AbbreviateCleanupFactoryTest {
    @Test
    void abbreviateCleanupReturnsEmptyWhenJournalModeIsNone() {
        JournalAbbreviationRepository repository = mock(JournalAbbreviationRepository.class);
        AbbreviationPreferences preferences = preferencesWithMode(JournalMode.NONE);
        Optional<CleanupJob> result = AbbreviateCleanupFactory.abbreviateCleanup(
                Optional.empty(),
                repository,
                preferences);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void abbreviateCleanupCreatesAbbreviateJobWhenJournalModeIsAbbreviate() {
        JournalAbbreviationRepository repository = mock(JournalAbbreviationRepository.class);
        AbbreviationPreferences preferences = preferencesWithMode(JournalMode.ABBREVIATE);
        Optional<CleanupJob> result = AbbreviateCleanupFactory.abbreviateCleanup(Optional.empty(), repository, preferences);
        assertTrue(result.isPresent());
        assertInstanceOf(AbbreviateJournalCleanup.class, result.orElseThrow());
    }

    @Test
    void abbreviateCleanupCreatesUnabbreviateJobWhenJournalModeIsUnabbreviate() {
        JournalAbbreviationRepository repository = mock(JournalAbbreviationRepository.class);
        AbbreviationPreferences preferences = preferencesWithMode(JournalMode.UNABBREVIATE);
        Optional<CleanupJob> result = AbbreviateCleanupFactory.abbreviateCleanup(Optional.empty(), repository, preferences);
        assertTrue(result.isPresent());
        assertInstanceOf(UnabbreviateJournalCleanup.class, result.orElseThrow());
    }

    @Test
    void abbreviateCleanupWithDatabaseAbbreviateJobWhenDatabaseIsProvided() {
        BibDatabase database = new BibDatabase();
        JournalAbbreviationRepository repository = mock(JournalAbbreviationRepository.class);
        AbbreviationPreferences preferences = preferencesWithMode(JournalMode.ABBREVIATE);
        Optional<CleanupJob> result = AbbreviateCleanupFactory.abbreviateCleanup(Optional.of(database), repository, preferences);
        assertTrue(result.isPresent());
        assertInstanceOf(AbbreviateJournalCleanup.class, result.orElseThrow());
    }

    @Test
    void abbreviateCleanupWithDatabaseUnabbreviateJobWhenDatabaseIsProvided() {
        BibDatabase database = new BibDatabase();
        JournalAbbreviationRepository repository = mock(JournalAbbreviationRepository.class);
        AbbreviationPreferences preferences = preferencesWithMode(JournalMode.UNABBREVIATE);
        Optional<CleanupJob> result = AbbreviateCleanupFactory.abbreviateCleanup(Optional.of(database), repository, preferences);
        assertTrue(result.isPresent());
        assertInstanceOf(UnabbreviateJournalCleanup.class, result.orElseThrow());
    }

    private static AbbreviationPreferences preferencesWithMode(JournalMode journalMode) {
        AbbreviationPreferences preferences = mock(AbbreviationPreferences.class);
        when(preferences.useJournalAbbreviationMode()).thenReturn(journalMode);
        when(preferences.useAbbreviationTypeOnFetch()).thenReturn(AbbreviationType.DEFAULT);
        when(preferences.shouldUseFJournalField()).thenReturn(false);
        return preferences;
    }
}
