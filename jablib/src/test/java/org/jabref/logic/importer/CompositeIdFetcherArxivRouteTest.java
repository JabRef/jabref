// src/test/java/org/jabref/logic/importer/CompositeIdFetcher_ArxivRouteTest.java
package org.jabref.logic.importer;

import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.INSPIREFetcher;
import org.jabref.model.entry.BibEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeIdFetcherArxivRouteTest {

    @Test
    void inspireHit_noFallback() throws Exception {
        ImportFormatPreferences prefs = mock(ImportFormatPreferences.class);
        BibEntry hit = new BibEntry();

        try (MockedConstruction<INSPIREFetcher> iCons =
                 mockConstruction(INSPIREFetcher.class, (m, c) ->
                     when(m.performSearchById("arXiv:2101.00001")).thenReturn(Optional.of(hit)));
             MockedConstruction<ArXivFetcher> aCons =
                 mockConstruction(ArXivFetcher.class)) {

            var fetcher = new CompositeIdFetcher(prefs);
            var out = fetcher.performSearchById("arXiv:2101.00001");
            assertThat(out).containsSame(hit);
            assertThat(aCons.constructed()).isEmpty();
        }
    }

    @Test
    void inspireThrows_fallbackToArxiv() throws Exception {
        ImportFormatPreferences prefs = mock(ImportFormatPreferences.class);
        BibEntry fallback = new BibEntry();

        try (MockedConstruction<INSPIREFetcher> iCons =
                 mockConstruction(INSPIREFetcher.class, (m, c) ->
                     when(m.performSearchById(anyString())).thenThrow(new FetcherException("boom")));
             MockedConstruction<ArXivFetcher> aCons =
                 mockConstruction(ArXivFetcher.class, (m, c) ->
                     when(m.performSearchById(anyString())).thenReturn(Optional.of(fallback)))) {

            var fetcher = new CompositeIdFetcher(prefs);
            var out = fetcher.performSearchById("arXiv:2101.00001");
            assertThat(out).containsSame(fallback);
        }
    }

    @Test
    void inspireEmpty_fallbackToArxiv() throws Exception {
        ImportFormatPreferences prefs = mock(ImportFormatPreferences.class);
        BibEntry fallback = new BibEntry();

        try (MockedConstruction<INSPIREFetcher> iCons =
                 mockConstruction(INSPIREFetcher.class, (m, c) ->
                     when(m.performSearchById(anyString())).thenReturn(Optional.empty()));
             MockedConstruction<ArXivFetcher> aCons =
                 mockConstruction(ArXivFetcher.class, (m, c) ->
                     when(m.performSearchById(anyString())).thenReturn(Optional.of(fallback)))) {

            var fetcher = new CompositeIdFetcher(prefs);
            var out = fetcher.performSearchById("arXiv:2101.00001");
            assertThat(out).containsSame(fallback);
        }
    }
}


