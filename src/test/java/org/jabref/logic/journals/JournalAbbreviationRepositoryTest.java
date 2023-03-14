package org.jabref.logic.journals;

import java.util.Set;

import javax.swing.undo.CompoundEdit;

import org.jabref.architecture.AllowedToUseSwing;
import org.jabref.gui.journals.AbbreviationType;
import org.jabref.gui.journals.UndoableAbbreviator;
import org.jabref.gui.journals.UndoableUnabbreviator;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AllowedToUseSwing("UndoableUnabbreviator and UndoableAbbreviator requires Swing Compound Edit in order test the abbreviation and unabbreviation of journal titles")
class JournalAbbreviationRepositoryTest {

    private JournalAbbreviationRepository repository;

    private BibDatabase bibDatabase = new BibDatabase();
    private UndoableUnabbreviator undoableUnabbreviator;

    @BeforeEach
    void setUp() {
        repository = JournalAbbreviationLoader.loadBuiltInRepository();
        undoableUnabbreviator = new UndoableUnabbreviator(repository);
    }

    @Test
    void empty() {
        assertTrue(repository.getCustomAbbreviations().isEmpty());
    }

    @Test
    void oneElement() {
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N."));
        assertEquals(1, repository.getCustomAbbreviations().size());

        assertEquals("L. N.", repository.getDefaultAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getDefaultAbbreviation("?").orElse("UNKNOWN"));

        assertEquals("L N", repository.getDotless("Long Name").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getDotless("?").orElse("UNKNOWN"));

        assertEquals("L. N.", repository.getShortestUniqueAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getShortestUniqueAbbreviation("?").orElse("UNKNOWN"));

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
    void oneElementWithShortestUniqueAbbreviation() {
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N.", "LN"));
        assertEquals(1, repository.getCustomAbbreviations().size());

        assertEquals("L. N.", repository.getDefaultAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getDefaultAbbreviation("?").orElse("UNKNOWN"));

        assertEquals("L N", repository.getDotless("Long Name").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getDotless("?").orElse("UNKNOWN"));

        assertEquals("LN", repository.getShortestUniqueAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getShortestUniqueAbbreviation("?").orElse("UNKNOWN"));

        assertEquals("L. N.", repository.getNextAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("L N", repository.getNextAbbreviation("L. N.").orElse("WRONG"));
        assertEquals("LN", repository.getNextAbbreviation("L N").orElse("WRONG"));
        assertEquals("Long Name", repository.getNextAbbreviation("LN").orElse("WRONG"));
        assertEquals("UNKNOWN", repository.getNextAbbreviation("?").orElse("UNKNOWN"));

        assertTrue(repository.isKnownName("Long Name"));
        assertTrue(repository.isKnownName("L. N."));
        assertTrue(repository.isKnownName("L N"));
        assertTrue(repository.isKnownName("LN"));
        assertFalse(repository.isKnownName("?"));
    }

    @Test
    void testDuplicates() {
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N."));
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N."));
        assertEquals(1, repository.getCustomAbbreviations().size());
    }

    @Test
    void testDuplicatesWithShortestUniqueAbbreviation() {
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N.", "LN"));
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N.", "LN"));
        assertEquals(1, repository.getCustomAbbreviations().size());
    }

    @Test
    void testDuplicatesIsoOnly() {
        repository.addCustomAbbreviation(new Abbreviation("Old Long Name", "L. N."));
        repository.addCustomAbbreviation(new Abbreviation("New Long Name", "L. N."));
        assertEquals(2, repository.getCustomAbbreviations().size());
    }

    @Test
    void testDuplicatesIsoOnlyWithShortestUniqueAbbreviation() {
        repository.addCustomAbbreviation(new Abbreviation("Old Long Name", "L. N.", "LN"));
        repository.addCustomAbbreviation(new Abbreviation("New Long Name", "L. N.", "LN"));
        assertEquals(2, repository.getCustomAbbreviations().size());
    }

    @Test
    void testDuplicateKeys() {
        Abbreviation abbreviationOne = new Abbreviation("Long Name", "L. N.");
        repository.addCustomAbbreviation(abbreviationOne);
        assertEquals(Set.of(abbreviationOne), repository.getCustomAbbreviations());
        assertEquals("L. N.", repository.getDefaultAbbreviation("Long Name").orElse("WRONG"));

        Abbreviation abbreviationTwo = new Abbreviation("Long Name", "LA. N.");
        repository.addCustomAbbreviation(abbreviationTwo);
        assertEquals(Set.of(abbreviationOne, abbreviationTwo), repository.getCustomAbbreviations());

        // Both abbreviations are kept in the repository
        // "L. N." is smaller than "LA. N.", therefore "L. N." is returned
        assertEquals("L. N.", repository.getDefaultAbbreviation("Long Name").orElse("WRONG"));
    }

    @Test
    void testDuplicateKeysWithShortestUniqueAbbreviation() {
        Abbreviation abbreviationOne = new Abbreviation("Long Name", "L. N.", "LN");
        repository.addCustomAbbreviation(abbreviationOne);
        assertEquals(Set.of(abbreviationOne), repository.getCustomAbbreviations());
        assertEquals("L. N.", repository.getDefaultAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("LN", repository.getShortestUniqueAbbreviation("Long Name").orElse("WRONG"));

        Abbreviation abbreviationTwo = new Abbreviation("Long Name", "LA. N.", "LAN");
        repository.addCustomAbbreviation(abbreviationTwo);
        assertEquals(Set.of(abbreviationOne, abbreviationTwo), repository.getCustomAbbreviations());

        // Both abbreviations are kept in the repository
        // "L. N." is smaller than "LA. N.", therefore "L. N." is returned
        assertEquals("L. N.", repository.getDefaultAbbreviation("Long Name").orElse("WRONG"));
        assertEquals("LN", repository.getShortestUniqueAbbreviation("Long Name").orElse("WRONG"));
    }

    @Test
    void getFromFullName() {
        assertEquals(new Abbreviation("American Journal of Public Health", "Am. J. Public Health"), repository.get("American Journal of Public Health").get());
    }

    @Test
    void getFromAbbreviatedName() {
        assertEquals(new Abbreviation("American Journal of Public Health", "Am. J. Public Health"), repository.get("Am. J. Public Health").get());
    }

    @Test
    void testAbbreviationsWithEscapedAmpersand() {
        assertEquals(new Abbreviation("ACS Applied Materials & Interfaces", "ACS Appl. Mater. Interfaces"), repository.get("ACS Applied Materials & Interfaces").get());
        assertEquals(new Abbreviation("ACS Applied Materials & Interfaces", "ACS Appl. Mater. Interfaces"), repository.get("ACS Applied Materials \\& Interfaces").get());
        assertEquals(new Abbreviation("Antioxidants & Redox Signaling", "Antioxid. Redox Signaling"), repository.get("Antioxidants & Redox Signaling").get());
        assertEquals(new Abbreviation("Antioxidants & Redox Signaling", "Antioxid. Redox Signaling"), repository.get("Antioxidants \\& Redox Signaling").get());

        repository.addCustomAbbreviation(new Abbreviation("Long & Name", "L. N.", "LN"));
        assertEquals(new Abbreviation("Long & Name", "L. N.", "LN"), repository.get("Long & Name").get());
        assertEquals(new Abbreviation("Long & Name", "L. N.", "LN"), repository.get("Long \\& Name").get());
    }

    @Test
    void testJournalAbbreviationWithEscapedAmpersand() {
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(repository, AbbreviationType.DEFAULT, false);

        BibEntry entryWithEscapedAmpersandInJournal = new BibEntry(StandardEntryType.Article);
        entryWithEscapedAmpersandInJournal.setField(StandardField.JOURNAL, "ACS Applied Materials \\& Interfaces");

        undoableAbbreviator.abbreviate(bibDatabase, entryWithEscapedAmpersandInJournal, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, entryWithEscapedAmpersandInJournal);
    }

    @Test
    void testJournalUnabbreviate() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article);
        abbreviatedJournalEntry.setField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void testJournalAbbreviateWithoutEscapedAmpersand() {
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(repository, AbbreviationType.DEFAULT, false);

        BibEntry entryWithoutEscapedAmpersandInJournal = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");

        undoableAbbreviator.abbreviate(bibDatabase, entryWithoutEscapedAmpersandInJournal, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, entryWithoutEscapedAmpersandInJournal);
    }

    @Test
    void testJournalAbbreviateWithEmptyFJournal() {
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(repository, AbbreviationType.DEFAULT, true);

        BibEntry entryWithoutEscapedAmpersandInJournal = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces")
            .withField(AMSField.FJOURNAL, "&nbsp;");

        undoableAbbreviator.abbreviate(bibDatabase, entryWithoutEscapedAmpersandInJournal, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces")
                .withField(AMSField.FJOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, entryWithoutEscapedAmpersandInJournal);
    }

    @Test
    void testUnabbreviateWithJournalExistsAndFJournalNot() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void testUnabbreviateWithJournalExistsAndFJournalExists() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces")
            .withField(AMSField.FJOURNAL, "ACS Applied Materials & Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void testJournalDotlessAbbreviation() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl Mater Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void testJournalDotlessAbbreviationWithCurlyBraces() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "{ACS Appl Mater Interfaces}");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    /**
     * Tests <a href="https://github.com/JabRef/jabref/issues/9475">Issue 9475</a>
     */
    @Test
    void testTitleEmbeddedWithCurlyBracesHavingNoChangesKeepsBraces() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.JOURNAL, "{The Visualization Handbook}");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());

        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.JOURNAL, "{The Visualization Handbook}");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    /**
     * Tests <a href="https://github.com/JabRef/jabref/issues/9503">Issue 9503</a>
     */
    @Test
    void testTitleWithNestedCurlyBracesHavingNoChangesKeepsBraces() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.BOOKTITLE, "2015 {IEEE} International Conference on Digital Signal Processing, {DSP} 2015, Singapore, July 21-24, 2015");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());

        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.BOOKTITLE, "2015 {IEEE} International Conference on Digital Signal Processing, {DSP} 2015, Singapore, July 21-24, 2015");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void testDotlessForPhysRevB() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "Phys Rev B");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());

        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "Physical Review B");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }
}
