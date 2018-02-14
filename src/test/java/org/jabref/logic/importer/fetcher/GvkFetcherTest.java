package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class GvkFetcherTest {

    private GvkFetcher fetcher;
    private BibEntry bibEntryPPN591166003;
    private BibEntry bibEntryPPN66391437X;

    @BeforeEach
    public void setUp() {
        fetcher = new GvkFetcher();

        bibEntryPPN591166003 = new BibEntry();
        bibEntryPPN591166003.setType(BiblatexEntryTypes.BOOK);
        bibEntryPPN591166003.setField("title", "Effective Java");
        bibEntryPPN591166003.setField("publisher", "Addison-Wesley");
        bibEntryPPN591166003.setField("year", "2008");
        bibEntryPPN591166003.setField("author", "Joshua Bloch");
        bibEntryPPN591166003.setField("series", "The @Java series");
        bibEntryPPN591166003.setField("address", "Upper Saddle River, NJ [u.a.]");
        bibEntryPPN591166003.setField("edition", "2. ed., 5. print.");
        bibEntryPPN591166003.setField("note", "Literaturverz. S. 321 - 325");
        bibEntryPPN591166003.setField("isbn", "9780321356680");
        bibEntryPPN591166003.setField("pagetotal", "XXI, 346");
        bibEntryPPN591166003.setField("ppn_gvk", "591166003");
        bibEntryPPN591166003.setField("subtitle", "[revised and updated for JAVA SE 6]");

        bibEntryPPN66391437X = new BibEntry();
        bibEntryPPN66391437X.setType(BiblatexEntryTypes.BOOK);
        bibEntryPPN66391437X.setField("title", "Effective unit testing");
        bibEntryPPN66391437X.setField("publisher", "Manning");
        bibEntryPPN66391437X.setField("year", "2013");
        bibEntryPPN66391437X.setField("author", "Lasse Koskela");
        bibEntryPPN66391437X.setField("address", "Shelter Island, NY");
        bibEntryPPN66391437X.setField("isbn", "9781935182573");
        bibEntryPPN66391437X.setField("pagetotal", "XXIV, 223");
        bibEntryPPN66391437X.setField("ppn_gvk", "66391437X");
        bibEntryPPN66391437X.setField("subtitle", "A guide for Java developers");
    }

    @Test
    public void testGetName() {
        assertEquals("GVK", fetcher.getName());
    }

    @Test
    public void testGetHelpPage() {
        assertEquals("GVK", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void simpleSearchQueryStringCorrect() {
        String query = "java jdk";
        String result = fetcher.getSearchQueryString(query);
        assertEquals("pica.all=java jdk", result);
    }

    @Test
    public void simpleSearchQueryURLCorrect() throws MalformedURLException, URISyntaxException, FetcherException {
        String query = "java jdk";
        URL url = fetcher.getURLForQuery(query);
        assertEquals("http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=pica.all%3Djava+jdk&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1", url.toString());
    }

    @Test
    public void complexSearchQueryStringCorrect() {
        String query = "kon java tit jdk";
        String result = fetcher.getSearchQueryString(query);
        assertEquals("pica.kon=java and pica.tit=jdk", result);
    }

    @Test
    public void complexSearchQueryURLCorrect() throws MalformedURLException, URISyntaxException, FetcherException {
        String query = "kon java tit jdk";
        URL url = fetcher.getURLForQuery(query);
        assertEquals("http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=pica.kon%3Djava+and+pica.tit%3Djdk&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1", url.toString());
    }

    @Test
    public void testPerformSearchMatchingMultipleEntries() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("tit effective java");
        assertTrue(searchResult.contains(bibEntryPPN591166003));
        assertTrue(searchResult.contains(bibEntryPPN66391437X));
    }

    @Test
    public void testPerformSearch591166003() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("ppn 591166003");
        assertEquals(Collections.singletonList(bibEntryPPN591166003), searchResult);
    }

    @Test
    public void testPerformSearch66391437X() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("ppn 66391437X");
        assertEquals(Collections.singletonList(bibEntryPPN66391437X), searchResult);
    }

    @Test
    public void testPerformSearchEmpty() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("");
        assertEquals(Collections.emptyList(), searchResult);
    }
}
