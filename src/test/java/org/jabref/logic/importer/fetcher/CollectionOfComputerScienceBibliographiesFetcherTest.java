package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
class CollectionOfComputerScienceBibliographiesFetcherTest {
    private CollectionOfComputerScienceBibliographiesFetcher fetcher;
    private BibEntry bibEntry1;
    private BibEntry bibEntry2;

    @BeforeEach
    public void setUp() {
        fetcher = new CollectionOfComputerScienceBibliographiesFetcher();

        bibEntry1 = new BibEntry();
        bibEntry1.setField(StandardField.TITLE, "The relationship of code churn and architectural violations in the open source software JabRef");
        bibEntry1.setField(StandardField.AUTHOR, "Tobias Olsson, Morgan Ericsson, Anna Wingkvist");
        bibEntry1.setField(StandardField.DATE, "2017");
        bibEntry1.setField(StandardField.URL, "http://liinwww.ira.uka.de/searchbib/index?query=lgqcdpmrnlbbtgtqnxgpnddcrtxhcdxl&results=bibtex&mode=dup&rss=1");

        bibEntry2 = new BibEntry();
        bibEntry2.setField(StandardField.TITLE, "Literaturverwaltungsprogramme im Überblick");
        bibEntry2.setField(StandardField.AUTHOR, "Michaele Adam, Jutta Musiat, Kathleen Hoffmann, Sandra Rahm, Matti Stöhr, Christina Wenzel");
        bibEntry2.setField(StandardField.DATE, "2018");
        bibEntry2.setField(StandardField.URL, "http://liinwww.ira.uka.de/searchbib/index?query=qrxmnfnthltrkcgnxdxtfdrhrxjnttxg&results=bibtex&mode=dup&rss=1");
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
        assertTrue(searchResult.contains(bibEntry1));
        assertTrue(searchResult.contains(bibEntry2));
    }

    @Test
    public void performSearchReturnsEmptyListForEmptySearch() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("");
        assertEquals(Collections.emptyList(), searchResult);
    }
}
