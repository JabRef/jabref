package org.jabref.logic.importer.fetcher.citation;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllCitationFetcherTest {

    private CitationFetcher fetcher1;
    private CitationFetcher fetcher2;
    private AllCitationFetcher allFetcher;

    @BeforeEach
    void setUp() {
        fetcher1 = mock(CitationFetcher.class);
        fetcher2 = mock(CitationFetcher.class);
        when(fetcher1.getName()).thenReturn("MockFetcher1");
        when(fetcher2.getName()).thenReturn("MockFetcher2");
        allFetcher = new AllCitationFetcher(List.of(fetcher1, fetcher2));
    }

    @Test
    void toleratesProviderFailure() throws Exception {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1000/test");

        when(fetcher1.getReferences(entry)).thenThrow(new FetcherException("Failure"));
        when(fetcher2.getReferences(entry)).thenReturn(List.of(new BibEntry()));

        List<BibEntry> result = allFetcher.getReferences(entry);

        assertEquals(1, result.size());
    }

    @Test
    void removesDuplicateEntries() throws Exception {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1000/test");

        BibEntry duplicate1 = new BibEntry().withField(StandardField.DOI, "10.1234/dup");
        BibEntry duplicate2 = new BibEntry().withField(StandardField.DOI, "10.1234/dup");

        when(fetcher1.getReferences(entry)).thenReturn(List.of(duplicate1));
        when(fetcher2.getReferences(entry)).thenReturn(List.of(duplicate2));

        List<BibEntry> result = allFetcher.getReferences(entry);

        assertEquals(1, result.size());
    }

    @Test
    void returnsMaximumCitationCount() throws Exception {
        BibEntry entry = new BibEntry();

        when(fetcher1.getCitationCount(entry)).thenReturn(Optional.of(5));
        when(fetcher2.getCitationCount(entry)).thenReturn(Optional.of(20));

        Optional<Integer> result = allFetcher.getCitationCount(entry);

        assertTrue(result.isPresent());
        assertEquals(20, result.get());
    }
}
