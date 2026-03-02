package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQueryNode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchBasedFetcherTest {

    private static class StubSearchBasedFetcher implements SearchBasedFetcher {
        private BaseQueryNode lastQueryNode;

        @Override
        public List<BibEntry> performSearch(BaseQueryNode queryList) throws FetcherException {
            this.lastQueryNode = queryList;
            return List.of();
        }

        @Override
        public String getName() {
            return "StubFetcher";
        }

        BaseQueryNode getLastQueryNode() {
            return lastQueryNode;
        }
    }

    @Test
    void emptyQueryReturnsEmptyList() throws FetcherException {
        StubSearchBasedFetcher fetcher = new StubSearchBasedFetcher();
        assertEquals(List.of(), fetcher.performSearch(""));
    }

    @Test
    void blankQueryReturnsEmptyList() throws FetcherException {
        StubSearchBasedFetcher fetcher = new StubSearchBasedFetcher();
        assertEquals(List.of(), fetcher.performSearch("   "));
    }

    @Test
    void validQueryIsParsedIntoStructuredNode() throws FetcherException {
        StubSearchBasedFetcher fetcher = new StubSearchBasedFetcher();
        fetcher.performSearch("quantum");

        SearchQueryNode searchNode = assertInstanceOf(SearchQueryNode.class, fetcher.getLastQueryNode());
        assertEquals("quantum", searchNode.term());
        assertTrue(searchNode.field().isEmpty(), "Unfielded term should have empty field");
    }

    @Test
    void invalidQueryFallsBackToRawTerm() throws FetcherException {
        StubSearchBasedFetcher fetcher = new StubSearchBasedFetcher();
        fetcher.performSearch("!term");

        SearchQueryNode searchNode = assertInstanceOf(SearchQueryNode.class, fetcher.getLastQueryNode());
        assertEquals("!term", searchNode.term());
        assertTrue(searchNode.field().isEmpty());
    }

    @Test
    void fetcherSpecificSyntaxFallsBackToRawTerm() throws FetcherException {
        StubSearchBasedFetcher fetcher = new StubSearchBasedFetcher();
        fetcher.performSearch("pica.tit=quantum");

        SearchQueryNode searchNode = assertInstanceOf(SearchQueryNode.class, fetcher.getLastQueryNode());
        assertEquals("pica.tit=quantum", searchNode.term());
    }

    @Test
    void specialCharactersInQueryFallBackToRawTerm() throws FetcherException {
        StubSearchBasedFetcher fetcher = new StubSearchBasedFetcher();

        fetcher.performSearch("t(erm");
        SearchQueryNode node1 = assertInstanceOf(SearchQueryNode.class, fetcher.getLastQueryNode());
        assertEquals("t(erm", node1.term());

        fetcher.performSearch("t~erm");
        SearchQueryNode node2 = assertInstanceOf(SearchQueryNode.class, fetcher.getLastQueryNode());
        assertEquals("t~erm", node2.term());
    }

    @Test
    void doiQueryFallsBackToRawTerm() throws FetcherException {
        StubSearchBasedFetcher fetcher = new StubSearchBasedFetcher();
        fetcher.performSearch("10.1109/5.771073");

        // DOI fails ANTLR parsing, so it becomes a raw SearchQueryNode
        SearchQueryNode searchNode = assertInstanceOf(SearchQueryNode.class, fetcher.getLastQueryNode());
        assertEquals("10.1109/5.771073", searchNode.term());
    }
}

