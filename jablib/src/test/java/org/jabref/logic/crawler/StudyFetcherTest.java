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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        when(pagedFetcher.performRawSearchQueryPaged("native:query", 0))
                .thenReturn(new Page<>("native:query", 0, List.of(new BibEntry())));

        StudyFetcher studyFetcher = new StudyFetcher(List.of(pagedFetcher), List.of(studyQuery), Map.of());
        List<QueryResult> results = studyFetcher.crawl();

        assertFalse(results.isEmpty());
        verify(pagedFetcher).performRawSearchQueryPaged("native:query", 0);
        verify(pagedFetcher, never()).performSearchPaged(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
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
        verify(plainFetcher, never()).performSearch(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void noOverrideFallsBackToStandardPath() throws FetcherException {
        StudyQuery studyQuery = new StudyQuery("machine learning");

        PagedSearchBasedFetcher pagedFetcher = mock(PagedSearchBasedFetcher.class);
        when(pagedFetcher.getName()).thenReturn("TestPagedFetcher");
        when(pagedFetcher.getPageSize()).thenReturn(10);
        when(pagedFetcher.performSearchPaged(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new Page<>("machine learning", 0, List.of()));
        when(pagedFetcher.performSearchPaged("machine learning", 0))
                .thenReturn(new Page<>("machine learning", 0, List.of(new BibEntry())));

        StudyFetcher studyFetcher = new StudyFetcher(List.of(pagedFetcher), List.of(studyQuery), Map.of());
        studyFetcher.crawl();

        verify(pagedFetcher).performSearchPaged("machine learning", 0);
        verify(pagedFetcher, never()).performRawSearchQueryPaged(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }
}
