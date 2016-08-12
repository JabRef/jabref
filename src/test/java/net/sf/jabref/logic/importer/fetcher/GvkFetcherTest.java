package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GvkFetcherTest {

    private GvkFetcher fetcher;
    private List<BibEntry> entryList;

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        fetcher = new GvkFetcher();
    }

    @Test
    public void testGetName() {
        assertEquals("GVK", fetcher.getName());
    }

    @Test
    public void testGetHelpPage() {
        assertEquals("GVKHelp", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void simpleSearchQueryStringCorrect() {
        String query = "java jdk";
        String result = fetcher.getSearchQueryString(query);
        assertEquals("pica.all=java jdk", result);
    }

    @Test
    public void simpleSearchQueryURLCorrect() throws MalformedURLException, URISyntaxException {
        String query = "java jdk";
        URL url = fetcher.getQueryURL(query);
        assertEquals("http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=pica.all%3Djava+jdk&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1", url.toString());
    }

    @Test
    public void complexSearchQueryStringCorrect() {
        String query = "kon java tit jdk";
        String result = fetcher.getSearchQueryString(query);
        assertEquals("pica.kon=java and pica.tit=jdk", result);
    }

    @Test
    public void complexSearchQueryURLCorrect() throws MalformedURLException, URISyntaxException {
        String query = "kon java tit jdk";
        URL url = fetcher.getQueryURL(query);
        assertEquals("http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=pica.kon%3Djava+and+pica.tit%3Djdk&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1", url.toString());
    }

    @Test
    public void testPerformSearch() throws FetcherException {
        List<BibEntry> list = fetcher.performSearch("java jdk");
        // TODO
    }
}
