package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ArXivTest implements SearchBasedFetcherCapabilityTest {
    private ArXiv fetcher;
    private BibEntry entry;
    private BibEntry sliceTheoremPaper;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        fetcher = new ArXiv(importFormatPreferences);
        entry = new BibEntry();
        sliceTheoremPaper = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Tobias Diez")
                .withField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory")
                .withField(StandardField.DATE, "2014-05-09")
                .withField(StandardField.ABSTRACT, "A general slice theorem for the action of a Fr\\'echet Lie group on a Fr\\'echet manifolds is established. The Nash-Moser theorem provides the fundamental tool to generalize the result of Palais to this infinite-dimensional setting. The presented slice theorem is illustrated by its application to gauge theories: the action of the gauge transformation group admits smooth slices at every point and thus the gauge orbit space is stratified by Fr\\'echet manifolds. Furthermore, a covariant and symplectic formulation of classical field theory is proposed and extensively discussed. At the root of this novel framework is the incorporation of field degrees of freedom F and spacetime M into the product manifold F * M. The induced bigrading of differential forms is used in order to carry over the usual symplectic theory to this new setting. The examples of the Klein-Gordon field and general Yang-Mills theory illustrate that the presented approach conveniently handles the occurring symmetries.")
                .withField(StandardField.EPRINT, "1405.2249")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1405.2249v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "math-ph")
                .withField(StandardField.KEYWORDS, "math-ph, math.DG, math.MP, math.SG, 58B99, 58Z05, 58B25, 22E65, 58D19, 53D20, 53D42");
    }

    @Test
    void findFullTextForEmptyEntryResultsEmptyOptional() throws IOException {
        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextRejectsNullParameter() {
        assertThrows(NullPointerException.class, () -> fetcher.findFullText(null));
    }

    @Test
    void findFullTextByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1529/biophysj.104.047340");
        entry.setField(StandardField.TITLE, "Pause Point Spectra in DNA Constant-Force Unzipping");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/cond-mat/0406246v1")), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByEprint() throws IOException {
        entry.setField(StandardField.EPRINT, "1603.06570");
        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/1603.06570v1")), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByEprintWithPrefix() throws IOException {
        entry.setField(StandardField.EPRINT, "arXiv:1603.06570");
        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/1603.06570v1")), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByEprintWithUnknownDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1529/unknown");
        entry.setField(StandardField.EPRINT, "1603.06570");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/1603.06570v1")), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByTitle() throws IOException {
        entry.setField(StandardField.TITLE, "Pause Point Spectra in DNA Constant-Force Unzipping");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/cond-mat/0406246v1")), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByTitleAndPartOfAuthor() throws IOException {
        entry.setField(StandardField.TITLE, "Pause Point Spectra in DNA Constant-Force Unzipping");
        entry.setField(StandardField.AUTHOR, "Weeks and Lucks");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/cond-mat/0406246v1")), fetcher.findFullText(entry));
    }

    @Test
    void notFindFullTextByUnknownDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1529/unknown");
        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void notFindFullTextByUnknownId() throws IOException {
        entry.setField(StandardField.EPRINT, "1234.12345");
        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByDOINotAvailableInCatalog() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/0370-2693(77)90015-6");
        entry.setField(StandardField.TITLE, "Superspace formulation of supergravity");

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextEntityWithoutDoi() throws IOException {
        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextTrustLevel() {
        assertEquals(TrustLevel.PREPRINT, fetcher.getTrustLevel());
    }

    @Test
    void searchEntryByPartOfTitle() throws Exception {
        assertEquals(Collections.singletonList(sliceTheoremPaper),
                fetcher.performSearch("ti:\"slice theorem for Frechet\""));
    }

    @Test
    void searchEntryByPartOfTitleWithAcuteAccent() throws Exception {
        assertEquals(Collections.singletonList(sliceTheoremPaper),
                fetcher.performSearch("ti:\"slice theorem for Fréchet\""));
    }

    @Test
    void searchEntryByOldId() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "H1 Collaboration")
                .withField(StandardField.TITLE, "Multi-Electron Production at High Transverse Momenta in ep Collisions at HERA")
                .withField(StandardField.DATE, "2003-07-07")
                .withField(StandardField.ABSTRACT, "Multi-electron production is studied at high electron transverse momentum in positron- and electron-proton collisions using the H1 detector at HERA. The data correspond to an integrated luminosity of 115 pb-1. Di-electron and tri-electron event yields are measured. Cross sections are derived in a restricted phase space region dominated by photon-photon collisions. In general good agreement is found with the Standard Model predictions. However, for electron pair invariant masses above 100 GeV, three di-electron events and three tri-electron events are observed, compared to Standard Model expectations of 0.30 \\pm 0.04 and 0.23 \\pm 0.04, respectively.")
                .withField(StandardField.EPRINT, "hep-ex/0307015")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/hep-ex/0307015v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "hep-ex")
                .withField(StandardField.KEYWORDS, "hep-ex")
                .withField(StandardField.DOI, "10.1140/epjc/s2003-01326-x")
                .withField(StandardField.JOURNALTITLE, "Eur.Phys.J.C31:17-29,2003");

        assertEquals(Optional.of(expected), fetcher.performSearchById("hep-ex/0307015"));
    }

    @Test
    void searchEntryByIdWith4DigitsAndVersion() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), fetcher.performSearchById("1405.2249v1"));
    }

    @Test
    void searchEntryByIdWith4Digits() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), fetcher.performSearchById("1405.2249"));
    }

    @Test
    void searchEntryByIdWith4DigitsAndPrefix() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), fetcher.performSearchById("arXiv:1405.2249"));
    }

    @Test
    void searchEntryByIdWith4DigitsAndPrefixAndNotTrimmed() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), fetcher.performSearchById("arXiv : 1405. 2249"));
    }

    @Test
    void searchEntryByIdWith5Digits() throws Exception {
        assertEquals(Optional.of(
                "An Optimal Convergence Theorem for Mean Curvature Flow of Arbitrary Codimension in Hyperbolic Spaces"),
                fetcher.performSearchById("1503.06747").flatMap(entry -> entry.getField(StandardField.TITLE)));
    }

    @Test
    void searchWithMalformedIdThrowsException() throws Exception {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("123412345"));
    }

    @Test
    void searchIdentifierForSlicePaper() throws Exception {
        sliceTheoremPaper.clearField(StandardField.EPRINT);

        assertEquals(ArXivIdentifier.parse("1405.2249"), fetcher.findIdentifier(sliceTheoremPaper));
    }

    @Test
    void searchEmptyId() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById(""));
    }

    @Test
    void searchWithHttpUrl() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), fetcher.performSearchById("http://arxiv.org/abs/1405.2249"));
    }

    @Test
    void searchWithHttpsUrl() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), fetcher.performSearchById("https://arxiv.org/abs/1405.2249"));
    }

    @Test
    void searchWithHttpsUrlNotTrimmed() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), fetcher.performSearchById("https : // arxiv . org / abs / 1405 . 2249 "));
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return fetcher;
    }

    @Override
    public List<String> getTestAuthors() {
        return List.of("\"Tobias Diez\"");
    }

    @Disabled("Is not supported by the current API")
    @Test
    @Override
    public void supportsYearSearch() throws Exception {
    }

    @Disabled("Is not supported by the current API")
    @Test
    @Override
    public void supportsYearRangeSearch() throws Exception {

    }

    @Override
    public String getTestJournal() {
        return "\"Journal of Geometry and Physics (2013)\"";
    }

    @Test
    public void supportsPhraseSearch() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Tobias Büscher and Angel L. Diez and Gerhard Gompper and Jens Elgeti")
                .withField(StandardField.TITLE, "Instability and fingering of interfaces in growing tissue")
                .withField(StandardField.DATE, "2020-03-10")
                .withField(StandardField.ABSTRACT, "Interfaces in tissues are ubiquitous, both between tissue and environment as well as between populations of different cell types. The propagation of an interface can be driven mechanically. % e.g. by a difference in the respective homeostatic stress of the different cell types. Computer simulations of growing tissues are employed to study the stability of the interface between two tissues on a substrate. From a mechanical perspective, the dynamics and stability of this system is controlled mainly by four parameters of the respective tissues: (i) the homeostatic stress (ii) cell motility (iii) tissue viscosity and (iv) substrate friction. For propagation driven by a difference in homeostatic stress, the interface is stable for tissue-specific substrate friction even for very large differences of homeostatic stress; however, it becomes unstable above a critical stress difference when the tissue with the larger homeostatic stress has a higher viscosity. A small difference in directed bulk motility between the two tissues suffices to result in propagation with a stable interface, even for otherwise identical tissues. Larger differences in motility force, however, result in a finite-wavelength instability of the interface. Interestingly, the instability is apparently bound by nonlinear effects and the amplitude of the interface undulations only grows to a finite value in time.")
                .withField(StandardField.DOI, "10.1088/1367-2630/ab9e88")
                .withField(StandardField.EPRINT, "2003.04601")
                .withField(StandardField.DOI, "10.1088/1367-2630/ab9e88")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/2003.04601v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "q-bio.TO")
                .withField(StandardField.KEYWORDS, "q-bio.TO");

        List<BibEntry> resultWithPhraseSearch = fetcher.performSearch("au:\"Tobias Diez\"");
        List<BibEntry> resultWithOutPhraseSearch = fetcher.performSearch("au:Tobias Diez");
        // Ensure that phrase search result is just a subset of the default search result
        assertTrue(resultWithOutPhraseSearch.containsAll(resultWithPhraseSearch));
        resultWithOutPhraseSearch.removeAll(resultWithPhraseSearch);

        // There is only a single paper found by searching for Tobias Diez as author that is not authored by "Tobias Diez".
        assertEquals(Collections.singletonList(expected), resultWithOutPhraseSearch);
    }

    @Test
    public void supportsBooleanANDSearch() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Tobias Büscher and Angel L. Diez and Gerhard Gompper and Jens Elgeti")
                .withField(StandardField.TITLE, "Instability and fingering of interfaces in growing tissue")
                .withField(StandardField.DATE, "2020-03-10")
                .withField(StandardField.ABSTRACT, "Interfaces in tissues are ubiquitous, both between tissue and environment as well as between populations of different cell types. The propagation of an interface can be driven mechanically. % e.g. by a difference in the respective homeostatic stress of the different cell types. Computer simulations of growing tissues are employed to study the stability of the interface between two tissues on a substrate. From a mechanical perspective, the dynamics and stability of this system is controlled mainly by four parameters of the respective tissues: (i) the homeostatic stress (ii) cell motility (iii) tissue viscosity and (iv) substrate friction. For propagation driven by a difference in homeostatic stress, the interface is stable for tissue-specific substrate friction even for very large differences of homeostatic stress; however, it becomes unstable above a critical stress difference when the tissue with the larger homeostatic stress has a higher viscosity. A small difference in directed bulk motility between the two tissues suffices to result in propagation with a stable interface, even for otherwise identical tissues. Larger differences in motility force, however, result in a finite-wavelength instability of the interface. Interestingly, the instability is apparently bound by nonlinear effects and the amplitude of the interface undulations only grows to a finite value in time.")
                .withField(StandardField.DOI, "10.1088/1367-2630/ab9e88")
                .withField(StandardField.EPRINT, "2003.04601")
                .withField(StandardField.DOI, "10.1088/1367-2630/ab9e88")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/2003.04601v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "q-bio.TO")
                .withField(StandardField.KEYWORDS, "q-bio.TO");

        List<BibEntry> result = fetcher.performSearch("au:\"Tobias Büscher\" AND ti:\"Instability and fingering of interfaces\"");

        // There is only one paper authored by Tobias Büscher with that phrase in the title
        assertEquals(Collections.singletonList(expected), result);
    }
}
