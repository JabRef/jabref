package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
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
        bibEntryPPN591166003.setType(StandardEntryType.Book);
        bibEntryPPN591166003.setField(StandardField.TITLE, "Effective Java");
        bibEntryPPN591166003.setField(StandardField.PUBLISHER, "Addison-Wesley");
        bibEntryPPN591166003.setField(StandardField.YEAR, "2008");
        bibEntryPPN591166003.setField(StandardField.AUTHOR, "Joshua Bloch");
        bibEntryPPN591166003.setField(StandardField.SERIES, "The @Java series");
        bibEntryPPN591166003.setField(StandardField.ADDRESS, "Upper Saddle River, NJ [u.a.]");
        bibEntryPPN591166003.setField(StandardField.EDITION, "2. ed., 5. print.");
        bibEntryPPN591166003.setField(StandardField.NOTE, "Literaturverz. S. 321 - 325");
        bibEntryPPN591166003.setField(StandardField.ISBN, "9780321356680");
        bibEntryPPN591166003.setField(StandardField.PAGETOTAL, "XXI, 346");
        bibEntryPPN591166003.setField(new UnknownField("ppn_gvk"), "591166003");
        bibEntryPPN591166003.setField(StandardField.SUBTITLE, "[revised and updated for JAVA SE 6]");

        bibEntryPPN66391437X = new BibEntry();
        bibEntryPPN66391437X.setType(StandardEntryType.Book);
        bibEntryPPN66391437X.setField(StandardField.TITLE, "Effective unit testing");
        bibEntryPPN66391437X.setField(StandardField.PUBLISHER, "Manning");
        bibEntryPPN66391437X.setField(StandardField.YEAR, "2013");
        bibEntryPPN66391437X.setField(StandardField.AUTHOR, "Lasse Koskela");
        bibEntryPPN66391437X.setField(StandardField.ADDRESS, "Shelter Island, NY");
        bibEntryPPN66391437X.setField(StandardField.ISBN, "9781935182573");
        bibEntryPPN66391437X.setField(StandardField.PAGETOTAL, "XXIV, 223");
        bibEntryPPN66391437X.setField(new UnknownField("ppn_gvk"), "66391437X");
        bibEntryPPN66391437X.setField(StandardField.SUBTITLE, "A guide for Java developers");
    }

    @Test
    public void testGetName() {
        assertEquals("GVK", fetcher.getName());
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
