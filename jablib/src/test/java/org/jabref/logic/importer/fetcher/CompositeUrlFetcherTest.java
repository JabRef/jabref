package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.CompositeUrlFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.ExternalServicesTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeUrlFetcherTest {

    private CompositeUrlFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        fetcher = new CompositeUrlFetcher(importFormatPreferences);
    }

    @Test
    @ExternalServicesTest
    void acmEpdfUrlRoutesThroughDoiFetcher() throws FetcherException {
        String url = "https://dl.acm.org/doi/epdf/10.1145/3651640.3651646";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(Optional.of("10.1145/3651640.3651646"), entry.getField(StandardField.DOI));
        assertEquals(Optional.of("Towards a Globally Distributed Testing Network in the Automotive Industry"), entry.getField(StandardField.TITLE));
    }

    @Test
    @ExternalServicesTest
    void arxivUrlRoutesThroughArXivFetcher() throws FetcherException {
        String url = "https://arxiv.org/abs/2110.02957";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(Optional.of("2110.02957"), entry.getField(StandardField.EPRINT));
        assertEquals(Optional.of("Reading the CARDs: the Imprint of Accretion History in the Chemical Abundances of the Milky Way's Stellar Halo"), entry.getField(StandardField.TITLE));
    }

    @Test
    @ExternalServicesTest
    void nonExistentDoiUrlFallsBackToMisc() throws FetcherException {
        String url = "https://doi.org/10.1000/182notfound123456789";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(Optional.of(url), entry.getField(StandardField.URL));
    }

    @Test
    @ExternalServicesTest
    void dxDoiOrgWithUnrecognizedIdentifierFallsBackToMisc() throws FetcherException {
        // http://dx.doi.org/10.1.1.99.3548 contains no recognized identifier format, so it falls back to GenericUrlBasedFetcher.
        String url = "http://dx.doi.org/10.1.1.99.3548";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(Optional.of(url), entry.getField(StandardField.URL));
    }

    @Test
    @ExternalServicesTest
    void giRadarUrlCreatesMiscEntryWithUrl() throws FetcherException {
        String url = "https://gi-radar.de/397-coding-unterstuetzung-im-lauf-der-zeit/";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(Optional.of(url), entry.getField(StandardField.URL));
        assertNotEquals(Optional.of(url), entry.getField(StandardField.TITLE));
    }

    @Test
    @ExternalServicesTest
    void semanticScholarPaperUrlCreatesMiscEntry() throws FetcherException {
        String url = "https://www.semanticscholar.org/paper/Logic-LangChain%3A-Translating-Natural-Language-to-Lalwani-Lunawat/2ad8dbc163ce289e23e9c02b324c82d9c2fe8190";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(Optional.of(url), entry.getField(StandardField.URL));
    }

    @Test
    @ExternalServicesTest
    void semanticScholarCorpusIdUrlCreatesMiscEntry() throws FetcherException {
        String url = "https://api.semanticscholar.org/CorpusID:269305101";

        List<BibEntry> result = fetcher.performSearch(url);

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(Optional.of(url), entry.getField(StandardField.URL));
    }

    @Test
    void nonUrlStringThrowsFetcherException() {
        assertThrows(FetcherException.class, () -> fetcher.performSearch("not a url at all"));
    }

    @Test
    @ExternalServicesTest
    void urlWithSurroundingWhitespaceIsTrimmed() throws FetcherException {
        String url = "https://this-host-should-not-resolve.jabref-test.invalid/some-page";

        List<BibEntry> result = fetcher.performSearch("  " + url + "  ");

        assertEquals(1, result.size());
        BibEntry entry = result.getFirst();
        assertEquals(Optional.of(url), entry.getField(StandardField.URL));
    }
}
