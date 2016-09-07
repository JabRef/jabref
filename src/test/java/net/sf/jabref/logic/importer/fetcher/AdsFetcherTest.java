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
    private BibEntry firstEntry, secondEntry;

    @Before
    public void setUp() {
        fetcher = new AdsFetcher(Globals.prefs.getImportFormatPreferences());

        firstEntry = new BibEntry();
        firstEntry.setType(BibLatexEntryTypes.ARTICLE);
        firstEntry.setField("bibtexkey", "2012LRR....15...10F");
        firstEntry.setField("author", "Famaey , B. and McGaugh , S.~S.");
        firstEntry.setField("title", "Modified Newtonian Dynamics (MOND): Observational Phenomenology and Relativistic Extensions");
        firstEntry.setField("journal", "Living Reviews in Relativity");
        firstEntry.setField("year", "2012");
        firstEntry.setField("volume", "15");
        firstEntry.setField("month", "#sep#");
        firstEntry.setField("adsnote", "Provided by the SAO/NASA Astrophysics Data System");
        firstEntry.setField("adsurl", "http://adsabs.harvard.edu/abs/2012LRR....15...10F");
        firstEntry.setField("archiveprefix", "arXiv");
        firstEntry.setField("doi", "10.12942/lrr-2012-10");
        firstEntry.setField("eprint", "1112.3960");
        firstEntry.setField("keywords", "astronomical observations, Newtonian limit, equations of motion, extragalactic astronomy, cosmology, theories of gravity, fundamental physics, astrophysics");
        firstEntry.setField("abstract", "A wealth of astronomical data indicate the presence of mass\n" +
                "discrepancies in the Universe. The motions observed in a variety of\n" +
                "classes of extragalactic systems exceed what can be explained by the\n" +
                "mass visible in stars and gas. Either (i) there is a vast amount of\n" +
                "unseen mass in some novel form - dark matter - or (ii) the data indicate\n" +
                "a breakdown of our understanding of dynamics on the relevant scales, or\n" +
                "(iii) both. Here, we first review a few outstanding challenges for the\n" +
                "dark matter interpretation of mass discrepancies in galaxies, purely\n" +
                "based on observations and independently of any alternative theoretical\n" +
                "framework. We then show that many of these puzzling observations are\n" +
                "predicted by one single relation - Milgrom's law - involving an\n" +
                "acceleration constant a\\_0 (or a characteristic surface density\n" +
                "{$\\Sigma$}\\_{\\dagger} = a\\_0/G) on the order of the square-root of the\n" +
                "cosmological constant in natural units. This relation can at present\n" +
                "most easily be interpreted as the effect of a single universal force law\n" +
                "resulting from a modification of Newtonian dynamics (MOND) on galactic\n" +
                "scales. We exhaustively review the current observational successes and\n" +
                "problems of this alternative paradigm at all astrophysical scales, and\n" +
                "summarize the various theoretical attempts (TeVeS, GEA, BIMOND, and\n" +
                "others) made to effectively embed this modification of Newtonian\n" +
                "dynamics within a relativistic theory of gravity.");


        secondEntry = new BibEntry();
        secondEntry.setType(BibLatexEntryTypes.ARTICLE);
        secondEntry.setField("bibtexkey", "2012NatMa..11...44S");
        secondEntry.setField("abstract", "Organic photovoltaic devices that can be fabricated by simple processing\n" +
                "techniques are under intense investigation in academic and industrial\n" +
                "laboratories because of their potential to enable mass production of\n" +
                "flexible and cost-effective devices. Most of the attention has been\n" +
                "focused on solution-processed polymer bulk-heterojunction (BHJ) solar\n" +
                "cells. A combination of polymer design, morphology control, structural\n" +
                "insight and device engineering has led to power conversion efficiencies\n" +
                "(PCEs) reaching the 6-8\\% range for conjugated polymer/fullerene blends.\n" +
                "Solution-processed small-molecule BHJ (SM BHJ) solar cells have received\n" +
                "less attention, and their efficiencies have remained below those of\n" +
                "their polymeric counterparts. Here, we report efficient\n" +
                "solution-processed SM BHJ solar cells based on a new molecular donor,\n" +
                "DTS(PTTh$_{2}$)$_{2}$. A record PCE of 6.7\\% under AM\n" +
                "1.5{\\thinsp}G irradiation (100{\\thinsp}mW{\\thinsp}cm$^{-2}$) is\n" +
                "achieved for small-molecule BHJ devices from\n" +
                "DTS(PTTh$_{2}$)$_{2}$:PC$_{70}$BM (donor to acceptor\n" +
                "ratio of 7:3). This high efficiency was obtained by using remarkably\n" +
                "small percentages of solvent additive (0.25\\%{\\thinsp}v/v of\n" +
                "1,8-diiodooctane, DIO) during the film-forming process, which leads to\n" +
                "reduced domain sizes in the BHJ layer. These results provide important\n" +
                "progress for solution-processed organic photovoltaics and demonstrate\n" +
                "that solar cells fabricated from small donor molecules can compete with\n" +
                "their polymeric counterparts.");
        secondEntry.setField("adsnote", "Provided by the SAO/NASA Astrophysics Data System");
        secondEntry.setField("adsurl", "http://adsabs.harvard.edu/abs/2012NatMa..11...44S");
        secondEntry.setField("author", "Sun , Y. and Welch , G.~C. and Leong , W.~L. and Takacs , C.~J. and Bazan , G.~C. and Heeger , A.~J.");
        secondEntry.setField("doi", "10.1038/nmat3160");
        secondEntry.setField("journal", "Nature Materials");
        secondEntry.setField("month", "#jan#");
        secondEntry.setField("pages", "44-48");
        secondEntry.setField("title", "Solution-processed small-molecule solar cells with 6.7\\% efficiency");
        secondEntry.setField("volume", "11");
        secondEntry.setField("year", "2012");

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
        assertEquals(Optional.of(firstEntry), fetchedEntry);
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

    @Test
    public void testPerformSearchById2() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1038/nmat3160");
        assertEquals(Optional.of(secondEntry), fetchedEntry);
    }

}
