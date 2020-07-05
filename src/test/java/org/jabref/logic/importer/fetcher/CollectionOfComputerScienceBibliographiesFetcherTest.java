package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class CollectionOfComputerScienceBibliographiesFetcherTest {
    private CollectionOfComputerScienceBibliographiesFetcher fetcher;
    private BibEntry bibEntry1;
    private BibEntry bibEntry2;

    @BeforeEach
    public void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        fetcher = new CollectionOfComputerScienceBibliographiesFetcher(importFormatPreferences, new DummyFileUpdateMonitor());
    }

    @Test
    public void getNameReturnsCorrectName() {
        assertEquals("Collection of Computer Science Bibliographies", fetcher.getName());
    }

    @Test
    public void getUrlForQueryReturnsCorrectUrl() throws MalformedURLException, URISyntaxException, FetcherException {
        String query = "java jdk";
        URL url = fetcher.getURLForQuery(query);
        assertEquals("http://liinwww.ira.uka.de/bibliography/rss?query=java+jdk&sort=score", url.toString());
    }

    @Test
    public void performSearchReturnsMatchingMultipleEntries() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("jabref");
        BibEntry bibEntry = searchResult.get(0);
        assertNotNull(bibEntry.getField(StandardField.ABSTRACT));
        assertNotNull(bibEntry.getField(StandardField.AUTHOR));
        assertNotNull(bibEntry.getField(StandardField.URL));
        assertNotNull(bibEntry.getField(StandardField.YEAR));
        assertNotNull(bibEntry.getField(StandardField.TITLE));
        assertNotNull(bibEntry.getField(StandardField.TYPE));
    }

    @Test
    public void performSearchReturnsEmptyListForEmptySearch() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("");
        assertEquals(Collections.emptyList(), searchResult);
    }
}
