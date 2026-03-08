package org.jabref.logic.importer.fetcher.citation;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllCitationFetcherTest {

    private CitationFetcher successfulFetcher;
    private CitationFetcher failingFetcher;
    private AllCitationFetcher allFetcher;

    @BeforeEach
    void setUp() {
        successfulFetcher = mock(CitationFetcher.class);
        failingFetcher = mock(CitationFetcher.class);
        when(successfulFetcher.getName()).thenReturn("SuccessfulFetcher");
        when(failingFetcher.getName()).thenReturn("FailingFetcher");
        allFetcher = new AllCitationFetcher(List.of(successfulFetcher, failingFetcher));
    }

    @Test
    void toleratesPartialProviderFailure() throws Exception {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1000/test");

        when(failingFetcher.getReferences(entry)).thenThrow(new FetcherException("Failure"));
        when(successfulFetcher.getReferences(entry)).thenReturn(List.of(new BibEntry()));

        List<BibEntry> result = allFetcher.getReferences(entry);

        assertEquals(1, result.size());
    }

    @Test
    void throwsWhenAllProvidersFail() throws Exception {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1000/test");

        when(failingFetcher.getReferences(entry)).thenThrow(new FetcherException("Failure 1"));
        when(successfulFetcher.getReferences(entry)).thenThrow(new FetcherException("Failure 2"));

        assertThrows(FetcherException.class, () -> allFetcher.getReferences(entry));
    }

    @Test
    void removesDuplicateEntries() throws Exception {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1000/test");

        BibEntry duplicate1 = new BibEntry().withField(StandardField.DOI, "10.1234/dup");
        BibEntry duplicate2 = new BibEntry().withField(StandardField.DOI, "10.1234/dup");

        when(successfulFetcher.getReferences(entry)).thenReturn(List.of(duplicate1));
        when(failingFetcher.getReferences(entry)).thenReturn(List.of(duplicate2));

        List<BibEntry> result = allFetcher.getReferences(entry);

        assertEquals(1, result.size());
    }

    @Test
    void returnsMaximumCitationCount() throws Exception {
        BibEntry entry = new BibEntry();

        when(successfulFetcher.getCitationCount(entry)).thenReturn(Optional.of(5));
        when(failingFetcher.getCitationCount(entry)).thenReturn(Optional.of(20));

        assertEquals(Optional.of(20), allFetcher.getCitationCount(entry));
    }

    @Test
    void throwsWhenAllCitationCountProvidersFail() throws Exception {
        BibEntry entry = new BibEntry();

        when(successfulFetcher.getCitationCount(entry)).thenThrow(new FetcherException("Failure 1"));
        when(failingFetcher.getCitationCount(entry)).thenThrow(new FetcherException("Failure 2"));

        assertThrows(FetcherException.class, () -> allFetcher.getCitationCount(entry));
    }
}
