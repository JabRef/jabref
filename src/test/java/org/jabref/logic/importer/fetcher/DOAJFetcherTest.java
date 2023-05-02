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
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class DOAJFetcherTest {

    DOAJFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        fetcher = new DOAJFetcher(importFormatPreferences);
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
        .withField(StandardField.AUTHOR, "Nísea de A. Corrêa and Maria P. Foss and Paula R. B. Diniz")
        .withField(StandardField.DOI, "10.11606/issn.2176-7262.v49i6p533-548")
        .withField(StandardField.ISSN, "2176-7262")
        .withField(StandardField.JOURNAL, "Medicina")
        .withField(StandardField.PUBLISHER, "Universidade de São Paulo")
        .withField(StandardField.TITLE, "Structural and functional changes related to memory deficit in non-demential elderly individuals")
        .withField(StandardField.URL, "http://www.revistas.usp.br/rmrp/article/view/127443")
        .withField(StandardField.VOLUME, "49")
        .withField(StandardField.NUMBER, "6")
        .withField(StandardField.YEAR, "2016")
        .withField(StandardField.MONTH, "December")
        .withField(StandardField.KEYWORDS, "Central Nervous System. Structural Changes. Functional Changes. Memory deficits. Aging. Normal Aging. Magnetic Resonance Imaging")
        .withField(StandardField.ABSTRACT, "Objective: Based on Magnetic Resonance Imaging (MRI), verify the structural and functional changes related to memory deficits in non-demented elderly individuals in comparison with young adults. Methodology: Proceeded a systematic review based on Preferred Reporting Items for Systematic Review and Meta-Analysis (PRISMA) fluxogram. The search was done on PubMed, Scopus and EBSCO databases using JabRef 2.10, and Web of Science. It was included in the analysis quasi-experimental, cross-sectional, cohort and case-control studies published between 2005 and 2014 in national and international indexed periodicals that had as sample: non-demented individuals older than 60 years old, who were submitted to MRI investigation of their for any brain structural and functional changes associated with memory deficits, identified in neuropsychologicals tests. Results: About the imaging technique, we reviewed studies that used structural MRIs (two articles), functional MRIs (six articles), both techniques (four articles). In the 12 studies, 38 distinct neuropsychological tests were used, an average of five different tests for each study (variation of 1-12). The most used tests were WAIS Digit Span Backwards (seven articles), Trail Making Test A and B (four articles) and Wechsler Memory Scale (four articles). Conclusion: The review showed that in normal aging the parahippocampal white substance, the hippocampus volume and entorhinal cortex slightly shrink, causing verbal memory reduction, possibly due to fiber demyelination; reduced connections between temporal and frontal lobes contributing to an impairement of episodic, working memory and verbal fluency; reduction suppression of irrelevant information, affecting the register of information; changes on frontal and parietal areas, compromising recognition memory; modifications in activity and connectivity of the default mode network; reorganization of cognitive functions and also a slower response, probably due to reduction of pre-frontal cortex activation");

        List<BibEntry> fetchedEntries = fetcher.performSearch("JabRef MRI");
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
