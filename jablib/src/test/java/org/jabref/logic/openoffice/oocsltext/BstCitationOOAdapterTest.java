package org.jabref.logic.openoffice.oocsltext;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BstCitationOOAdapterTest {

    private static final String AUTHOR_DOE_JOHN = "Doe, John";

    @Test
    void extractFirstAuthorLastName_singleAuthor() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe", BstCitationOOAdapter.extractFirstAuthorLastName(entry));
    }

    @Test
    void extractFirstAuthorLastName_twoAuthors() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN + " and Roe, Richard")
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe and Roe", BstCitationOOAdapter.extractFirstAuthorLastName(entry));
    }

    @Test
    void extractFirstAuthorLastName_threeAuthorsEtAl() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN + " and Roe, Richard and Poe, Peter")
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe et al.", BstCitationOOAdapter.extractFirstAuthorLastName(entry));
    }

    @Test
    void extractFirstAuthorLastName_missingAuthorYieldsQuestionMark() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "2007");
        assertEquals("?", BstCitationOOAdapter.extractFirstAuthorLastName(entry));
    }

    @Test
    void buildAuthorYearCitation_singleEntry() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "2007");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, 2007)", result);
    }

    @Test
    void buildAuthorYearCitation_twoEntriesJoinedWithSemicolon() {
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "2007");
        BibEntry entry2 = new BibEntry()
                .withField(StandardField.AUTHOR, "Roe, Richard")
                .withField(StandardField.YEAR, "2008");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry1, entry2)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(entry1, entry2), ctx);
        assertEquals("(Doe, 2007; Roe, 2008)", result);
    }

    @Test
    void buildAuthorYearCitation_missingYearFallsBackToNd() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN);
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, n.d.)", result);
    }

    @Test
    void buildAuthorYearCitation_blankYearFallsBackToNd() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "   ");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, n.d.)", result);
    }

    @Test
    void buildAuthorYearCitation_usesYearFromDateField() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.DATE, "2007-05-01");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, 2007)", result);
    }
}
