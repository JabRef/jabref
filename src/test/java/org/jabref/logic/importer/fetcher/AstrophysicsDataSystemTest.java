package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
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
        diezSliceTheoremEntry.setType(StandardEntryType.Article);
        diezSliceTheoremEntry.setCiteKey("2014arXiv1405.2249D");
        diezSliceTheoremEntry.setField(StandardField.AUTHOR, "Diez, T.");
        diezSliceTheoremEntry.setField(StandardField.TITLE, "Slice theorem for Fr$\\backslash$'echet group actions and covariant symplectic field theory");
        diezSliceTheoremEntry.setField(StandardField.YEAR, "2014");
        diezSliceTheoremEntry.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        diezSliceTheoremEntry.setField(StandardField.EPRINT, "1405.2249");
        diezSliceTheoremEntry.setField(StandardField.JOURNAL, "ArXiv e-prints");
        diezSliceTheoremEntry.setField(StandardField.KEYWORDS, "Mathematical Physics, Mathematics - Differential Geometry, Mathematics - Symplectic Geometry, 58B99, 58Z05, 58B25, 22E65, 58D19, 53D20, 53D42");
        diezSliceTheoremEntry.setField(StandardField.MONTH, "#may#");
        diezSliceTheoremEntry.setField(new UnknownField("primaryclass"), "math-ph");
        diezSliceTheoremEntry.setField(StandardField.URL, "http://adsabs.harvard.edu/abs/2014arXiv1405.2249D");
        diezSliceTheoremEntry.setField(StandardField.ABSTRACT,
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
        famaeyMcGaughEntry.setType(StandardEntryType.Article);
        famaeyMcGaughEntry.setCiteKey("2012LRR....15...10F");
        famaeyMcGaughEntry.setField(StandardField.AUTHOR, "Famaey, B. and McGaugh, S. S.");
        famaeyMcGaughEntry.setField(StandardField.TITLE, "Modified Newtonian Dynamics (MOND): Observational Phenomenology and Relativistic Extensions");
        famaeyMcGaughEntry.setField(StandardField.JOURNAL, "Living Reviews in Relativity");
        famaeyMcGaughEntry.setField(StandardField.YEAR, "2012");
        famaeyMcGaughEntry.setField(StandardField.VOLUME, "15");
        famaeyMcGaughEntry.setField(StandardField.MONTH, "#sep#");
        famaeyMcGaughEntry.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        famaeyMcGaughEntry.setField(StandardField.DOI, "10.12942/lrr-2012-10");
        famaeyMcGaughEntry.setField(new UnknownField("eid"), "10");
        famaeyMcGaughEntry.setField(StandardField.EPRINT, "1112.3960");
        famaeyMcGaughEntry.setField(StandardField.PAGES, "10");
        famaeyMcGaughEntry.setField(StandardField.KEYWORDS, "astronomical observations, Newtonian limit, equations of motion, extragalactic astronomy, cosmology, theories of gravity, fundamental physics, astrophysics");
        famaeyMcGaughEntry.setField(StandardField.URL, "http://adsabs.harvard.edu/abs/2012LRR....15...10F");

        sunWelchEntry = new BibEntry();
        sunWelchEntry.setType(StandardEntryType.Article);
        sunWelchEntry.setCiteKey("2012NatMa..11...44S");
        sunWelchEntry.setField(StandardField.AUTHOR, "Sun, Y. and Welch, G. C. and Leong, W. L. and Takacs, C. J. and Bazan, G. C. and Heeger, A. J.");
        sunWelchEntry.setField(StandardField.DOI, "10.1038/nmat3160");
        sunWelchEntry.setField(StandardField.JOURNAL, "Nature Materials");
        sunWelchEntry.setField(StandardField.MONTH, "#jan#");
        sunWelchEntry.setField(StandardField.PAGES, "44-48");
        sunWelchEntry.setField(StandardField.TITLE, "Solution-processed small-molecule solar cells with 6.7\\% efficiency");
        sunWelchEntry.setField(StandardField.VOLUME, "11");
        sunWelchEntry.setField(StandardField.YEAR, "2012");
        sunWelchEntry.setField(StandardField.URL, "http://adsabs.harvard.edu/abs/2012NatMa..11...44S");

        xiongSunEntry = new BibEntry();
        xiongSunEntry.setType(StandardEntryType.Article);
        xiongSunEntry.setCiteKey("2007ITGRS..45..879X");
        xiongSunEntry.setField(StandardField.AUTHOR, "Xiong, X. and Sun, J. and Barnes, W. and Salomonson, V. and Esposito, J. and Erives, H. and Guenther, B.");
        xiongSunEntry.setField(StandardField.DOI, "10.1109/TGRS.2006.890567");
        xiongSunEntry.setField(StandardField.JOURNAL, "IEEE Transactions on Geoscience and Remote Sensing");
        xiongSunEntry.setField(StandardField.MONTH, "#apr#");
        xiongSunEntry.setField(StandardField.PAGES, "879-889");
        xiongSunEntry.setField(StandardField.TITLE, "Multiyear On-Orbit Calibration and Performance of Terra MODIS Reflective Solar Bands");
        xiongSunEntry.setField(StandardField.VOLUME, "45");
        xiongSunEntry.setField(StandardField.YEAR, "2007");
        xiongSunEntry.setField(StandardField.URL, "http://adsabs.harvard.edu/abs/2007ITGRS..45..879X");

        ingersollPollardEntry = new BibEntry();
        ingersollPollardEntry.setType(StandardEntryType.Article);
        ingersollPollardEntry.setCiteKey("1982Icar...52...62I");
        ingersollPollardEntry.setField(StandardField.AUTHOR, "Ingersoll, A. P. and Pollard, D.");
        ingersollPollardEntry.setField(StandardField.DOI, "10.1016/0019-1035(82)90169-5");
        ingersollPollardEntry.setField(StandardField.JOURNAL, "\\icarus");
        ingersollPollardEntry.setField(StandardField.KEYWORDS, "Atmospheric Circulation, Barotropic Flow, Convective Flow, Flow Stability, Jupiter Atmosphere, Rotating Fluids, Saturn Atmosphere, Adiabatic Flow, Anelasticity, Compressible Fluids, Planetary Rotation, Rotating Cylinders, Scaling Laws, Wind Profiles, PLANETS, JUPITER, SATURN, MOTION, INTERIORS, ATMOSPHERE, ANALYSIS, SCALE, BAROTROPY, CHARACTERISTICS, STRUCTURE, WINDS, VISCOSITY, DATA, CONVECTION, ROTATION, EDDY EFFECTS, ENERGY, ADIABATICITY, DIAGRAMS, REVIEW, LATITUDE, ZONES, VELOCITY, MATHEMATICAL MODELS, HEAT FLOW, EQUATIONS OF MOTION, FLUIDS, DYNAMICS, TEMPERATURE, GRADIENTS");
        ingersollPollardEntry.setField(StandardField.MONTH, "#oct#");
        ingersollPollardEntry.setField(StandardField.PAGES, "62-80");
        ingersollPollardEntry.setField(StandardField.TITLE, "Motion in the interiors and atmospheres of Jupiter and Saturn - Scale analysis, anelastic equations, barotropic stability criterion");
        ingersollPollardEntry.setField(StandardField.VOLUME, "52");
        ingersollPollardEntry.setField(StandardField.YEAR, "1982");
        ingersollPollardEntry.setField(StandardField.URL, "http://adsabs.harvard.edu/abs/1982Icar...52...62I");

        luceyPaulEntry = new BibEntry();
        luceyPaulEntry.setType(StandardEntryType.Article);
        luceyPaulEntry.setCiteKey("2000JGR...10520297L");
        luceyPaulEntry.setField(StandardField.AUTHOR, "Lucey, P. G. and Blewett, D. T. and Jolliff, B. L.");
        luceyPaulEntry.setField(StandardField.DOI, "10.1029/1999JE001117");
        luceyPaulEntry.setField(StandardField.JOURNAL, "\\jgr");
        luceyPaulEntry.setField(StandardField.KEYWORDS, "Planetology: Solid Surface Planets: Composition, Planetology: Solid Surface Planets: Remote sensing, Planetology: Solid Surface Planets: Surface materials and properties, Planetology: Solar System Objects: Moon (1221)");
        luceyPaulEntry.setField(StandardField.PAGES, "20297-20306");
        luceyPaulEntry.setField(StandardField.TITLE, "Lunar iron and titanium abundance algorithms based on final processing of Clementine ultraviolet-visible images");
        luceyPaulEntry.setField(StandardField.VOLUME, "105");
        luceyPaulEntry.setField(StandardField.YEAR, "2000");
        luceyPaulEntry.setField(StandardField.URL, "http://adsabs.harvard.edu/abs/2000JGR...10520297L");
    }

    @Test
    public void testHelpPage() {
        assertEquals("ADS", fetcher.getHelpPage().get().getPageName());
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
        searchEntry.setField(StandardField.TITLE, "slice theorem");
        searchEntry.setField(StandardField.AUTHOR, "Diez");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertFalse(fetchedEntries.isEmpty());
        assertEquals(diezSliceTheoremEntry, fetchedEntries.get(0));
    }

    @Test
    public void testPerformSearchByFamaeyMcGaughEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.12942/lrr-2012-10");
        fetchedEntry.ifPresent(entry -> entry.clearField(StandardField.ABSTRACT));//Remove abstract due to copyright
        assertEquals(Optional.of(famaeyMcGaughEntry), fetchedEntry);
    }

    @Test
    public void testPerformSearchByIdEmptyDOI() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("");
        assertEquals(Optional.empty(), fetchedEntry);
    }

    @Test
    public void testPerformSearchByIdInvalidDoi() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById("this.doi.will.fail"));
    }

    @Test
    public void testPerformSearchBySunWelchEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1038/nmat3160");
        fetchedEntry.ifPresent(entry -> entry.clearField(StandardField.ABSTRACT)); //Remove abstract due to copyright
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
