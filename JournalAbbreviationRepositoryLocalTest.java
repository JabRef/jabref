package org.jabref.logic.journals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JournalAbbreviationRepositoryLocalTest {

    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JournalAbbreviationRepository();

        // Add custom test data so the test does not depend only on demo data
        repository.addCustomAbbreviation(
                new Abbreviation("Journal of Testing", "J. Test.", "JT"));
    }

    @Test
    void returnsNextAbbreviationWhenMatchExists() {
        Optional<String> result = repository.getNextAbbreviation("Journal of Testing");

        assertTrue(result.isPresent());
        assertEquals("J. Test.", result.get());
    }

    @Test
    void returnsEmptyWhenNoMatchExists() {
        Optional<String> result = repository.getNextAbbreviation("Nonexistent Journal Name");

        assertTrue(result.isEmpty());
    }

    @Test
    void throwsExceptionWhenInputIsNull() {
        assertThrows(NullPointerException.class,
                () -> repository.getNextAbbreviation(null));
    }
}