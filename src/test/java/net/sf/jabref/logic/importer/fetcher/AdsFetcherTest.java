package net.sf.jabref.logic.importer.fetcher;

import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AdsFetcherTest {

    private AdsFetcher fetcher;
    private BibEntry bibEntry;

    @Before
    public void setUp() {
        fetcher = new AdsFetcher(Globals.prefs.getImportFormatPreferences());

        bibEntry = new BibEntry();
        bibEntry.setType(BibLatexEntryTypes.ARTICLE);
        bibEntry.setField("bibtexkey", "2012LRR....15...10F");
        bibEntry.setField("author", "{Famaey}, B. and {McGaugh}, S.~S.");
        bibEntry.setField("title", "{Modified Newtonian Dynamics (MOND): Observational Phenomenology and Relativistic Extensions}");
        bibEntry.setField("journal", "Living Reviews in Relativity");
        bibEntry.setField("year", "2012");
        bibEntry.setField("volume", "15");
        bibEntry.setField("month", "#sep#");
        bibEntry.setField("adsnote", "Provided by the SAO/NASA Astrophysics Data System");
        bibEntry.setField("adsurl", "http://adsabs.harvard.edu/abs/2012LRR....15...10F");
        bibEntry.setField("archiveprefix", "arXiv");
        bibEntry.setField("doi", "10.12942/lrr-2012-10");
        bibEntry.setField("eprint", "1112.3960");
        bibEntry.setField("keywords", "astronomical observations, Newtonian limit, equations of motion, extragalactic astronomy, cosmology, theories of gravity, fundamental physics, astrophysics");
        bibEntry.setField("abstract", "A wealth of astronomical data indicate the presence of mass discrepancies in the Universe. The motions observed in a variety of classes of extragalactic systems exceed what can be explained by the mass visible in stars and gas. Either (i) there is a vast amount of unseen mass in some novel form - dark matter - or (ii) the data indicate a breakdown of our understanding of dynamics on the relevant scales, or (iii) both. Here, we first review a few outstanding challenges for the dark matter interpretation of mass discrepancies in galaxies, purely based on observations and independently of any alternative theoretical framework. We then show that many of these puzzling observations are predicted by one single relation - Milgrom's law - involving an acceleration constant a_0 (or a characteristic surface density Σ_† = a_0/G) on the order of the square-root of the cosmological constant in natural units. This relation can at present most easily be interpreted as the effect of a single universal force law resulting from a modification of Newtonian dynamics (MOND) on galactic scales. We exhaustively review the current observational successes and problems of this alternative paradigm at all astrophysical scales, and summarize the various theoretical attempts (TeVeS, GEA, BIMOND, and others) made to effectively embed this modification of Newtonian dynamics within a relativistic theory of gravity.");
    }


    @Test
    public void testName() {
        assertEquals("ADS from ADS-DOI", fetcher.getName());
    }

    @Test
    public void testHelpPage() {
        assertEquals("ADSHelp", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void testPerformSearchById() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.12942/lrr-2012-10");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    public void testPerformSearchByIdEmpty() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testPerformSearchByIdInvalidDoi() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("this.doi.will.fail");
        assertEquals(Optional.empty(), fetchedEntry);
    }
}
