package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fileformat.ACMPortalParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.logic.importer.fetcher.transformers.AbstractQueryTransformer.NO_EXPLICIT_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class ACMPortalFetcherTest {
    ACMPortalFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new ACMPortalFetcher();
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {

        List<BibEntry> searchEntryList = List.of(
                new BibEntry(StandardEntryType.Conference)
                        .withField(StandardField.AUTHOR, "Tobias Olsson and Morgan Ericsson and Anna Wingkvist")
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
                        .withField(StandardField.PAGES, "152–158"),
                new BibEntry(StandardEntryType.Book)
                        .withField(StandardField.YEAR, "2016")
                        .withField(StandardField.TITLE, "Proceedings of the 2016 24th ACM SIGSOFT International Symposium on Foundations of Software Engineering")
                        .withField(StandardField.LOCATION, "Seattle, WA, USA")
                        .withField(StandardField.ISBN, "9781450342186")
                        .withField(StandardField.PUBLISHER, "Association for Computing Machinery")
                        .withField(StandardField.ADDRESS, "New York, NY, USA")
        );

        List<BibEntry> fetchedEntries = fetcher.performSearch("The relationship of code churn and architectural violations in the open source software JabRef");
        // we clear the abstract due to copyright reasons (JabRef's code should not contain copyrighted abstracts)
        for (BibEntry bibEntry : fetchedEntries) {
            bibEntry.clearField(StandardField.ABSTRACT);
        }
        // check for a) that a single entry is returned and b) that the entry itself is the expected one
        assertEquals(searchEntryList, fetchedEntries);
    }

    @Test
    void testGetURLForQuery() throws FetcherException, MalformedURLException, URISyntaxException, QueryNodeParseException {
        String testQuery = "test query url";
        SyntaxParser parser = new StandardSyntaxParser();
        URL url = fetcher.getURLForQuery(parser.parse(testQuery, NO_EXPLICIT_FIELD));
        String expected = "https://dl.acm.org/action/doSearch?AllField=test+query+url";
        assertEquals(expected, url.toString());
    }

    @Test
    void testGetParser() {
        ACMPortalParser expected = new ACMPortalParser();
        assertEquals(expected.getClass(), fetcher.getParser().getClass());
    }

}
