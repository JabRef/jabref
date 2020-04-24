package org.jabref.logic.journals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbbreviationsTest {

    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JournalAbbreviationRepository();
    }

    @Test
    void getNextAbbreviationAbbreviatesJournalTitle() {
        assertEquals("Proc. IEEE",
                repository.getNextAbbreviation("Proceedings of the IEEE").get());
    }

    @Test
    void getNextAbbreviationRemovesPoint() {
        assertEquals("Proc IEEE",
                repository.getNextAbbreviation("Proc. IEEE").get());
    }

    @Test
    void getNextAbbreviationExpandsAbbreviation() {
        assertEquals("Proceedings of the IEEE",
                repository.getNextAbbreviation("Proc IEEE").get());
    }
}
