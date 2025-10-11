package org.jabref.logic.journals_gui;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.undo.CompoundEdit;

import org.jabref.architecture.AllowedToUseSwing;
import org.jabref.gui.journals.AbbreviationType;
import org.jabref.gui.journals.UndoableAbbreviator;
import org.jabref.gui.journals.UndoableUnabbreviator;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AllowedToUseSwing("UndoableUnabbreviator and UndoableAbbreviator requires Swing Compound Edit in order test the abbreviation and unabbreviation of journal titles")
class JournalAbbreviationRepositoryTest {

    private JournalAbbreviationRepository repository;

    private final BibDatabase bibDatabase = new BibDatabase();
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
    void duplicates() {
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N."));
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N."));
        assertEquals(1, repository.getCustomAbbreviations().size());
    }

    @Test
    void duplicatesWithShortestUniqueAbbreviation() {
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N.", "LN"));
        repository.addCustomAbbreviation(new Abbreviation("Long Name", "L. N.", "LN"));
        assertEquals(1, repository.getCustomAbbreviations().size());
    }

    @Test
    void duplicatesIsoOnly() {
        repository.addCustomAbbreviation(new Abbreviation("Old Long Name", "L. N."));
        repository.addCustomAbbreviation(new Abbreviation("New Long Name", "L. N."));
        assertEquals(2, repository.getCustomAbbreviations().size());
    }

    @Test
    void duplicatesIsoOnlyWithShortestUniqueAbbreviation() {
        repository.addCustomAbbreviation(new Abbreviation("Old Long Name", "L. N.", "LN"));
        repository.addCustomAbbreviation(new Abbreviation("New Long Name", "L. N.", "LN"));
        assertEquals(2, repository.getCustomAbbreviations().size());
    }

    @Test
    void duplicateKeys() {
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
    void duplicateKeysWithShortestUniqueAbbreviation() {
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
    void abbreviationsWithEscapedAmpersand() {
        assertEquals(new Abbreviation("ACS Applied Materials & Interfaces", "ACS Appl. Mater. Interfaces"), repository.get("ACS Applied Materials & Interfaces").get());
        assertEquals(new Abbreviation("ACS Applied Materials & Interfaces", "ACS Appl. Mater. Interfaces"), repository.get("ACS Applied Materials \\& Interfaces").get());
        assertEquals(new Abbreviation("Antioxidants & Redox Signaling", "Antioxid. Redox Signaling"), repository.get("Antioxidants & Redox Signaling").get());
        assertEquals(new Abbreviation("Antioxidants & Redox Signaling", "Antioxid. Redox Signaling"), repository.get("Antioxidants \\& Redox Signaling").get());

        repository.addCustomAbbreviation(new Abbreviation("Long & Name", "L. N.", "LN"));
        assertEquals(new Abbreviation("Long & Name", "L. N.", "LN"), repository.get("Long & Name").get());
        assertEquals(new Abbreviation("Long & Name", "L. N.", "LN"), repository.get("Long \\& Name").get());
    }

    @Test
    void journalAbbreviationWithEscapedAmpersand() {
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(repository, AbbreviationType.DEFAULT, false);

        BibEntry entryWithEscapedAmpersandInJournal = new BibEntry(StandardEntryType.Article);
        entryWithEscapedAmpersandInJournal.setField(StandardField.JOURNAL, "ACS Applied Materials \\& Interfaces");

        undoableAbbreviator.abbreviate(bibDatabase, entryWithEscapedAmpersandInJournal, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, entryWithEscapedAmpersandInJournal);
    }

    @Test
    void journalUnabbreviate() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article);
        abbreviatedJournalEntry.setField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void journalAbbreviateWithoutEscapedAmpersand() {
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(repository, AbbreviationType.DEFAULT, false);

        BibEntry entryWithoutEscapedAmpersandInJournal = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");

        undoableAbbreviator.abbreviate(bibDatabase, entryWithoutEscapedAmpersandInJournal, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, entryWithoutEscapedAmpersandInJournal);
    }

    @Test
    void journalAbbreviateWithEmptyFJournal() {
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
    void unabbreviateWithJournalExistsAndFJournalNot() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void unabbreviateWithJournalExistsAndFJournalExists() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces")
                .withField(AMSField.FJOURNAL, "ACS Applied Materials & Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void journalDotlessAbbreviation() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl Mater Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void journalDotlessAbbreviationWithCurlyBraces() {
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
    void titleEmbeddedWithCurlyBracesHavingNoChangesKeepsBraces() {
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
    void titleWithNestedCurlyBracesHavingNoChangesKeepsBraces() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.BOOKTITLE, "2015 {IEEE} International Conference on Digital Signal Processing, {DSP} 2015, Singapore, July 21-24, 2015");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());

        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.BOOKTITLE, "2015 {IEEE} International Conference on Digital Signal Processing, {DSP} 2015, Singapore, July 21-24, 2015");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void dotlessForPhysRevB() {
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "Phys Rev B");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());

        BibEntry expectedAbbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "Physical Review B");
        assertEquals(expectedAbbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @ParameterizedTest
    @MethodSource("provideAbbreviationTestCases")
    void fuzzyMatch(List<Abbreviation> abbreviationList, String input, String expectedAbbreviation, String expectedDotless, String expectedShortest, String ambiguousInput) {
        repository.addCustomAbbreviations(abbreviationList);

        assertEquals(expectedAbbreviation, repository.getDefaultAbbreviation(input).orElse("WRONG"));

        assertEquals(expectedDotless, repository.getDotless(input).orElse("WRONG"));

        assertEquals(expectedShortest, repository.getShortestUniqueAbbreviation(input).orElse("WRONG"));

        assertTrue(repository.getDefaultAbbreviation(ambiguousInput).isEmpty());
    }

    static Stream<Arguments> provideAbbreviationTestCases() {
        return Stream.of(
                Arguments.of(
                        List.of(
                                new Abbreviation("Journal of Physics A", "J. Phys. A", "JPA"),
                                new Abbreviation("Journal of Physics B", "J. Phys. B", "JPB"),
                                new Abbreviation("Journal of Physics C", "J. Phys. C", "JPC")
                        ),
                        "ournal f hysics A",
                        "J. Phys. A",
                        "J Phys A",
                        "JPA",
                        "Journal of Physics"
                ),
                Arguments.of(
                        List.of(
                                new Abbreviation("中国物理学报", "物理学报", "ZWP"),
                                new Abbreviation("中国物理学理", "物理学报报", "ZWP"),
                                new Abbreviation("中国科学: 物理学", "中科物理", "ZKP")
                        ),
                        "国物理学报",
                        "物理学报",
                        "物理学报",
                        "ZWP",
                        "中国物理学"
                ),
                Arguments.of(
                        List.of(
                                new Abbreviation("Zeitschrift für Chem", "Z. Phys. Chem.", "ZPC"),
                                new Abbreviation("Zeitschrift für Chys", "Z. Angew. Chem.", "ZAC")
                        ),
                        "eitschrift ür Chem",
                        "Z. Phys. Chem.",
                        "Z Phys Chem",
                        "ZPC",
                        "Zeitschrift für C"
                )
        );
    }
}
