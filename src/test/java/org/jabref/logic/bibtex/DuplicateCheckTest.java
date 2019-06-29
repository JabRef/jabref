package org.jabref.logic.bibtex;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DuplicateCheckTest {

    private BibEntry simpleArticle;
    private BibEntry unrelatedArticle;
    private BibEntry simpleInbook;
    private BibEntry simpleIncollection;

    @BeforeEach
    public void setUp() {
        simpleArticle = new BibEntry(BibtexEntryTypes.ARTICLE)
                .withField(StandardField.AUTHOR, "Single Author")
                .withField(StandardField.TITLE, "A serious paper about something")
                .withField(StandardField.YEAR, "2017");
        unrelatedArticle = new BibEntry(BibtexEntryTypes.ARTICLE)
                .withField(StandardField.AUTHOR, "Completely Different")
                .withField(StandardField.TITLE, "Holy Moly Uffdada und Trallalla")
                .withField(StandardField.YEAR, "1992");
        simpleInbook = new BibEntry(BibtexEntryTypes.INBOOK)
                .withField(StandardField.TITLE, "Alice in Wonderland")
                .withField(StandardField.AUTHOR, "Charles Lutwidge Dodgson")
                .withField(StandardField.CHAPTER, "Chapter One – Down the Rabbit Hole")
                .withField(StandardField.LANGUAGE, "English")
                .withField(StandardField.PUBLISHER, "Macmillan")
                .withField(StandardField.YEAR, "1865");
        simpleIncollection = new BibEntry(BibtexEntryTypes.INCOLLECTION)
                .withField(StandardField.TITLE, "Innovation and Intellectual Property Rights")
                .withField(StandardField.AUTHOR, "Ove Grandstrand")
                .withField(StandardField.BOOKTITLE, "The Oxford Handbook of Innovation")
                .withField(StandardField.PUBLISHER, "Oxford University Press")
                .withField(StandardField.YEAR, "2004");
    }

    @Test
    public void testDuplicateDetection() {
        BibEntry one = new BibEntry(BibtexEntryTypes.ARTICLE);

        BibEntry two = new BibEntry(BibtexEntryTypes.ARTICLE);

        one.setField("author", "Billy Bob");
        two.setField("author", "Billy Bob");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("author", "James Joyce");
        assertFalse(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("author", "Billy Bob");
        two.setType(BibtexEntryTypes.BOOK);
        assertFalse(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setType(BibtexEntryTypes.ARTICLE);
        one.setField("year", "2005");
        two.setField("year", "2005");
        one.setField("title", "A title");
        two.setField("title", "A title");
        one.setField("journal", "A");
        two.setField("journal", "A");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
        assertEquals(1.01, DuplicateCheck.compareEntriesStrictly(one, two), 0.01);

        two.setField("journal", "B");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
        assertEquals(0.75, DuplicateCheck.compareEntriesStrictly(one, two), 0.01);

        two.setField("journal", "A");
        one.setField("number", "1");
        two.setField("volume", "21");
        one.setField("pages", "334--337");
        two.setField("pages", "334--337");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("number", "1");
        one.setField("volume", "21");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("volume", "22");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("journal", "B");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        one.setField("journal", "");
        two.setField("journal", "");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("title", "Another title");
        assertFalse(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
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
        assertFalse(DuplicateCheck.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithDifferentDoisAreNoDuplicates() {
        simpleArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.002");
        unrelatedArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.00X");

        assertFalse(DuplicateCheck.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithEqualDoisAreDuplicates() {
        simpleArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.002");
        unrelatedArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.002");

        assertTrue(DuplicateCheck.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithEqualPmidAreDuplicates() {
        simpleArticle.setField(StandardField.PMID, "12345678");
        unrelatedArticle.setField(StandardField.PMID, "12345678");

        assertTrue(DuplicateCheck.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithEqualEprintAreDuplicates() {
        simpleArticle.setField(StandardField.EPRINT, "12345678");
        unrelatedArticle.setField(StandardField.EPRINT, "12345678");

        assertTrue(DuplicateCheck.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoEntriesWithSameDoiButDifferentTypesAreDuplicates() {
        simpleArticle.setField(StandardField.DOI, "10.1016/j.is.2004.02.002");
        BibEntry duplicateWithDifferentType = (BibEntry) simpleArticle.clone();
        duplicateWithDifferentType.setType(BibtexEntryTypes.INCOLLECTION);

        assertTrue(DuplicateCheck.isDuplicate(simpleArticle, duplicateWithDifferentType, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoInbooksWithDifferentChaptersAreNotDuplicates() {
        twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(simpleInbook, StandardField.CHAPTER,
                "Chapter One – Down the Rabbit Hole",
                "Chapter Two – The Pool of Tears");
    }

    @Test
    public void twoInbooksWithDifferentPagesAreNotDuplicates() {
        twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(simpleInbook, StandardField.PAGES, "1-20", "21-40");
    }

    @Test
    public void twoIncollectionsWithDifferentChaptersAreNotDuplicates() {
        twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(simpleIncollection, StandardField.CHAPTER, "10", "9");
    }

    @Test
    public void twoIncollectionsWithDifferentPagesAreNotDuplicates() {
        twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(simpleIncollection, StandardField.PAGES, "1-20", "21-40");
    }

    private void twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(final BibEntry cloneable,
                                                                       final String fieldType,
                                                                       final String firstValue,
                                                                       final String secondValue) {
        final BibEntry entry1 = (BibEntry) cloneable.clone();
        entry1.setField(fieldType, firstValue);

        final BibEntry entry2 = (BibEntry) cloneable.clone();
        entry2.setField(fieldType, secondValue);

        assertFalse(DuplicateCheck.isDuplicate(entry1, entry2, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void inbookWithoutChapterCouldBeDuplicateOfInbookWithChapter() {
        final BibEntry inbook1 = (BibEntry) simpleInbook.clone();
        final BibEntry inbook2 = (BibEntry) simpleInbook.clone();
        inbook2.setField(StandardField.CHAPTER, "");

        assertTrue(DuplicateCheck.isDuplicate(inbook1, inbook2, BibDatabaseMode.BIBTEX));
        assertTrue(DuplicateCheck.isDuplicate(inbook2, inbook1, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoBooksWithDifferentEditionsAreNotDuplicates() {
        BibEntry editionOne = new BibEntry(BibtexEntryTypes.BOOK);
        editionOne.setField(StandardField.TITLE, "Effective Java");
        editionOne.setField(StandardField.AUTHOR, "Bloch, Joshua");
        editionOne.setField(StandardField.PUBLISHER, "Prentice Hall");
        editionOne.setField(StandardField.DATE, "2001");
        editionOne.setField(StandardField.EDITION, "1");

        BibEntry editionTwo = new BibEntry(BibtexEntryTypes.BOOK);
        editionTwo.setField(StandardField.TITLE, "Effective Java");
        editionTwo.setField(StandardField.AUTHOR, "Bloch, Joshua");
        editionTwo.setField(StandardField.PUBLISHER, "Prentice Hall");
        editionTwo.setField(StandardField.DATE, "2008");
        editionTwo.setField(StandardField.EDITION, "2");

        assertFalse(DuplicateCheck.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void sameBooksWithMissingEditionAreDuplicates() {
        BibEntry editionOne = new BibEntry(BibtexEntryTypes.BOOK);
        editionOne.setField(StandardField.TITLE, "Effective Java");
        editionOne.setField(StandardField.AUTHOR, "Bloch, Joshua");
        editionOne.setField(StandardField.PUBLISHER, "Prentice Hall");
        editionOne.setField(StandardField.DATE, "2001");

        BibEntry editionTwo = new BibEntry(BibtexEntryTypes.BOOK);
        editionTwo.setField(StandardField.TITLE, "Effective Java");
        editionTwo.setField(StandardField.AUTHOR, "Bloch, Joshua");
        editionTwo.setField(StandardField.PUBLISHER, "Prentice Hall");
        editionTwo.setField(StandardField.DATE, "2008");

        assertTrue(DuplicateCheck.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void sameBooksWithPartiallyMissingEditionAreDuplicates() {
        BibEntry editionOne = new BibEntry(BibtexEntryTypes.BOOK);
        editionOne.setField(StandardField.TITLE, "Effective Java");
        editionOne.setField(StandardField.AUTHOR, "Bloch, Joshua");
        editionOne.setField(StandardField.PUBLISHER, "Prentice Hall");
        editionOne.setField(StandardField.DATE, "2001");

        BibEntry editionTwo = new BibEntry(BibtexEntryTypes.BOOK);
        editionTwo.setField(StandardField.TITLE, "Effective Java");
        editionTwo.setField(StandardField.AUTHOR, "Bloch, Joshua");
        editionTwo.setField(StandardField.PUBLISHER, "Prentice Hall");
        editionTwo.setField(StandardField.DATE, "2008");
        editionTwo.setField(StandardField.EDITION, "2");

        assertTrue(DuplicateCheck.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void sameBooksWithDifferentEditionsAreNotDuplicates() {
        BibEntry editionTwo = new BibEntry(BibtexEntryTypes.BOOK);
        editionTwo.setCiteKey("Sutton17reinfLrnIntroBook");
        editionTwo.setField(StandardField.TITLE, "Reinforcement learning:An introduction");
        editionTwo.setField(StandardField.PUBLISHER, "MIT Press");
        editionTwo.setField(StandardField.YEAR, "2017");
        editionTwo.setField(StandardField.AUTHOR, "Sutton, Richard S and Barto, Andrew G");
        editionTwo.setField(StandardField.ADDRESS, "Cambridge, MA.USA");
        editionTwo.setField(StandardField.EDITION, "Second");
        editionTwo.setField(StandardField.JOURNAL, "MIT Press");
        editionTwo.setField(StandardField.URL, "https://webdocs.cs.ualberta.ca/~sutton/book/the-book-2nd.html");

        BibEntry editionOne = new BibEntry(BibtexEntryTypes.BOOK);
        editionOne.setCiteKey("Sutton98reinfLrnIntroBook");
        editionOne.setField(StandardField.TITLE, "Reinforcement learning: An introduction");
        editionOne.setField(StandardField.PUBLISHER, "MIT press Cambridge");
        editionOne.setField(StandardField.YEAR, "1998");
        editionOne.setField(StandardField.AUTHOR, "Sutton, Richard S and Barto, Andrew G");
        editionOne.setField(StandardField.VOLUME, "1");
        editionOne.setField(StandardField.NUMBER, "1");
        editionOne.setField(StandardField.EDITION, "First");

        assertFalse(DuplicateCheck.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }
}
