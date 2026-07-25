package org.jabref.logic.crawler;

import java.util.List;
import java.util.Map;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;
import org.jabref.model.study.QueryResult;
import org.jabref.model.study.StudyQuery;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class StudyFetcherTest {

    @Test
    void catalogSpecificOverrideUsesRawPathForPagedFetcher() throws FetcherException {
        StudyQuery studyQuery = new StudyQuery("machine learning");
        studyQuery.setCatalogSpecific(Map.of("TestPagedFetcher", "native:query"));

        PagedSearchBasedFetcher pagedFetcher = mock(PagedSearchBasedFetcher.class);
        when(pagedFetcher.getName()).thenReturn("TestPagedFetcher");
        when(pagedFetcher.getPageSize()).thenReturn(10);
        when(pagedFetcher.performRawSearchQueryPaged(anyString(), anyInt()))
                .thenReturn(new Page<>("native:query", 0, List.of()));
        when(pagedFetcher.performRawSearchQueryPaged("native:query", 0))
                .thenReturn(new Page<>("native:query", 0, List.of(new BibEntry())));

        StudyFetcher studyFetcher = new StudyFetcher(List.of(pagedFetcher), List.of(studyQuery), Map.of());
        List<QueryResult> results = studyFetcher.crawl();

        assertFalse(results.isEmpty());
        verify(pagedFetcher).performRawSearchQueryPaged("native:query", 0);
        verify(pagedFetcher, times(10)).performRawSearchQueryPaged(eq("native:query"), anyInt());
        verify(pagedFetcher, never()).performSearchPaged(anyString(), anyInt());
    }

    @Test
    void catalogSpecificOverrideUsesRawPathForPlainFetcher() throws FetcherException {
        StudyQuery studyQuery = new StudyQuery("machine learning");
        studyQuery.setCatalogSpecific(Map.of("TestPlainFetcher", "native:query"));

        SearchBasedFetcher plainFetcher = mock(SearchBasedFetcher.class);
        when(plainFetcher.getName()).thenReturn("TestPlainFetcher");
        when(plainFetcher.performRawSearchQuery("native:query")).thenReturn(List.of(new BibEntry()));

        StudyFetcher studyFetcher = new StudyFetcher(List.of(plainFetcher), List.of(studyQuery), Map.of());
        List<QueryResult> results = studyFetcher.crawl();

        assertFalse(results.isEmpty());
        verify(plainFetcher).performRawSearchQuery("native:query");
        verify(plainFetcher, never()).performSearch(anyString());
    }

    @Test
    void noOverrideFallsBackToStandardPath() throws FetcherException {
        StudyQuery studyQuery = new StudyQuery("machine learning");

        PagedSearchBasedFetcher pagedFetcher = mock(PagedSearchBasedFetcher.class);
        when(pagedFetcher.getName()).thenReturn("TestPagedFetcher");
        when(pagedFetcher.getPageSize()).thenReturn(10);
        when(pagedFetcher.performSearchPaged(anyString(), anyInt()))
                .thenReturn(new Page<>("machine learning", 0, List.of()));
        when(pagedFetcher.performSearchPaged("machine learning", 0))
                .thenReturn(new Page<>("machine learning", 0, List.of(new BibEntry())));

        StudyFetcher studyFetcher = new StudyFetcher(List.of(pagedFetcher), List.of(studyQuery), Map.of());
        studyFetcher.crawl();

        verify(pagedFetcher).performSearchPaged("machine learning", 0);
        verify(pagedFetcher, never()).performRawSearchQueryPaged(anyString(), anyInt());
    }

    @Test
    void caseInsensitiveOverrideMatchesFetcher() throws FetcherException {
        StudyQuery studyQuery = new StudyQuery("machine learning");
        studyQuery.setCatalogSpecific(Map.of("ieeexplore", "native:query"));

        PagedSearchBasedFetcher pagedFetcher = mock(PagedSearchBasedFetcher.class);
        when(pagedFetcher.getName()).thenReturn("IEEEXplore");
        when(pagedFetcher.getPageSize()).thenReturn(10);
        when(pagedFetcher.performRawSearchQueryPaged(anyString(), anyInt()))
                .thenReturn(new Page<>("native:query", 0, List.of()));
        when(pagedFetcher.performRawSearchQueryPaged("native:query", 0))
                .thenReturn(new Page<>("native:query", 0, List.of(new BibEntry())));

        StudyFetcher studyFetcher = new StudyFetcher(List.of(pagedFetcher), List.of(studyQuery), Map.of());
        studyFetcher.crawl();

        verify(pagedFetcher).performRawSearchQueryPaged("native:query", 0);
        verify(pagedFetcher, never()).performSearchPaged(anyString(), anyInt());
    }

    @Test
    void unsupportedOperationOnRawPathOmitsFetcherFromResults() throws FetcherException {
        StudyQuery studyQuery = new StudyQuery("machine learning");
        studyQuery.setCatalogSpecific(Map.of("TestPagedFetcher", "native:query"));

        PagedSearchBasedFetcher pagedFetcher = mock(PagedSearchBasedFetcher.class);
        when(pagedFetcher.getName()).thenReturn("TestPagedFetcher");
        when(pagedFetcher.getPageSize()).thenReturn(10);
        when(pagedFetcher.performRawSearchQueryPaged(anyString(), anyInt()))
                .thenThrow(new UnsupportedOperationException("not implemented"));

        StudyFetcher studyFetcher = new StudyFetcher(List.of(pagedFetcher), List.of(studyQuery), Map.of());
        List<QueryResult> results = studyFetcher.crawl();

        assertFalse(results.isEmpty());
        assertTrue(results.getFirst().getResultsPerFetcher().isEmpty());
        verify(pagedFetcher, never()).performSearchPaged(anyString(), anyInt());
    }
}
