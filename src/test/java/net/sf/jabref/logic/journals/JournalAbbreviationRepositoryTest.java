package net.sf.jabref.logic.journals;

import net.sf.jabref.Globals;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

public class JournalAbbreviationRepositoryTest {

    @Test
    public void empty() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        assertEquals(0, repository.size());
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

        assertEquals(String.format("Long Name = L. N.%n"), repository.toPropertiesString());
    }

    @Test
    public void testSorting() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        repository.addEntry(new Abbreviation("Long Name", "L. N."));
        repository.addEntry(new Abbreviation("A Long Name", "AL. N."));
        assertEquals("A Long Name", repository.getAbbreviations().first().getName());
        assertEquals("Long Name", repository.getAbbreviations().last().getName());
    }

    @Test
    public void testDuplicates() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        repository.addEntry(new Abbreviation("Long Name", "L. N."));
        repository.addEntry(new Abbreviation("Long Name", "L. N."));
        assertEquals(1, repository.size());
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

        assertEquals("Long Name = LA. N.", repository.getAbbreviations().first().toPropertiesLine());
    }

    @Test
    public void testParsing() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        repository.readJournalListFromResource(Globals.JOURNALS_FILE_BUILTIN);
        repository.readJournalListFromResource(Globals.JOURNALS_IEEE_INTERNAL_LIST);
        assertTrue(!repository.getAbbreviations().isEmpty());
    }

}