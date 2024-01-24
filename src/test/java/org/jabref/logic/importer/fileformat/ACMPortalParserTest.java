package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@FetcherTest
public class ACMPortalParserTest {

    ACMPortalParser parser;
    List<BibEntry> searchEntryList;
    URL searchUrl;
    String searchQuery = "The relationship of code churn and architectural violations in the open source software JabRef";
    String jsonStr = "{\"id\":\"10.1145/3129790.3129810\",\"type\":\"PAPER_CONFERENCE\",\"author\":[{\"family\":\"Olsson\",\"given\":\"Tobias\"},{\"family\":\"Ericsson\",\"given\":\"Morgan\"},{\"family\":\"Wingkvist\",\"given\":\"Anna\"}],\"accessed\":{\"date-parts\":[[2021,5,12]]},\"issued\":{\"date-parts\":[[2017,9,11]]},\"original-date\":{\"date-parts\":[[2017,9,11]]},\"abstract\":\"The open source application JabRef has existed since 2003. In 2015, the developers decided to make an architectural refactoring as continued development was deemed too demanding. The developers also introduced Static Architecture Conformance Checking (SACC) to prevent violations to the intended architecture. Measurements mined from source code repositories such as code churn and code ownership has been linked to several problems, for example fault proneness, security vulnerabilities, code smells, and degraded maintainability. The root cause of such problems can be architectural. To determine the impact of the refactoring of JabRef, we measure the code churn and code ownership before and after the refactoring and find that large files with violations had a significantly higher code churn than large files without violations before the refactoring. After the refactoring, the files that had violations show a more normal code churn. We find no such effect on code ownership. We conclude that files that contain violations detectable by SACC methods are connected to higher than normal code churn.\",\"call-number\":\"10.1145/3129790.3129810\",\"collection-title\":\"ECSA '17\",\"container-title\":\"Proceedings of the 11th European Conference on Software Architecture: Companion Proceedings\",\"DOI\":\"10.1145/3129790.3129810\",\"event-place\":\"Canterbury, United Kingdom\",\"ISBN\":\"9781450352178\",\"keyword\":\"software architecture, conformance checking, repository data mining\",\"number-of-pages\":\"7\",\"page\":\"152–158\",\"publisher\":\"Association for Computing Machinery\",\"publisher-place\":\"New York, NY, USA\",\"title\":\"The relationship of code churn and architectural violations in the open source software JabRef\",\"URL\":\"https://doi.org/10.1145/3129790.3129810\"}";

    @BeforeEach
    void setUp() throws URISyntaxException, MalformedURLException {
        parser = new ACMPortalParser();
        searchUrl = new URIBuilder("https://dl.acm.org/action/doSearch")
                .addParameter("AllField", searchQuery).build().toURL();
        searchEntryList = List.of(
                new BibEntry(StandardEntryType.Conference)
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
                        .withField(StandardField.PAGES, "152–158"),
                new BibEntry(StandardEntryType.Book)
                        .withField(StandardField.YEAR, "2016")
                        .withField(StandardField.MONTH, "10")
                        .withField(StandardField.TITLE, "Proceedings of the 2016 24th ACM SIGSOFT International Symposium on Foundations of Software Engineering")
                        .withField(StandardField.LOCATION, "Seattle, WA, USA")
                        .withField(StandardField.ISBN, "9781450342186")
                        .withField(StandardField.PUBLISHER, "Association for Computing Machinery")
                        .withField(StandardField.ADDRESS, "New York, NY, USA")
        );
    }

    @Test
    void parseEntries() throws IOException, ParseException {
        CookieHandler.setDefault(new CookieManager());
        List<BibEntry> bibEntries = parser.parseEntries(new URLDownload(searchUrl).asInputStream());
        for (BibEntry bibEntry : bibEntries) {
            bibEntry.clearField(StandardField.ABSTRACT);
        }
        assertEquals(Optional.of(searchEntryList.getFirst()), bibEntries.stream().findFirst());
    }

    @Test
    void parseDoiSearchPage() throws ParseException, IOException {
        String testDoi = "10.1145/3129790.3129810";
        CookieHandler.setDefault(new CookieManager());
        List<String> doiList = parser.parseDoiSearchPage(new URLDownload(searchUrl).asInputStream());
        assertFalse(doiList.isEmpty());
        assertEquals(testDoi, doiList.getFirst());
    }

    @Test
    void getBibEntriesFromDoiList() throws FetcherException {
        List<String> testDoiList = List.of("10.1145/3129790.3129810", "10.1145/2950290");
        List<BibEntry> bibEntries = parser.getBibEntriesFromDoiList(testDoiList);
        for (BibEntry bibEntry : bibEntries) {
            bibEntry.clearField(StandardField.ABSTRACT);
        }
        assertEquals(searchEntryList, bibEntries);
    }

    @Test
    void getUrlFromDoiList() throws MalformedURLException, URISyntaxException {
        String target = "https://dl.acm.org/action/exportCiteProcCitation?targetFile=custom-bibtex&format=bibTex&dois=10.1145%2F3129790.3129810%2C10.1145%2F2950290";

        List<String> doiList = List.of("10.1145/3129790.3129810", "10.1145/2950290");
        URL url = parser.getUrlFromDoiList(doiList);
        assertEquals(target, url.toString());
    }

    @Test
    void parseBibEntry() {
        BibEntry bibEntry = parser.parseBibEntry(jsonStr);
        bibEntry.clearField(StandardField.ABSTRACT);
        assertEquals(searchEntryList.getFirst(), bibEntry);
    }

    @Test
    void parseBibEntryWithFamilyAuthorOnly() {
        String json = "{\"id\":\"10.1145/3011077.3011113\",\"type\":\"PAPER_CONFERENCE\",\"author\":[{\"family\":\"Ngo-Thi-Thu-Trang\"},{\"family\":\"Bui\",\"given\":\"Hieu T.\"},{\"family\":\"Nguyen\",\"given\":\"Nhan D.\"}],\"accessed\":{\"date-parts\":[[2023,8,4]]},\"issued\":{\"date-parts\":[[2016,12,8]]},\"original-date\":{\"date-parts\":[[2016,12,8]]},\"abstract\":\"\",\"call-number\":\"10.1145/3011077.3011113\",\"collection-title\":\"SoICT '16\",\"container-title\":\"Proceedings of the 7th Symposium on Information and Communication Technology\",\"DOI\":\"10.1145/3011077.3011113\",\"event-place\":\"Ho Chi Minh City, Vietnam\",\"ISBN\":\"9781450348157\",\"keyword\":\"orthogonal frequency division multiplexing (OFDM), long-range passive optical network (LR PON), four-wave mixing (FWM), wavelength division multiplexing (WDM)\",\"number-of-pages\":\"6\",\"page\":\"216–221\",\"publisher\":\"Association for Computing Machinery\",\"publisher-place\":\"New York, NY, USA\",\"title\":\"A simple performance analysis of IM-DD OFDM WDM systems in long range PON application\",\"URL\":\"https://doi.org/10.1145/3011077.3011113\"}";
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Conference)
            .withField(StandardField.AUTHOR, "Ngo-Thi-Thu-Trang and Bui, Hieu T. and Nguyen, Nhan D.")
            .withField(StandardField.TITLE, "A simple performance analysis of IM-DD OFDM WDM systems in long range PON application")
            .withField(StandardField.BOOKTITLE, "Proceedings of the 7th Symposium on Information and Communication Technology")
            .withField(StandardField.YEAR, "2016")
            .withField(StandardField.SERIES, "SoICT '16")
            .withField(StandardField.PAGES, "216–221")
            .withField(StandardField.ADDRESS, "New York, NY, USA")
            .withField(StandardField.MONTH, "12")
            .withField(StandardField.PUBLISHER, "Association for Computing Machinery")
            .withField(StandardField.LOCATION, "Ho Chi Minh City, Vietnam")
            .withField(StandardField.ISBN, "9781450348157")
            .withField(StandardField.DAY, "8")
            .withField(StandardField.PAGETOTAL, "6")
            .withField(StandardField.DOI, "10.1145/3011077.3011113")
            .withField(StandardField.KEYWORDS, "four-wave mixing (FWM), long-range passive optical network (LR PON), orthogonal frequency division multiplexing (OFDM), wavelength division multiplexing (WDM)")
            .withField(StandardField.URL, "https://doi.org/10.1145/3011077.3011113");

        BibEntry parsedEntry = parser.parseBibEntry(json);
        assertEquals(expectedEntry, parsedEntry);
    }

    @Test
    void noEntryFound() throws URISyntaxException, IOException, ParseException {
        CookieHandler.setDefault(new CookieManager());
        URL url = new URIBuilder("https://dl.acm.org/action/doSearch?AllField=10.1145/3129790.31298").build().toURL();
        List<BibEntry> bibEntries = parser.parseEntries(new URLDownload(url).asInputStream());
        assertEquals(Collections.emptyList(), bibEntries);
    }
}
