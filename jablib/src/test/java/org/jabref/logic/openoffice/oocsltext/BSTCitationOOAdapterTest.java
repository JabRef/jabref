package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BSTCitationOOAdapterTest {

    private static final String AUTHOR_DOE_JOHN = "Doe, John";

    @Test
    void extractFirstAuthorLastName_singleAuthor() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe", BSTCitationOOAdapter.extractFirstAuthorLastName(entry));
    }

    @Test
    void extractFirstAuthorLastName_twoAuthors() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN + " and Roe, Richard")
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe and Roe", BSTCitationOOAdapter.extractFirstAuthorLastName(entry));
    }

    @Test
    void extractFirstAuthorLastName_threeAuthorsEtAl() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN + " and Roe, Richard and Poe, Peter")
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe et al.", BSTCitationOOAdapter.extractFirstAuthorLastName(entry));
    }

    @Test
    void extractFirstAuthorLastName_missingAuthorYieldsQuestionMark() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "2007");
        assertEquals("?", BSTCitationOOAdapter.extractFirstAuthorLastName(entry));
    }

    @Test
    void buildAuthorYearCitation_singleEntry() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "2007");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BSTCitationOOAdapter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, 2007)", result);
    }

    @Test
    void keyOrId_returnsCitationKeyWhenPresent() {
        BibEntry entry = new BibEntry()
                .withCitationKey("MyKey");
        assertEquals("MyKey", BSTCitationOOAdapter.keyOrId(entry));
    }

    @Test
    void keyOrId_fallsBackToUniqueIdWhenKeyMissing() {
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();
        String k1 = BSTCitationOOAdapter.keyOrId(entry1);
        String k2 = BSTCitationOOAdapter.keyOrId(entry2);
        // Falls back to the internal id and those should be non-empty and distinct
        assertEquals(entry1.getId(), k1);
        assertEquals(entry2.getId(), k2);
        org.junit.jupiter.api.Assertions.assertNotEquals(k1, k2);
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
        String result = BSTCitationOOAdapter.buildAuthorYearCitation(List.of(entry1, entry2), ctx);
        assertEquals("(Doe, 2007; Roe, 2008)", result);
    }

    @Test
    void buildAuthorYearCitation_missingYearFallsBackToNd() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN);
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BSTCitationOOAdapter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, n.d.)", result);
    }

    @Test
    void buildAuthorYearCitation_blankYearFallsBackToNd() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "   ");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BSTCitationOOAdapter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, n.d.)", result);
    }

    @Test
    void buildAuthorYearCitation_usesYearFromDateField() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.DATE, "2007-05-01");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BSTCitationOOAdapter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, 2007)", result);
    }

    @Test
    void computeStyleOrderAndLabels_extractsStyleDefinedLabels() {
        String renderedBibliography = """
                \\begin{thebibliography}{}
                \\bibitem[SG20]{smith2020}
                Smith entry
                \\bibitem[SG20a]{smith2020a}
                Smith entry with suffix
                \\end{thebibliography}
                """;

        BSTCitationOOAdapter.StyleOrderAndLabels styleOrderAndLabels = BSTCitationOOAdapter.computeStyleOrderAndLabels(
                renderedBibliography,
                Map.of("smith2020", "smith2020", "smith2020a", "smith2020a"));

        assertEquals(Map.of("smith2020", 1, "smith2020a", 2), styleOrderAndLabels.identifierToNumberMap());
        assertEquals(Map.of("smith2020", "SG20", "smith2020a", "SG20a"), styleOrderAndLabels.identifierToLabelMap());
    }

    @Test
    void computeStyleOrderAndLabels_ignoresMissingBibitemLabels() {
        String renderedBibliography = """
                \\begin{thebibliography}{}
                \\bibitem{smith2020}
                Smith entry
                \\bibitem[SG20]{smith2020a}
                Smith entry with suffix
                \\end{thebibliography}
                """;

        BSTCitationOOAdapter.StyleOrderAndLabels styleOrderAndLabels = BSTCitationOOAdapter.computeStyleOrderAndLabels(
                renderedBibliography,
                Map.of("smith2020", "smith2020", "smith2020a", "smith2020a"));

        assertEquals(Map.of("smith2020", 1, "smith2020a", 2), styleOrderAndLabels.identifierToNumberMap());
        assertEquals(Map.of("smith2020a", "SG20"), styleOrderAndLabels.identifierToLabelMap());
    }

    @Test
    void getStyleDefinedLabelOrThrow_throwsWhenLabelMissing() {
        assertThrows(MissingStyleDefinedCitationLabelException.class,
                () -> BSTCitationOOAdapter.getStyleDefinedLabelOrThrow("smith2020", Map.of()));
    }

    @Test
    void computeStyleOrderAndLabels_normalizesEtalcharLabels() {
        String renderedBibliography = """
                \\begin{thebibliography}{}
                \\bibitem[TLY{\\etalchar{+}}21]{Tan_2021}
                Tan entry
                \\end{thebibliography}
                """;

        BSTCitationOOAdapter.StyleOrderAndLabels styleOrderAndLabels = BSTCitationOOAdapter.computeStyleOrderAndLabels(
                renderedBibliography,
                Map.of("Tan_2021", "Tan_2021"));

        assertEquals(Map.of("Tan_2021", 1), styleOrderAndLabels.identifierToNumberMap());
        assertEquals(Map.of("Tan_2021", "TLY+21"), styleOrderAndLabels.identifierToLabelMap());
    }
}
