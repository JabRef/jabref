package org.jabref.logic.bibtex;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;

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
        simpleArticle = new BibEntry(BibtexEntryTypes.ARTICLE.getName())
                .withField(FieldName.AUTHOR, "Single Author")
                .withField(FieldName.TITLE, "A serious paper about something")
                .withField(FieldName.YEAR, "2017");
        unrelatedArticle = new BibEntry(BibtexEntryTypes.ARTICLE.getName())
                .withField(FieldName.AUTHOR, "Completely Different")
                .withField(FieldName.TITLE, "Holy Moly Uffdada und Trallalla")
                .withField(FieldName.YEAR, "1992");
        simpleInbook = new BibEntry(BibtexEntryTypes.INBOOK.getName())
                .withField(FieldName.TITLE, "Alice in Wonderland")
                .withField(FieldName.AUTHOR, "Charles Lutwidge Dodgson")
                .withField(FieldName.CHAPTER, "Chapter One – Down the Rabbit Hole")
                .withField(FieldName.LANGUAGE, "English")
                .withField(FieldName.PUBLISHER, "Macmillan")
                .withField(FieldName.YEAR, "1865");
        simpleIncollection = new BibEntry(BibtexEntryTypes.INCOLLECTION.getName())
                .withField(FieldName.TITLE, "Innovation and Intellectual Property Rights")
                .withField(FieldName.AUTHOR, "Ove Grandstrand")
                .withField(FieldName.BOOKTITLE, "The Oxford Handbook of Innovation")
                .withField(FieldName.PUBLISHER, "Oxford University Press")
                .withField(FieldName.YEAR, "2004");
    }

    @Test
    public void testDuplicateDetection() {
        BibEntry one = new BibEntry(BibtexEntryTypes.ARTICLE.getName());

        BibEntry two = new BibEntry(BibtexEntryTypes.ARTICLE.getName());

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
        simpleArticle.setField(FieldName.DOI, "10.1016/j.is.2004.02.002");
        unrelatedArticle.setField(FieldName.DOI, "10.1016/j.is.2004.02.00X");

        assertFalse(DuplicateCheck.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithEqualDoisAreDuplicates() {
        simpleArticle.setField(FieldName.DOI, "10.1016/j.is.2004.02.002");
        unrelatedArticle.setField(FieldName.DOI, "10.1016/j.is.2004.02.002");

        assertTrue(DuplicateCheck.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithEqualPmidAreDuplicates() {
        simpleArticle.setField(FieldName.PMID, "12345678");
        unrelatedArticle.setField(FieldName.PMID, "12345678");

        assertTrue(DuplicateCheck.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoUnrelatedEntriesWithEqualEprintAreDuplicates() {
        simpleArticle.setField(FieldName.EPRINT, "12345678");
        unrelatedArticle.setField(FieldName.EPRINT, "12345678");

        assertTrue(DuplicateCheck.isDuplicate(simpleArticle, unrelatedArticle, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoEntriesWithSameDoiButDifferentTypesAreDuplicates() {
        simpleArticle.setField(FieldName.DOI, "10.1016/j.is.2004.02.002");
        BibEntry duplicateWithDifferentType = (BibEntry) simpleArticle.clone();
        duplicateWithDifferentType.setType(BibtexEntryTypes.INCOLLECTION);

        assertTrue(DuplicateCheck.isDuplicate(simpleArticle, duplicateWithDifferentType, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoInbooksWithDifferentChaptersAreNotDuplicates() {
        twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(simpleInbook, FieldName.CHAPTER,
                "Chapter One – Down the Rabbit Hole",
                "Chapter Two – The Pool of Tears");
    }

    @Test
    public void twoInbooksWithDifferentPagesAreNotDuplicates() {
        twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(simpleInbook, FieldName.PAGES, "1-20", "21-40");
    }

    @Test
    public void twoIncollectionsWithDifferentChaptersAreNotDuplicates() {
        twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(simpleIncollection, FieldName.CHAPTER, "10", "9");
    }

    @Test
    public void twoIncollectionsWithDifferentPagesAreNotDuplicates() {
        twoEntriesWithDifferentSpecificFieldsAreNotDuplicates(simpleIncollection, FieldName.PAGES, "1-20", "21-40");
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
        inbook2.setField(FieldName.CHAPTER, "");

        assertTrue(DuplicateCheck.isDuplicate(inbook1, inbook2, BibDatabaseMode.BIBTEX));
        assertTrue(DuplicateCheck.isDuplicate(inbook2, inbook1, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void twoBooksWithDifferentEditionsAreNotDuplicates() {
        BibEntry editionOne = new BibEntry(BibtexEntryTypes.BOOK.getName());
        editionOne.setField(FieldName.TITLE, "Effective Java");
        editionOne.setField(FieldName.AUTHOR, "Bloch, Joshua");
        editionOne.setField(FieldName.PUBLISHER, "Prentice Hall");
        editionOne.setField(FieldName.DATE, "2001");
        editionOne.setField(FieldName.EDITION, "1");

        BibEntry editionTwo = new BibEntry(BibtexEntryTypes.BOOK.getName());
        editionTwo.setField(FieldName.TITLE, "Effective Java");
        editionTwo.setField(FieldName.AUTHOR, "Bloch, Joshua");
        editionTwo.setField(FieldName.PUBLISHER, "Prentice Hall");
        editionTwo.setField(FieldName.DATE, "2008");
        editionTwo.setField(FieldName.EDITION, "2");

        assertFalse(DuplicateCheck.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void sameBooksWithMissingEditionAreDuplicates() {
        BibEntry editionOne = new BibEntry(BibtexEntryTypes.BOOK.getName());
        editionOne.setField(FieldName.TITLE, "Effective Java");
        editionOne.setField(FieldName.AUTHOR, "Bloch, Joshua");
        editionOne.setField(FieldName.PUBLISHER, "Prentice Hall");
        editionOne.setField(FieldName.DATE, "2001");

        BibEntry editionTwo = new BibEntry(BibtexEntryTypes.BOOK.getName());
        editionTwo.setField(FieldName.TITLE, "Effective Java");
        editionTwo.setField(FieldName.AUTHOR, "Bloch, Joshua");
        editionTwo.setField(FieldName.PUBLISHER, "Prentice Hall");
        editionTwo.setField(FieldName.DATE, "2008");

        assertTrue(DuplicateCheck.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void sameBooksWithPartiallyMissingEditionAreDuplicates() {
        BibEntry editionOne = new BibEntry(BibtexEntryTypes.BOOK.getName());
        editionOne.setField(FieldName.TITLE, "Effective Java");
        editionOne.setField(FieldName.AUTHOR, "Bloch, Joshua");
        editionOne.setField(FieldName.PUBLISHER, "Prentice Hall");
        editionOne.setField(FieldName.DATE, "2001");

        BibEntry editionTwo = new BibEntry(BibtexEntryTypes.BOOK.getName());
        editionTwo.setField(FieldName.TITLE, "Effective Java");
        editionTwo.setField(FieldName.AUTHOR, "Bloch, Joshua");
        editionTwo.setField(FieldName.PUBLISHER, "Prentice Hall");
        editionTwo.setField(FieldName.DATE, "2008");
        editionTwo.setField(FieldName.EDITION, "2");

        assertTrue(DuplicateCheck.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void sameBooksWithDifferentEditionsAreNotDuplicates() {
        BibEntry editionTwo = new BibEntry(BibtexEntryTypes.BOOK.getName());
        editionTwo.setCiteKey("Sutton17reinfLrnIntroBook");
        editionTwo.setField(FieldName.TITLE, "Reinforcement learning:An introduction");
        editionTwo.setField(FieldName.PUBLISHER, "MIT Press");
        editionTwo.setField(FieldName.YEAR, "2017");
        editionTwo.setField(FieldName.AUTHOR, "Sutton, Richard S and Barto, Andrew G");
        editionTwo.setField(FieldName.ADDRESS, "Cambridge, MA.USA");
        editionTwo.setField(FieldName.EDITION, "Second");
        editionTwo.setField(FieldName.JOURNAL, "MIT Press");
        editionTwo.setField(FieldName.URL, "https://webdocs.cs.ualberta.ca/~sutton/book/the-book-2nd.html");

        BibEntry editionOne = new BibEntry(BibtexEntryTypes.BOOK.getName());
        editionOne.setCiteKey("Sutton98reinfLrnIntroBook");
        editionOne.setField(FieldName.TITLE, "Reinforcement learning: An introduction");
        editionOne.setField(FieldName.PUBLISHER, "MIT press Cambridge");
        editionOne.setField(FieldName.YEAR, "1998");
        editionOne.setField(FieldName.AUTHOR, "Sutton, Richard S and Barto, Andrew G");
        editionOne.setField(FieldName.VOLUME, "1");
        editionOne.setField(FieldName.NUMBER, "1");
        editionOne.setField(FieldName.EDITION, "First");

        assertFalse(DuplicateCheck.isDuplicate(editionOne, editionTwo, BibDatabaseMode.BIBTEX));
    }
}
