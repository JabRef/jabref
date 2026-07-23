package org.jabref.logic.openoffice.oocsltext;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BstCitationOOAdapterTest {

    @Test
    void extractFirstAuthorLastName_singleAuthor() {
        BibEntry e = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe", BstCitationOOAdapter.extractFirstAuthorLastName(e));
    }

    @Test
    void extractFirstAuthorLastName_twoAuthors() {
        BibEntry e = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John and Roe, Richard")
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe and Roe", BstCitationOOAdapter.extractFirstAuthorLastName(e));
    }

    @Test
    void extractFirstAuthorLastName_threeAuthorsEtAl() {
        BibEntry e = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John and Roe, Richard and Poe, Peter")
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe et al.", BstCitationOOAdapter.extractFirstAuthorLastName(e));
    }

    @Test
    void extractFirstAuthorLastName_missingAuthorYieldsQuestionMark() {
        BibEntry e = new BibEntry()
                .withField(StandardField.YEAR, "2007");
        assertEquals("?", BstCitationOOAdapter.extractFirstAuthorLastName(e));
    }

    @Test
    void buildAuthorYearCitation_singleEntry() {
        BibEntry e = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.YEAR, "2007");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(e)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(e), ctx);
        assertEquals("(Doe, 2007)", result);
    }

    @Test
    void buildAuthorYearCitation_twoEntriesJoinedWithSemicolon() {
        BibEntry e1 = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.YEAR, "2007");
        BibEntry e2 = new BibEntry()
                .withField(StandardField.AUTHOR, "Roe, Richard")
                .withField(StandardField.YEAR, "2008");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(e1, e2)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(e1, e2), ctx);
        assertEquals("(Doe, 2007; Roe, 2008)", result);
    }

    @Test
    void buildAuthorYearCitation_missingYearFallsBackToNd() {
        BibEntry e = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(e)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(e), ctx);
        assertEquals("(Doe, n.d.)", result);
    }

    @Test
    void buildAuthorYearCitation_blankYearFallsBackToNd() {
        BibEntry e = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.YEAR, "   ");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(e)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(e), ctx);
        assertEquals("(Doe, n.d.)", result);
    }

    @Test
    void buildAuthorYearCitation_usesYearFromDateField() {
        BibEntry e = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.DATE, "2007-05-01");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(e)));
        String result = BstCitationOOAdapter.buildAuthorYearCitation(List.of(e), ctx);
        assertEquals("(Doe, 2007)", result);
    }
}
