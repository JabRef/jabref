package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class BstRendererParityTest {

    private static final String AUTHOR_DOE_JOHN = "Doe, John";

    @Test
    void extractFirstAuthorLastName_singleAuthor() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe", BstCitationFormatter.extractFirstAuthorLastName(entry));
    }

    @Test
    void extractFirstAuthorLastName_twoAuthors() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN + " and Roe, Richard")
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe and Roe", BstCitationFormatter.extractFirstAuthorLastName(entry));
    }

    @Test
    void extractFirstAuthorLastName_threeAuthorsEtAl() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN + " and Roe, Richard and Poe, Peter")
                .withField(StandardField.YEAR, "2007");
        assertEquals("Doe et al.", BstCitationFormatter.extractFirstAuthorLastName(entry));
    }

    @Test
    void extractFirstAuthorLastName_missingAuthorYieldsQuestionMark() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.YEAR, "2007");
        assertEquals("?", BstCitationFormatter.extractFirstAuthorLastName(entry));
    }

    @Test
    void buildAuthorYearCitation_singleEntry() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "2007");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BstCitationFormatter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, 2007)", result);
    }

    @Test
    void stableCitationKeyIsRequiredAndRetained() throws RenderException {
        BibEntry entry = new BibEntry().withCitationKey("MyKey").withField(StandardField.TITLE, "Title");
        BstCitationFormatter.StyleOrderAndLabels result = BstCitationFormatter.computeStyleOrderAndLabels(new BstVM(RendererTestData.SIMPLE_BST), List.of(entry), new BibDatabase(List.of(entry)));
        assertEquals(Map.of("MyKey", 1), result.identifierToNumberMap());
    }

    @Test
    void distinctInternalIdsAreNeverUsedAsCitationKeys() {
        BibEntry first = new BibEntry();
        BibEntry second = new BibEntry();
        assertNotEquals(first.getId(), second.getId());
        for (BibEntry entry : List.of(first, second)) {
            assertThrows(RenderException.class, () -> BstCitationFormatter.computeStyleOrderAndLabels(
                    new BstVM(RendererTestData.SIMPLE_BST), List.of(entry), new BibDatabase(List.of(entry))));
        }
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
        String result = BstCitationFormatter.buildAuthorYearCitation(List.of(entry1, entry2), ctx);
        assertEquals("(Doe, 2007; Roe, 2008)", result);
    }

    @Test
    void buildAuthorYearCitation_missingYearFallsBackToNd() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN);
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BstCitationFormatter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, n.d.)", result);
    }

    @Test
    void buildAuthorYearCitation_blankYearFallsBackToNd() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.YEAR, "   ");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BstCitationFormatter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, n.d.)", result);
    }

    @Test
    void buildAuthorYearCitation_usesYearFromDateField() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_DOE_JOHN)
                .withField(StandardField.DATE, "2007-05-01");
        BibDatabaseContext ctx = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String result = BstCitationFormatter.buildAuthorYearCitation(List.of(entry), ctx);
        assertEquals("(Doe, 2007)", result);
    }

    @Test
    void buildNumericCitationText_sortsNumbersAscending() {
        assertEquals("[1, 3, 7]", BstCitationFormatter.buildNumericCitationText(
                List.of("gamma", "alpha", "beta"),
                Map.of("alpha", 1, "beta", 7, "gamma", 3)));
    }

    @ParameterizedTest
    @MethodSource
    void buildNumericCitationTextPreservesDuplicatesWithoutCollapsingRanges(List<String> identifiers, String expected) {
        assertEquals(expected, BstCitationFormatter.buildNumericCitationText(identifiers, Map.of("alpha", 1, "beta", 2, "gamma", 3)));
    }

    static Stream<Arguments> buildNumericCitationTextPreservesDuplicatesWithoutCollapsingRanges() {
        return Stream.of(
                Arguments.of(List.of(), "[]"),
                Arguments.of(List.of("gamma", "alpha", "beta"), "[1, 2, 3]"),
                Arguments.of(List.of("beta", "alpha", "beta"), "[1, 2, 2]"));
    }

    @ParameterizedTest
    @MethodSource
    void buildStyleDefinedCitationTextPreservesCitationOrderAndSuffixes(List<String> identifiers, String expected) throws MissingCitationLabelException {
        assertEquals(expected, BstCitationFormatter.buildStyleDefinedCitationText(identifiers, Map.of("first", "ABC20a", "second", "ABC20b")));
    }

    static Stream<Arguments> buildStyleDefinedCitationTextPreservesCitationOrderAndSuffixes() {
        return Stream.of(
                Arguments.of(List.of(), "[]"),
                Arguments.of(List.of("second", "first"), "[ABC20b, ABC20a]"),
                Arguments.of(List.of("first", "first"), "[ABC20a, ABC20a]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void buildStyleDefinedCitationTextRejectsBlankLabelsInMixedGroups(String blankLabel) {
        assertThrows(MissingCitationLabelException.class,
                () -> BstCitationFormatter.buildStyleDefinedCitationText(List.of("resolved", "missing"), Map.of("resolved", "ABC20", "missing", blankLabel)));
    }

    @Test
    void computeStyleOrderAndLabelsUsesRenderedOrderAndMappedIdentifiers() {
        String renderedBibliography = """
                \\bibitem[Z24]{zebra}
                Zebra entry
                \\bibitem[A20]{alpha}
                Alpha entry
                \\bibitem[U22]{unmapped}
                Unmapped entry
                """;

        assertEquals(new BstCitationFormatter.StyleOrderAndLabels(
                        Map.of("zebra-id", 1, "alpha-id", 2, "unmapped", 3),
                        Map.of("zebra-id", "Z24", "alpha-id", "A20", "unmapped", "U22")),
                BstCitationFormatter.computeStyleOrderAndLabels(renderedBibliography, Map.of("alpha", "alpha-id", "zebra", "zebra-id")));
    }

    @Test
    void computeStyleOrderAndLabelsKeepsFirstNumberButLastSupportedLabelForDuplicateIdentifiers() {
        String renderedBibliography = """
                \\bibitem[AB20]{first-key}
                \\bibitem[CD21]{other-key}
                \\bibitem[AB20a]{alias-key}
                \\bibitem[\\unsupported{2020}]{first-key}
                \\bibitem{alias-key}
                """;

        assertEquals(new BstCitationFormatter.StyleOrderAndLabels(
                        Map.of("shared-id", 1, "other-key", 2),
                        Map.of("shared-id", "AB20a", "other-key", "CD21")),
                BstCitationFormatter.computeStyleOrderAndLabels(renderedBibliography, Map.of("first-key", "shared-id", "alias-key", "shared-id")));
    }

    @ParameterizedTest
    @MethodSource
    void computeStyleOrderAndLabelsNumbersAccordingToTheBstProgram(String resourcePath, Map<String, Integer> expectedNumbers) throws IOException, RenderException {
        BibEntry zebra = new BibEntry()
                .withCitationKey("zebra")
                .withField(StandardField.AUTHOR, "Zebra, Zoe")
                .withField(StandardField.TITLE, "First cited")
                .withField(StandardField.YEAR, "2024");
        BibEntry alpha = new BibEntry()
                .withCitationKey("alpha")
                .withField(StandardField.AUTHOR, "Alpha, Alice")
                .withField(StandardField.TITLE, "Second cited")
                .withField(StandardField.YEAR, "2020");
        BibDatabase database = new BibDatabase(List.of(alpha, zebra));
        BstStyle style = BstStyle.createInternal(resourcePath);

        assertEquals(new BstCitationFormatter.StyleOrderAndLabels(expectedNumbers, Map.of()),
                BstCitationFormatter.computeStyleOrderAndLabels(style.createBstVM(), List.of(zebra, alpha), database));
    }

    static Stream<Arguments> computeStyleOrderAndLabelsNumbersAccordingToTheBstProgram() {
        return Stream.of(
                Arguments.of(BstStyle.INTERNAL_IEEETRAN_PATH, Map.of("zebra", 1, "alpha", 2)),
                Arguments.of(BstStyle.INTERNAL_ABBRV_PATH, Map.of("alpha", 1, "zebra", 2)));
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

        BstCitationFormatter.StyleOrderAndLabels styleOrderAndLabels = BstCitationFormatter.computeStyleOrderAndLabels(
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

        BstCitationFormatter.StyleOrderAndLabels styleOrderAndLabels = BstCitationFormatter.computeStyleOrderAndLabels(
                renderedBibliography,
                Map.of("smith2020", "smith2020", "smith2020a", "smith2020a"));

        assertEquals(Map.of("smith2020", 1, "smith2020a", 2), styleOrderAndLabels.identifierToNumberMap());
        assertEquals(Map.of("smith2020a", "SG20"), styleOrderAndLabels.identifierToLabelMap());
    }

    @Test
    void getStyleDefinedLabelOrThrow_throwsWhenLabelMissing() {
        assertThrows(MissingCitationLabelException.class,
                () -> BstCitationFormatter.getStyleDefinedLabelOrThrow("smith2020", Map.of()));
    }

    @Test
    void buildStyleDefinedCitationText_throwsWhenLabelMissing() {
        assertThrows(MissingCitationLabelException.class,
                () -> BstCitationFormatter.buildStyleDefinedCitationText(List.of("smith2020"), Map.of()));
    }

    @Test
    void computeStyleOrderAndLabels_ignoresCommandLabels() {
        String renderedBibliography = """
                \\begin{thebibliography}{}
                \\bibitem[\\protect\\astroncite{Tan et al.}{2021}]{Tan_2021}
                Tan entry
                \\end{thebibliography}
                """;

        BstCitationFormatter.StyleOrderAndLabels styleOrderAndLabels = BstCitationFormatter.computeStyleOrderAndLabels(
                renderedBibliography,
                Map.of("Tan_2021", "Tan_2021"));

        assertEquals(Map.of("Tan_2021", 1), styleOrderAndLabels.identifierToNumberMap());
        assertEquals(Map.of(), styleOrderAndLabels.identifierToLabelMap());
    }

    @Test
    void computeStyleOrderAndLabels_normalizesEtalcharLabels() {
        String renderedBibliography = """
                \\begin{thebibliography}{}
                \\bibitem[TLY{\\etalchar{+}}21]{Tan_2021}
                Tan entry
                \\end{thebibliography}
                """;

        BstCitationFormatter.StyleOrderAndLabels styleOrderAndLabels = BstCitationFormatter.computeStyleOrderAndLabels(
                renderedBibliography,
                Map.of("Tan_2021", "Tan_2021"));

        assertEquals(Map.of("Tan_2021", 1), styleOrderAndLabels.identifierToNumberMap());
        assertEquals(Map.of("Tan_2021", "TLY+21"), styleOrderAndLabels.identifierToLabelMap());
    }
}
