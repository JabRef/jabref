package org.jabref.logic.importer.plaincitation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class RuleBasedPlainCitationParserTest {

    private static final String IEEE_ARTICLE = "J. Smith, M. Tan, D. Lee, and A. Wong, \"Improving AI systems,\" Journal of Computer Science, vol. 12, no. 2, pp. 45-60, 2024, doi: 10.1234/example.";
    private static final String APA_ARTICLE = "Smith, J., Tan, M., Lee, D., & Wong, A. (2024). Improving AI systems. Journal of Computer Science, 12(2), 45-60.";
    private static final String DOUBLE_SLASH_ARTICLE = "Ivanov, I. I. Title of the paper // Journal Name. 2015. P. 10-20.";
    private static final String BOOK = "Smith, J. Some book. Springer, Berlin, 2010.";

    private RuleBasedPlainCitationParser parser;

    @BeforeEach
    void setUp() {
        parser = new RuleBasedPlainCitationParser();
    }

    private Optional<String> parseField(String citation, Field field) throws FetcherException {
        return parser.parsePlainCitation(citation).flatMap(entry -> entry.getField(field));
    }

    // DOI
    // [utest->req~import.plain-citation.doi~1]

    /// A DOI is found with a `doi:` or `DOI:` prefix and as a `doi.org` link. Punctuation right after it
    /// (`.`, `,`, `;`) is not part of it, and of two DOIs only the first is kept.
    static Stream<Arguments> findsDoi() {
        return Stream.of(
                Arguments.of("10.1000/xyz123", "Smith, J. A title. Some Journal, 2020. doi:10.1000/xyz123"),
                Arguments.of("10.1000/xyz123", "Smith, J. A title. Journal, 2020. https://doi.org/10.1000/xyz123"),
                Arguments.of("10.1000/first", "Smith, J. A title. Journal, 2020. doi:10.1000/first doi:10.1000/second"),
                Arguments.of("10.1000/XYZ123", "Smith, J. A title. Journal, 2020. DOI: 10.1000/XYZ123"),
                Arguments.of("10.1000/xyz123", "Smith, J. A title. Journal, 2020. https://doi.org/10.1000/xyz123."),
                Arguments.of("10.1000/xyz123", "Smith, J. A title. Journal, doi:10.1000/xyz123, 2020."),
                Arguments.of("10.1000/xyz123", "Smith, J. A title. Journal, 2020. doi:10.1000/xyz123; accessed online."));
    }

    @ParameterizedTest
    @MethodSource
    void findsDoi(String expectedDoi, String citation) throws FetcherException {
        assertEquals(Optional.of(expectedDoi), parseField(citation, StandardField.DOI));
    }

    /// No DOI must mean no doi field at all, not an empty one.
    @Test
    void citationWithoutDoiHasNoDoiField() throws FetcherException {
        assertEquals(Optional.empty(), parseField("Smith, J. A title. Journal, 2020.", StandardField.DOI));
    }

    /// DOI extraction runs before URL extraction, so a `doi.org` link ends up only in the doi field.
    /// Both fields are checked, so the link must not get lost either.
    @ParameterizedTest
    @ValueSource(strings = {"https://doi.org/", "http://dx.doi.org/", "https://dx.doi.org/",
            "www.doi.org/", "http://www.doi.org/", "https://www.doi.org/"})
    void doiLinkIsStoredAsDoiNotUrl(String resolver) throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. A title. Journal, 2020. " + resolver + "10.1000/xyz123");
        assertEquals(List.of(Optional.of("10.1000/xyz123"), Optional.empty()),
                List.of(entry.flatMap(e -> e.getField(StandardField.DOI)),
                        entry.flatMap(e -> e.getField(StandardField.URL))));
    }

    /// The DOI is removed before year and pages extraction, so its digits must not become the year or pages.
    @Test
    void digitsInDoiAreNotTakenAsYearOrPages() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. A title. Journal, 2021. doi:10.1109/ICSE.2019.00012");
        assertEquals(List.of(Optional.of("2021"), Optional.empty()),
                List.of(entry.flatMap(e -> e.getField(StandardField.YEAR)),
                        entry.flatMap(e -> e.getField(StandardField.PAGES))));
    }

    /// A citation with both a DOI and a URL must keep both. Both orders are tested: DOI before URL, and URL before DOI.
    @ParameterizedTest
    @ValueSource(strings = {"Smith, J. A title. Journal, 2020. doi:10.1000/xyz123 https://example.com/page",
            "Smith, J. A title. Journal, 2020. https://example.com/page doi:10.1000/xyz123"})
    void findsDoiAndUrlInSameCitation(String citation) throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation(citation);
        assertEquals(List.of(Optional.of("10.1000/xyz123"), Optional.of("https://example.com/page")),
                List.of(entry.flatMap(e -> e.getField(StandardField.DOI)),
                        entry.flatMap(e -> e.getField(StandardField.URL))));
    }

    /// A DOI inside a publisher URL belongs to that URL: removing it would truncate the URL.
    /// The explicit `doi:` reference is the one that is removed.
    @ParameterizedTest
    @ValueSource(strings = {"10.1000/xyz123", "10.1000/different"})
    void doiInsidePublisherUrlKeepsUrlIntact(String explicitDoi) throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation(
                "Smith, J. A title. Journal, 2020. https://publisher.example.com/10.1000/xyz123 doi:" + explicitDoi);
        assertEquals(List.of(Optional.of(explicitDoi), Optional.of("https://publisher.example.com/10.1000/xyz123")),
                List.of(entry.flatMap(e -> e.getField(StandardField.DOI)),
                        entry.flatMap(e -> e.getField(StandardField.URL))));
    }

    @Test
    void firstStandaloneDoiAfterPublisherUrlWins() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation(
                "Smith, J. A title. Journal, 2020. https://publisher.example.com/10.1000/xyz123 doi:10.1000/different doi:10.1000/xyz123");
        assertEquals(List.of(Optional.of("10.1000/different"), Optional.of("https://publisher.example.com/10.1000/xyz123")),
                List.of(entry.flatMap(e -> e.getField(StandardField.DOI)),
                        entry.flatMap(e -> e.getField(StandardField.URL))));
    }

    @ParameterizedTest
    @CsvSource({"10.1000/different, https://doi.org/10.1000/different", "10/abcde, https://doi.org/abcde"})
    void doiResolverAfterPublisherUrlKeepsUrlIntact(String expectedDoi, String resolverUrl) throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation(
                "Smith, J. A title. Journal, 2020. https://publisher.example.com/10.1000/xyz123 " + resolverUrl);
        assertEquals(List.of(Optional.of(expectedDoi), Optional.of("https://publisher.example.com/10.1000/xyz123")),
                List.of(entry.flatMap(e -> e.getField(StandardField.DOI)),
                        entry.flatMap(e -> e.getField(StandardField.URL))));
    }

    @Test
    void doiInsidePublisherUrlIsUsedWhenNoStandaloneDoiExists() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. A title. Journal, 2020. https://publisher.example.com/10.1000/xyz123");
        assertEquals(List.of(Optional.of("10.1000/xyz123"), Optional.of("https://publisher.example.com/10.1000/xyz123")),
                List.of(entry.flatMap(e -> e.getField(StandardField.DOI)),
                        entry.flatMap(e -> e.getField(StandardField.URL))));
    }

    /// Lowercasing `İ` makes the text longer (it becomes two chars), which must not shift the offsets
    /// used to cut the DOI out of the original text.
    @Test
    void turkishCapitalIBeforeDoiDoesNotShiftRemoval() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("İnönü, E. A title. doi:10.1000/xyz123 Journal, 2020.");
        assertEquals(List.of(Optional.of("10.1000/xyz123"), Optional.of("Journal")),
                List.of(entry.flatMap(e -> e.getField(StandardField.DOI)),
                        entry.flatMap(e -> e.getField(StandardField.JOURNAL))));
    }

    // URL

    /// Every scheme the URL pattern accepts (`http`, `https`, `ftp`, a bare `www.`) is kept whole, including query strings.
    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/page", "http://example.com", "www.example.com/page", "ftp://files.example.com/data.zip",
            "https://example.com/page?id=123", "https://example.com/search?id=123&lang=en"})
    void findsUrl(String url) throws FetcherException {
        assertEquals(Optional.of(url), parseField("Smith, J. A title. Journal, 2020. " + url, StandardField.URL));
    }

    /// No URL must mean no url field at all, not an empty one.
    @Test
    void citationWithoutUrlHasNoUrlField() throws FetcherException {
        assertEquals(Optional.empty(), parseField("Smith, J. A title. Journal, 2020.", StandardField.URL));
    }

    /// Citations end with a period; it belongs to the sentence, not to the URL.
    @Test
    void urlDoesNotIncludeSentencePeriod() throws FetcherException {
        assertEquals(Optional.of("https://example.com/page"), parseField("Smith, J. A title. Journal, 2020. https://example.com/page.", StandardField.URL));
    }

    /// A closing bracket that closes a bracket inside the URL is part of the URL. One without an opening partner
    /// belongs to the text around it.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "https://en.wikipedia.org/wiki/Foo_(bar)|Smith, J. A title. Journal, 2020. https://en.wikipedia.org/wiki/Foo_(bar)",
            "https://en.wikipedia.org/wiki/Foo_(bar)|Smith, J. A title. Journal, 2020. https://en.wikipedia.org/wiki/Foo_(bar).",
            "https://example.com/page|Smith, J. A title. Journal, 2020 (see https://example.com/page)."})
    void keepsOnlyBalancedClosingBracketsInUrl(String expectedUrl, String citation) throws FetcherException {
        assertEquals(Optional.of(expectedUrl), parseField(citation, StandardField.URL));
    }

    /// The url field holds one value, so several URLs are joined with `, `.
    @Test
    void joinsMultipleUrls() throws FetcherException {
        assertEquals(Optional.of("https://a.example.com, https://b.example.com"),
                parseField("Smith, J. A title. Journal, 2020. https://a.example.com https://b.example.com", StandardField.URL));
    }

    /// URLs are removed before year extraction, so a year inside a URL must not become the year.
    @Test
    void yearInUrlIsNotTakenAsYear() throws FetcherException {
        assertEquals(Optional.of("2021"), parseField("Smith, J. A title. Journal, 2021. https://example.com/2019/paper", StandardField.YEAR));
    }

    // Authors
    // [utest->req~import.plain-citation.authors~1]

    /// Both author patterns, surname first and initials first, produce the BibTeX form `Last, I.`.
    @ParameterizedTest
    @ValueSource(strings = {"Smith, J. A title. Journal, 2020.",
            "J. Smith. A title. Journal, 2020."})
    void normalizesBothAuthorFormats(String citation) throws FetcherException {
        assertEquals(Optional.of("Smith, J."), parseField(citation, StandardField.AUTHOR));
    }

    /// The initials group allows up to three initials, and both must be kept.
    @Test
    void findsAuthorWithTwoInitials() throws FetcherException {
        assertEquals(Optional.of("Smith, J. and Doe, A. B."),
                parseField("Smith, J. and Doe, A. B. Deep learning for citations. Journal of AI, 2020.", StandardField.AUTHOR));
    }

    /// IEEE separates the last author with `and`, APA with `&`; neither separator may become part of a name.
    @ParameterizedTest
    @ValueSource(strings = {IEEE_ARTICLE, APA_ARTICLE})
    void findsMultipleAuthorsWithInitials(String citation) throws FetcherException {
        assertEquals(Optional.of("Smith, J. and Tan, M. and Lee, D. and Wong, A."), parseField(citation, StandardField.AUTHOR));
    }

    /// Without a final `and` or `&`, commas are the only separators between authors.
    @Test
    void findsAuthorsSeparatedOnlyByCommas() throws FetcherException {
        assertEquals(Optional.of("Smith, J. and Tan, M. and Lee, D."),
                parseField("Smith, J., Tan, M., Lee, D. A title. Journal, 2020.", StandardField.AUTHOR));
    }

    /// An authors block may mix both name orders. Neither pattern may end the block, and the second author
    /// must not be left in the text from which the title is inferred.
    @Test
    void findsMixedAuthorFormats() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. and A. Doe, Title. Journal, 2020.");
        assertEquals(List.of(Optional.of("Smith, J. and Doe, A."), Optional.of("Title")),
                List.of(entry.flatMap(e -> e.getField(StandardField.AUTHOR)),
                        entry.flatMap(e -> e.getField(StandardField.TITLE))));
    }

    /// IEEE reference lists put a reference number in front of each citation. It is neither part of the authors
    /// nor of the title.
    @Test
    void findsAuthorsBehindReferenceNumber() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("[1] J. Smith, \"Improving AI systems,\" Journal of Computer Science, 2020.");
        assertEquals(List.of(Optional.of("Smith, J."), Optional.of("Improving AI systems")),
                List.of(entry.flatMap(e -> e.getField(StandardField.AUTHOR)),
                        entry.flatMap(e -> e.getField(StandardField.TITLE))));
    }

    /// `\w` only matches ASCII letters by default, which cuts surnames with accented letters, hyphens or
    /// apostrophes short. Hyphenated initials (`K.-K.`) must stay whole as well.
    static Stream<Arguments> findsAuthorWithNonAsciiOrPunctuatedName() {
        return Stream.of(
                Arguments.of("Müller, K.", "Müller, K. A title. Journal, 2020."),
                Arguments.of("García-López, M.", "García-López, M. A title. Journal, 2020."),
                Arguments.of("O'Brien, P.", "O'Brien, P. A title. Journal, 2020."),
                Arguments.of("Kemell, K.-K.", "K.-K. Kemell. A title. Journal, 2020."));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void findsAuthorWithNonAsciiOrPunctuatedName(String expectedAuthor, String citation) throws FetcherException {
        assertEquals(Optional.of(expectedAuthor), parseField(citation, StandardField.AUTHOR));
    }

    /// Full first names, written first name first.
    @Test
    void findsMultipleAuthorsWithFullNames() throws FetcherException {
        assertEquals(Optional.of("Smith, John and Tan, Mary and Lee, David and Wong, Alice"),
                parseField("John Smith, Mary Tan, David Lee, and Alice Wong. Improving AI systems. Journal of Computer Science, 2024.", StandardField.AUTHOR));
    }

    /// A single author written with a full first name.
    @Test
    void findsSingleAuthorWithFullName() throws FetcherException {
        assertEquals(Optional.of("Smith, John"), parseField("John Smith. A title. Journal, 2020.", StandardField.AUTHOR));
    }

    static Stream<Arguments> findsFullNameAuthorsBeforeParenthesizedYear() {
        return Stream.of(
                Arguments.of("Smith, John", "John Smith (2020). A title. Journal."),
                Arguments.of("Smith, John", "John Smith(2020). A title. Journal."),
                Arguments.of("Smith, John", "John Smith ( 2020a ). A title. Journal."),
                Arguments.of("Smith, John", "John Smith (2020). A title. Journal, 2021."),
                Arguments.of("Smith, John and Tan, Mary", "John Smith and Mary Tan (2020). A title. Journal."),
                Arguments.of("Smith, Jr., John", "John Smith Jr. (2020). A title. Journal."),
                Arguments.of("Smith, John and others", "John Smith et al. (2020). A title. Journal."),
                Arguments.of("Smith, John", "John Smith (2020). \"A title\" Journal."),
                Arguments.of("Smith, John", "John Smith (2020). A title // Journal."));
    }

    // [utest->req~import.plain-citation.authors~1]
    @ParameterizedTest
    @MethodSource
    void findsFullNameAuthorsBeforeParenthesizedYear(String expectedAuthor, String citation) throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation(citation);
        assertEquals(List.of(Optional.of(expectedAuthor), Optional.of("2020"), Optional.of("A title")),
                List.of(entry.flatMap(e -> e.getField(StandardField.AUTHOR)),
                        entry.flatMap(e -> e.getField(StandardField.YEAR)),
                        entry.flatMap(e -> e.getField(StandardField.TITLE))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Machine Learning (2020). Nature.",
            "Machine Learning. (2020). Nature.",
            "John Smith (2020).",
            "John Smith (2020). A title"})
    void parenthesizedYearDoesNotReplaceSeparateTitleBoundary(String citation) throws FetcherException {
        assertEquals(Optional.empty(), parseField(citation, StandardField.AUTHOR));
    }

    static Stream<Arguments> keepsSuffixWithFullNameAuthor() {
        return Stream.of(
                Arguments.of("Smith, Jr., John", "John Smith Jr."),
                Arguments.of("Smith, Sr., John", "John Smith Sr."),
                Arguments.of("Smith, II, John", "John Smith II."),
                Arguments.of("Smith, III, John", "John Smith III."),
                Arguments.of("Smith, IV, John", "John Smith IV."),
                Arguments.of("van Beethoven, Jr., Ludwig", "Ludwig van Beethoven Jr."),
                Arguments.of("Smith, Jr., John and Tan, Mary", "John Smith Jr. and Mary Tan."),
                Arguments.of("Smith, Jr., John and others", "John Smith Jr. et al."));
    }

    @ParameterizedTest
    @MethodSource
    void keepsSuffixWithFullNameAuthor(String expectedAuthor, String authorBlock) throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation(authorBlock + " A title. Journal, 2020.");
        assertEquals(List.of(Optional.of(expectedAuthor), Optional.of("A title")),
                List.of(entry.flatMap(e -> e.getField(StandardField.AUTHOR)),
                        entry.flatMap(e -> e.getField(StandardField.TITLE))));
    }

    @Test
    void capitalizedTitleIsNotTakenAsFullNameAuthor() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Machine Learning")
                .withField(StandardField.JOURNAL, "Nature")
                .withField(StandardField.YEAR, "2020");
        Optional<BibEntry> actual = parser.parsePlainCitation("Machine Learning. Nature, 2020.");
        actual.ifPresent(entry -> entry.clearField(StandardField.COMMENT));
        assertEquals(Optional.of(expected), actual);
    }

    @ParameterizedTest
    @ValueSource(strings = {"John Smith. \"Machine Learning.\" Nature, 2020.",
            "John Smith. Machine Learning // Nature, 2020."})
    void findsFullNameAuthorBeforeExplicitTitleBoundary(String citation) throws FetcherException {
        assertEquals(Optional.of("Smith, John"), parseField(citation, StandardField.AUTHOR));
    }

    /// A particle such as `van` belongs to the surname, as in BibTeX's `von Last, First` form.
    @Test
    void keepsNameParticleWithSurname() throws FetcherException {
        assertEquals(Optional.of("van Beethoven, Ludwig"), parseField("Ludwig van Beethoven. A title. Journal, 2020.", StandardField.AUTHOR));
    }

    /// `et al.` becomes BibTeX's `and others`, also when the period after `al` is missing.
    static Stream<Arguments> convertsEtAlToOthers() {
        return Stream.of(
                Arguments.of("Smith, J. and others", "Smith, J., et al. Improving AI systems. Journal of Computer Science, 2024."),
                Arguments.of("Smith, J. and others", "Smith, J. et al Improving AI systems. Journal of Computer Science, 2024."),
                Arguments.of("Smith, John and others", "John Smith et al. Improving AI systems. Journal of Computer Science, 2024."),
                Arguments.of("Smith, John and others", "John Smith, et al. Improving AI systems. Journal of Computer Science, 2024."),
                Arguments.of("Smith, John and others", "John Smith ET AL. Improving AI systems. Journal of Computer Science, 2024."),
                Arguments.of("Smith, John and Tan, Mary and others", "John Smith and Mary Tan et al. Improving AI systems. Journal of Computer Science, 2024."));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource
    void convertsEtAlToOthers(String expectedAuthor, String citation) throws FetcherException {
        assertEquals(Optional.of(expectedAuthor), parseField(citation, StandardField.AUTHOR));
    }

    @Test
    void fullNameAuthorsWithEtAlAreRemovedBeforeParsingTitle() throws FetcherException {
        String citation = "John Smith and Mary Tan et al. Improving AI systems. Journal of Computer Science, 2024.";
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John and Tan, Mary and others")
                .withField(StandardField.TITLE, "Improving AI systems")
                .withField(StandardField.JOURNAL, "Journal of Computer Science")
                .withField(StandardField.YEAR, "2024")
                .withField(StandardField.COMMENT, citation);
        assertEquals(Optional.of(expected), parser.parsePlainCitation(citation));
    }

    /// The author patterns are case-sensitive, so a lowercase word is never taken as initial:
    /// `authors. Journal` must not be read as the author `Journal, s.`.
    @Test
    void lowercaseWordIsNotTakenAsInitial() throws FetcherException {
        assertEquals(Optional.empty(), parseField("A title without authors. Journal, 2020.", StandardField.AUTHOR));
    }

    // Year
    // [utest->req~import.plain-citation.year-and-pages~1]

    /// 1701 is the lowest year accepted.
    @ParameterizedTest
    @ValueSource(ints = {1701, 2020})
    void acceptsYearInRange(int year) throws FetcherException {
        assertEquals(Optional.of(String.valueOf(year)), parseField("Smith, J. A title. Journal, " + year + ".", StandardField.YEAR));
    }

    /// The current year is the highest year accepted. It is computed, so the test does not go stale.
    @Test
    void acceptsCurrentYear() throws FetcherException {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        assertEquals(Optional.of(currentYear), parseField("Smith, J. A title. Journal, " + currentYear + ".", StandardField.YEAR));
    }

    /// The years just outside both ends of the accepted range.
    static Stream<Integer> rejectsYearOutOfRange() {
        return Stream.of(1700, LocalDate.now().getYear() + 1);
    }

    @ParameterizedTest
    @MethodSource
    void rejectsYearOutOfRange(int year) throws FetcherException {
        assertEquals(Optional.empty(), parseField("Smith, J. A title. Journal, " + year + ".", StandardField.YEAR));
    }

    /// Only the publication year is replaced by the placeholder, so a year in the title stays in the title.
    @Test
    void titleKeepsYearItMentions() throws FetcherException {
        assertEquals(Optional.of("Review of 2020"), parseField("Smith, J. Review of 2020. Some Journal, 2020.", StandardField.TITLE));
    }

    /// Without parentheses, the last valid year wins: in most styles the publication year comes after the title.
    @Test
    void takesPublicationYearOverYearInTitle() throws FetcherException {
        assertEquals(Optional.of("2021"), parseField("Smith, J. Lessons from 2019. Journal, 2021.", StandardField.YEAR));
    }

    /// In APA style the year comes first, in parentheses; a later year in the title must not replace it.
    @ParameterizedTest
    @ValueSource(strings = {"Smith, J. (2021). Lessons from 2019. Journal, 12, 45-60.",
            "Smith, J. (2021a). Lessons from 2019. Journal, 12, 45-60.",
            "(2021a). Lessons from 2019. Journal, 12, 45-60."})
    void findsYearInParenthesesBeforeTitle(String citation) throws FetcherException {
        assertEquals(Optional.of("2021"), parseField(citation, StandardField.YEAR));
    }

    static Stream<Arguments> publicationYearTakesPrecedenceOverTitleYear() {
        return Stream.of(
                Arguments.of("Lessons from (2019) revisited", "2021", "Smith, J. Lessons from (2019) revisited. Journal, 2021."),
                Arguments.of("Lessons from (2019)", "2021", "Smith, J. Lessons from (2019). Journal, 2021."),
                Arguments.of("(2019) in review", "2021", "Smith, J. (2019) in review. Journal, 2021."),
                Arguments.of("Lessons from 2018", "2019", "Smith, J. Lessons from 2018. Journal, 2019–2021."),
                Arguments.of("Lessons from (2018)", "2019", "Smith, J. Lessons from (2018). Journal, 2019–2021."),
                Arguments.of("Lessons from 2018–2019", "2021", "Smith, J. Lessons from 2018–2019. Journal, 2021."),
                Arguments.of("Lessons from (2019)", "2021", "Smith, J. \"Lessons from (2019)\" Journal, 2021."),
                Arguments.of("Part one. Lessons from 2018–2019", "2021", "Smith, J. (2021). \"Part one. Lessons from 2018–2019\" Journal."));
    }

    @ParameterizedTest
    @MethodSource
    void publicationYearTakesPrecedenceOverTitleYear(String expectedTitle, String expectedYear, String citation) throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation(citation);
        assertEquals(List.of(Optional.of(expectedTitle), Optional.of(expectedYear), Optional.empty()),
                List.of(entry.flatMap(e -> e.getField(StandardField.TITLE)),
                        entry.flatMap(e -> e.getField(StandardField.YEAR)),
                        entry.flatMap(e -> e.getField(StandardField.PAGES))));
    }

    /// A future year in the title is out of range, so the publication year after it is used.
    @Test
    void ignoresFutureYearInTitle() throws FetcherException {
        assertEquals(Optional.of("2020"), parseField("Smith, J. Visions of 2050. Journal, 2020.", StandardField.YEAR));
    }

    /// For a year range, the start year is stored, and the end year must not end up in pages.
    @Test
    void takesStartOfYearRange() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. A title. Journal, 2019–2021.");
        assertEquals(List.of(Optional.of("2019"), Optional.empty()),
                List.of(entry.flatMap(e -> e.getField(StandardField.YEAR)),
                        entry.flatMap(e -> e.getField(StandardField.PAGES))));
    }

    // Title and journal

    // [utest->req~import.plain-citation.leading-in~1]
    @ParameterizedTest
    @ValueSource(strings = {"Smith, J. A title. In Vitro, 2020.",
            "Smith, J. \"A title\" In Vitro, 2020.",
            "Smith, J. A title // In Vitro, 2020.",
            "Smith, J. A title, In Vitro, 2020."})
    void preservesLeadingInInJournalName(String citation) throws FetcherException {
        assertEquals(Optional.of("In Vitro"), parseField(citation, StandardField.JOURNAL));
    }

    // [utest->req~import.plain-citation.leading-in~1]
    @Test
    void removesInBeforeProceedings() throws FetcherException {
        assertEquals(Optional.of("Proceedings of the Conference"),
                parseField("Smith, J. A title. In Proceedings of the Conference, 2020.", StandardField.JOURNAL));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Title. 2020. Journal Name", "Title. 2020, Journal Name",
            "Title. 2020; Journal Name", "Title. 2020 Journal Name"})
    void yearBetweenTitleAndJournalPreservesBoundary(String citation) throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Title")
                .withField(StandardField.JOURNAL, "Journal Name")
                .withField(StandardField.YEAR, "2020");
        Optional<BibEntry> actual = parser.parsePlainCitation(citation);
        actual.ifPresent(entry -> entry.clearField(StandardField.COMMENT));
        assertEquals(Optional.of(expected), actual);
    }

    /// The boundary between title and journal is found even when the title has a colon, a question mark,
    /// or words like `journal` and `Proceedings`, and when the journal name is abbreviated with periods.
    static Stream<Arguments> findsTitleAndJournal() {
        return Stream.of(
                Arguments.of("Improving AI: A survey", "Journal of AI", "Smith, J. Improving AI: A survey. Journal of AI, 2020."),
                Arguments.of("Is AI safe?", "Journal of AI", "Smith, J. Is AI safe? Journal of AI, 2020."),
                Arguments.of("Deep learning for AI", "Nature", "Smith, J. Deep learning for AI. Nature, 2020."),
                Arguments.of("A title", "J. Comp. Sci.", "Smith, J. A title. J. Comp. Sci., 2020."),
                Arguments.of("A title", "eLife", "Smith, J. A title. eLife, 2020."),
                Arguments.of("A title", "eLife", "Smith, J. A title. 2020. eLife"),
                Arguments.of("A title", "2D Materials", "Smith, J. A title. 2D Materials, 2020."),
                Arguments.of("A title", "2D Materials", "Smith, J. A title. 2020. 2D Materials"),
                Arguments.of("Is graphene useful?", "2D Materials", "Smith, J. Is graphene useful? 2D Materials, 2020."),
                Arguments.of("A study by J. de Vries", "eLife", "Smith, J. A study by J. de Vries. eLife, 2020."),
                Arguments.of("A study by Dr. van Dijk", "eLife", "Smith, J. A study by Dr. van Dijk. eLife, 2020."),
                Arguments.of("A journal of my travels", "Travel Review", "Smith, J. A journal of my travels. Travel Review, 2020."),
                Arguments.of("Proceedings of the past", "History Review", "Smith, J. Proceedings of the past. History Review, 2020."));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource
    void findsTitleAndJournal(String expectedTitle, String expectedJournal, String citation) throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation(citation);
        assertEquals(List.of(Optional.of(expectedTitle), Optional.of(expectedJournal)),
                List.of(entry.flatMap(e -> e.getField(StandardField.TITLE)),
                        entry.flatMap(e -> e.getField(StandardField.JOURNAL))));
    }

    /// The journal is optional; without one, leftover text such as `[year_tag]` must not become the journal.
    @Test
    void citationWithoutJournalHasNoJournalField() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. A title. 2020.");
        assertEquals(List.of(Optional.of("A title"), Optional.empty()),
                List.of(entry.flatMap(e -> e.getField(StandardField.TITLE)),
                        entry.flatMap(e -> e.getField(StandardField.JOURNAL))));
    }

    // Pages
    // [utest->req~import.plain-citation.year-and-pages~1]

    /// The `p.`/`pp.` prefix is dropped. Hyphen, en dash and BibTeX `--` ranges and a single page are stored
    /// with the BibTeX range separator `--` (a single page has no separator).
    static Stream<Arguments> findsPages() {
        return Stream.of(
                Arguments.of("12--20", "12-20"),
                Arguments.of("12--34", "p. 12-34"),
                Arguments.of("12--34", "pp. 12-34"),
                Arguments.of("12--34", "P. 12-34"),
                Arguments.of("45--60", "45–60"),
                Arguments.of("45--60", "pp. 45--60"),
                Arguments.of("12", "p. 12"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource
    void findsPages(String expectedPages, String writtenPages) throws FetcherException {
        assertEquals(Optional.of(expectedPages), parseField("Smith, J. A title. Journal, 2020, " + writtenPages + ".", StandardField.PAGES));
    }

    @ParameterizedTest
    @ValueSource(strings = {"p. 5", "pp. 5-10", "page 5", "pages 5-10"})
    void pageMarkersInTitleAreNotTakenAsPages(String pageMarker) throws FetcherException {
        String expectedTitle = "A guide to " + pageMarker + " notation";
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. " + expectedTitle + ". Journal, 2020.");
        assertEquals(List.of(Optional.of(expectedTitle), Optional.empty()),
                List.of(entry.flatMap(parsedEntry -> parsedEntry.getField(StandardField.TITLE)),
                        entry.flatMap(parsedEntry -> parsedEntry.getField(StandardField.PAGES))));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"p. 5|12|p. 12",
            "pp. 5-10|12|p. 12",
            "p. 5|20--30|pp. 20-30",
            "pp. 5-10|20--30|pp. 20-30"})
    void pageMarkersInTitleDoNotDisplacePublicationPages(String titlePageMarker, String expectedPages, String publicationPages) throws FetcherException {
        String expectedTitle = "A guide to " + titlePageMarker + " notation";
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. " + expectedTitle + ". Journal, 2020, " + publicationPages + ".");
        assertEquals(List.of(Optional.of(expectedTitle), Optional.of(expectedPages)),
                List.of(entry.flatMap(parsedEntry -> parsedEntry.getField(StandardField.TITLE)),
                        entry.flatMap(parsedEntry -> parsedEntry.getField(StandardField.PAGES))));
    }

    /// Year extraction runs before page extraction, so the year does not end up in pages.
    @Test
    void yearIsNotTakenAsPages() throws FetcherException {
        assertEquals(Optional.empty(), parseField("Smith, J. A title. Journal, 2020.", StandardField.PAGES));
    }

    /// A year rejected as out of range is still in the text when pages are extracted.
    @Test
    void rejectedYearIsNotTakenAsPages() throws FetcherException {
        assertEquals(Optional.empty(), parseField("Smith, J. A title. Journal, 1700.", StandardField.PAGES));
    }

    /// Only one year is replaced, so a second year is still in the text when pages are extracted.
    @Test
    void secondYearIsNotTakenAsPages() throws FetcherException {
        assertEquals(Optional.empty(), parseField("Smith, J. Lessons from 2019. Journal, 2021.", StandardField.PAGES));
    }

    /// The pages pattern must skip volume and issue numbers (`vol. 12`, `vol. 12, no. 2`, APA `12(2)`).
    @ParameterizedTest
    @ValueSource(strings = {"Smith, J. A title. Journal, vol. 12, pp. 45-60, 2020.",
            "Smith, J. A title. Journal, vol. 12, no. 2, pp. 45-60, 2020.",
            "Smith, J. (2020). A title. Journal, 12(2), 45-60."})
    void volumeAndIssueAreNotTakenAsPages(String citation) throws FetcherException {
        assertEquals(Optional.of("45--60"), parseField(citation, StandardField.PAGES));
    }

    @ParameterizedTest
    @ValueSource(strings = {"vol. 12-13", "volume 12-13", "issue 2-3", "no. 2-3", "nr. 2-3", "VOL. 12–13"})
    void volumeAndIssueRangesAreNotTakenAsPages(String volumeOrIssue) throws FetcherException {
        assertEquals(Optional.empty(), parseField("Smith, J. A title. Journal, 2020, " + volumeOrIssue + ".", StandardField.PAGES));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"45--60|vol. 12-13, pp. 45-60",
            "45--60|pp. 45-60, vol. 12-13",
            "45--60|45-60, issue 2-3",
            "45--60|issue 2-3, 45-60",
            "5|p. 5, vol. 12-13",
            "5|issue 2-3, p. 5"})
    void volumeAndIssueRangesDoNotReplacePages(String expectedPages, String publicationDetails) throws FetcherException {
        assertEquals(Optional.of(expectedPages), parseField("Smith, J. A title. Journal, 2020, " + publicationDetails + ".", StandardField.PAGES));
    }

    /// A DOI right after the page range must not change it.
    @Test
    void findsPagesFollowedByDoi() throws FetcherException {
        assertEquals(Optional.of("45--60"), parseField("Smith, J. A title. Journal, 2020, pp. 45-60. doi:10.1000/xyz123", StandardField.PAGES));
    }

    /// A page range of four-digit numbers (`1998-2005`) behind `pp.` stays in pages and is not taken as the year.
    @Test
    void pageRangeResemblingYearsIsNotTakenAsYear() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. A title. Journal, 2020, pp. 1998-2005.");
        assertEquals(List.of(Optional.of("2020"), Optional.of("1998--2005")),
                List.of(entry.flatMap(e -> e.getField(StandardField.YEAR)),
                        entry.flatMap(e -> e.getField(StandardField.PAGES))));
    }

    /// An unmarked page range of four-digit numbers behind the year is taken as pages, not as the year.
    @Test
    void unmarkedPageRangeAfterYearIsNotTakenAsYear() throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation("Smith, J. A title. Journal, 2020, 1725-1730.");
        assertEquals(List.of(Optional.of("2020"), Optional.of("1725--1730")),
                List.of(entry.flatMap(e -> e.getField(StandardField.YEAR)),
                        entry.flatMap(e -> e.getField(StandardField.PAGES))));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"2020|1725--1730|Smith, J. Lessons from 2018. Journal, 2020, 1725-1730.",
            "2020|1998--2005|Smith, J. Lessons from (2018). Journal, 2020, pp. 1998-2005.",
            "2020|1725--1730|Smith, J. (2020). Lessons from 2018. Journal, 1725-1730.",
            "2019|1725--1730|Smith, J. Lessons from 2018. Journal, 2019–2021, 1725-1730.",
            "2019|1725--1730|Smith, J. Lessons from 2018. Journal, pp. 1725-1730, 2019–2021."})
    void titleYearDoesNotDisplacePublicationYearBeforePages(String expectedYear, String expectedPages, String citation) throws FetcherException {
        Optional<BibEntry> entry = parser.parsePlainCitation(citation);
        assertEquals(List.of(Optional.of(expectedYear), Optional.of(expectedPages)),
                List.of(entry.flatMap(e -> e.getField(StandardField.YEAR)),
                        entry.flatMap(e -> e.getField(StandardField.PAGES))));
    }

    // Full citation

    /// Complete citations in IEEE, APA and double-slash style, and one book: every field and the entry type must match.
    /// The comment field holds the original citation. Its content is not part of the parsing
    /// contract, so it is cleared before comparing.
    static Stream<Arguments> parsesFullCitation() {
        return Stream.of(
                Arguments.of("IEEE article",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith, J. and Tan, M. and Lee, D. and Wong, A.")
                                .withField(StandardField.TITLE, "Improving AI systems")
                                .withField(StandardField.JOURNAL, "Journal of Computer Science")
                                .withField(StandardField.YEAR, "2024")
                                .withField(StandardField.PAGES, "45--60")
                                .withField(StandardField.DOI, "10.1234/example"),
                        IEEE_ARTICLE),
                Arguments.of("APA article",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith, J. and Tan, M. and Lee, D. and Wong, A.")
                                .withField(StandardField.TITLE, "Improving AI systems")
                                .withField(StandardField.JOURNAL, "Journal of Computer Science")
                                .withField(StandardField.YEAR, "2024")
                                .withField(StandardField.PAGES, "45--60"),
                        APA_ARTICLE),
                Arguments.of("article with DOI",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith, J.")
                                .withField(StandardField.TITLE, "A title")
                                .withField(StandardField.JOURNAL, "Some Journal")
                                .withField(StandardField.YEAR, "2020")
                                .withField(StandardField.DOI, "10.1000/xyz123"),
                        "Smith, J. A title. Some Journal, 2020. doi:10.1000/xyz123"),
                Arguments.of("double-slash article",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Ivanov, I. I.")
                                .withField(StandardField.TITLE, "Title of the paper")
                                .withField(StandardField.JOURNAL, "Journal Name")
                                .withField(StandardField.YEAR, "2015")
                                .withField(StandardField.PAGES, "10--20"),
                        DOUBLE_SLASH_ARTICLE),
                Arguments.of("double-slash article with numbered journal section",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Ivanov, I. I.")
                                .withField(StandardField.TITLE, "Title of the paper")
                                .withField(StandardField.JOURNAL, "Journal Name")
                                .withField(StandardField.YEAR, "2015")
                                .withField(StandardField.PAGES, "10--20"),
                        "Ivanov, I. I. Title of the paper // Journal Name, Section 2, 2015. P. 10-20."),
                Arguments.of("book",
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.AUTHOR, "Smith, J.")
                                .withField(StandardField.TITLE, "Some book")
                                .withField(StandardField.PUBLISHER, "Springer")
                                .withField(StandardField.YEAR, "2010"),
                        BOOK),
                Arguments.of("book with numbered imprint location",
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.AUTHOR, "Smith, J.")
                                .withField(StandardField.TITLE, "Some book")
                                .withField(StandardField.PUBLISHER, "Springer")
                                .withField(StandardField.YEAR, "2010"),
                        "Smith, J. Some book. Springer, Berlin 2, 2010."));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void parsesFullCitation(String name, BibEntry expected, String citation) throws FetcherException {
        Optional<BibEntry> actual = parser.parsePlainCitation(citation);
        actual.ifPresent(entry -> entry.clearField(StandardField.COMMENT));
        assertEquals(Optional.of(expected), actual);
    }

    // Invalid input and state

    /// Text without a citation gives no entry; otherwise an empty `@article` would be added to the library.
    @ParameterizedTest
    @ValueSource(strings = {"", "😋😋😋", "¦@#¦@#¦@#¦@°#¦@¦°¦@°"})
    void returnsEmptyForTextWithoutCitation(String text) throws FetcherException {
        assertEquals(Optional.empty(), parser.parsePlainCitation(text));
    }

    /// The parser keeps its results in fields between the extraction steps, so they must be reset for each call.
    @Test
    void stateIsResetBetweenCalls() throws FetcherException {
        parser.parsePlainCitation("Smith, J. A title. Journal, 2020. doi:10.1000/xyz123");
        parser.parsePlainCitation("Smith, J. A title. Journal, 2020. https://example.com/page");
        Optional<BibEntry> third = parser.parsePlainCitation("Doe, A. Other title. Journal, 2019.");
        assertEquals(List.of(Optional.empty(), Optional.empty()),
                List.of(third.flatMap(entry -> entry.getField(StandardField.DOI)),
                        third.flatMap(entry -> entry.getField(StandardField.URL))));
    }

    // Multiple citations

    /// Citations are split at blank lines by [CitationSplitter], giving one entry per citation.
    @Test
    void parsesCitationsSeparatedByBlankLine() throws FetcherException {
        List<BibEntry> entries = parser.parseMultiplePlainCitations(
                "Smith, J. A title. Journal, 2020.\n\nDoe, A. Other title. Journal, 2019.");
        assertEquals(List.of(Optional.of("2020"), Optional.of("2019")),
                entries.stream().map(entry -> entry.getField(StandardField.YEAR)).toList());
    }

    /// A single line break, as in text copied from a PDF, does not split a citation.
    @Test
    void lineBreakInsideCitationDoesNotSplitIt() throws FetcherException {
        List<BibEntry> entries = parser.parseMultiplePlainCitations("Smith, J. A title\nspanning two lines. Journal, 2020.");
        assertEquals(List.of(Optional.of("2020")),
                entries.stream().map(entry -> entry.getField(StandardField.YEAR)).toList());
    }

    /// Empty text gives an empty list, not a list with one empty entry.
    @Test
    void parsesNoCitationsFromEmptyText() throws FetcherException {
        assertEquals(List.of(), parser.parseMultiplePlainCitations(""));
    }
}
