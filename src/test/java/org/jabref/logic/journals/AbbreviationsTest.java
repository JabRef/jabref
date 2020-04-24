package org.jabref.logic.journals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbbreviationsTest {

    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        repository = JournalAbbreviationLoader.loadBuiltInRepository();
    }

    @Test
    void getNextAbbreviationAbbreviatesJournalTitle() {
        assertEquals("2D Mater.",
                repository.getNextAbbreviation("2D Materials").get());
    }

    @Test
    void getNextAbbreviationExpandsAbbreviation() {
        assertEquals("2D Materials",
                repository.getNextAbbreviation("2D Mater.").get());
    }
}
