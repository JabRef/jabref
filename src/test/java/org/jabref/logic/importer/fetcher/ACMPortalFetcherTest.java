package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
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
        BibEntry expected = new BibEntry(StandardEntryType.Conference);

        expected.setField(StandardField.AUTHOR, "Tobias Olsson and Morgan Ericsson and Anna Wingkvist");
        expected.setField(StandardField.YEAR, "2017");
        expected.setField(StandardField.MONTH, "9");
        expected.setField(StandardField.DAY, "11");
        // expected.setField(StandardField.ABSTRACT, "The open source application JabRef has existed since 2003. In 2015, the developers decided to make an architectural refactoring as continued development was deemed too demanding. The developers also introduced Static Architecture Conformance Checking (SACC) to prevent violations to the intended architecture. Measurements mined from source code repositories such as code churn and code ownership has been linked to several problems, for example fault proneness, security vulnerabilities, code smells, and degraded maintainability. The root cause of such problems can be architectural. To determine the impact of the refactoring of JabRef, we measure the code churn and code ownership before and after the refactoring and find that large files with violations had a significantly higher code churn than large files without violations before the refactoring. After the refactoring, the files that had violations show a more normal code churn. We find no such effect on code ownership. We conclude that files that contain violations detectable by SACC methods are connected to higher than normal code churn.");
        expected.setField(StandardField.SERIES, "ECSA '17");
        expected.setField(StandardField.BOOKTITLE, "Proceedings of the 11th European Conference on Software Architecture: Companion Proceedings");
        expected.setField(StandardField.DOI, "10.1145/3129790.3129810");
        expected.setField(StandardField.LOCATION, "Canterbury, United Kingdom");
        expected.setField(StandardField.ISBN, "9781450352178");
        expected.setField(StandardField.KEYWORDS, "conformance checking, repository data mining, software architecture");
        expected.setField(StandardField.PUBLISHER, "Association for Computing Machinery");
        expected.setField(StandardField.ADDRESS, "New York, NY, USA");
        expected.setField(StandardField.TITLE, "The relationship of code churn and architectural violations in the open source software JabRef");
        expected.setField(StandardField.URL, "https://doi.org/10.1145/3129790.3129810");
        expected.setField(StandardField.PAGETOTAL, "7");
        expected.setField(StandardField.PAGES, "152–158");

        List<BibEntry> fetchedEntries = fetcher.performSearch("jabref architectural churn");
        BibEntry bibEntry = fetchedEntries.get(0);
        bibEntry.clearField(StandardField.ABSTRACT);
        assertEquals(Collections.singletonList(expected), Collections.singletonList(bibEntry));
    }

    @Test
    void testGetURLForQuery() throws FetcherException, MalformedURLException, URISyntaxException, QueryNodeParseException {
        String testQuery = "test query url";
        SyntaxParser parser = new StandardSyntaxParser();
        URL url = fetcher.getURLForQuery(parser.parse(testQuery, NO_EXPLICIT_FIELD));
        String expected = "https://dl.acm.org/action/doSearch?AllField=test%2Bquery%2Burl";
        assertEquals(expected, url.toString());
    }

    @Test
    void testGetParser() {
        ACMPortalParser expected = new ACMPortalParser();
        assertEquals(expected.getClass(), fetcher.getParser().getClass());
    }

}
