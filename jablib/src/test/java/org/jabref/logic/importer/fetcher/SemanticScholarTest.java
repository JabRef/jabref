package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class SemanticScholarTest implements PagedSearchFetcherTest {

    private static final String DOI = "10.23919/IFIPNetworking52078.2021.9472772";

    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class);

    private final BibEntry IGOR_NEWCOMERS = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.AUTHOR, "Igor Steinmacher and T. Conte and Christoph Treude and M. Gerosa")
            .withField(StandardField.YEAR, "2016")
            .withField(StandardField.DOI, "10.1145/2884781.2884806")
            .withField(StandardField.TITLE, "Overcoming Open Source Project Entry Barriers with a Portal for Newcomers")
            .withField(StandardField.URL, "https://www.semanticscholar.org/paper/4bea2b4029a895bf898701329409e5a784fc2090")
            .withField(StandardField.VENUE, "International Conference on Software Engineering");

    private SemanticScholar fetcher;
    private final Optional<String> apiKey = Optional.of(new BuildInfo().semanticScholarApiKey);

    @BeforeEach
    void setUp() {
        when(importerPreferences.getApiKey(SemanticScholarCitationFetcher.FETCHER_NAME)).thenReturn(apiKey);
        fetcher = new SemanticScholar(importerPreferences);
    }

    @Test
    void getDocument() throws IOException, FetcherException {
        String source = fetcher.getURLBySource(
                "https://api.semanticscholar.org/v1/paper/%s".formatted(DOI));

        assertEquals("https://www.semanticscholar.org/paper/7f7b38604a2c167f6d5fb1c5dffcbb127d0525c0", source);
    }

    @Test
    @Disabled("Returns a DOI instead of the required link")
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFindByDOI() throws URISyntaxException, FetcherException, IOException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1038/nrn3241");
        assertEquals(
                Optional.of(new URI("https://europepmc.org/articles/pmc4907333?pdf=render").toURL()),
                fetcher.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    @Disabled("Sometimes, does not find any thing")
    void fullTextFindByDOIAlternate() throws FetcherException, IOException, URISyntaxException {
        assertEquals(
                Optional.of(new URI("https://pdfs.semanticscholar.org/7f6e/61c254bc2df38a784c1228f56c13317caded.pdf").toURL()),
                fetcher.findFullText(new BibEntry()
                        .withField(StandardField.DOI, "10.3390/healthcare9020206")));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextSearchOnEmptyEntry() throws IOException, FetcherException {
        assertEquals(Optional.empty(), fetcher.findFullText(new BibEntry()));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextNotFoundByDOI() throws IOException, FetcherException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, DOI)
                                       .withField(StandardField.DOI, "10.1021/bk-2006-WWW.ch014");

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFindByArXiv() throws URISyntaxException, IOException, FetcherException {
        BibEntry entry = new BibEntry().withField(StandardField.EPRINT, "1407.3561")
                                       .withField(StandardField.ARCHIVEPREFIX, "arXiv");
        assertEquals(
                Optional.of(new URI("https://arxiv.org/pdf/1407.3561.pdf").toURL()),
                fetcher.findFullText(entry)
        );
    }

    @Test
    void fullTextEntityWithoutDoi() throws IOException, FetcherException {
        assertEquals(Optional.empty(), fetcher.findFullText(new BibEntry()));
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.META_SEARCH, fetcher.getTrustLevel());
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return fetcher;
    }

    @Test
    void getURLForQueryWithLucene() throws QueryNodeParseException, MalformedURLException, URISyntaxException {
        String query = "Software engineering";
        SearchQuery searchQueryObject = new SearchQuery(query);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
        URL url = fetcher.getURLForQuery(visitor.visitStart(searchQueryObject.getContext()), 0);
        assertEquals("https://api.semanticscholar.org/graph/v1/paper/search?query=Software%20engineering&offset=0&limit=20&fields=paperId%2CexternalIds%2Curl%2Ctitle%2Cabstract%2Cvenue%2Cyear%2Cauthors", url.toString());
    }

    @Test
    @Disabled("We seem to be blocked")
    void searchByQueryFindsEntry() throws FetcherException {
        BibEntry master = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Tobias Diez")
                .withField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory")
                .withField(StandardField.YEAR, "2014")
                .withField(StandardField.EPRINT, "1405.2249")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.URL, "https://www.semanticscholar.org/paper/4986c1060236e7190b63f934df7806fbf2056cec");
        List<BibEntry> fetchedEntries = fetcher.performSearch("Slice theorem for Fréchet group actions and covariant symplectic");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(List.of(master), fetchedEntries);
    }

    @Test
    @Disabled("We seem to be blocked")
    void searchByPlainQueryFindsEntry() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Overcoming Open Source Project Entry Barriers with a Portal for Newcomers");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(List.of(IGOR_NEWCOMERS), fetchedEntries);
    }

    @Test
    @Disabled("We seem to be blocked")
    void searchByQuotedQueryFindsEntry() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("\"Overcoming Open Source Project Entry Barriers with a Portal for Newcomers\"");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(List.of(IGOR_NEWCOMERS), fetchedEntries);
    }

    @Test
    void performSearchByEmptyQuery() throws FetcherException {
        assertEquals(List.of(), fetcher.performSearch(""));
    }

    @Test
    @Disabled("We seem to be blocked")
    void findByEntry() throws FetcherException {
        BibEntry barrosEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Formalising BPMN Service Interaction Patterns")
                .withField(StandardField.AUTHOR, "Chiara Muzi and Luise Pufahl and Lorenzo Rossi and M. Weske and F. Tiezzi")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.DOI, "10.1007/978-3-030-02302-7_1")
                .withField(StandardField.URL, "https://www.semanticscholar.org/paper/3bb026fd67db7d8e0e25de3189d6b7031b12783e")
                .withField(StandardField.VENUE, "The Practice of Enterprise Modeling");

        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "Formalising BPMN Service Interaction Patterns");
        BibEntry actual = fetcher.performSearch(entry).getFirst();
        // Abstract should not be included in JabRef tests
        actual.clearField(StandardField.ABSTRACT);
        assertEquals(barrosEntry, actual);
    }

    @Test
    @Override
    @DisabledOnCIServer("Unstable on CI")
    public void pageSearchReturnsUniqueResultsPerPage() {
        // Implementation is done in the interface
    }
}
