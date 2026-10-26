package org.jabref.logic.importer.fileformat.pdf;

import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class BookCitationBlockTest {

    private static final String HEADER = """
            УДК 53(075.3)
            ББК 22.3я721
            К56
            Рекомендовано Міністерством освіти і науки України
            (наказ МОН України від 17.03.2008 № 179)
            Рецензенти:
            О. В. Шевченко, доктор фізико-математичних наук, професор;
            Н. П. Мельник, учитель фізики вищої категорії
            """;

    private static final String AUTHOR_HEADING = "Коваль О. П.";

    private static final String TITLE_LINE = "К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — 2-ге вид., перероб. — Харків : Ранок, 2010. — 176 с.";

    private static final String CITATION_BLOCK = AUTHOR_HEADING + "\n" + TITLE_LINE;

    private static final String FOOTER = """
            ISBN 978-966-08-0000-7
            Посібник містить задачі з усіх тем курсу фізики 8 класу.
            © Коваль О. П., Ткач Н. І., 2008
            © ТОВ «Видавництво Ранок», 2010
            Підписано до друку 12.05.2010. Наклад 5000 пр.
            """;

    private static final String EXAMPLE_PAGE = HEADER + CITATION_BLOCK + "\n" + FOOTER;

    private static final String ISBN_LINE = "ISBN 978-966-08-0000-7\n";

    private static String citation(String areasAfterAuthors) {
        return "Збірник задач / О. П. Коваль. — " + areasAfterAuthors + ". — 176 с.";
    }

    static Stream<Arguments> pagesWithCompetingDates() {
        return Stream.of(
                Arguments.of("© Коваль О. П., Ткач Н. І., 2008\n", ""),
                Arguments.of("Рекомендовано Міністерством освіти і науки України\n(наказ МОН України від 17.03.2008 № 179)\n", ""),
                Arguments.of("", "Підписано до друку 12.05.2009. Наклад 5000 пр.\n"),
                Arguments.of("", "© Коваль О. П., Ткач Н. І., 2008\n© ТОВ «Видавництво Ранок», 2009\n"));
    }

    static Stream<Arguments> publicationAreas() {
        return Stream.of(
                Arguments.of("Харків : Ранок, 2010", "Ранок", "2010"),
                Arguments.of("Х. : Ранок, 2010", "Ранок", "2010"),
                Arguments.of("Біла Церква : Наук. думка, 2011", "Наук. думка", "2011"),
                Arguments.of("Київ : Видавництво «Генеза», 2015", "Видавництво «Генеза»", "2015"));
    }

    static Stream<Arguments> editions() {
        return Stream.of(
                Arguments.of(citation("2-ге вид., перероб. — Харків : Ранок, 2010"), Optional.of("2")),
                Arguments.of(citation("3-тє вид. — Харків : Ранок, 2010"), Optional.of("3")),
                Arguments.of(citation("10-те вид. — Харків : Ранок, 2010"), Optional.of("10")),
                Arguments.of(citation("Харків : Ранок, 2010"), Optional.empty()));
    }

    @Test
    void findsCitationBlockOnExamplePage() {
        assertEquals(Optional.of(CITATION_BLOCK), BookCitationBlock.find(EXAMPLE_PAGE));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void findsPublisherOnExamplePage() {
        assertEquals(Optional.of("Ранок"), BookCitationBlock.findPublisher(CITATION_BLOCK));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void findsYearOnExamplePage() {
        assertEquals(Optional.of("2010"), BookCitationBlock.findYear(CITATION_BLOCK));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @ParameterizedTest
    @MethodSource("pagesWithCompetingDates")
    void ignoresDatesOutsideCitation(String above, String below) {
        String page = above + CITATION_BLOCK + "\n" + below;

        assertEquals(Optional.of("2010"), BookCitationBlock.find(page).flatMap(BookCitationBlock::findYear));
    }

    @ParameterizedTest
    @MethodSource("pagesWithCompetingDates")
    void excludesLinesWithOtherDatesFromBlock(String above, String below) {
        String page = above + CITATION_BLOCK + "\n" + below;

        assertEquals(Optional.of(CITATION_BLOCK), BookCitationBlock.find(page));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void findsNothingOnPageWithoutCitation() {
        String page = """
                УДК 53(075.3)
                © Коваль О. П., Ткач Н. І., 2008
                © ТОВ «Видавництво Ранок», 2010
                Підписано до друку 12.05.2010. Наклад 5000 пр.
                """;

        assertEquals(Optional.empty(), BookCitationBlock.find(page));
        assertEquals(Optional.empty(), BookCitationBlock.findPublisher(page));
        assertEquals(Optional.empty(), BookCitationBlock.findYear(page));
    }

    @Test
    void excludesReviewerLineWithColonAndCommaFromBlock() {
        String page = "Рецензент: Київський університет, кафедра фізики\n" + TITLE_LINE + "\n" + ISBN_LINE;

        assertEquals(Optional.of(TITLE_LINE), BookCitationBlock.find(page));
    }

    @Test
    void findsPublicationAreaWrappedAcrossLines() {
        String page = """
                Коваль О. П.
                К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — Харків :
                Ранок, 2010. — 176 с.
                ISBN 978-966-08-0000-7
                """;
        Optional<String> block = BookCitationBlock.find(page);

        assertEquals(Optional.of("""
                Коваль О. П.
                К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — Харків :
                Ранок, 2010. — 176 с."""), block);
        assertEquals(Optional.of("Ранок"), block.flatMap(BookCitationBlock::findPublisher));
        assertEquals(Optional.of("2010"), block.flatMap(BookCitationBlock::findYear));
    }

    @ParameterizedTest
    @ValueSource(strings = {"—", "–", "-"})
    void acceptsDashVariantsAsAreaSeparator(String dash) {
        String citation = "Збірник / О. П. Коваль. " + dash + " Харків : Ранок, 2010. " + dash + " 176 с.";

        assertEquals(Optional.of("Ранок"), BookCitationBlock.findPublisher(citation));
        assertEquals(Optional.of("2010"), BookCitationBlock.findYear(citation));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @ParameterizedTest
    @MethodSource("publicationAreas")
    void findsPublisherAndYearOfPublicationArea(String publicationArea, String publisher, String year) {
        String citation = citation(publicationArea);

        assertEquals(Optional.of(publisher), BookCitationBlock.findPublisher(citation));
        assertEquals(Optional.of(year), BookCitationBlock.findYear(citation));
    }

    @Test
    void findsNothingWhenPageHasTwoCitations() {
        String page = EXAMPLE_PAGE + """
                Ткач Н. І.
                Т48 Збірник задач з хімії / Н. І. Ткач. — Харків : Ранок, 2011. — 160 с.
                """;

        assertEquals(Optional.empty(), BookCitationBlock.find(page));
        assertEquals(Optional.empty(), BookCitationBlock.findPublisher(page));
        assertEquals(Optional.empty(), BookCitationBlock.findYear(page));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void findsNoPublisherForSeveralPublishers() {
        assertEquals(Optional.empty(), BookCitationBlock.findPublisher(citation("Київ : Генеза ; Харків : Ранок, 2010")));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void findsYearForSeveralPublishers() {
        assertEquals(Optional.of("2010"), BookCitationBlock.findYear(citation("Київ : Генеза ; Харків : Ранок, 2010")));
    }

    @Test
    void startsBlockAtTitleLineWithoutAuthorHeading() {
        String page = HEADER + TITLE_LINE + "\n" + FOOTER;

        assertEquals(Optional.of(TITLE_LINE), BookCitationBlock.find(page));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ISBN_LINE,
            "Посібник містить задачі з усіх тем курсу фізики 8 класу.\n" + ISBN_LINE
    })
    void endsBlockAtPublicationAreaWithoutPagesArea(String below) {
        String titleLine = "К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — Харків : Ранок, 2010.";
        String page = HEADER + AUTHOR_HEADING + "\n" + titleLine + "\n" + below;

        assertEquals(Optional.of(AUTHOR_HEADING + "\n" + titleLine), BookCitationBlock.find(page));
    }

    @Test
    void findsNothingOnUnrelatedEnglishPage() {
        String page = """
                Chapter 1
                Mechanics describes how bodies move.
                It starts with kinematics, the study of motion, in 2010.
                """;

        assertEquals(Optional.empty(), BookCitationBlock.find(page));
        assertEquals(Optional.empty(), BookCitationBlock.findPublisher(page));
        assertEquals(Optional.empty(), BookCitationBlock.findYear(page));
    }

    @Test
    void findsNothingWhenSentenceFollowsAreaSeparatorInsteadOfPlace() {
        String page = "Посібник. — Містить задачі з усіх тем курсу фізики : механіка, 2010.\n";

        assertEquals(Optional.empty(), BookCitationBlock.find(page));
    }

    @Test
    void findsBlockOnPageWithWindowsLineBreaks() {
        assertEquals(Optional.of(CITATION_BLOCK), BookCitationBlock.find(EXAMPLE_PAGE.replace("\n", "\r\n")));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void acceptsNoBreakSpacesAsWhitespace() {
        String noBreakSpace = "\u00A0";
        String headingLine = AUTHOR_HEADING + noBreakSpace;
        String titleLine = "К56 Збірник задач / О. П. Коваль." + noBreakSpace + "— Харків" + noBreakSpace + ": Видавництво"
                + noBreakSpace + "«Ранок»," + noBreakSpace + "2010." + noBreakSpace + "— 176" + noBreakSpace + "с.";
        Optional<String> block = BookCitationBlock.find(headingLine + "\n" + titleLine + "\n" + ISBN_LINE);

        assertEquals(Optional.of(headingLine + "\n" + titleLine), block);
        assertEquals(Optional.of("Видавництво «Ранок»"), block.flatMap(BookCitationBlock::findPublisher));
        assertEquals(Optional.of("2010"), block.flatMap(BookCitationBlock::findYear));
    }

    @Test
    void includesWrappedTitleBelowAuthorHeading() {
        String block = """
                Коваль О. П.
                К56 Збірник задач з фізики для 8 класу загальноосвітніх
                навчальних закладів / О. П. Коваль, Н. І. Ткач. — Харків : Ранок, 2010. — 176 с.""";
        String page = HEADER + block + "\n" + FOOTER;

        assertEquals(Optional.of(block), BookCitationBlock.find(page));
    }

    @Test
    void startsWrappedTitleWithoutHeadingAtSlashLine() {
        String slashLine = "навчальних закладів / О. П. Коваль, Н. І. Ткач. — Харків : Ранок, 2010. — 176 с.";
        String page = HEADER
                + "К56 Збірник задач з фізики для 8 класу загальноосвітніх\n"
                + slashLine + "\n"
                + FOOTER;

        assertEquals(Optional.of(slashLine), BookCitationBlock.find(page));
    }

    @Test
    void startsBlockAtAreaLineWithoutSlash() {
        String line = "Збірник задач з фізики : навч. посіб. — Харків : Ранок, 2010. — 176 с.";
        String page = HEADER + line + "\n" + FOOTER;
        Optional<String> block = BookCitationBlock.find(page);

        assertEquals(Optional.of(line), block);
        assertEquals(Optional.of("Ранок"), block.flatMap(BookCitationBlock::findPublisher));
    }

    @ParameterizedTest
    @MethodSource("editions")
    void findsEdition(String citation, Optional<String> expectedEdition) {
        assertEquals(expectedEdition, BookCitationBlock.findEdition(citation));
    }
}
