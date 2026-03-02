package org.jabref.logic.importer;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQueryNode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static class StubIdAndSearchFetcher implements SearchBasedFetcher, IdBasedFetcher {
        private BaseQueryNode lastQueryNode;
        private String lastSearchedId;

        @Override
        public List<BibEntry> performSearch(BaseQueryNode queryList) throws FetcherException {
            this.lastQueryNode = queryList;
            return List.of();
        }

        @Override
        public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
            this.lastSearchedId = identifier;
            return Optional.of(new BibEntry()
                    .withField(StandardField.DOI, identifier));
        }

        @Override
        public String getName() {
            return "StubIdAndSearchFetcher";
        }

        BaseQueryNode getLastQueryNode() {
            return lastQueryNode;
        }

        String getLastSearchedId() {
            return lastSearchedId;
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
        // "!term" is invalid in Search.g4 (! must be escaped as \!)
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
    void doiQueryRoutesToIdBasedFetcher() throws FetcherException {
        StubIdAndSearchFetcher fetcher = new StubIdAndSearchFetcher();

        List<BibEntry> result = fetcher.performSearch("10.1109/5.771073");

        assertEquals("10.1109/5.771073", fetcher.getLastSearchedId());
        assertEquals(1, result.size());
        assertEquals(Optional.of("10.1109/5.771073"), result.getFirst().getField(StandardField.DOI));
        assertNull(fetcher.getLastQueryNode(), "performSearch(BaseQueryNode) should not have been called");
    }

    @Test
    void doiUrlQueryRoutesToIdBasedFetcher() throws FetcherException {
        StubIdAndSearchFetcher fetcher = new StubIdAndSearchFetcher();

        List<BibEntry> result = fetcher.performSearch("https://doi.org/10.1109/5.771073");

        assertEquals("https://doi.org/10.1109/5.771073", fetcher.getLastSearchedId());
        assertEquals(1, result.size());
    }

    @Test
    void doiQueryFallsBackToRawTermWhenFetcherDoesNotSupportIdSearch() throws FetcherException {
        StubSearchBasedFetcher fetcher = new StubSearchBasedFetcher();
        fetcher.performSearch("10.1109/5.771073");

        // DOI fails ANTLR parsing, so it becomes a raw SearchQueryNode
        assertInstanceOf(SearchQueryNode.class, fetcher.getLastQueryNode());
    }
}

