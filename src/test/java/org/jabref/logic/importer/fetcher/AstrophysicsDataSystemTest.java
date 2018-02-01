package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class AstrophysicsDataSystemTest {

    private AstrophysicsDataSystem fetcher;
    private BibEntry diezSliceTheoremEntry, famaeyMcGaughEntry, sunWelchEntry, xiongSunEntry, ingersollPollardEntry, luceyPaulEntry;

    @BeforeEach
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
        diezSliceTheoremEntry.setField("url", "http://adsabs.harvard.edu/abs/2014arXiv1405.2249D");
        diezSliceTheoremEntry.setField("abstract",
                "A general slice theorem for the action of a Fr$\\backslash$'echet Lie group on a "
                        + "Fr$\\backslash$'echet manifolds is established. The Nash-Moser theorem provides the "
                        + "fundamental tool to generalize the result of Palais to this "
                        + "infinite-dimensional setting. The presented slice theorem is illustrated "
                        + "by its application to gauge theories: the action of the gauge "
                        + "transformation group admits smooth slices at every point and thus the "
                        + "gauge orbit space is stratified by Fr$\\backslash$'echet manifolds. Furthermore, a "
                        + "covariant and symplectic formulation of classical field theory is "
                        + "proposed and extensively discussed. At the root of this novel framework "
                        + "is the incorporation of field degrees of freedom F and spacetime M into "
                        + "the product manifold F * M. The induced bigrading of differential forms "
                        + "is used in order to carry over the usual symplectic theory to this new "
                        + "setting. The examples of the Klein-Gordon field and general Yang-Mills "
                        + "theory illustrate that the presented approach conveniently handles the "
                        + "occurring symmetries.");

        famaeyMcGaughEntry = new BibEntry();
        famaeyMcGaughEntry.setType(BiblatexEntryTypes.ARTICLE);
        famaeyMcGaughEntry.setField("bibtexkey", "2012LRR....15...10F");
        famaeyMcGaughEntry.setField("author", "Famaey, B. and McGaugh, S. S.");
        famaeyMcGaughEntry.setField("title", "Modified Newtonian Dynamics (MOND): Observational Phenomenology and Relativistic Extensions");
        famaeyMcGaughEntry.setField("journal", "Living Reviews in Relativity");
        famaeyMcGaughEntry.setField("year", "2012");
        famaeyMcGaughEntry.setField("volume", "15");
        famaeyMcGaughEntry.setField("month", "#sep#");
        famaeyMcGaughEntry.setField("archiveprefix", "arXiv");
        famaeyMcGaughEntry.setField("doi", "10.12942/lrr-2012-10");
        famaeyMcGaughEntry.setField("eid", "10");
        famaeyMcGaughEntry.setField("eprint", "1112.3960");
        famaeyMcGaughEntry.setField("pages", "10");
        famaeyMcGaughEntry.setField("keywords", "astronomical observations, Newtonian limit, equations of motion, extragalactic astronomy, cosmology, theories of gravity, fundamental physics, astrophysics");
        famaeyMcGaughEntry.setField("url", "http://adsabs.harvard.edu/abs/2012LRR....15...10F");

        sunWelchEntry = new BibEntry();
        sunWelchEntry.setType(BiblatexEntryTypes.ARTICLE);
        sunWelchEntry.setField("bibtexkey", "2012NatMa..11...44S");
        sunWelchEntry.setField("author", "Sun, Y. and Welch, G. C. and Leong, W. L. and Takacs, C. J. and Bazan, G. C. and Heeger, A. J.");
        sunWelchEntry.setField("doi", "10.1038/nmat3160");
        sunWelchEntry.setField("journal", "Nature Materials");
        sunWelchEntry.setField("month", "#jan#");
        sunWelchEntry.setField("pages", "44-48");
        sunWelchEntry.setField("title", "Solution-processed small-molecule solar cells with 6.7\\% efficiency");
        sunWelchEntry.setField("volume", "11");
        sunWelchEntry.setField("year", "2012");
        sunWelchEntry.setField("url", "http://adsabs.harvard.edu/abs/2012NatMa..11...44S");

        xiongSunEntry = new BibEntry();
        xiongSunEntry.setType(BiblatexEntryTypes.ARTICLE);
        xiongSunEntry.setField("bibtexkey", "2007ITGRS..45..879X");
        xiongSunEntry.setField("author", "Xiong, X. and Sun, J. and Barnes, W. and Salomonson, V. and Esposito, J. and Erives, H. and Guenther, B.");
        xiongSunEntry.setField("doi", "10.1109/TGRS.2006.890567");
        xiongSunEntry.setField("journal", "IEEE Transactions on Geoscience and Remote Sensing");
        xiongSunEntry.setField("month", "#apr#");
        xiongSunEntry.setField("pages", "879-889");
        xiongSunEntry.setField("title", "Multiyear On-Orbit Calibration and Performance of Terra MODIS Reflective Solar Bands");
        xiongSunEntry.setField("volume", "45");
        xiongSunEntry.setField("year", "2007");
        xiongSunEntry.setField("url", "http://adsabs.harvard.edu/abs/2007ITGRS..45..879X");

        ingersollPollardEntry = new BibEntry();
        ingersollPollardEntry.setType(BiblatexEntryTypes.ARTICLE);
        ingersollPollardEntry.setField("bibtexkey", "1982Icar...52...62I");
        ingersollPollardEntry.setField("author", "Ingersoll, A. P. and Pollard, D.");
        ingersollPollardEntry.setField("doi", "10.1016/0019-1035(82)90169-5");
        ingersollPollardEntry.setField("journal", "\\icarus");
        ingersollPollardEntry.setField("keywords", "Atmospheric Circulation, Barotropic Flow, Convective Flow, Flow Stability, Jupiter Atmosphere, Rotating Fluids, Saturn Atmosphere, Adiabatic Flow, Anelasticity, Compressible Fluids, Planetary Rotation, Rotating Cylinders, Scaling Laws, Wind Profiles, PLANETS, JUPITER, SATURN, MOTION, INTERIORS, ATMOSPHERE, ANALYSIS, SCALE, BAROTROPY, CHARACTERISTICS, STRUCTURE, WINDS, VISCOSITY, DATA, CONVECTION, ROTATION, EDDY EFFECTS, ENERGY, ADIABATICITY, DIAGRAMS, REVIEW, LATITUDE, ZONES, VELOCITY, MATHEMATICAL MODELS, HEAT FLOW, EQUATIONS OF MOTION, FLUIDS, DYNAMICS, TEMPERATURE, GRADIENTS");
        ingersollPollardEntry.setField("month", "#oct#");
        ingersollPollardEntry.setField("pages", "62-80");
        ingersollPollardEntry.setField("title", "Motion in the interiors and atmospheres of Jupiter and Saturn - Scale analysis, anelastic equations, barotropic stability criterion");
        ingersollPollardEntry.setField("volume", "52");
        ingersollPollardEntry.setField("year", "1982");
        ingersollPollardEntry.setField("url", "http://adsabs.harvard.edu/abs/1982Icar...52...62I");

        luceyPaulEntry = new BibEntry();
        luceyPaulEntry.setType(BiblatexEntryTypes.ARTICLE);
        luceyPaulEntry.setField("bibtexkey", "2000JGR...10520297L");
        luceyPaulEntry.setField("author", "Lucey, P. G. and Blewett, D. T. and Jolliff, B. L.");
        luceyPaulEntry.setField("doi", "10.1029/1999JE001117");
        luceyPaulEntry.setField("journal", "\\jgr");
        luceyPaulEntry.setField("keywords", "Planetology: Solid Surface Planets: Composition, Planetology: Solid Surface Planets: Remote sensing, Planetology: Solid Surface Planets: Surface materials and properties, Planetology: Solar System Objects: Moon (1221)");
        luceyPaulEntry.setField("pages", "20297-20306");
        luceyPaulEntry.setField("title", "Lunar iron and titanium abundance algorithms based on final processing of Clementine ultraviolet-visible images");
        luceyPaulEntry.setField("volume", "105");
        luceyPaulEntry.setField("year", "2000");
        luceyPaulEntry.setField("url", "http://adsabs.harvard.edu/abs/2000JGR...10520297L");
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
        List<BibEntry> fetchedEntries = fetcher.performSearch("Diez slice theorem Lie");
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
        fetchedEntry.ifPresent(entry -> entry.clearField(FieldName.ABSTRACT));//Remove abstract due to copyright
        assertEquals(Optional.of(famaeyMcGaughEntry), fetchedEntry);
    }

    @Test
    public void testPerformSearchByIdEmptyDOI() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testPerformSearchByIdInvalidDoi() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("this.doi.will.fail");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testPerformSearchBySunWelchEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1038/nmat3160");
        fetchedEntry.ifPresent(entry -> entry.clearField(FieldName.ABSTRACT)); //Remove abstract due to copyright
        assertEquals(Optional.of(sunWelchEntry), fetchedEntry);
    }

    @Test
    public void testPerformSearchByXiongSunEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1109/TGRS.2006.890567");
        assertEquals(Optional.of(xiongSunEntry), fetchedEntry);
    }

    @Test
    public void testPerformSearchByIngersollPollardEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1016/0019-1035(82)90169-5");
        assertEquals(Optional.of(ingersollPollardEntry), fetchedEntry);
    }

    @Test
    public void testPerformSearchByLuceyPaulEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1029/1999JE001117");
        assertEquals(Optional.of(luceyPaulEntry), fetchedEntry);
    }
}
