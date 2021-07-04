package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.paging.Page;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class AstrophysicsDataSystemTest implements PagedSearchFetcherTest {

    private AstrophysicsDataSystem fetcher;
    private BibEntry diezSliceTheoremEntry;
    private BibEntry famaeyMcGaughEntry;
    private BibEntry sunWelchEntry;
    private BibEntry xiongSunEntry;
    private BibEntry ingersollPollardEntry;
    private BibEntry luceyPaulEntry;

    @BeforeEach
    public void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(
                mock(FieldContentFormatterPreferences.class));
        fetcher = new AstrophysicsDataSystem(importFormatPreferences);

        diezSliceTheoremEntry = new BibEntry();
        diezSliceTheoremEntry.setType(StandardEntryType.Article);
        diezSliceTheoremEntry.setCitationKey("2018arXiv181204698D");
        diezSliceTheoremEntry.setField(StandardField.AUTHOR, "Diez, Tobias and Rudolph, Gerd");
        diezSliceTheoremEntry.setField(StandardField.TITLE, "Slice theorem and orbit type stratification in infinite dimensions");
        diezSliceTheoremEntry.setField(StandardField.YEAR, "2018");
        diezSliceTheoremEntry.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        diezSliceTheoremEntry.setField(StandardField.EPRINT, "1812.04698");
        diezSliceTheoremEntry.setField(StandardField.JOURNAL, "arXiv e-prints");
        diezSliceTheoremEntry.setField(StandardField.KEYWORDS, "Mathematics - Differential Geometry, Mathematical Physics, 58B25, (58D19, 58B20, 22E99, 58A35)");
        diezSliceTheoremEntry.setField(StandardField.MONTH, "#dec#");
        diezSliceTheoremEntry.setField(StandardField.PAGES, "arXiv:1812.04698");
        diezSliceTheoremEntry.setField(StandardField.EID, "arXiv:1812.04698");
        diezSliceTheoremEntry.setField(StandardField.PRIMARYCLASS, "math.DG");
        diezSliceTheoremEntry.setField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2018arXiv181204698D");
        diezSliceTheoremEntry.setField(StandardField.ABSTRACT,
                "We establish a general slice theorem for the action of a locally convex         Lie group on a locally convex manifold, which generalizes the         classical slice theorem of Palais to infinite dimensions. We         discuss two important settings under which the assumptions of         this theorem are fulfilled. First, using Gl{\\\"o}ckner's inverse         function theorem, we show that the linear action of a compact         Lie group on a Fr{\\'e}chet space admits a slice. Second, using         the Nash--Moser theorem, we establish a slice theorem for the         tame action of a tame Fr{\\'e}chet Lie group on a tame         Fr{\\'e}chet manifold. For this purpose, we develop the concept         of a graded Riemannian metric, which allows the construction of         a path-length metric compatible with the manifold topology and         of a local addition. Finally, generalizing a classical result in         finite dimensions, we prove that the existence of a slice         implies that the decomposition of the manifold into orbit types         of the group action is a stratification.");

        famaeyMcGaughEntry = new BibEntry();
        famaeyMcGaughEntry.setType(StandardEntryType.Article);
        famaeyMcGaughEntry.setCitationKey("2012LRR....15...10F");
        famaeyMcGaughEntry.setField(StandardField.AUTHOR, "Famaey, Beno{\\^\\i}t and McGaugh, Stacy S.");
        famaeyMcGaughEntry.setField(StandardField.TITLE, "Modified Newtonian Dynamics (MOND): Observational Phenomenology and Relativistic Extensions");
        famaeyMcGaughEntry.setField(StandardField.JOURNAL, "Living Reviews in Relativity");
        famaeyMcGaughEntry.setField(StandardField.YEAR, "2012");
        famaeyMcGaughEntry.setField(StandardField.VOLUME, "15");
        famaeyMcGaughEntry.setField(StandardField.MONTH, "#sep#");
        famaeyMcGaughEntry.setField(StandardField.NUMBER, "1");
        famaeyMcGaughEntry.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        famaeyMcGaughEntry.setField(StandardField.DOI, "10.12942/lrr-2012-10");
        famaeyMcGaughEntry.setField(StandardField.PRIMARYCLASS, "astro-ph.CO");
        famaeyMcGaughEntry.setField(StandardField.EID, "10");
        famaeyMcGaughEntry.setField(StandardField.EPRINT, "1112.3960");
        famaeyMcGaughEntry.setField(StandardField.PAGES, "10");
        famaeyMcGaughEntry.setField(StandardField.KEYWORDS, "astronomical observations, Newtonian limit, equations of motion, extragalactic astronomy, cosmology, theories of gravity, fundamental physics, astrophysics, Astrophysics - Cosmology and Nongalactic Astrophysics, Astrophysics - Astrophysics of Galaxies, General Relativity and Quantum Cosmology, High Energy Physics - Phenomenology, High Energy Physics - Theory");
        famaeyMcGaughEntry.setField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2012LRR....15...10F");

        sunWelchEntry = new BibEntry();
        sunWelchEntry.setType(StandardEntryType.Article);
        sunWelchEntry.setCitationKey("2012NatMa..11...44S");
        sunWelchEntry.setField(StandardField.AUTHOR, "Sun, Yanming and Welch, Gregory C. and Leong, Wei Lin and Takacs, Christopher J. and Bazan, Guillermo C. and Heeger, Alan J.");
        sunWelchEntry.setField(StandardField.DOI, "10.1038/nmat3160");
        sunWelchEntry.setField(StandardField.JOURNAL, "Nature Materials");
        sunWelchEntry.setField(StandardField.MONTH, "#jan#");
        sunWelchEntry.setField(StandardField.NUMBER, "1");
        sunWelchEntry.setField(StandardField.PAGES, "44-48");
        sunWelchEntry.setField(StandardField.TITLE, "Solution-processed small-molecule solar cells with 6.7\\% efficiency");
        sunWelchEntry.setField(StandardField.VOLUME, "11");
        sunWelchEntry.setField(StandardField.YEAR, "2012");
        sunWelchEntry.setField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2012NatMa..11...44S");

        xiongSunEntry = new BibEntry();
        xiongSunEntry.setType(StandardEntryType.Article);
        xiongSunEntry.setCitationKey("2007ITGRS..45..879X");
        xiongSunEntry.setField(StandardField.AUTHOR, "Xiong, Xiaoxiong and Sun, Junqiang and Barnes, William and Salomonson, Vincent and Esposito, Joseph and Erives, Hector and Guenther, Bruce");
        xiongSunEntry.setField(StandardField.DOI, "10.1109/TGRS.2006.890567");
        xiongSunEntry.setField(StandardField.JOURNAL, "IEEE Transactions on Geoscience and Remote Sensing");
        xiongSunEntry.setField(StandardField.MONTH, "#apr#");
        xiongSunEntry.setField(StandardField.NUMBER, "4");
        xiongSunEntry.setField(StandardField.PAGES, "879-889");
        xiongSunEntry.setField(StandardField.TITLE, "Multiyear On-Orbit Calibration and Performance of Terra MODIS Reflective Solar Bands");
        xiongSunEntry.setField(StandardField.VOLUME, "45");
        xiongSunEntry.setField(StandardField.YEAR, "2007");
        xiongSunEntry.setField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2007ITGRS..45..879X");

        ingersollPollardEntry = new BibEntry();
        ingersollPollardEntry.setType(StandardEntryType.Article);
        ingersollPollardEntry.setCitationKey("1982Icar...52...62I");
        ingersollPollardEntry.setField(StandardField.ABSTRACT, "If Jupiter's and Saturn's fluid interiors were inviscid and adiabatic,         any steady zonal motion would take the form of differentially         rotating cylinders concentric about the planetary axis of         rotation. B. A. Smith et al. [ Science215, 504-537 (1982)]         showed that Saturn's observed zonal wind profile extends a         significant distance below cloud base. Further extension into         the interior occurs if the values of the eddy viscosity and         superadiabaticity are small. We estimate these values using a         scaling analysis of deep convection in the presence of         differential rotation. The differential rotation inhibits the         convection and reduces the effective eddy viscosity. Viscous         dissipation of zonal mean kinetic energy is then within the         bounds set by the internal heat source. The differential         rotation increases the superadiabaticity, but not so much as to         eliminate the cylindrical structure of the flow. Very large         departures from adiabaticity, necessary for decoupling the         atmosphere and interior, do not occur. Using our scaling         analysis we develop the anelastic equations that describe         motions in Jupiter's and Saturn's interiors. A simple problem is         solved, that of an adiabatic fluid with a steady zonal wind         varying as a function of cylindrical radius. Low zonal         wavenumber perturbations are two dimensional (independent of the         axial coordinate) and obey a modified barotropic stability         equation. The parameter analogous to {\\ensuremath{\\beta}} is         negative and is three to four times larger than the         {\\ensuremath{\\beta}} for thin atmospheres. Jupiter's and         Saturn's observed zonal wind profiles are close to marginal         stability according to this deep sphere criterion, but are         several times supercritical according to the thin atmosphere         criterion.");
        ingersollPollardEntry.setField(StandardField.AUTHOR, "Ingersoll, A. P. and Pollard, D.");
        ingersollPollardEntry.setField(StandardField.DOI, "10.1016/0019-1035(82)90169-5");
        ingersollPollardEntry.setField(StandardField.JOURNAL, "\\icarus");
        ingersollPollardEntry.setField(StandardField.KEYWORDS, "Atmospheric Circulation, Barotropic Flow, Convective Flow, Flow Stability, Jupiter Atmosphere, Rotating Fluids, Saturn Atmosphere, Adiabatic Flow, Anelasticity, Compressible Fluids, Planetary Rotation, Rotating Cylinders, Scaling Laws, Wind Profiles, PLANETS, JUPITER, SATURN, MOTION, INTERIORS, ATMOSPHERE, ANALYSIS, SCALE, BAROTROPY, CHARACTERISTICS, STRUCTURE, WINDS, VISCOSITY, DATA, CONVECTION, ROTATION, EDDY EFFECTS, ENERGY, ADIABATICITY, DIAGRAMS, REVIEW, LATITUDE, ZONES, VELOCITY, MATHEMATICAL MODELS, HEAT FLOW, EQUATIONS OF MOTION, FLUIDS, DYNAMICS, TEMPERATURE, GRADIENTS, Lunar and Planetary Exploration; Planets");
        ingersollPollardEntry.setField(StandardField.MONTH, "#oct#");
        ingersollPollardEntry.setField(StandardField.NUMBER, "1");
        ingersollPollardEntry.setField(StandardField.PAGES, "62-80");
        ingersollPollardEntry.setField(StandardField.TITLE, "Motion in the interiors and atmospheres of Jupiter and Saturn: scale analysis, anelastic equations, barotropic stability criterion");
        ingersollPollardEntry.setField(StandardField.VOLUME, "52");
        ingersollPollardEntry.setField(StandardField.YEAR, "1982");
        ingersollPollardEntry.setField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/1982Icar...52...62I");

        luceyPaulEntry = new BibEntry();
        luceyPaulEntry.setType(StandardEntryType.Article);
        luceyPaulEntry.setCitationKey("2000JGR...10520297L");
        luceyPaulEntry.setField(StandardField.AUTHOR, "Lucey, Paul G. and Blewett, David T. and Jolliff, Bradley L.");
        luceyPaulEntry.setField(StandardField.DOI, "10.1029/1999JE001117");
        luceyPaulEntry.setField(StandardField.JOURNAL, "\\jgr");
        luceyPaulEntry.setField(StandardField.KEYWORDS, "Planetology: Solid Surface Planets: Composition, Planetology: Solid Surface Planets: Remote sensing, Planetology: Solid Surface Planets: Surface materials and properties, Planetology: Solar System Objects: Moon (1221)");
        luceyPaulEntry.setField(StandardField.PAGES, "20297-20306");
        luceyPaulEntry.setField(StandardField.TITLE, "Lunar iron and titanium abundance algorithms based on final processing of Clementine ultraviolet-visible images");
        luceyPaulEntry.setField(StandardField.VOLUME, "105");
        luceyPaulEntry.setField(StandardField.YEAR, "2000");
        luceyPaulEntry.setField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2000JGR...10520297L");
        luceyPaulEntry.setField(StandardField.MONTH, "#jan#");
        luceyPaulEntry.setField(StandardField.NUMBER, "E8");
    }

    @Test
    public void testGetName() {
        assertEquals("SAO/NASA ADS", fetcher.getName());
    }

    @Test
    public void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Diez slice theorem Lie");
        assertFalse(fetchedEntries.isEmpty());
        assertTrue(fetchedEntries.contains(diezSliceTheoremEntry));
    }

    @Test
    public void searchByEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.TITLE, "slice theorem");
        searchEntry.setField(StandardField.AUTHOR, "Diez");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);

        // The list contains more than one element, thus we need to check in two steps and cannot use assertEquals(List.of(diezSliceTheoremEntry, fetchedEntries))
        assertFalse(fetchedEntries.isEmpty());
        assertTrue(fetchedEntries.contains(diezSliceTheoremEntry));
    }

    @Test
    public void testPerformSearchByFamaeyMcGaughEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.12942/lrr-2012-10");
        fetchedEntry.ifPresent(entry -> entry.clearField(StandardField.ABSTRACT)); // Remove abstract due to copyright
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
        fetchedEntry.ifPresent(entry -> entry.clearField(StandardField.ABSTRACT)); // Remove abstract due to copyright
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
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("2000JGR...10520297L");
        assertEquals(Optional.of(luceyPaulEntry), fetchedEntry);
    }

    @Test
    public void performSearchByQueryPaged_searchLimitsSize() throws Exception {
        Page<BibEntry> page = fetcher.performSearchPaged("author:\"A\"", 0);
        assertEquals(fetcher.getPageSize(), page.getSize(), "fetcher return wrong page size");
    }

    @Test
    public void performSearchByQueryPaged_invalidAuthorsReturnEmptyPages() throws Exception {
        Page<BibEntry> page = fetcher.performSearchPaged("author:\"ThisAuthorWillNotBeFound\"", 0);
        Page<BibEntry> page5 = fetcher.performSearchPaged("author:\"ThisAuthorWillNotBeFound\"", 5);
        assertEquals(0, page.getSize(), "fetcher doesnt return empty pages for invalid author");
        assertEquals(0, page5.getSize(), "fetcher doesnt return empty pages for invalid author");
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return fetcher;
    }
}
