package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.support.ExternalServicesTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// [utest->feat~fetchers.bibsonomy~1]
class BibSonomyFetcherTest {

    private static final Field INTERHASH = FieldFactory.parseField("interhash");

    private final BuildInfo buildInfo = new BuildInfo();

    private BibSonomyFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importerPreferences.getApiKey(BibSonomyFetcher.FETCHER_NAME)).thenReturn(Optional.of(buildInfo.bibSonomyUsername + ":" + buildInfo.bibSonomyApiKey));
        fetcher = new BibSonomyFetcher(importFormatPreferences, importerPreferences);
    }

    /// The live tests need the `BibSonomyUsername` and `BibSonomyApiKey` environment variables at build time and are skipped without them
    private void assumeCredentialsAvailable() {
        assumeTrue(BibSonomyFetcher.hasUsernameAndApiKey(buildInfo.bibSonomyUsername + ":" + buildInfo.bibSonomyApiKey));
    }

    @Test
    void getName() {
        assertEquals("BibSonomy", fetcher.getName());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "learning                          | search=learning&start=0&end=50",
            "learning year=2015                | search=learning&tags=sys%3Ayear%3A2015&start=0&end=50",
            "learning year-range=2010-2012     | search=learning&tags=sys%3Ayear%3A2010-2012&start=0&end=50",
            "title=foo OR year=2015            | search=foo&start=0&end=50",
            "year=2015 OR year=2017            | search=&start=0&end=50",
            "learning AND NOT year=2015        | search=learning&start=0&end=50"
    })
    void yearIsSentAsSystemTagOnlyIfRequired(String query, String expectedParameters) throws MalformedURLException, URISyntaxException {
        SearchQuery searchQuery = new SearchQuery(query);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQuery.getSearchFlags());

        assertEquals("https://www.bibsonomy.org/api/posts?resourcetype=publication&format=bibtex&" + expectedParameters,
                fetcher.getURLForQuery(visitor.visitStart(searchQuery.getContext())).toString());
    }

    @Test
    @ExternalServicesTest
    void searchByQueryFindsEntry() throws FetcherException {
        assumeCredentialsAvailable();
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Tang, Chunlei and Jia, Xing and Xiong, Yun")
                .withField(StandardField.TITLE, "From data to value in multimodal learning.")
                .withField(StandardField.JOURNAL, "Patterns")
                .withField(StandardField.YEAR, "2026")
                .withField(StandardField.VOLUME, "7")
                .withField(StandardField.NUMBER, "8")
                .withField(StandardField.PAGES, "101615")
                .withField(StandardField.DOI, "10.1016/j.patter.2026.101615");
        // Fields such as keywords or biburl belong to the user who posted the entry and may change, so only the bibliographic fields are compared
        List<Field> bibliographicFields = List.copyOf(expected.getFields());

        BibEntry firstEntry = fetcher.performSearch("From data to value in multimodal learning").getFirst();

        assertEquals(expected.getType(), firstEntry.getType());
        assertEquals(bibliographicFields.stream().map(expected::getField).toList(),
                bibliographicFields.stream().map(firstEntry::getField).toList());
    }

    @Test
    @ExternalServicesTest
    void searchReturnsEntriesWithoutInternalFields() throws FetcherException {
        assumeCredentialsAvailable();
        List<Field> internalFields = List.of(FieldFactory.parseField("added-at"), FieldFactory.parseField("interhash"), FieldFactory.parseField("intrahash"));

        List<Field> foundInternalFields = fetcher.performSearch("From data to value in multimodal learning").stream()
                                                 .flatMap(entry -> entry.getFields().stream())
                                                 .filter(internalFields::contains)
                                                 .toList();

        assertEquals(List.of(), foundInternalFields);
    }

    @Test
    @ExternalServicesTest
    void searchWithNonsenseQueryReturnsEmptyList() throws FetcherException {
        assumeCredentialsAvailable();
        assertEquals(List.of(), fetcher.performSearch("xqzvwplkjhgfdsa1234567890"));
    }

    @Test
    @ExternalServicesTest
    void searchWithEmptyQueryReturnsEmptyList() throws FetcherException {
        assumeCredentialsAvailable();
        assertEquals(List.of(), fetcher.performSearch(""));
    }

    @Test
    void cleanupConvertsTagsToKeywords() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "tagging folksonomy tagora");

        fetcher.doPostCleanup(entry);

        assertEquals(new BibEntry().withField(StandardField.KEYWORDS, "tagging, folksonomy, tagora"), entry);
    }

    @Test
    void cleanupNormalizesMonth() {
        BibEntry entry = new BibEntry().withField(StandardField.MONTH, "09");

        fetcher.doPostCleanup(entry);

        assertEquals(new BibEntry().withField(StandardField.MONTH, Month.SEPTEMBER.getJabRefFormat()), entry);
    }

    @Test
    void cleanupNormalizesDoi() {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "https://doi.org/10.1145/1135777.1135843");

        fetcher.doPostCleanup(entry);

        assertEquals(new BibEntry().withField(StandardField.DOI, "10.1145/1135777.1135843"), entry);
    }

    @Test
    void cleanupJoinsAbstractLines() {
        BibEntry entry = new BibEntry().withField(StandardField.ABSTRACT, " first line\nsecond line ");

        fetcher.doPostCleanup(entry);

        assertEquals(new BibEntry().withField(StandardField.ABSTRACT, "first line second line"), entry);
    }

    @ParameterizedTest
    @ValueSource(strings = {"added-at", "interhash", "intrahash", "timestamp", "priority", "citeulike-article-id", "description"})
    void cleanupRemovesInternalField(String fieldName) {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "Tagging")
                                       .withField(FieldFactory.parseField(fieldName), "internal value");

        fetcher.doPostCleanup(entry);

        assertEquals(new BibEntry().withField(StandardField.TITLE, "Tagging"), entry);
    }

    @Test
    void cleanupKeepsLinksToThePublication() {
        BibEntry entry = new BibEntry().withField(StandardField.URL, "https://example.org/paper")
                                       .withField(FieldFactory.parseField("biburl"), "https://www.bibsonomy.org/bibtex/abc/jabref-test-user");
        BibEntry expected = new BibEntry().withField(StandardField.URL, "https://example.org/paper")
                                          .withField(FieldFactory.parseField("biburl"), "https://www.bibsonomy.org/bibtex/abc/jabref-test-user");

        fetcher.doPostCleanup(entry);

        assertEquals(expected, entry);
    }

    @Test
    void parsesAndCleansRealResponse() throws IOException, ParseException {
        BibEntry expected = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("shen2005folksonomy")
                .withField(StandardField.ABSTRACT, "Folksonomy is an emerging technology that works to classify the information over WWW through tagging the bookmarks, photos or other web-based contents. It is understood to be organized by every user while not limited to the authors of the contents and the professional editors. This study surveyed the folksonomy as a complex network. The result indicates that the network, which is composed of the tags from the folksonomy, displays both properties of small world and scale-free. However, the statistics only shows a local and static slice of the vast body of folksonomy which is still evolving.")
                .withField(StandardField.AUTHOR, "Shen, Kaikai and Wu, Lide")
                .withField(StandardField.EPRINT, "cs.IR/0509072")
                .withField(StandardField.KEYWORDS, "tagging, folksonomy, tagora, d4.1, networks, statistics")
                .withField(StandardField.MONTH, Month.SEPTEMBER.getJabRefFormat())
                .withField(StandardField.TITLE, "Folksonomy as a Complex Network")
                .withField(StandardField.URL, "http://arxiv.org/abs/cs.IR/0509072")
                .withField(StandardField.YEAR, "2005");

        assertEquals(expected, parseAndCleanRealResponse().getFirst());
    }

    @Test
    void movesDoiOfRealResponseFromEeField() throws IOException, ParseException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("journals/patterns/TangJX26")
                .withField(StandardField.AUTHOR, "Tang, Chunlei and Jia, Xing and Xiong, Yun")
                .withField(StandardField.DOI, "10.1016/j.patter.2026.101615")
                .withField(StandardField.JOURNAL, "Patterns")
                .withField(StandardField.KEYWORDS, "dblp")
                .withField(StandardField.NUMBER, "8")
                .withField(StandardField.PAGES, "101615")
                .withField(StandardField.TITLE, "From data to value in multimodal learning.")
                .withField(StandardField.URL, "http://dblp.uni-trier.de/db/journals/patterns/patterns7.html#TangJX26")
                .withField(StandardField.VOLUME, "7")
                .withField(StandardField.YEAR, "2026");

        assertEquals(expected, parseAndCleanRealResponse().getLast());
    }

    @Test
    void removeDuplicatePostsKeepsFirstPostOfEachPublication() {
        BibEntry firstPost = new BibEntry().withField(StandardField.TITLE, "first post").withField(INTERHASH, "same");
        BibEntry secondPost = new BibEntry().withField(StandardField.TITLE, "second post").withField(INTERHASH, "same");

        assertEquals(List.of(firstPost), BibSonomyFetcher.removeDuplicatePosts(List.of(firstPost, secondPost)));
    }

    @Test
    void removeDuplicatePostsKeepsDifferentPublicationsInOrder() {
        BibEntry laterPublication = new BibEntry().withField(INTERHASH, "z");
        BibEntry earlierPublication = new BibEntry().withField(INTERHASH, "a");

        assertEquals(List.of(laterPublication, earlierPublication), BibSonomyFetcher.removeDuplicatePosts(List.of(laterPublication, earlierPublication)));
    }

    @Test
    void removeDuplicatePostsKeepsPostsWithoutInterhash() {
        BibEntry firstPost = new BibEntry().withField(StandardField.TITLE, "same");
        BibEntry secondPost = new BibEntry().withField(StandardField.TITLE, "same");

        assertEquals(List.of(firstPost, secondPost), BibSonomyFetcher.removeDuplicatePosts(List.of(firstPost, secondPost)));
    }

    @Test
    void removeDuplicatePostsReducesRealResponseToOnePostPerPublication() throws IOException, ParseException {
        List<BibEntry> uniquePosts = BibSonomyFetcher.removeDuplicatePosts(parseRealResponse());

        assertEquals(List.of(Optional.of("shen2005folksonomy"), Optional.of("journals/patterns/TangJX26")),
                uniquePosts.stream().map(BibEntry::getCitationKey).toList());
    }

    private List<BibEntry> parseAndCleanRealResponse() throws IOException, ParseException {
        List<BibEntry> entries = parseRealResponse();
        entries.forEach(fetcher::doPostCleanup);
        return entries;
    }

    private List<BibEntry> parseRealResponse() throws IOException, ParseException {
        try (InputStream response = BibSonomyFetcherTest.class.getResourceAsStream("bibsonomy-folksonomy.bib")) {
            return fetcher.getParser().parseEntries(response);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", ":", "user:", ":key", "", "   "})
    void malformedKeyIsInvalid(String key) {
        assertFalse(fetcher.isValidKey(key));
    }

    @Test
    @ExternalServicesTest
    void wrongCredentialsAreInvalid() {
        assertFalse(fetcher.isValidKey("jabref-test-user:not-a-real-api-key"));
    }

    @Test
    void searchWithoutCredentialsThrows() {
        ImporterPreferences noKeyPreferences = mock(ImporterPreferences.class);
        when(noKeyPreferences.getApiKey(BibSonomyFetcher.FETCHER_NAME)).thenReturn(Optional.empty());
        BibSonomyFetcher fetcherWithoutKey = new BibSonomyFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), noKeyPreferences);

        assertThrows(FetcherException.class, () -> fetcherWithoutKey.performSearch("machine learning"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", ":", "user:", ":key", "   "})
    void searchWithMalformedKeyAsksForCredentials(String key) {
        ImporterPreferences malformedKeyPreferences = mock(ImporterPreferences.class);
        when(malformedKeyPreferences.getApiKey(BibSonomyFetcher.FETCHER_NAME)).thenReturn(Optional.of(key));
        BibSonomyFetcher fetcherWithMalformedKey = new BibSonomyFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), malformedKeyPreferences);

        FetcherException exception = assertThrows(FetcherException.class, () -> fetcherWithMalformedKey.performSearch("machine learning"));

        assertEquals("BibSonomy needs your username and API key. Enter them as 'username:apikey' in Preferences > Web search > BibSonomy.", exception.getLocalizedMessage());
    }

    @Test
    @ExternalServicesTest
    void searchWithWrongCredentialsDoesNotShowThem() {
        ImporterPreferences wrongKeyPreferences = mock(ImporterPreferences.class);
        when(wrongKeyPreferences.getApiKey(BibSonomyFetcher.FETCHER_NAME)).thenReturn(Optional.of("jabref-test-user:not-a-real-api-key"));
        BibSonomyFetcher fetcherWithWrongKey = new BibSonomyFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), wrongKeyPreferences);

        FetcherException exception = assertThrows(FetcherException.class, () -> fetcherWithWrongKey.performSearch("machine learning"));

        // BibSonomy's load balancer sometimes answers 503 before the credentials are checked, which says nothing about the credentials
        assumeFalse(exception instanceof FetcherServerException);
        assertEquals("BibSonomy rejected the username or API key. Check them in Preferences > Web search > BibSonomy.", exception.getLocalizedMessage());
    }
}
