package org.jabref.logic.journals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
