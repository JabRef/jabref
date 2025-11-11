package org.jabref.logic.importer.fetcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
class OpenAlexFetcherTest {
    private OpenAlex fetcher;
    private final BibEntry NERF = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.AUTHOR, "Haithem Turki and Deva Ramanan and Mahadev Satyanarayanan")
            .withField(StandardField.YEAR, "2022")
            .withField(StandardField.DOI, "10.1109/cvpr52688.2022.01258")
            .withField(StandardField.TITLE, "Mega-NeRF: Scalable Construction of Large-Scale NeRFs for Virtual Fly- Throughs")
            .withField(StandardField.URL, "https://openalex.org/W4313031684");

    @BeforeEach
    void setUp() {
        fetcher = new OpenAlex();
    }

    @Test
    void getURLForQueryBuildsSearchUrl() throws MalformedURLException, URISyntaxException {
        String testQuery = "deep learning";
        SearchQuery searchQuery = new SearchQuery(testQuery);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQuery.getSearchFlags());
        URL url = fetcher.getURLForQuery(visitor.visitStart(searchQuery.getContext()));
        assertTrue(url.toString().startsWith("https://api.openalex.org/works?search="));
        assertTrue(url.toString().contains("deep%20learning"));
    }

    @Test
    void parserParsesResultsArray() throws Exception {
        Parser parser = fetcher.getParser();
        String json = "{" +
                "\"results\":[{" +
                "\"type\":\"article\"," +
                "\"title\":\"Sample Title\"," +
                "\"publication_year\":2020," +
                "\"doi\":\"https://doi.org/10.1234/ABC.5678\"," +
                "\"id\":\"https://openalex.org/W12345\"," +
                "\"authorships\":[{" +
                "\"author\":{\"display_name\":\"Alice\"}}," +
                "{\"author\":{\"display_name\":\"Bob\"}}]," +
                "\"biblio\":{\"volume\":\"12\",\"issue\":\"3\",\"first_page\":\"45\",\"last_page\":\"67\"}," +
                "\"concepts\":[{\"display_name\":\"Machine Learning\"},{\"display_name\":\"AI\"}]" +
                "}]}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<BibEntry> entries = parser.parseEntries(is);
        assertEquals(1, entries.size());
        BibEntry e = entries.getFirst();
        assertEquals("article", e.getType().getName());
        assertEquals("Sample Title", e.getField(StandardField.TITLE).orElse(""));
        assertEquals("2020", e.getField(StandardField.YEAR).orElse(""));
        assertEquals("10.1234/ABC.5678", e.getField(StandardField.DOI).orElse(""));
        assertEquals("https://openalex.org/W12345", e.getField(StandardField.URL).orElse(""));
        assertEquals("Alice and Bob", e.getField(StandardField.AUTHOR).orElse(""));
        assertEquals("12", e.getField(StandardField.VOLUME).orElse(""));
        assertEquals("3", e.getField(StandardField.NUMBER).orElse(""));
        assertEquals("45--67", e.getField(StandardField.PAGES).orElse(""));
        assertTrue(e.getField(StandardField.KEYWORDS).orElse("").contains("Machine Learning"));
        assertTrue(e.getField(StandardField.KEYWORDS).orElse("").contains("AI"));
    }

    @Test
    void parserParsesSingleWorkObject() throws Exception {
        Parser parser = fetcher.getParser();
        String json = """
                {
                    "type":"article",
                    "title":"Single Title",
                    "publication_year":2019,
                    "doi":"https://doi.org/10.5555/xyz",
                    "id":"https://openalex.org/W999",
                    "authorships":[{"author":{"display_name":"Carol"}}],
                    "biblio":{"first_page":"1","last_page":"10"}
                }
                """;
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<BibEntry> entries = parser.parseEntries(is);
        assertEquals(1, entries.size());
        BibEntry e = entries.getFirst();
        assertEquals("Single Title", e.getField(StandardField.TITLE).orElse(""));
        assertEquals("2019", e.getField(StandardField.YEAR).orElse(""));
        assertEquals("10.5555/xyz", e.getField(StandardField.DOI).orElse(""));
        assertEquals("https://openalex.org/W999", e.getField(StandardField.URL).orElse(""));
        assertEquals("Carol", e.getField(StandardField.AUTHOR).orElse(""));
        assertEquals("1--10", e.getField(StandardField.PAGES).orElse(""));
    }

    @Test
    void fullTextFindByDOI() throws URISyntaxException, FetcherException, IOException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1145/3503250");
        assertEquals(
                Optional.of(new URI("https://dl.acm.org/doi/pdf/10.1145/3503250").toURL()),
                fetcher.findFullText(entry)
        );
    }

    @Test
    void fullTextFindByDOIAlternate() throws FetcherException, IOException, URISyntaxException {
        assertEquals(
                Optional.of(new URI("https://www.mdpi.com/2227-9032/9/2/206/pdf?version=1614152367").toURL()),
                fetcher.findFullText(new BibEntry()
                        .withField(StandardField.DOI, "10.3390/healthcare9020206")));
    }

    @Test
    void fullTextSearchOnEmptyEntry() throws IOException, FetcherException {
        assertEquals(Optional.empty(), fetcher.findFullText(new BibEntry()));
    }

    @Test
    void fullTextSearchByopenAlexURl() throws IOException, FetcherException, URISyntaxException {
        assertEquals(
                Optional.of(new URI("https://www.mdpi.com/2227-9032/9/2/206/pdf?version=1614152367").toURL()),
                fetcher.findFullText(new BibEntry()
                        .withField(StandardField.URL, "https://openalex.org/W3130362418")));
    }

    @Test
    void getURLForQueryWithLucene() throws MalformedURLException, URISyntaxException {
        String query = "nerf";
        SearchQuery searchQueryObject = new SearchQuery(query);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
        URL url = fetcher.getURLForQuery(visitor.visitStart(searchQueryObject.getContext()));
        assertEquals("https://api.openalex.org/works?search=nerf", url.toString());
    }

    @Test
    void searchByQueryFindsEntry() throws FetcherException {
        BibEntry master = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Matthew Tancik and Vincent Casser and Xinchen Yan and Sabeek Pradhan and Ben Mildenhall and Pratul P. Srinivasan and Jonathan T. Barron and Henrik Kretzschmar")
                .withField(StandardField.TITLE, "Block-NeRF: Scalable Large Scene Neural View Synthesis")
                .withField(StandardField.YEAR, "2022")
                .withField(StandardField.DOI, "10.1109/cvpr52688.2022.00807")
                .withField(StandardField.URL, "https://openalex.org/W4312280420");
        List<BibEntry> fetchedEntries = fetcher.performSearch("Block-NeRF: Scalable Large Scene Neural View Synthesis");
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.PAGES));
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.KEYWORDS));
        assertEquals(master, fetchedEntries.getFirst());
    }

    @Test
    void performSearchByEmptyQuery() throws FetcherException {
        assertEquals(List.of(), fetcher.performSearch(""));
    }

    @Test
    void searchByQuotedQueryFindsEntry() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("\"Mega-NeRF: Scalable Construction of Large-Scale NeRFs for Virtual Fly- Throughs\"");
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.PAGES));
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.KEYWORDS));
        assertEquals(NERF, fetchedEntries.getFirst());
    }
}
