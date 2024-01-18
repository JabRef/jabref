package org.jabref.logic.database;

import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DuplicateCheckTest {

    private BibEntry simpleArticle;
    private BibEntry unrelatedArticle;
    private BibEntry simpleInbook;
    private BibEntry simpleIncollection;
    private DuplicateCheck duplicateChecker;

    private static BibEntry getSimpleArticle() {
        return new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Single Author")
                .withField(StandardField.TITLE, "A serious paper about something")
                .withField(StandardField.YEAR, "2017");
    }

    private static BibEntry getSimpleIncollection() {
        return new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.TITLE, "Innovation and Intellectual Property Rights")
                .withField(StandardField.AUTHOR, "Ove Grandstrand")
                .withField(StandardField.BOOKTITLE, "The Oxford Handbook of Innovation")
                .withField(StandardField.PUBLISHER, "Oxford University Press")
                .withField(StandardField.YEAR, "2004");
    }

    private static BibEntry getSimpleInbook() {
        return new BibEntry(StandardEntryType.InBook)
                .withField(StandardField.TITLE, "Alice in Wonderland")
                .withField(StandardField.AUTHOR, "Charles Lutwidge Dodgson")
                .withField(StandardField.CHAPTER, "Chapter One – Down the Rabbit Hole")
                .withField(StandardField.LANGUAGE, "English")
                .withField(StandardField.PUBLISHER, "Macmillan")
                .withField(StandardField.YEAR, "1865");
    }

    private static BibEntry getUnrelatedArticle() {
        return new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Completely Different")
                .withField(StandardField.TITLE, "Holy Moly Uffdada und Trallalla")
                .withField(StandardField.YEAR, "1992");
    }

    @BeforeEach
    public void setUp() {
        simpleArticle = getSimpleArticle();
        unrelatedArticle = getUnrelatedArticle();
        simpleInbook = getSimpleInbook();
        simpleIncollection = getSimpleIncollection();
        duplicateChecker = new DuplicateCheck(new BibEntryTypesManager());
    }

    @Test
    public void testDuplicateDetectionWithSameAuthor() {
        BibEntry one = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Billy Bob");
        BibEntry two = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Billy Bob");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionWithSameAuthorAndUmlauts() {
        BibEntry one = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Billy Bobä");
        BibEntry two = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Bill{\\\"{a}} Bob{\\\"{a}}");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionWithDifferentAuthors() {
        BibEntry one = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Billy Bob");
        BibEntry two = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "James Joyce");

        assertFalse(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionWithDifferentTypes() {
        BibEntry one = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Billy Bob");
        BibEntry two = new BibEntry(StandardEntryType.Book).withField(StandardField.AUTHOR, "Billy Bob");

        assertFalse(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionWithSameYearTitleJournal() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
        assertEquals(1.01, DuplicateCheck.compareEntriesStrictly(one, two), 0.01);
    }

    @Test
    public void testDuplicateDetectionWithDifferentJournal() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "B");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
        assertEquals(0.75, DuplicateCheck.compareEntriesStrictly(one, two), 0.01);
    }

    @Test
    public void testDuplicateDetectionWithDifferentVolume() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.VOLUME, "21");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.VOLUME, "22");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionWithDifferentTitleSameVolume() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.VOLUME, "21");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "Another title")
                .withField(StandardField.JOURNAL, "")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.VOLUME, "21");

        assertFalse(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionWithSamePages() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A")
                .withField(StandardField.VOLUME, "21")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A")
                .withField(StandardField.VOLUME, "21")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionWithSamePagesOneEntryNoVolume() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A")
                .withField(StandardField.VOLUME, "21")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionDifferentVolumeNoJournal() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.VOLUME, "21")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.VOLUME, "22")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionDifferentTitleNoJournal() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.VOLUME, "21")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "Another title")
                .withField(StandardField.VOLUME, "22")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        assertFalse(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionDifferentVolumeAllOthersEqual() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A")
                .withField(StandardField.VOLUME, "21")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A")
                .withField(StandardField.VOLUME, "22")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testDuplicateDetectionDifferentVolumeDifferentJournalAllOthersEqual() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "A")
                .withField(StandardField.VOLUME, "21")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Billy Bob")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.TITLE, "A title")
                .withField(StandardField.JOURNAL, "B")
                .withField(StandardField.VOLUME, "22")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "334--337");

        assertTrue(duplicateChecker.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testWordCorrelation() {
        String d1 = "Characterization of Calanus finmarchicus habitat in the North Sea";
        String d2 = "Characterization of Calunus finmarchicus habitat in the North Sea";
        String d3 = "Characterization of Calanus glacialissss habitat in the South Sea";

        assertEquals(1.0, (DuplicateCheck.correlateByWords(d1, d2)), 0.01);
        assertEquals(0.78, (DuplicateCheck.correlateByWords(d1, d3)), 0.01);
        assertEquals(0.78, (DuplicateCheck.correlateByWords(d2, d3)), 0.01);
    }

    @Test
    public void twoUnrelatedEntriesAreNoDuplicates() {
        assertFalse(duplicateChecker.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithDifferentDoisAreNoDuplicates() {
        simpleArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.002");
        unrelatedArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.00X");

        assertFalse(duplicateChecker.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithEqualDoisAreDuplicates() {
        simpleArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.002");
        unrelatedArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.002");

        assertTrue(duplicateChecker.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithEqualPmidAreDuplicates() {
        simpleArticle.setField(StandardField.PMID, "12345678");
        unrelatedArticle.setField(StandardField.PMID, "12345678");

        assertTrue(duplicateChecker.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithEqualEprintAreDuplicates() {
        simpleArticle.setField(StandardField.EPRINT, "12345678");
        unrelatedArticle.setField(StandardField.EPRINT, "12345678");

        assertTrue(duplicateChecker.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoEntriesWithSameDoiButDifferentTypesAreDuplicates() {
        simpleArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.002");
        BibEntry duplicateWithDifferentType = (BibEntry) simpleArticle.clone();
        duplicateWithDifferentType.setType(StandardEntryType.InCollection);

        assertTrue(duplicateChecker.isDuplicate(simpleArticle, duplicateWithDifferentType, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoEntriesWithDoiContainingUnderscoresAreNotEqual() {
        simpleArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.002");
        // An underscore in a DOI can indicate a totally different DOI
        unrelatedArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.0_02");
        BibEntry duplicateWithDifferentType = unrelatedArticle;
        duplicateWithDifferentType.setType(StandardEntryType.InCollection);

        assertFalse(duplicateChecker.isDuplicate(simpleArticle, duplicateWithDifferentType, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoEntriesWithSameISBNButDifferentTypesAreDuplicates() {
        simpleArticle.setField(StandardField.ISBN, "0-123456-47-9");
        unrelatedArticle.setField(StandardField.ISBN, "0-123456-47-9");
        BibEntry duplicateWithDifferentType = unrelatedArticle;
        duplicateWithDifferentType.setType(StandardEntryType.InCollection);

        assertTrue(duplicateChecker.isDuplicate(simpleArticle, duplicateWithDifferentType, BibDatabaseMode.BIBTEX));
    }

    public static Stream<Arguments> twoEntriesWithDifferentSpecificFieldsAreNotDuplicates() {
        return Stream.of(
                // twoInbooksWithDifferentChaptersAreNotDuplicates
                Arguments.of(getSimpleInbook(), StandardField.CHAPTER,
                        "Chapter One – Down the Rabbit Hole",
                        "Chapter Two – The Pool of Tears"),
                // twoInbooksWithDifferentPagesAreNotDuplicates
                Arguments.of(getSimpleInbook(), StandardField.PAGES, "1-20", "21-40"),
                // twoIncollectionsWithDifferentChaptersAreNotDuplicates
                Arguments.of(getSimpleIncollection(), StandardField.CHAPTER, "10", "9"),
                // twoEntriesWithDifferentSpecificFieldsAreNotDuplicates
                Arguments.of(getSimpleIncollection(), StandardField.PAGES, "1-20", "21-40")
        );
    }

    @ParameterizedTest
    @MethodSource
    private void twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(final BibEntry cloneable,
                                                                       final Field field,
                                                                       final String firstValue,
                                                                       final String secondValue) {
        final BibEntry entry1 = ((BibEntry) cloneable.clone()).withField(field, firstValue);
        final BibEntry entry2 = ((BibEntry) cloneable.clone()).withField(field, secondValue);
        assertFalse(duplicateChecker.isDuplicate(entry1, entry2, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void inbookWithoutChapterCouldBeDuplicateOfInbookWithChapter() {
        final BibEntry inbook2 = ((BibEntry) simpleInbook.clone()).withField(StandardField.CHAPTER, "");

        assertTrue(duplicateChecker.isDuplicate(simpleInbook, inbook2, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoBooksWithDifferentEditionsAreNotDuplicates() {
        BibEntry editionOne = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.PUBLISHER, "Prentice Hall")
                .withField(StandardField.DATE, "2001")
                .withField(StandardField.EDITION, "1");

        BibEntry editionTwo = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.PUBLISHER, "Prentice Hall")
                .withField(StandardField.DATE, "2008")
                .withField(StandardField.EDITION, "2");

        assertFalse(duplicateChecker.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void sameBooksWithMissingEditionAreDuplicates() {
        BibEntry editionOne = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.PUBLISHER, "Prentice Hall")
                .withField(StandardField.DATE, "2001");

        BibEntry editionTwo = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.PUBLISHER, "Prentice Hall")
                .withField(StandardField.DATE, "2008");

        assertTrue(duplicateChecker.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void sameBooksWithPartiallyMissingEditionAreDuplicates() {
        BibEntry editionOne = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.PUBLISHER, "Prentice Hall")
                .withField(StandardField.DATE, "2001");

        BibEntry editionTwo = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.PUBLISHER, "Prentice Hall")
                .withField(StandardField.DATE, "2008")
                .withField(StandardField.EDITION, "2");

        assertTrue(duplicateChecker.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void sameBooksWithDifferentEditionsAreNotDuplicates() {
        BibEntry editionTwo = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Sutton17reinfLrnIntroBook")
                .withField(StandardField.TITLE, "Reinforcement learning:An introduction")
                .withField(StandardField.PUBLISHER, "MIT Press")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.AUTHOR, "Sutton, Richard S and Barto, Andrew G")
                .withField(StandardField.ADDRESS, "Cambridge, MA.USA")
                .withField(StandardField.EDITION, "Second")
                .withField(StandardField.JOURNAL, "MIT Press")
                .withField(StandardField.URL, "https://webdocs.cs.ualberta.ca/~sutton/book/the-book-2nd.html");

        BibEntry editionOne = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Sutton98reinfLrnIntroBook")
                .withField(StandardField.TITLE, "Reinforcement learning: An introduction")
                .withField(StandardField.PUBLISHER, "MIT press Cambridge")
                .withField(StandardField.YEAR, "1998")
                .withField(StandardField.AUTHOR, "Sutton, Richard S and Barto, Andrew G")
                .withField(StandardField.VOLUME, "1")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.EDITION, "First");

        assertFalse(duplicateChecker.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    void compareOfTwoEntriesWithSameContentAndLfEndingsReportsNoDifferences() throws Exception {
        BibEntry entryOne = new BibEntry().withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        BibEntry entryTwo = new BibEntry().withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        assertTrue(duplicateChecker.isDuplicate(entryOne, entryTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    void compareOfTwoEntriesWithSameContentAndCrLfEndingsReportsNoDifferences() throws Exception {
        BibEntry entryOne = new BibEntry().withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        BibEntry entryTwo = new BibEntry().withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        assertTrue(duplicateChecker.isDuplicate(entryOne, entryTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    void compareOfTwoEntriesWithSameContentAndMixedLineEndingsReportsNoDifferences() throws Exception {
        BibEntry entryOne = new BibEntry().withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        BibEntry entryTwo = new BibEntry().withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        assertTrue(duplicateChecker.isDuplicate(entryOne, entryTwo, BibDatabaseMode.BIBTEX));
    }

    @Disabled("Book entries can have the same ISBN due to different chapters. The Test fails as crossref identifies both entries as the same.")
    @Test
    void differentArticlesFromTheSameBookAreNotDuplicates() {
        BibEntry entryOne = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Atkinson_1993")
                .withField(StandardField.AUTHOR, "Richard Atkinson")
                .withField(StandardField.CHAPTER, "11")
                .withField(StandardField.PAGES, "91-100")
                .withField(StandardField.TITLE, "Performance on a Signal")
                .withField(StandardField.BOOKTITLE, "ABC")
                .withField(StandardField.EDITOR, "ABC")
                .withField(StandardField.PUBLISHER, "ABC")
                .withField(StandardField.ISBN, "978-1-4684-8585-1")
                .withField(StandardField.YEAR, "1993");

        BibEntry entryTwo = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Ballard_1993")
                .withField(StandardField.AUTHOR, "Elizabeth Ballard")
                .withField(StandardField.CHAPTER, "20")
                .withField(StandardField.PAGES, "187-203")
                .withField(StandardField.TITLE, "Rest in Treatment")
                .withField(StandardField.BOOKTITLE, "ABC")
                .withField(StandardField.EDITOR, "ABC")
                .withField(StandardField.PUBLISHER, "ABC")
                .withField(StandardField.ISBN, "978-1-4684-8585-1")
                .withField(StandardField.YEAR, "1993");

        assertFalse(duplicateChecker.isDuplicate(entryOne, entryTwo, BibDatabaseMode.BIBTEX));
    }
}
