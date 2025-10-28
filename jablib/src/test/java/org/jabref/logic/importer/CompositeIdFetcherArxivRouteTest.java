// src/test/java/org/jabref/logic/importer/CompositeIdFetcherArxivRouteTest.java
package org.jabref.logic.importer;

import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.INSPIREFetcher;
import org.jabref.model.entry.BibEntry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests the arXiv routing logic of CompositeIdFetcher using Mockito
 * to mock fetcher construction and verify routing decisions.
 */
class CompositeIdFetcherArxivRouteTest {

    @Test
    void inspireHit_noFallback() throws Exception {
        var prefs = new ImportFormatPreferences(null, null, null, null, null, null, null);
        var hit = new BibEntry();

        try (var inspireCtor = mockConstruction(INSPIREFetcher.class, (mock, ctx) ->
                 when(mock.performSearchById(anyString())).thenReturn(Optional.of(hit)));
             var arxivCtor = mockConstruction(ArXivFetcher.class)) {

            var fetcher = new CompositeIdFetcher(prefs);

            var out = fetcher.performSearchById("arXiv:2101.00001");

            assertTrue(out.isPresent());
            assertEquals(hit, out.get());

            // Verify ArXiv never used
            assertEquals(0, arxivCtor.constructed().size());
        }
    }

    @Test
    void inspireThrows_fallbackToArxiv() throws Exception {
        var prefs = new ImportFormatPreferences(null, null, null, null, null, null, null);
        var fallback = new BibEntry();

        try (var inspireCtor = mockConstruction(INSPIREFetcher.class, (mock, ctx) ->
                 when(mock.performSearchById(anyString())).thenThrow(new FetcherException("boom")));
             var arxivCtor = mockConstruction(ArXivFetcher.class, (mock, ctx) ->
                 when(mock.performSearchById(anyString())).thenReturn(Optional.of(fallback)))) {

            var fetcher = new CompositeIdFetcher(prefs);

            var out = fetcher.performSearchById("arXiv:2101.00001");

            assertTrue(out.isPresent());
            assertEquals(fallback, out.get());
            assertEquals(1, arxivCtor.constructed().size());
        }
    }

    @Test
    void inspireEmpty_fallbackToArxiv() throws Exception {
        var prefs = new ImportFormatPreferences(null, null, null, null, null, null, null);
        var fallback = new BibEntry();

        try (var inspireCtor = mockConstruction(INSPIREFetcher.class, (mock, ctx) ->
                 when(mock.performSearchById(anyString())).thenReturn(Optional.empty()));
             var arxivCtor = mockConstruction(ArXivFetcher.class, (mock, ctx) ->
                 when(mock.performSearchById(anyString())).thenReturn(Optional.of(fallback)))) {

            var fetcher = new CompositeIdFetcher(prefs);

            var out = fetcher.performSearchById("arXiv:2101.00001");

            assertTrue(out.isPresent());
            assertEquals(fallback, out.get());
            assertEquals(1, arxivCtor.constructed().size());
        }
    }
}


