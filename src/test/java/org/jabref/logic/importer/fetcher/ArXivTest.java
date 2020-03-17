package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ArXivTest {
    private ArXiv finder;
    private BibEntry entry;
    private BibEntry sliceTheoremPaper;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        finder = new ArXiv(importFormatPreferences);
        entry = new BibEntry();

        sliceTheoremPaper = new BibEntry();
        sliceTheoremPaper.setType(StandardEntryType.Article);
        sliceTheoremPaper.setField(StandardField.AUTHOR, "Tobias Diez");
        sliceTheoremPaper.setField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory");
        sliceTheoremPaper.setField(StandardField.DATE, "2014-05-09");
        sliceTheoremPaper.setField(StandardField.ABSTRACT, "A general slice theorem for the action of a Fr\\'echet Lie group on a Fr\\'echet manifolds is established. The Nash-Moser theorem provides the fundamental tool to generalize the result of Palais to this infinite-dimensional setting. The presented slice theorem is illustrated by its application to gauge theories: the action of the gauge transformation group admits smooth slices at every point and thus the gauge orbit space is stratified by Fr\\'echet manifolds. Furthermore, a covariant and symplectic formulation of classical field theory is proposed and extensively discussed. At the root of this novel framework is the incorporation of field degrees of freedom F and spacetime M into the product manifold F * M. The induced bigrading of differential forms is used in order to carry over the usual symplectic theory to this new setting. The examples of the Klein-Gordon field and general Yang-Mills theory illustrate that the presented approach conveniently handles the occurring symmetries.");
        sliceTheoremPaper.setField(StandardField.EPRINT, "1405.2249");
        sliceTheoremPaper.setField(StandardField.FILE, ":http\\://arxiv.org/pdf/1405.2249v1:PDF");
        sliceTheoremPaper.setField(StandardField.EPRINTTYPE, "arXiv");
        sliceTheoremPaper.setField(StandardField.EPRINTCLASS, "math-ph");
        sliceTheoremPaper.setField(StandardField.KEYWORDS, "math-ph, math.DG, math.MP, math.SG, 58B99, 58Z05, 58B25, 22E65, 58D19, 53D20, 53D42");
    }

    @Test
    void findFullTextForEmptyEntryResultsEmptyOptional() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void findFullTextRejectsNullParameter() {
        assertThrows(NullPointerException.class, () -> finder.findFullText(null));
    }

    @Test
    void findFullTextByDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1529/biophysj.104.047340");
        entry.setField(StandardField.TITLE, "Pause Point Spectra in DNA Constant-Force Unzipping");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/cond-mat/0406246v1")), finder.findFullText(entry));
    }

    @Test
    void findFullTextByEprint() throws IOException {
        entry.setField(StandardField.EPRINT, "1603.06570");
        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/1603.06570v1")), finder.findFullText(entry));
    }

    @Test
    void findFullTextByEprintWithPrefix() throws IOException {
        entry.setField(StandardField.EPRINT, "arXiv:1603.06570");
        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/1603.06570v1")), finder.findFullText(entry));
    }

    @Test
    void findFullTextByEprintWithUnknownDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1529/unknown");
        entry.setField(StandardField.EPRINT, "1603.06570");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/1603.06570v1")), finder.findFullText(entry));
    }

    @Test
    void findFullTextByTitle() throws IOException {
        entry.setField(StandardField.TITLE, "Pause Point Spectra in DNA Constant-Force Unzipping");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/cond-mat/0406246v1")), finder.findFullText(entry));
    }

    @Test
    void findFullTextByTitleAndPartOfAuthor() throws IOException {
        entry.setField(StandardField.TITLE, "Pause Point Spectra in DNA Constant-Force Unzipping");
        entry.setField(StandardField.AUTHOR, "Weeks and Lucks");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/cond-mat/0406246v1")), finder.findFullText(entry));
    }

    @Test
    void notFindFullTextByUnknownDOI() throws IOException {
        entry.setField(StandardField.DOI, "10.1529/unknown");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void notFindFullTextByUnknownId() throws IOException {
        entry.setField(StandardField.EPRINT, "1234.12345");
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void findFullTextByDOINotAvailableInCatalog() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/0370-2693(77)90015-6");
        entry.setField(StandardField.TITLE, "Superspace formulation of supergravity");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void findFullTextEntityWithoutDoi() throws IOException {
        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void findFullTextTrustLevel() {
        assertEquals(TrustLevel.PREPRINT, finder.getTrustLevel());
    }

    @Test
    void searchEntryByPartOfTitle() throws Exception {
        assertEquals(Collections.singletonList(sliceTheoremPaper),
                finder.performSearch("ti:\"slice theorem for Frechet\""));
    }

    @Test
    void searchEntryByPartOfTitleWithAcuteAccent() throws Exception {
        assertEquals(Collections.singletonList(sliceTheoremPaper),
                finder.performSearch("ti:\"slice theorem for Fréchet\""));
    }

    @Test
    void searchEntryByOldId() throws Exception {
        BibEntry expected = new BibEntry();
        expected.setType(StandardEntryType.Article);
        expected.setField(StandardField.AUTHOR, "H1 Collaboration");
        expected.setField(StandardField.TITLE, "Multi-Electron Production at High Transverse Momenta in ep Collisions at HERA");
        expected.setField(StandardField.DATE, "2003-07-07");
        expected.setField(StandardField.ABSTRACT, "Multi-electron production is studied at high electron transverse momentum in positron- and electron-proton collisions using the H1 detector at HERA. The data correspond to an integrated luminosity of 115 pb-1. Di-electron and tri-electron event yields are measured. Cross sections are derived in a restricted phase space region dominated by photon-photon collisions. In general good agreement is found with the Standard Model predictions. However, for electron pair invariant masses above 100 GeV, three di-electron events and three tri-electron events are observed, compared to Standard Model expectations of 0.30 \\pm 0.04 and 0.23 \\pm 0.04, respectively.");
        expected.setField(StandardField.EPRINT, "hep-ex/0307015");
        expected.setField(StandardField.FILE, ":http\\://arxiv.org/pdf/hep-ex/0307015v1:PDF");
        expected.setField(StandardField.EPRINTTYPE, "arXiv");
        expected.setField(StandardField.EPRINTCLASS, "hep-ex");
        expected.setField(StandardField.KEYWORDS, "hep-ex");
        expected.setField(StandardField.DOI, "10.1140/epjc/s2003-01326-x");
        expected.setField(StandardField.JOURNALTITLE, "Eur.Phys.J.C31:17-29,2003");

        assertEquals(Optional.of(expected), finder.performSearchById("hep-ex/0307015"));
    }

    @Test
    void searchEntryByIdWith4DigitsAndVersion() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), finder.performSearchById("1405.2249v1"));
    }

    @Test
    void searchEntryByIdWith4Digits() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), finder.performSearchById("1405.2249"));
    }

    @Test
    void searchEntryByIdWith4DigitsAndPrefix() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), finder.performSearchById("arXiv:1405.2249"));
    }

    @Test
    void searchEntryByIdWith4DigitsAndPrefixAndNotTrimmed() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), finder.performSearchById("arXiv : 1405. 2249"));
    }

    @Test
    void searchEntryByIdWith5Digits() throws Exception {
        assertEquals(Optional.of(
                "An Optimal Convergence Theorem for Mean Curvature Flow of Arbitrary Codimension in Hyperbolic Spaces"),
                finder.performSearchById("1503.06747").flatMap(entry -> entry.getField(StandardField.TITLE)));
    }

    @Test
    void searchWithMalformedIdThrowsException() throws Exception {
        assertThrows(FetcherException.class, () -> finder.performSearchById("123412345"));
    }

    @Test
    void searchIdentifierForSlicePaper() throws Exception {
        sliceTheoremPaper.clearField(StandardField.EPRINT);

        assertEquals(ArXivIdentifier.parse("1405.2249"), finder.findIdentifier(sliceTheoremPaper));
    }

    @Test
    void searchEmptyId() throws Exception {
        assertEquals(Optional.empty(), finder.performSearchById(""));
    }

    @Test
    void searchWithHttpUrl() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), finder.performSearchById("http://arxiv.org/abs/1405.2249"));
    }

    @Test
    void searchWithHttpsUrl() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), finder.performSearchById("https://arxiv.org/abs/1405.2249"));
    }

    @Test
    void searchWithHttpsUrlNotTrimmed() throws Exception {
        assertEquals(Optional.of(sliceTheoremPaper), finder.performSearchById("https : // arxiv . org / abs / 1405 . 2249 "));
    }
}
