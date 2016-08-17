package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GvkFetcherTest {

    private GvkFetcher fetcher;
    private BibEntry bibEntry;

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        fetcher = new GvkFetcher();

        bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.BOOK);
        bibEntry.setField("title", "Effective Java");
        bibEntry.setField("publisher", "Addison-Wesley");
        bibEntry.setField("year", "2008");
        bibEntry.setField("author", "Joshua Bloch");
        bibEntry.setField("series", "The @Java series");
        bibEntry.setField("address", "Upper Saddle River, NJ [u.a.]");
        bibEntry.setField("edition", "2. ed., 5. print.");
        bibEntry.setField("note", "Literaturverz. S. 321 - 325");
        bibEntry.setField("isbn", "9780321356680");
        bibEntry.setField("pagetotal", "XXI, 346");
        bibEntry.setField("ppn_gvk", "591166003");
        bibEntry.setField("subtitle", "[revised and updated for JAVA SE 6]");
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
    public void simpleSearchQueryStringCorrect() throws FetcherException {
        String query = "java jdk";
        String result = fetcher.getSearchQueryString(query);
        assertEquals("pica.all=java jdk", result);
    }

    @Test
    public void simpleSearchQueryURLCorrect() throws MalformedURLException, URISyntaxException, FetcherException {
        String query = "java jdk";
        URL url = fetcher.getQueryURL(query);
        assertEquals("http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=pica.all%3Djava+jdk&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1", url.toString());
    }

    @Test
    public void complexSearchQueryStringCorrect() throws FetcherException {
        String query = "kon java tit jdk";
        String result = fetcher.getSearchQueryString(query);
        assertEquals("pica.kon=java and pica.tit=jdk", result);
    }

    @Test
    public void complexSearchQueryURLCorrect() throws MalformedURLException, URISyntaxException, FetcherException {
        String query = "kon java tit jdk";
        URL url = fetcher.getQueryURL(query);
        assertEquals("http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=pica.kon%3Djava+and+pica.tit%3Djdk&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1", url.toString());
    }

    @Test
    public void testPerformSearch() throws FetcherException {
        List<BibEntry> list = fetcher.performSearch("tit effective java");
        Optional<BibEntry> entryToCompare = Optional.empty();

        for (BibEntry entry : list) {
            if (entry.getFieldOptional("ppn_gvk").get().equals("591166003")) {
                entryToCompare = Optional.of(entry);
                break;
            }
        }
        assertEquals(Optional.of(bibEntry), entryToCompare);
    }

    @Test
    public void testPerformSearchEmpty() throws FetcherException {
        List<BibEntry> list = fetcher.performSearch("");
        assertEquals(0, list.size());
    }
}
