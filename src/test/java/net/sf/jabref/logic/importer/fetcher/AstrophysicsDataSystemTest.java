package net.sf.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.bibtex.FieldContentParserPreferences;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Before;
import org.junit.Test;

import static net.sf.jabref.logic.util.OS.NEWLINE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AstrophysicsDataSystemTest {

    AstrophysicsDataSystem fetcher;
    BibEntry diezSliceTheoremEntry,famaeyMcGaughEntry, sunWelchEntry;

    @Before
    public void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(
                mock(FieldContentParserPreferences.class));
        fetcher = new AstrophysicsDataSystem(importFormatPreferences);

        diezSliceTheoremEntry = new BibEntry();
        diezSliceTheoremEntry.setType(BibtexEntryTypes.ARTICLE);
        diezSliceTheoremEntry.setCiteKey("2014arXiv1405.2249D");
        diezSliceTheoremEntry.setField("author", "Diez, T.");
        diezSliceTheoremEntry.setField("title", "Slice theorem for Fr$\\backslash$'echet group actions and covariant symplectic field theory");
        diezSliceTheoremEntry.setField("year", "2014");
        diezSliceTheoremEntry.setField("archiveprefix", "arXiv");
        diezSliceTheoremEntry.setField("eprint", "1405.2249");
        diezSliceTheoremEntry.setField("journal", "ArXiv e-prints");
        diezSliceTheoremEntry.setField("keywords", "Mathematical Physics, Mathematics - Differential Geometry, Mathematics - Symplectic Geometry, 58B99, 58Z05, 58B25, 22E65, 58D19, 53D20, 53D42");
        diezSliceTheoremEntry.setField("month", "#may#");
        diezSliceTheoremEntry.setField("primaryclass", "math-ph");
        diezSliceTheoremEntry.setField("abstract",
                "A general slice theorem for the action of a Fr$\\backslash$'echet Lie group on a" + NEWLINE
                        + "Fr$\\backslash$'echet manifolds is established. The Nash-Moser theorem provides the" + NEWLINE
                        + "fundamental tool to generalize the result of Palais to this" + NEWLINE
                        + "infinite-dimensional setting. The presented slice theorem is illustrated" + NEWLINE
                        + "by its application to gauge theories: the action of the gauge" + NEWLINE
                        + "transformation group admits smooth slices at every point and thus the" + NEWLINE
                        + "gauge orbit space is stratified by Fr$\\backslash$'echet manifolds. Furthermore, a" + NEWLINE
                        + "covariant and symplectic formulation of classical field theory is" + NEWLINE
                        + "proposed and extensively discussed. At the root of this novel framework" + NEWLINE
                        + "is the incorporation of field degrees of freedom F and spacetime M into" + NEWLINE
                        + "the product manifold F * M. The induced bigrading of differential forms" + NEWLINE
                        + "is used in order to carry over the usual symplectic theory to this new" + NEWLINE
                        + "setting. The examples of the Klein-Gordon field and general Yang-Mills" + NEWLINE
                        + "theory illustrate that the presented approach conveniently handles the" + NEWLINE
                        + "occurring symmetries." + NEWLINE);


        famaeyMcGaughEntry = new BibEntry();
        famaeyMcGaughEntry.setType(BibLatexEntryTypes.ARTICLE);
        famaeyMcGaughEntry.setField("bibtexkey", "2012LRR....15...10F");
        famaeyMcGaughEntry.setField("author", "Famaey, B. and McGaugh, S. S.");
        famaeyMcGaughEntry.setField("title", "Modified Newtonian Dynamics (MOND): Observational Phenomenology and Relativistic Extensions");
        famaeyMcGaughEntry.setField("journal", "Living Reviews in Relativity");
        famaeyMcGaughEntry.setField("year", "2012");
        famaeyMcGaughEntry.setField("volume", "15");
        famaeyMcGaughEntry.setField("month", "#sep#");
        famaeyMcGaughEntry.setField("archiveprefix", "arXiv");
        famaeyMcGaughEntry.setField("doi", "10.12942/lrr-2012-10");
        famaeyMcGaughEntry.setField("eprint", "1112.3960");
        famaeyMcGaughEntry.setField("keywords", "astronomical observations, Newtonian limit, equations of motion, extragalactic astronomy, cosmology, theories of gravity, fundamental physics, astrophysics");
        famaeyMcGaughEntry.setField("abstract", "A wealth of astronomical data indicate the presence of mass" + NEWLINE
                + "discrepancies in the Universe. The motions observed in a variety of" + NEWLINE
                + "classes of extragalactic systems exceed what can be explained by the" + NEWLINE
                + "mass visible in stars and gas. Either (i) there is a vast amount of" + NEWLINE
                + "unseen mass in some novel form - dark matter - or (ii) the data indicate" + NEWLINE
                + "a breakdown of our understanding of dynamics on the relevant scales, or" + NEWLINE
                + "(iii) both. Here, we first review a few outstanding challenges for the" + NEWLINE
                + "dark matter interpretation of mass discrepancies in galaxies, purely" + NEWLINE
                + "based on observations and independently of any alternative theoretical" + NEWLINE
                + "framework. We then show that many of these puzzling observations are" + NEWLINE
                + "predicted by one single relation - Milgrom's law - involving an" + NEWLINE
                + "acceleration constant a\\_0 (or a characteristic surface density" + NEWLINE
                + "{$\\Sigma$}\\_{\\dagger} = a\\_0/G) on the order of the square-root of the" + NEWLINE
                + "cosmological constant in natural units. This relation can at present" + NEWLINE
                + "most easily be interpreted as the effect of a single universal force law" + NEWLINE
                + "resulting from a modification of Newtonian dynamics (MOND) on galactic" + NEWLINE
                + "scales. We exhaustively review the current observational successes and" + NEWLINE
                + "problems of this alternative paradigm at all astrophysical scales, and" + NEWLINE
                + "summarize the various theoretical attempts (TeVeS, GEA, BIMOND, and" + NEWLINE
                + "others) made to effectively embed this modification of Newtonian" + NEWLINE
                + "dynamics within a relativistic theory of gravity."+  NEWLINE);


        sunWelchEntry = new BibEntry();
        sunWelchEntry.setType(BibLatexEntryTypes.ARTICLE);
        sunWelchEntry.setField("bibtexkey", "2012NatMa..11...44S");
        sunWelchEntry.setField("abstract", "Organic photovoltaic devices that can be fabricated by simple processing"+ NEWLINE
                + "techniques are under intense investigation in academic and industrial"+ NEWLINE
                + "laboratories because of their potential to enable mass production of"+ NEWLINE
                + "flexible and cost-effective devices. Most of the attention has been"+ NEWLINE
                + "focused on solution-processed polymer bulk-heterojunction (BHJ) solar"+ NEWLINE
                + "cells. A combination of polymer design, morphology control, structural"+ NEWLINE
                + "insight and device engineering has led to power conversion efficiencies"+ NEWLINE
                + "(PCEs) reaching the 6-8\\% range for conjugated polymer/fullerene blends."+ NEWLINE
                + "Solution-processed small-molecule BHJ (SM BHJ) solar cells have received"+ NEWLINE
                + "less attention, and their efficiencies have remained below those of"+ NEWLINE
                + "their polymeric counterparts. Here, we report efficient"+ NEWLINE
                + "solution-processed SM BHJ solar cells based on a new molecular donor,"+ NEWLINE
                + "DTS(PTTh$_{2}$)$_{2}$. A record PCE of 6.7\\% under AM"+ NEWLINE
                + "1.5{\\thinsp}G irradiation (100{\\thinsp}mW{\\thinsp}cm$^{-2}$) is"+ NEWLINE
                + "achieved for small-molecule BHJ devices from"+ NEWLINE
                + "DTS(PTTh$_{2}$)$_{2}$:PC$_{70}$BM (donor to acceptor"+ NEWLINE
                + "ratio of 7:3). This high efficiency was obtained by using remarkably"+ NEWLINE
                + "small percentages of solvent additive (0.25\\%{\\thinsp}v/v of"+ NEWLINE
                + "1,8-diiodooctane, DIO) during the film-forming process, which leads to"+ NEWLINE
                + "reduced domain sizes in the BHJ layer. These results provide important"+ NEWLINE
                + "progress for solution-processed organic photovoltaics and demonstrate"+ NEWLINE
                + "that solar cells fabricated from small donor molecules can compete with"+ NEWLINE
                + "their polymeric counterparts."+ NEWLINE);
        sunWelchEntry.setField("author", "Sun, Y. and Welch, G. C. and Leong, W. L. and Takacs, C. J. and Bazan, G. C. and Heeger, A. J.");
        sunWelchEntry.setField("doi", "10.1038/nmat3160");
        sunWelchEntry.setField("journal", "Nature Materials");
        sunWelchEntry.setField("month", "#jan#");
        sunWelchEntry.setField("pages", "44-48");
        sunWelchEntry.setField("title", "Solution-processed small-molecule solar cells with 6.7\\% efficiency");
        sunWelchEntry.setField("volume", "11");
        sunWelchEntry.setField("year", "2012");
    }

    @Test
    public void testHelpPage() {
        assertEquals("ADS", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void testGetName() {
        assertEquals("SAO/NASA Astrophysics Data System", fetcher.getName());
    }

    @Test
    public void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Diez slice theorem");
        assertEquals(Collections.singletonList(diezSliceTheoremEntry), fetchedEntries);
    }

    @Test
    public void searchByEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField("title", "slice theorem");
        searchEntry.setField("author", "Diez");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertFalse(fetchedEntries.isEmpty());
        assertEquals(diezSliceTheoremEntry, fetchedEntries.get(0));
    }

    @Test
    public void testPerformSearchByFamaeyMcGaughEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.12942/lrr-2012-10");
        assertEquals(Optional.of(famaeyMcGaughEntry), fetchedEntry);
    }

    @Test
    public void testPerformSearchByIdEmptyDOI() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test(expected = FetcherException.class)
    public void testPerformSearchByIdInvalidDoi() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("this.doi.will.fail");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testPerformSearchBySunWelchEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1038/nmat3160");
        assertEquals(Optional.of(sunWelchEntry), fetchedEntry);
    }
}
