package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class DOAJFetcherTest {

    DOAJFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        fetcher = new DOAJFetcher(importFormatPreferences);
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article);
        expected.setField(StandardField.AUTHOR, "Wei Wang and Yun He and Tong Li and Jiajun Zhu and Jinzhuo Liu");
        expected.setField(StandardField.DOI, "10.1155/2018/5913634");
        expected.setField(StandardField.ISSN, "1875-919X");
        expected.setField(StandardField.JOURNAL, "Scientific Programming");
        expected.setField(StandardField.PUBLISHER, "Hindawi Limited");
        expected.setField(StandardField.TITLE, "An Integrated Model for Information Retrieval Based Change Impact Analysis");
        expected.setField(StandardField.URL, "http://dx.doi.org/10.1155/2018/5913634");
        expected.setField(StandardField.VOLUME, "2018");
        expected.setField(StandardField.YEAR, "2018");
        expected.setField(StandardField.ABSTRACT, "The paper presents an approach to combine multiple existing information retrieval (IR) techniques to support change impact analysis, which seeks to identify the possible outcomes of a change or determine the necessary modifications for affecting a desired change. The approach integrates a bag-of-words based IR technique, where each class or method is abstracted as a set of words, and a neural network based IR technique to derive conceptual couplings from the source code of a software system. We report rigorous empirical assessments of the changes of three open source systems: jEdit, muCommander, and JabRef. The impact sets obtained are evaluated at the method level of granularity, and the results show that our integrated approach provides statistically significant improvements in accuracy across several cut points relative to the accuracies provided by the individual methods employed independently. Improvements in F-score values of up to 7.3%, 10.9%, and 17.3% are obtained over a baseline technique for jEdit, muCommander, and JabRef, respectively.");

        List<BibEntry> fetchedEntries = fetcher.performSearch("JabRef impact");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }

    @Test
    void testBibJSONConverter() {
        String jsonString = "{\"title\":\"Design of Finite Word Length Linear-Phase FIR Filters in the Logarithmic Number System Domain\",\"journal\":{\"publisher\":\"Hindawi Publishing Corporation\",\"language\":[\"English\"],\"title\":\"VLSI Design\",\"country\":\"US\",\"volume\":\"2014\"},\"author\":[{\"name\":\"Syed Asad Alam\"},{\"name\":\"Oscar Gustafsson\"}],\"link\":[{\"url\":\"http://dx.doi.org/10.1155/2014/217495\",\"type\":\"fulltext\"}],\"year\":\"2014\",\"identifier\":[{\"type\":\"pissn\",\"id\":\"1065-514X\"},{\"type\":\"eissn\",\"id\":\"1563-5171\"},{\"type\":\"doi\",\"id\":\"10.1155/2014/217495\"}],\"created_date\":\"2014-05-09T19:38:31Z\"}";
        JSONObject jsonObject = new JSONObject(jsonString);
        BibEntry bibEntry = DOAJFetcher.parseBibJSONtoBibtex(jsonObject, ',');

        assertEquals(StandardEntryType.Article, bibEntry.getType());
        assertEquals(Optional.of("VLSI Design"), bibEntry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("10.1155/2014/217495"), bibEntry.getField(StandardField.DOI));
        assertEquals(Optional.of("Syed Asad Alam and Oscar Gustafsson"), bibEntry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Design of Finite Word Length Linear-Phase FIR Filters in the Logarithmic Number System Domain"), bibEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("2014"), bibEntry.getField(StandardField.YEAR));
    }

    @Test
    public void searchByEmptyQuery() throws Exception {
        assertEquals(Collections.emptyList(), fetcher.performSearch(""));
    }

    @Test
    void appendSingleWord() throws Exception {
        URIBuilder builder = new URIBuilder("http://example.com/test");
        DOAJFetcher.addPath(builder, "/example");
        assertEquals("http://example.com/test/example", builder.build().toASCIIString());
    }

    @Test
    void appendSingleWordWithSlash() throws Exception {
        URIBuilder builder = new URIBuilder("http://example.com/test");
        DOAJFetcher.addPath(builder, "/example");
        assertEquals("http://example.com/test/example", builder.build().toASCIIString());
    }

    @Test
    void appendSlash() throws Exception {
        URIBuilder builder = new URIBuilder("http://example.com/test");
        DOAJFetcher.addPath(builder, "/");
        assertEquals("http://example.com/test", builder.build().toASCIIString());
    }

    @Test
    void appendTwoWords() throws Exception {
        URIBuilder builder = new URIBuilder("http://example.com/test");
        DOAJFetcher.addPath(builder, "example two");
        assertEquals("http://example.com/test/example%20two", builder.build().toASCIIString());
    }
}
