package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
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
    private BibEntry bibEntryPPN591166003;

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        fetcher = new GvkFetcher();

        bibEntryPPN591166003 = new BibEntry();
        bibEntryPPN591166003.setType(BibLatexEntryTypes.BOOK);
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
    public void testPerformSearchMatchingMultipleEntries() throws FetcherException {
        List<BibEntry> list = fetcher.performSearch("tit effective java");

        //Search result may vary over time. PPN 591166003 is contained for sure.
        Optional<BibEntry> entryToCompare = Optional.empty();
        for (BibEntry entry : list) {
            if (entry.getFieldOptional("ppn_gvk").get().equals("591166003")) {
                entryToCompare = Optional.of(entry);
                break;
            }
        }
        assertEquals(Optional.of(bibEntryPPN591166003), entryToCompare);
    }

    @Test
    public void testPerformSearch() throws FetcherException {
        List<BibEntry> list = fetcher.performSearch("ppn 591166003");
        assertEquals(Collections.singletonList(bibEntryPPN591166003), list);
    }

    @Test
    public void testPerformSearchEmpty() throws FetcherException {
        List<BibEntry> list = fetcher.performSearch("");
        assertEquals(0, list.size());
    }
}
