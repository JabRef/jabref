package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
public class SemanticScholarTest implements PagedSearchFetcherTest {


    private static final String DOI = "10.23919/IFIPNetworking52078.2021.9472772";

    private final BibEntry IGOR_NEWCOMERS = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.AUTHOR, "Igor Steinmacher and Tayana Conte and Christoph Treude and M. Gerosa")
            .withField(StandardField.YEAR, "2016")
            .withField(StandardField.DOI, "10.1145/2884781.2884806")
            .withField(StandardField.TITLE, "Overcoming Open Source Project Entry Barriers with a Portal for Newcomers")
            .withField(StandardField.URL, "https://www.semanticscholar.org/paper/4bea2b4029a895bf898701329409e5a784fc2090")
            .withField(StandardField.VENUE, "2016 IEEE/ACM 38th International Conference on Software Engineering (ICSE)");

    private SemanticScholar fetcher;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        fetcher = new SemanticScholar();
        entry = new BibEntry();
    }

    @Test
    void getDocument() throws IOException {
        String source = fetcher.getURLBySource(
                String.format("https://api.semanticscholar.org/v1/paper/%s", DOI));

        assertEquals("https://www.semanticscholar.org/paper/7f7b38604a2c167f6d5fb1c5dffcbb127d0525c0", source);
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFindByDOI() throws IOException {
        entry.withField(StandardField.DOI, "10.1038/nrn3241");
        assertEquals(
                Optional.of(new URL("https://europepmc.org/articles/pmc4907333?pdf=render")),
                fetcher.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFindByDOIAlternate() throws IOException {
        assertEquals(
                Optional.of(new URL("https://pdfs.semanticscholar.org/7f6e/61c254bc2df38a784c1228f56c13317caded.pdf")),
                fetcher.findFullText(new BibEntry()
                        .withField(StandardField.DOI, "10.3390/healthcare9020206")));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextSearchOnEmptyEntry() throws IOException {

        assertEquals(Optional.empty(), fetcher.findFullText(new BibEntry()));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextNotFoundByDOI() throws IOException {
        entry = new BibEntry().withField(StandardField.DOI, DOI);
        entry.setField(StandardField.DOI, "10.1021/bk-2006-WWW.ch014");

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFindByArXiv() throws IOException {
        entry = new BibEntry().withField(StandardField.EPRINT, "1407.3561")
                .withField(StandardField.ARCHIVEPREFIX, "arXiv");
        assertEquals(
                Optional.of(new URL("https://arxiv.org/pdf/1407.3561.pdf")),
                fetcher.findFullText(entry)
        );
    }

    @Test
    void fullTextEntityWithoutDoi() throws IOException {
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
    void getURLForQueryWithLucene() throws QueryNodeParseException, MalformedURLException, FetcherException, URISyntaxException {
        String query = "Software engineering";
        SyntaxParser parser = new StandardSyntaxParser();
        URL url = fetcher.getURLForQuery(parser.parse(query, "default"), 0);
        assertEquals("https://api.semanticscholar.org/graph/v1/paper/search?query=Software+engineering&offset=0&limit=20&fields=paperId%2CexternalIds%2Curl%2Ctitle%2Cabstract%2Cvenue%2Cyear%2Cauthors", url.toString());
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry master = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Tobias Diez")
                .withField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory")
                .withField(StandardField.YEAR, "2014")
                .withField(StandardField.EPRINT, "1405.2249")
                .withField(StandardField.ARCHIVEPREFIX, "arXiv")
                .withField(StandardField.URL, "https://www.semanticscholar.org/paper/4986c1060236e7190b63f934df7806fbf2056cec");
        List<BibEntry> fetchedEntries = fetcher.performSearch("Slice theorem for Fréchet group actions and covariant symplectic");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(Collections.singletonList(master), fetchedEntries);
    }

    @Test
    void searchByPlainQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Overcoming Open Source Project Entry Barriers with a Portal for Newcomers");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(Collections.singletonList(IGOR_NEWCOMERS), fetchedEntries);
    }

    @Test
    void searchByQuotedQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("\"Overcoming Open Source Project Entry Barriers with a Portal for Newcomers\"");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(Collections.singletonList(IGOR_NEWCOMERS), fetchedEntries);
    }

    @Test
    public void performSearchByEmptyQuery() throws Exception {
        assertEquals(Collections.emptyList(), fetcher.performSearch(""));
    }

    @Test
    public void findByEntry() throws Exception {
        BibEntry barrosEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Formalising BPMN Service Interaction Patterns")
                .withField(StandardField.AUTHOR, "Chiara Muzi and Luise Pufahl and Lorenzo Rossi and M. Weske and F. Tiezzi")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.DOI, "10.1007/978-3-030-02302-7_1")
                .withField(StandardField.URL, "https://www.semanticscholar.org/paper/3bb026fd67db7d8e0e25de3189d6b7031b12783e")
                .withField(StandardField.VENUE, "PoEM");

        entry.withField(StandardField.TITLE, "Formalising BPMN Service Interaction Patterns");
        BibEntry actual = fetcher.performSearch(entry).get(0);
        // Abstract should not be included in JabRef tests
        actual.clearField(StandardField.ABSTRACT);
        assertEquals(barrosEntry, actual);
    }
}
