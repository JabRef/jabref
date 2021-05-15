package org.jabref.logic.importer;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jabref.logic.importer.fileformat.ACMPortalParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.microsoft.applicationinsights.core.dependencies.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ACMPortalParserTest {

    ACMPortalParser parser;
    List<BibEntry> searchEntryList;
    URL searchUrl;
    String searchDoi = "10.1145/3129790.3129810";
    String jsonStr = "{\"id\":\"10.1145/3129790.3129810\",\"type\":\"PAPER_CONFERENCE\",\"author\":[{\"family\":\"Olsson\",\"given\":\"Tobias\"},{\"family\":\"Ericsson\",\"given\":\"Morgan\"},{\"family\":\"Wingkvist\",\"given\":\"Anna\"}],\"accessed\":{\"date-parts\":[[2021,5,12]]},\"issued\":{\"date-parts\":[[2017,9,11]]},\"original-date\":{\"date-parts\":[[2017,9,11]]},\"abstract\":\"The open source application JabRef has existed since 2003. In 2015, the developers decided to make an architectural refactoring as continued development was deemed too demanding. The developers also introduced Static Architecture Conformance Checking (SACC) to prevent violations to the intended architecture. Measurements mined from source code repositories such as code churn and code ownership has been linked to several problems, for example fault proneness, security vulnerabilities, code smells, and degraded maintainability. The root cause of such problems can be architectural. To determine the impact of the refactoring of JabRef, we measure the code churn and code ownership before and after the refactoring and find that large files with violations had a significantly higher code churn than large files without violations before the refactoring. After the refactoring, the files that had violations show a more normal code churn. We find no such effect on code ownership. We conclude that files that contain violations detectable by SACC methods are connected to higher than normal code churn.\",\"call-number\":\"10.1145/3129790.3129810\",\"collection-title\":\"ECSA '17\",\"container-title\":\"Proceedings of the 11th European Conference on Software Architecture: Companion Proceedings\",\"DOI\":\"10.1145/3129790.3129810\",\"event-place\":\"Canterbury, United Kingdom\",\"ISBN\":\"9781450352178\",\"keyword\":\"software architecture, conformance checking, repository data mining\",\"number-of-pages\":\"7\",\"page\":\"152–158\",\"publisher\":\"Association for Computing Machinery\",\"publisher-place\":\"New York, NY, USA\",\"title\":\"The relationship of code churn and architectural violations in the open source software JabRef\",\"URL\":\"https://doi.org/10.1145/3129790.3129810\"}";

    @BeforeEach
    void setUp() throws URISyntaxException, MalformedURLException {
        parser = new ACMPortalParser();
        searchUrl = new URIBuilder("https://dl.acm.org/action/doSearch?AllField=" + searchDoi).build().toURL();
        searchEntryList = new ArrayList<>();

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

        searchEntryList.add(expected);
    }

    @Test
    void testParseEntries() throws IOException, ParseException {
        CookieHandler.setDefault(new CookieManager());
        List<BibEntry> bibEntries = parser.parseEntries(new URLDownload(searchUrl).asInputStream());
        for (BibEntry bibEntry : bibEntries) {
            bibEntry.clearField(StandardField.ABSTRACT);
        }
        assertEquals(Collections.singletonList(searchEntryList), Collections.singletonList(bibEntries));
    }

    @Test
    void testParseDoiSearchPage() throws ParseException, IOException {
        List<String> testDoiList = new LinkedList<>();
        testDoiList.add(searchDoi);

        CookieHandler.setDefault(new CookieManager());
        List<String> doiList = parser.parseDoiSearchPage(new URLDownload(searchUrl).asInputStream());
        assertEquals(testDoiList, doiList);
    }

    @Test
    void testGetBibEntriesFromDoiList() throws FetcherException {
        List<String> testDoiList = new LinkedList<>();
        testDoiList.add(searchDoi);
        List<BibEntry> bibEntries = parser.getBibEntriesFromDoiList(testDoiList);
        for (BibEntry bibEntry : bibEntries) {
            bibEntry.clearField(StandardField.ABSTRACT);
        }
        assertEquals(Collections.singletonList(searchEntryList), Collections.singletonList(bibEntries));
    }

    @Test
    void testGetUrlFromDoiList() throws MalformedURLException, URISyntaxException {
        String target = "https://dl.acm.org/action/exportCiteProcCitation?targetFile=custom-bibtex&format=bibTex&dois=10.1145%2F3129790.3129810%2C10.1145%2F3129790.3129811";

        List<String> doiList = new ArrayList<>();
        doiList.add("10.1145/3129790.3129810");
        doiList.add("10.1145/3129790.3129811");
        URL url = parser.getUrlFromDoiList(doiList);
        assertEquals(target, url.toString());
    }

    @Test
    void testParseBibEntry() {
        BibEntry bibEntry = parser.parseBibEntry(jsonStr);
        bibEntry.clearField(StandardField.ABSTRACT);
        assertEquals(searchEntryList.get(0), bibEntry);
    }

    @Test
    void testNoEntryFound() throws URISyntaxException, IOException, ParseException {
        CookieHandler.setDefault(new CookieManager());
        URL url = new URIBuilder("https://dl.acm.org/action/doSearch?AllField=10.1145/3129790.31298").build().toURL();
        List<BibEntry> bibEntries = parser.parseEntries(new URLDownload(url).asInputStream());
        assertEquals(Collections.emptyList(), bibEntries);
    }
}
