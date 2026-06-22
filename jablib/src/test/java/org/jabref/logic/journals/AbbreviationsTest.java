package org.jabref.logic.journals;

import org.jabref.logic.util.CurrentThreadTaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AbbreviationsTest {

    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        repository = JournalAbbreviationLoader.loadBuiltInRepository();
    }

    @Test
    void getNextAbbreviationAbbreviatesJournalTitle() {
        assertEquals("2D Mater.", repository.getNextAbbreviation("2D Materials").get());
    }

    @Test
    void getNextAbbreviationConvertsAbbreviationToDotlessAbbreviation() {
        assertEquals("2D Mater", repository.getNextAbbreviation("2D Mater.").get());
    }

    @Test
    void backgroundLoadedRepositoryReturnsBuiltInAbbreviations() {
        JournalAbbreviationRepository backgroundLoadedRepository = JournalAbbreviationLoader.loadRepositoryInBackground(
                new AbbreviationPreferences(java.util.List.of(), true, false),
                new CurrentThreadTaskExecutor()
        );

        assertNotNull(backgroundLoadedRepository);
        assertEquals("2D Mater.", backgroundLoadedRepository.getNextAbbreviation("2D Materials").get());
    }
}
