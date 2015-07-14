package net.sf.jabref.journals.logic;

import net.sf.jabref.Globals;
import net.sf.jabref.journals.logic.Abbreviation;
import net.sf.jabref.journals.logic.JournalAbbreviationRepository;
import static org.junit.Assert.*;
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

        assertEquals("L. N.", repository.getIsoAbbreviation("Long Name").or("WRONG"));
        assertEquals("UNKNOWN", repository.getIsoAbbreviation("?").or("UNKNOWN"));

        assertEquals("L N", repository.getMedlineAbbreviation("Long Name").or("WRONG"));
        assertEquals("UNKNOWN", repository.getMedlineAbbreviation("?").or("UNKNOWN"));

        assertEquals("L. N.", repository.getNextAbbreviation("Long Name").or("WRONG"));
        assertEquals("L N", repository.getNextAbbreviation("L. N.").or("WRONG"));
        assertEquals("Long Name", repository.getNextAbbreviation("L N").or("WRONG"));
        assertEquals("UNKNOWN", repository.getNextAbbreviation("?").or("UNKNOWN"));

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
        assertEquals("L. N.", repository.getIsoAbbreviation("Long Name").or("WRONG"));

        repository.addEntry(new Abbreviation("Long Name", "LA. N."));
        assertEquals(1, repository.size());
        assertEquals("LA. N.", repository.getIsoAbbreviation("Long Name").or("WRONG"));

        assertEquals("Long Name = LA. N.", repository.getAbbreviations().first().toPropertiesLine());
    }

    @Test //@Ignore(value = "only used for checking the parse logic")
    public void testParsing() {
        JournalAbbreviationRepository repository = new JournalAbbreviationRepository();
        repository.readJournalListFromResource(Globals.JOURNALS_FILE_BUILTIN);
        //repository.readJournalListFromResource(Globals.JOURNALS_IEEE_INTERNAL_LIST);
        System.out.println(repository.toPropertiesString());
    }

}