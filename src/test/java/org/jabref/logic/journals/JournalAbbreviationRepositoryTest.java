package org.jabref.logic.journals;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.undo.CompoundEdit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AllowedToUseSwing("UndoableUnabbreviator and UndoableAbbreviator requires Swing Compound Edit in order test the abbreviation and unabbreviation of journal titles")
class JournalAbbreviationRepositoryTest {

    private JournalAbbreviationRepository repository;
    private JournalAbbreviationPreferences abbreviationPreferences;
    
    private final BibDatabase bibDatabase = new BibDatabase();
    private UndoableUnabbreviator undoableUnabbreviator;
    
    private static final Abbreviation AMERICAN_JOURNAL = new Abbreviation("American Journal of Public Health", "Am. J. Public Health");
    private static final Abbreviation ACS_MATERIALS = new Abbreviation("ACS Applied Materials & Interfaces", "ACS Appl. Mater. Interfaces");
    private static final Abbreviation ANTIOXIDANTS = new Abbreviation("Antioxidants & Redox Signaling", "Antioxid. Redox Signaling");
    private static final Abbreviation PHYSICAL_REVIEW = new Abbreviation("Physical Review B", "Phys. Rev. B");

    /**
     * Creates a test repository with pre-defined abbreviations and all sources enabled
     */
    private JournalAbbreviationRepository createTestRepository() {
        JournalAbbreviationRepository testRepo = new JournalAbbreviationRepository();
        
        testRepo.addCustomAbbreviations(List.of(
            AMERICAN_JOURNAL,
            ACS_MATERIALS,
            ANTIOXIDANTS,
            PHYSICAL_REVIEW
        ), JournalAbbreviationRepository.BUILTIN_LIST_ID, true);
        
        return testRepo;
    }

    @BeforeEach
    void setUp() {
        repository = new JournalAbbreviationRepository();
        
        abbreviationPreferences = mock(JournalAbbreviationPreferences.class);
        when(abbreviationPreferences.isSourceEnabled(anyString())).thenReturn(true);
        when(abbreviationPreferences.getExternalJournalLists()).thenReturn(FXCollections.observableArrayList());
        
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
        repository = createTestRepository();
        
        Optional<Abbreviation> result = repository.get("American Journal of Public Health");
        assertTrue(result.isPresent(), "Repository should contain American Journal of Public Health");
        assertEquals(AMERICAN_JOURNAL, result.get());
    }

    @Test
    void getFromAbbreviatedName() {
        repository = createTestRepository();
        
        Optional<Abbreviation> result = repository.get("Am. J. Public Health");
        assertTrue(result.isPresent(), "Repository should find the abbreviation");
        assertEquals(AMERICAN_JOURNAL, result.get());
    }

    @Test
    void abbreviationsWithEscapedAmpersand() {
        repository = createTestRepository();
        
        Optional<Abbreviation> result1 = repository.get("ACS Applied Materials & Interfaces");
        assertTrue(result1.isPresent());
        assertEquals(ACS_MATERIALS, result1.get());
        
        Optional<Abbreviation> result2 = repository.get("ACS Applied Materials \\& Interfaces");
        assertTrue(result2.isPresent());
        assertEquals(ACS_MATERIALS, result2.get());
        
        Optional<Abbreviation> result3 = repository.get("Antioxidants & Redox Signaling");
        assertTrue(result3.isPresent());
        assertEquals(ANTIOXIDANTS, result3.get());
        
        Optional<Abbreviation> result4 = repository.get("Antioxidants \\& Redox Signaling");
        assertTrue(result4.isPresent());
        assertEquals(ANTIOXIDANTS, result4.get());

        repository.addCustomAbbreviation(new Abbreviation("Long & Name", "L. N.", "LN"));
        Optional<Abbreviation> result5 = repository.get("Long & Name");
        assertTrue(result5.isPresent());
        assertEquals(new Abbreviation("Long & Name", "L. N.", "LN"), result5.get());
        
        Optional<Abbreviation> result6 = repository.get("Long \\& Name");
        assertTrue(result6.isPresent());
        assertEquals(new Abbreviation("Long & Name", "L. N.", "LN"), result6.get());
    }

    @Test
    void journalAbbreviationWithEscapedAmpersand() {
        repository = createTestRepository();
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
        repository = createTestRepository();
        undoableUnabbreviator = new UndoableUnabbreviator(repository);
        
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article);
        abbreviatedJournalEntry.setField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedUnabbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedUnabbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void journalAbbreviateWithoutEscapedAmpersand() {
        repository = createTestRepository();
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
        repository = createTestRepository();
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
        repository = createTestRepository();
        undoableUnabbreviator = new UndoableUnabbreviator(repository);
        
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedUnabbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedUnabbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void unabbreviateWithJournalExistsAndFJournalExists() {
        repository = createTestRepository();
        undoableUnabbreviator = new UndoableUnabbreviator(repository);
        
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.JOURNAL, "ACS Appl. Mater. Interfaces")
            .withField(AMSField.FJOURNAL, "ACS Applied Materials & Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedUnabbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedUnabbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void journalDotlessAbbreviation() {
        repository = createTestRepository();
        undoableUnabbreviator = new UndoableUnabbreviator(repository);
        
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Appl Mater Interfaces");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedUnabbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedUnabbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void journalDotlessAbbreviationWithCurlyBraces() {
        repository = createTestRepository();
        undoableUnabbreviator = new UndoableUnabbreviator(repository);
        
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "{ACS Appl Mater Interfaces}");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());
        BibEntry expectedUnabbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "ACS Applied Materials & Interfaces");
        assertEquals(expectedUnabbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    /**
     * Tests <a href="https://github.com/JabRef/jabref/issues/9475">Issue 9475</a>
     */
    @Test
    void titleEmbeddedWithCurlyBracesHavingNoChangesKeepsBraces() {
        repository = createTestRepository();
        undoableUnabbreviator = new UndoableUnabbreviator(repository);
        
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.JOURNAL, "{The Visualization Handbook}");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());

        BibEntry expectedUnabbreviatedJournalEntry = new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.JOURNAL, "{The Visualization Handbook}");
        assertEquals(expectedUnabbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    /**
     * Tests <a href="https://github.com/JabRef/jabref/issues/9503">Issue 9503</a>
     */
    @Test
    void titleWithNestedCurlyBracesHavingNoChangesKeepsBraces() {
        repository = createTestRepository();
        undoableUnabbreviator = new UndoableUnabbreviator(repository);
        
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.BOOKTITLE, "2015 {IEEE} International Conference on Digital Signal Processing, {DSP} 2015, Singapore, July 21-24, 2015");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());

        BibEntry expectedUnabbreviatedJournalEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.BOOKTITLE, "2015 {IEEE} International Conference on Digital Signal Processing, {DSP} 2015, Singapore, July 21-24, 2015");
        assertEquals(expectedUnabbreviatedJournalEntry, abbreviatedJournalEntry);
    }

    @Test
    void dotlessForPhysRevB() {
        repository = createTestRepository();
        undoableUnabbreviator = new UndoableUnabbreviator(repository);
        
        BibEntry abbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "Phys Rev B");

        undoableUnabbreviator.unabbreviate(bibDatabase, abbreviatedJournalEntry, StandardField.JOURNAL, new CompoundEdit());

        BibEntry expectedUnabbreviatedJournalEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "Physical Review B");
        assertEquals(expectedUnabbreviatedJournalEntry, abbreviatedJournalEntry);
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

    @Test
    void addCustomAbbreviationsWithEnabledState() {
        String sourceKey = "test-source";
        
        repository.addCustomAbbreviations(List.of(
            new Abbreviation("Journal One", "J. One"),
            new Abbreviation("Journal Two", "J. Two")
        ), sourceKey, true);
        
        assertEquals("J. One", repository.getDefaultAbbreviation("Journal One").orElse("WRONG"));
        assertEquals("J. Two", repository.getDefaultAbbreviation("Journal Two").orElse("WRONG"));
        
        assertTrue(repository.isSourceEnabled(sourceKey));
    }
    
    @Test
    void disablingSourcePreventsAccessToAbbreviations() {
        String sourceKey = "test-source";
        
        repository.addCustomAbbreviations(List.of(
            new Abbreviation("Unique Journal", "U. J.")
        ), sourceKey, true);
        
        assertEquals("U. J.", repository.getDefaultAbbreviation("Unique Journal").orElse("WRONG"));
        
        repository.setSourceEnabled(sourceKey, false);
        
        assertFalse(repository.isSourceEnabled(sourceKey));
        
        assertEquals("WRONG", repository.getDefaultAbbreviation("Unique Journal").orElse("WRONG"));
    }
    
    @Test
    void reenablingSourceRestoresAccessToAbbreviations() {
        String sourceKey = "test-source";
        
        repository.addCustomAbbreviations(List.of(
            new Abbreviation("Disabled Journal", "D. J.")
        ), sourceKey, true);
        
        repository.setSourceEnabled(sourceKey, false);
        
        assertEquals("WRONG", repository.getDefaultAbbreviation("Disabled Journal").orElse("WRONG"));
        
        repository.setSourceEnabled(sourceKey, true);
        
        assertTrue(repository.isSourceEnabled(sourceKey));
        
        assertEquals("D. J.", repository.getDefaultAbbreviation("Disabled Journal").orElse("WRONG"));
    }
    
    @Test
    void builtInListCanBeToggled() {
        repository = createTestRepository();
        
        assertTrue(repository.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID));
        
        String journalName = "American Journal of Public Health";
        String abbreviation = "Am. J. Public Health";
        
        assertEquals(abbreviation, repository.getDefaultAbbreviation(journalName).orElse("WRONG"));
        
        repository.setSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID, false);
        
        assertFalse(repository.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID));
        
        assertEquals("WRONG", repository.getDefaultAbbreviation(journalName).orElse("WRONG"));
        
        repository.setSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID, true);
        
        assertEquals(abbreviation, repository.getDefaultAbbreviation(journalName).orElse("WRONG"));
    }
    
    /**
     * This test specifically verifies that disabling sources directly affects how abbreviations are accessed.
     * We explicitly create a new test repository and check enabled state at each step.
     */
    @Test
    void multipleSourcesCanBeToggled() {
        JournalAbbreviationRepository testRepo = new JournalAbbreviationRepository();
        
        String sourceKey1 = "source-1-special";
        String sourceKey2 = "source-2-special";
        
        testRepo.addCustomAbbreviations(List.of(
            new Abbreviation("Unique Journal Source One XYZ", "UniqueJS1")
        ), sourceKey1, true);
        
        testRepo.addCustomAbbreviations(List.of(
            new Abbreviation("Unique Journal Source Two ABC", "UniqueJS2")
        ), sourceKey2, true);
        
        // Verify initial state
        assertTrue(testRepo.isSourceEnabled(sourceKey1), "Source 1 should be enabled initially");
        assertTrue(testRepo.isSourceEnabled(sourceKey2), "Source 2 should be enabled initially");
        
        // Verify both abbreviations are accessible when sources are enabled
        assertEquals("UniqueJS1", testRepo.getDefaultAbbreviation("Unique Journal Source One XYZ").orElse("WRONG"));
        assertEquals("UniqueJS2", testRepo.getDefaultAbbreviation("Unique Journal Source Two ABC").orElse("WRONG"));
        
        // Disable first source
        testRepo.setSourceEnabled(sourceKey1, false);
        
        // Verify first source is disabled, second still enabled
        assertFalse(testRepo.isSourceEnabled(sourceKey1), "Source 1 should be disabled");
        assertTrue(testRepo.isSourceEnabled(sourceKey2), "Source 2 should remain enabled");
        
        // Verify first abbreviation is no longer accessible, second still is
        assertEquals("WRONG", testRepo.getDefaultAbbreviation("Unique Journal Source One XYZ").orElse("WRONG"));
        assertEquals("UniqueJS2", testRepo.getDefaultAbbreviation("Unique Journal Source Two ABC").orElse("WRONG"));
        
        // Disable second source
        testRepo.setSourceEnabled(sourceKey2, false);
        
        // Verify both sources are disabled
        assertFalse(testRepo.isSourceEnabled(sourceKey1), "Source 1 should remain disabled");
        assertFalse(testRepo.isSourceEnabled(sourceKey2), "Source 2 should be disabled");
        
        // Verify both abbreviations are no longer accessible
        assertEquals("WRONG", testRepo.getDefaultAbbreviation("Unique Journal Source One XYZ").orElse("WRONG"));
        assertEquals("WRONG", testRepo.getDefaultAbbreviation("Unique Journal Source Two ABC").orElse("WRONG"));
    }
    
    @Test
    void noEnabledSourcesReturnsEmptyAbbreviation() {
        JournalAbbreviationRepository testRepo = createTestRepository();
        
        assertTrue(testRepo.get("American Journal of Public Health").isPresent());
        
        testRepo.setSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID, false);
        
        assertFalse(testRepo.get("American Journal of Public Health").isPresent());
    }
    
    @Test
    void getForUnabbreviationRespectsEnabledSources() {
        JournalAbbreviationRepository testRepo = createTestRepository();
        
        // Abbreviation should be initially present for unabbreviation
        String abbreviation = "Am. J. Public Health";
        assertTrue(testRepo.isAbbreviatedName(abbreviation), "Should recognize as abbreviation");
        
        Optional<Abbreviation> result = testRepo.getForUnabbreviation(abbreviation);
        assertTrue(result.isPresent(), "Should find abbreviation when source is enabled");
        assertEquals("American Journal of Public Health", result.get().getName());
        
        // After disabling the source, it should not be available
        testRepo.setSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID, false);
        
        // Should not return for unabbreviation since source is disabled
        Optional<Abbreviation> resultAfterDisabling = testRepo.getForUnabbreviation(abbreviation);
        assertFalse(resultAfterDisabling.isPresent(), "Should not find abbreviation when source is disabled");
    }
    
    @Test
    void isAbbreviatedNameRespectsEnabledSources() {
        JournalAbbreviationRepository testRepo = createTestRepository();
        
        String abbreviation = "Am. J. Public Health";
        assertTrue(testRepo.isAbbreviatedName(abbreviation), "Should recognize as abbreviation when source is enabled");
        
        testRepo.setSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID, false);
        
        assertFalse(testRepo.isAbbreviatedName(abbreviation), "Should not recognize as abbreviation when source is disabled");
    }
    
    @Test
    void getAllAbbreviationsWithSourcesReturnsCorrectSources() {
        // Create a custom repository with known abbreviations
        JournalAbbreviationRepository testRepo = new JournalAbbreviationRepository();
        
        // Clear any existing abbreviations (clear the maps)
        testRepo.getCustomAbbreviations().clear();
        
        // Add exactly 4 abbreviations with built-in source
        testRepo.addCustomAbbreviations(List.of(
            AMERICAN_JOURNAL,
            ACS_MATERIALS,
            ANTIOXIDANTS,
            PHYSICAL_REVIEW
        ), JournalAbbreviationRepository.BUILTIN_LIST_ID, true);
        
        // Add 1 abbreviation with custom source
        String customSource = "test-custom";
        testRepo.addCustomAbbreviations(List.of(
            new Abbreviation("Custom Journal", "Cust. J.")
        ), customSource, true);
        
        List<JournalAbbreviationRepository.AbbreviationWithSource> allWithSources = testRepo.getAllAbbreviationsWithSources();
        
        // Verify we have at least the number of abbreviations we added
        assertTrue(allWithSources.size() >= 5, 
                 "Should have at least 5 abbreviations (got " + allWithSources.size() + ")");
        
        // Verify custom abbreviation has correct source
        long customCount = allWithSources.stream()
                          .filter(aws -> customSource.equals(aws.getSource()))
                          .count();
        assertEquals(1, customCount, "Should have 1 custom source abbreviation");
        
        // Verify built-in abbreviations have correct source
        long builtInCount = allWithSources.stream()
                           .filter(aws -> JournalAbbreviationRepository.BUILTIN_LIST_ID.equals(aws.getSource()))
                           .count();
        assertTrue(builtInCount >= 4, "Should have at least 4 built-in abbreviations");
        
        // Verify we can find the specific custom abbreviation
        Optional<JournalAbbreviationRepository.AbbreviationWithSource> customAbbr = allWithSources.stream()
                                                                                  .filter(aws -> customSource.equals(aws.getSource()))
                                                                                  .findFirst();
        assertTrue(customAbbr.isPresent(), "Should find custom abbreviation with source");
        assertEquals("Custom Journal", customAbbr.get().getAbbreviation().getName());
        
        // Verify our specific built-in abbreviations have the correct source
        for (Abbreviation abbr : List.of(AMERICAN_JOURNAL, ACS_MATERIALS, ANTIOXIDANTS, PHYSICAL_REVIEW)) {
            boolean found = allWithSources.stream()
                           .anyMatch(aws -> aws.getSource().equals(JournalAbbreviationRepository.BUILTIN_LIST_ID) && 
                                     aws.getAbbreviation().getName().equals(abbr.getName()));
            assertTrue(found, "Should find " + abbr.getName() + " with built-in source");
        }
    }
}
