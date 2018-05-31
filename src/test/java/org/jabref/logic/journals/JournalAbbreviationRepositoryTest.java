package org.jabref.logic.journals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JournalAbbreviationRepositoryTest {

    @Test
    public void empty() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        assertTrue(repository.getAbbreviations().isEmpty());
    }

    @Test
    public void oneElement() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        repository.addEntry(new Abbreviation("Long Name", "L. N."));
        assertEquals(1, repository.size());
        assertFalse(repository.getAbbreviations().isEmpty());

        assertEquals("L. N.", repository.getIsoAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getIsoAbbreviation("?").orElse("UNKNOWN"));

        assertEquals("L N", repository.getMedlineAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getMedlineAbbreviation("?").orElse("UNKNOWN"));

        assertEquals("L. N.", repository.getNextAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("L N", repository.getNextAbbreviation("L. N.").orElse("WRONG"));
        assertEquals("Long Name", repository.getNextAbbreviation("L N").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getNextAbbreviation("?").orElse("UNKNOWN"));

        assertTrue(repository.isKnownName("Long Name"));
        assertTrue(repository.isKnownName("L. N."));
        assertTrue(repository.isKnownName("L N"));
        assertFalse(repository.isKnownName("?"));

    }

    @Test
    public void testDuplicates() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        repository.addEntry(new Abbreviation("Long Name", "L. N."));
        repository.addEntry(new Abbreviation("Long Name", "L. N."));
        assertEquals(1, repository.size());
    }

    @Test
    public void testDuplicatesIsoOnly() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        repository.addEntry(new Abbreviation("Old Long Name", "L. N."));
        repository.addEntry(new Abbreviation("New Long Name", "L. N."));
        assertEquals(2, repository.size());
    }

    @Test
    public void testDuplicateKeys() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        repository.addEntry(new Abbreviation("Long Name", "L. N."));
        assertEquals(1, repository.size());
        assertEquals("L. N.", repository.getIsoAbbreviation("Long Name").orElse("WRONG"));

        repository.addEntry(new Abbreviation("Long Name", "LA. N."));
        assertEquals(1, repository.size());
        assertEquals("LA. N.", repository.getIsoAbbreviation("Long Name").orElse("WRONG"));

    }

}
