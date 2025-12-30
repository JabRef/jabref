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
        assertEquals("2D Mater.", repository.getNextAbbreviation("2D Materials").get());
    }

    @Test
    void getNextAbbreviationConvertsAbbreviationToDotlessAbbreviation() {
        assertEquals("2D Mater", repository.getNextAbbreviation("2D Mater.").get());
    }

    @Test
    void getName() {
    }

    @Test
    void getAbbreviation() {
    }

    @Test
    void getShortestUniqueAbbreviation() {
    }

    @Test
    void isDefaultShortestUniqueAbbreviation() {
    }

    @Test
    void getDotlessAbbreviation() {
    }

    @Test
    void compareTo() {
    }

    @Test
    void getNext() {
    }

    @Test
    void testToString() {
    }

    @Test
    void testEquals() {
    }

    @Test
    void testHashCode() {
    }
}
