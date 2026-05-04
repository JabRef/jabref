package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fileformat.ACMPortalParser;
import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
@DisabledOnCIServer("ACM replied with 403 Forbidden on 2025-03-17")
class ACMPortalFetcherTest {
    ACMPortalFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new ACMPortalFetcher();
    }

    @Test
    void searchByQueryFindsEntry() throws FetcherException {
        BibEntry searchEntry = new BibEntry(StandardEntryType.Conference)
                .withField(StandardField.AUTHOR, "Olsson, Tobias and Ericsson, Morgan and Wingkvist, Anna")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.MONTH, "9")
                .withField(StandardField.DAY, "11")
                .withField(StandardField.SERIES, "ECSA '17")
                .withField(StandardField.BOOKTITLE, "Proceedings of the 11th European Conference on Software Architecture: Companion Proceedings")
                .withField(StandardField.DOI, "10.1145/3129790.3129810")
                .withField(StandardField.LOCATION, "Canterbury, United Kingdom")
                .withField(StandardField.ISBN, "9781450352178")
                .withField(StandardField.KEYWORDS, "conformance checking, repository data mining, software architecture")
                .withField(StandardField.PUBLISHER, "Association for Computing Machinery")
                .withField(StandardField.ADDRESS, "New York, NY, USA")
                .withField(StandardField.TITLE, "The relationship of code churn and architectural violations in the open source software JabRef")
                .withField(StandardField.URL, "https://doi.org/10.1145/3129790.3129810")
                .withField(StandardField.PAGETOTAL, "7")
                .withField(StandardField.PAGES, "152–158");

        List<BibEntry> fetchedEntries = fetcher.performSearch("The relationship of code churn and architectural violations in the open source software JabRef");
        // we clear the abstract due to copyright reasons (JabRef's code should not contain copyrighted abstracts)
        for (BibEntry bibEntry : fetchedEntries) {
            bibEntry.clearField(StandardField.ABSTRACT);
        }
        assertEquals(Optional.of(searchEntry), fetchedEntries.stream().findFirst());
    }

    @Test
    void getURLForQuery() throws MalformedURLException, URISyntaxException {
        String testQuery = "test query url";
        SearchQuery searchQueryObject = new SearchQuery(testQuery);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
        URL url = fetcher.getURLForQuery(visitor.visitStart(searchQueryObject.getContext()));
        String expected = "https://dl.acm.org/action/doSearch?AllField=test%20query%20url";
        assertEquals(expected, url.toString());
    }

    @Test
    void getParser() {
        ACMPortalParser expected = new ACMPortalParser();
        assertEquals(expected.getClass(), fetcher.getParser().getClass());
    }
}
