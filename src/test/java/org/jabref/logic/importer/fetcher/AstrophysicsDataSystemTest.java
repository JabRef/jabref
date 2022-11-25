package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
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
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(
                mock(FieldContentFormatterPreferences.class));
        fetcher = new AstrophysicsDataSystem(importFormatPreferences, importerPreferences);

        diezSliceTheoremEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("2018arXiv181204698D")
                .withField(StandardField.AUTHOR, "Diez, Tobias and Rudolph, Gerd")
                .withField(StandardField.TITLE, "Slice theorem and orbit type stratification in infinite dimensions")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.ARCHIVEPREFIX, "arXiv")
                .withField(StandardField.EPRINT, "1812.04698")
                .withField(StandardField.JOURNAL, "arXiv e-prints")
                .withField(StandardField.KEYWORDS, "Mathematics - Differential Geometry, Mathematical Physics, 58B25, (58D19, 58B20, 22E99, 58A35)")
                .withField(StandardField.MONTH, "#dec#")
                .withField(StandardField.PAGES, "arXiv:1812.04698")
                .withField(StandardField.EID, "arXiv:1812.04698")
                .withField(StandardField.PRIMARYCLASS, "math.DG")
                .withField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2018arXiv181204698D")
                .withField(StandardField.ABSTRACT,
                        "We establish a general slice theorem for the action of a locally convex         Lie group on a locally convex manifold, which generalizes the         classical slice theorem of Palais to infinite dimensions. We         discuss two important settings under which the assumptions of         this theorem are fulfilled. First, using Gl{\\\"o}ckner's inverse         function theorem, we show that the linear action of a compact         Lie group on a Fr{\\'e}chet space admits a slice. Second, using         the Nash--Moser theorem, we establish a slice theorem for the         tame action of a tame Fr{\\'e}chet Lie group on a tame         Fr{\\'e}chet manifold. For this purpose, we develop the concept         of a graded Riemannian metric, which allows the construction of         a path-length metric compatible with the manifold topology and         of a local addition. Finally, generalizing a classical result in         finite dimensions, we prove that the existence of a slice         implies that the decomposition of the manifold into orbit types         of the group action is a stratification.");

        famaeyMcGaughEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("2012LRR....15...10F")
                .withField(StandardField.AUTHOR, "Famaey, Beno{\\^\\i}t and McGaugh, Stacy S.")
                .withField(StandardField.TITLE, "Modified Newtonian Dynamics (MOND): Observational Phenomenology and Relativistic Extensions")
                .withField(StandardField.JOURNAL, "Living Reviews in Relativity")
                .withField(StandardField.YEAR, "2012")
                .withField(StandardField.VOLUME, "15")
                .withField(StandardField.MONTH, "#sep#")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.ARCHIVEPREFIX, "arXiv")
                .withField(StandardField.DOI, "10.12942/lrr-2012-10")
                .withField(StandardField.PRIMARYCLASS, "astro-ph.CO")
                .withField(StandardField.EID, "10")
                .withField(StandardField.EPRINT, "1112.3960")
                .withField(StandardField.PAGES, "10")
                .withField(StandardField.KEYWORDS, "astronomical observations, Newtonian limit, equations of motion, extragalactic astronomy, cosmology, theories of gravity, fundamental physics, astrophysics, Astrophysics - Cosmology and Nongalactic Astrophysics, Astrophysics - Astrophysics of Galaxies, General Relativity and Quantum Cosmology, High Energy Physics - Phenomenology, High Energy Physics - Theory")
                .withField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2012LRR....15...10F");

        sunWelchEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("2012NatMa..11...44S")
                .withField(StandardField.AUTHOR, "Sun, Yanming and Welch, Gregory C. and Leong, Wei Lin and Takacs, Christopher J. and Bazan, Guillermo C. and Heeger, Alan J.")
                .withField(StandardField.DOI, "10.1038/nmat3160")
                .withField(StandardField.JOURNAL, "Nature Materials")
                .withField(StandardField.MONTH, "#jan#")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "44-48")
                .withField(StandardField.TITLE, "Solution-processed small-molecule solar cells with 6.7\\% efficiency")
                .withField(StandardField.VOLUME, "11")
                .withField(StandardField.YEAR, "2012")
                .withField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2012NatMa..11...44S");

        xiongSunEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("2007ITGRS..45..879X")
                .withField(StandardField.AUTHOR, "Xiong, Xiaoxiong and Sun, Junqiang and Barnes, William and Salomonson, Vincent and Esposito, Joseph and Erives, Hector and Guenther, Bruce")
                .withField(StandardField.DOI, "10.1109/TGRS.2006.890567")
                .withField(StandardField.JOURNAL, "IEEE Transactions on Geoscience and Remote Sensing")
                .withField(StandardField.MONTH, "#apr#")
                .withField(StandardField.NUMBER, "4")
                .withField(StandardField.PAGES, "879-889")
                .withField(StandardField.TITLE, "Multiyear On-Orbit Calibration and Performance of Terra MODIS Reflective Solar Bands")
                .withField(StandardField.VOLUME, "45")
                .withField(StandardField.YEAR, "2007")
                .withField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2007ITGRS..45..879X");

        ingersollPollardEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("1982Icar...52...62I")
                .withField(StandardField.ABSTRACT, "If Jupiter's and Saturn's fluid interiors were inviscid and adiabatic,         any steady zonal motion would take the form of differentially         rotating cylinders concentric about the planetary axis of         rotation. B. A. Smith et al. [ Science215, 504-537 (1982)]         showed that Saturn's observed zonal wind profile extends a         significant distance below cloud base. Further extension into         the interior occurs if the values of the eddy viscosity and         superadiabaticity are small. We estimate these values using a         scaling analysis of deep convection in the presence of         differential rotation. The differential rotation inhibits the         convection and reduces the effective eddy viscosity. Viscous         dissipation of zonal mean kinetic energy is then within the         bounds set by the internal heat source. The differential         rotation increases the superadiabaticity, but not so much as to         eliminate the cylindrical structure of the flow. Very large         departures from adiabaticity, necessary for decoupling the         atmosphere and interior, do not occur. Using our scaling         analysis we develop the anelastic equations that describe         motions in Jupiter's and Saturn's interiors. A simple problem is         solved, that of an adiabatic fluid with a steady zonal wind         varying as a function of cylindrical radius. Low zonal         wavenumber perturbations are two dimensional (independent of the         axial coordinate) and obey a modified barotropic stability         equation. The parameter analogous to {\\ensuremath{\\beta}} is         negative and is three to four times larger than the         {\\ensuremath{\\beta}} for thin atmospheres. Jupiter's and         Saturn's observed zonal wind profiles are close to marginal         stability according to this deep sphere criterion, but are         several times supercritical according to the thin atmosphere         criterion.")
                .withField(StandardField.AUTHOR, "Ingersoll, A.~P. and Pollard, D.")
                .withField(StandardField.DOI, "10.1016/0019-1035(82)90169-5")
                .withField(StandardField.JOURNAL, "\\icarus")
                .withField(StandardField.KEYWORDS, "Atmospheric Circulation, Barotropic Flow, Convective Flow, Flow Stability, Jupiter Atmosphere, Rotating Fluids, Saturn Atmosphere, Adiabatic Flow, Anelasticity, Compressible Fluids, Planetary Rotation, Rotating Cylinders, Scaling Laws, Wind Profiles, PLANETS, JUPITER, SATURN, MOTION, INTERIORS, ATMOSPHERE, ANALYSIS, SCALE, BAROTROPY, CHARACTERISTICS, STRUCTURE, WINDS, VISCOSITY, DATA, CONVECTION, ROTATION, EDDY EFFECTS, ENERGY, ADIABATICITY, DIAGRAMS, REVIEW, LATITUDE, ZONES, VELOCITY, MATHEMATICAL MODELS, HEAT FLOW, EQUATIONS OF MOTION, FLUIDS, DYNAMICS, TEMPERATURE, GRADIENTS, Lunar and Planetary Exploration; Planets")
                .withField(StandardField.MONTH, "#oct#")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "62-80")
                .withField(StandardField.TITLE, "Motion in the interiors and atmospheres of Jupiter and Saturn: scale analysis, anelastic equations, barotropic stability criterion")
                .withField(StandardField.VOLUME, "52")
                .withField(StandardField.YEAR, "1982")
                .withField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/1982Icar...52...62I");

        luceyPaulEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("2000JGR...10520297L")
                .withField(StandardField.AUTHOR, "Lucey, Paul G. and Blewett, David T. and Jolliff, Bradley L.")
                .withField(StandardField.DOI, "10.1029/1999JE001117")
                .withField(StandardField.JOURNAL, "\\jgr")
                .withField(StandardField.KEYWORDS, "Planetology: Solid Surface Planets: Composition, Planetology: Solid Surface Planets: Remote sensing, Planetology: Solid Surface Planets: Surface materials and properties, Planetology: Solar System Objects: Moon (1221)")
                .withField(StandardField.PAGES, "20297-20306")
                .withField(StandardField.TITLE, "Lunar iron and titanium abundance algorithms based on final processing of Clementine ultraviolet-visible images")
                .withField(StandardField.VOLUME, "105")
                .withField(StandardField.YEAR, "2000")
                .withField(StandardField.URL, "https://ui.adsabs.harvard.edu/abs/2000JGR...10520297L")
                .withField(StandardField.MONTH, "#jan#")
                .withField(StandardField.NUMBER, "E8")
                .withField(StandardField.ABSTRACT, "The Clementine mission to the Moon returned global imaging data         collected by the ultraviolet visible (UVVIS) camera. This data         set is now in a final state of calibration, and a five-band         multispectral digital image model (DIM) of the lunar surface         will soon be available to the science community. We have used         observations of the lunar sample-return sites and stations         extracted from the final DIM in conjunction with compositional         information for returned lunar soils to revise our previously         published algorithms for the spectral determination of the FeO         and TiO$_{2}$ content of the lunar surface. The algorithms         successfully normalize the effects of space weathering so that         composition may be determined without regard to a surface's         state of maturity. These algorithms permit anyone with access to         the standard archived DIM to construct high spatial resolution         maps of FeO and TiO$_{2}$ abundance. Such maps will be of great         utility in a variety of lunar geologic studies.");
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
        BibEntry searchEntry = new BibEntry()
                .withField(StandardField.TITLE, "slice theorem")
                .withField(StandardField.AUTHOR, "Diez");

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
