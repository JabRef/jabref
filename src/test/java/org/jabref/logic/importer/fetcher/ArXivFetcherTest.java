package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ArXivFetcherTest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {
    private static ImportFormatPreferences importFormatPreferences;

    private ArXivFetcher fetcher;
    private BibEntry entry;
    private BibEntry sliceTheoremPaper;

    private BibEntry mainOriginalPaper;
    private BibEntry mainResultPaper;

    private BibEntry completePaper;

    @BeforeAll
    static void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        // Used during DOI fetch process
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(
                new FieldContentFormatterPreferences(
                        Arrays.stream("pdf;ps;url;doi;file;isbn;issn".split(";"))
                              .map(fieldName -> StandardField.fromName(fieldName).isPresent() ? StandardField.fromName(fieldName).get() : new UnknownField(fieldName))
                              .collect(Collectors.toList())));
    }

    @BeforeEach
    void eachSetUp() {
        fetcher = new ArXivFetcher(importFormatPreferences);
        entry = new BibEntry();

        // A BibEntry with information only from ArXiv API
        mainOriginalPaper = new BibEntry(StandardEntryType.Article)
                // ArXiv-original fields
                .withField(StandardField.AUTHOR, "Joeran Beel and Andrew Collins and Akiko Aizawa")
                .withField(StandardField.TITLE, "The Architecture of Mr. DLib's Scientific Recommender-System API")
                .withField(StandardField.DATE, "2018-11-26")
                .withField(StandardField.ABSTRACT, "Recommender systems in academia are not widely available. This may be in part due to the difficulty and cost of developing and maintaining recommender systems. Many operators of academic products such as digital libraries and reference managers avoid this effort, although a recommender system could provide significant benefits to their users. In this paper, we introduce Mr. DLib's \"Recommendations as-a-Service\" (RaaS) API that allows operators of academic products to easily integrate a scientific recommender system into their products. Mr. DLib generates recommendations for research articles but in the future, recommendations may include call for papers, grants, etc. Operators of academic products can request recommendations from Mr. DLib and display these recommendations to their users. Mr. DLib can be integrated in just a few hours or days; creating an equivalent recommender system from scratch would require several months for an academic operator. Mr. DLib has been used by GESIS Sowiport and by the reference manager JabRef. Mr. DLib is open source and its goal is to facilitate the application of, and research on, scientific recommender systems. In this paper, we present the motivation for Mr. DLib, the architecture and details about the effectiveness. Mr. DLib has delivered 94m recommendations over a span of two years with an average click-through rate of 0.12%.")
                .withField(StandardField.EPRINT, "1811.10364")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1811.10364v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "cs.IR")
                .withField(StandardField.KEYWORDS, "cs.IR, cs.AI, cs.DL, cs.LG");

        mainResultPaper = new BibEntry(StandardEntryType.Article)
                // ArXiv-original fields
//              .withField(StandardField.AUTHOR, "Joeran Beel and Andrew Collins and Akiko Aizawa")
                .withField(StandardField.TITLE, "The Architecture of Mr. DLib's Scientific Recommender-System API")
                .withField(StandardField.DATE, "2018-11-26")
                .withField(StandardField.ABSTRACT, "Recommender systems in academia are not widely available. This may be in part due to the difficulty and cost of developing and maintaining recommender systems. Many operators of academic products such as digital libraries and reference managers avoid this effort, although a recommender system could provide significant benefits to their users. In this paper, we introduce Mr. DLib's \"Recommendations as-a-Service\" (RaaS) API that allows operators of academic products to easily integrate a scientific recommender system into their products. Mr. DLib generates recommendations for research articles but in the future, recommendations may include call for papers, grants, etc. Operators of academic products can request recommendations from Mr. DLib and display these recommendations to their users. Mr. DLib can be integrated in just a few hours or days; creating an equivalent recommender system from scratch would require several months for an academic operator. Mr. DLib has been used by GESIS Sowiport and by the reference manager JabRef. Mr. DLib is open source and its goal is to facilitate the application of, and research on, scientific recommender systems. In this paper, we present the motivation for Mr. DLib, the architecture and details about the effectiveness. Mr. DLib has delivered 94m recommendations over a span of two years with an average click-through rate of 0.12%.")
                .withField(StandardField.EPRINT, "1811.10364")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1811.10364v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "cs.IR")
//              .withField(StandardField.KEYWORDS, "cs.IR, cs.AI, cs.DL, cs.LG")
                // Unavailable info:
                // StandardField.JOURNALTITLE // INFO NOT APPLICABLE TO THIS ENTRY
                // ArXiv-issue DOI fields
                .withField(new UnknownField("copyright"), "arXiv.org perpetual, non-exclusive license")
                .withField((InternalField.KEY_FIELD), "https://doi.org/10.48550/arxiv.1811.10364")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.KEYWORDS, "Information Retrieval (cs.IR), Artificial Intelligence (cs.AI), Digital Libraries (cs.DL), Machine Learning (cs.LG), FOS: Computer and information sciences")
                .withField(StandardField.AUTHOR, "Beel, Joeran and Collins, Andrew and Aizawa, Akiko")
                .withField(StandardField.PUBLISHER, "arXiv")
                .withField(StandardField.DOI, "10.48550/ARXIV.1811.10364");

        // Example of a robust result, with information from both ArXiv-assigned and user-assigned DOIs
        completePaper = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Büscher, Tobias and Diez, Angel L. and Gompper, Gerhard and Elgeti, Jens")
                .withField(StandardField.TITLE, "Instability and fingering of interfaces in growing tissue")
                .withField(StandardField.DATE, "2020-03-10")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.MONTH, "aug")
                .withField(StandardField.NUMBER, "8")
                .withField(StandardField.VOLUME, "22")
                .withField(StandardField.PAGES, "083005")
                .withField(StandardField.PUBLISHER, "{IOP} Publishing")
                .withField(StandardField.JOURNAL, "New Journal of Physics")
                .withField(StandardField.ABSTRACT, "Interfaces in tissues are ubiquitous, both between tissue and environment as well as between populations of different cell types. The propagation of an interface can be driven mechanically. % e.g. by a difference in the respective homeostatic stress of the different cell types. Computer simulations of growing tissues are employed to study the stability of the interface between two tissues on a substrate. From a mechanical perspective, the dynamics and stability of this system is controlled mainly by four parameters of the respective tissues: (i) the homeostatic stress (ii) cell motility (iii) tissue viscosity and (iv) substrate friction. For propagation driven by a difference in homeostatic stress, the interface is stable for tissue-specific substrate friction even for very large differences of homeostatic stress; however, it becomes unstable above a critical stress difference when the tissue with the larger homeostatic stress has a higher viscosity. A small difference in directed bulk motility between the two tissues suffices to result in propagation with a stable interface, even for otherwise identical tissues. Larger differences in motility force, however, result in a finite-wavelength instability of the interface. Interestingly, the instability is apparently bound by nonlinear effects and the amplitude of the interface undulations only grows to a finite value in time.")
                .withField(StandardField.DOI, "10.1088/1367-2630/ab9e88")
                .withField(StandardField.EPRINT, "2003.04601")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/2003.04601v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "q-bio.TO")
                .withField(StandardField.KEYWORDS, "Tissues and Organs (q-bio.TO), FOS: Biological sciences")
                .withField(InternalField.KEY_FIELD, "B_scher_2020")
                .withField(new UnknownField("copyright"), "arXiv.org perpetual, non-exclusive license");

        sliceTheoremPaper = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Diez, Tobias")
                .withField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory")
                .withField(StandardField.DATE, "2014-05-09")
                .withField(StandardField.YEAR, "2014")
                .withField(StandardField.PUBLISHER, "arXiv")
                .withField(StandardField.ABSTRACT, "A general slice theorem for the action of a Fr\\'echet Lie group on a Fr\\'echet manifolds is established. The Nash-Moser theorem provides the fundamental tool to generalize the result of Palais to this infinite-dimensional setting. The presented slice theorem is illustrated by its application to gauge theories: the action of the gauge transformation group admits smooth slices at every point and thus the gauge orbit space is stratified by Fr\\'echet manifolds. Furthermore, a covariant and symplectic formulation of classical field theory is proposed and extensively discussed. At the root of this novel framework is the incorporation of field degrees of freedom F and spacetime M into the product manifold F * M. The induced bigrading of differential forms is used in order to carry over the usual symplectic theory to this new setting. The examples of the Klein-Gordon field and general Yang-Mills theory illustrate that the presented approach conveniently handles the occurring symmetries.")
                .withField(StandardField.DOI, "10.48550/ARXIV.1405.2249")
                .withField(StandardField.EPRINT, "1405.2249")
                .withField(StandardField.EPRINTCLASS, "math-ph")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1405.2249v1:PDF")
                .withField(StandardField.KEYWORDS, "Mathematical Physics (math-ph), Differential Geometry (math.DG), Symplectic Geometry (math.SG), FOS: Physical sciences, FOS: Mathematics, 58B99, 58Z05, 58B25, 22E65, 58D19, 53D20, 53D42")
                .withField(InternalField.KEY_FIELD, "https://doi.org/10.48550/arxiv.1405.2249")
                .withField(new UnknownField("copyright"), "arXiv.org perpetual, non-exclusive license");
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return fetcher;
    }

    public List<String> getInputTestAuthors() {
        return Arrays.stream(mainOriginalPaper.getField(StandardField.AUTHOR).get()
                                              .split("and")).map(String::trim).collect(Collectors.toList());
    }

    @Override
    public List<String> getTestAuthors() {
        return Arrays.stream(mainResultPaper.getField(StandardField.AUTHOR).get()
                                            .split("and")).map(String::trim).collect(Collectors.toList());
    }

    @Override
    public String getTestJournal() {
        return "Journal of Geometry and Physics (2013)";
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return fetcher;
    }

    @Test
    @Override
    public void supportsAuthorSearch() throws FetcherException {
        StringJoiner queryBuilder = new StringJoiner("\" AND author:\"", "author:\"", "\"");
        getInputTestAuthors().forEach(queryBuilder::add);

        List<BibEntry> result = getFetcher().performSearch(queryBuilder.toString());
        new ImportCleanup(BibDatabaseMode.BIBTEX).doPostCleanup(result);

        assertFalse(result.isEmpty());
        result.forEach(bibEntry -> {
            String author = bibEntry.getField(StandardField.AUTHOR).orElse("");

            // The co-authors differ, thus we check for the author present at all papers
            getTestAuthors().forEach(expectedAuthor -> Assertions.assertTrue(author.contains(expectedAuthor.replace("\"", ""))));
        });
    }

    @Test
    public void noSupportsAuthorSearchWithLastFirstName() throws FetcherException {
        StringJoiner queryBuilder = new StringJoiner("\" AND author:\"", "author:\"", "\"");
        getTestAuthors().forEach(queryBuilder::add);

        List<BibEntry> result = getFetcher().performSearch(queryBuilder.toString());
        new ImportCleanup(BibDatabaseMode.BIBTEX).doPostCleanup(result);

        assertTrue(result.isEmpty());
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
    void findFullTextByTitleWithCurlyBracket() throws IOException {
        entry.setField(StandardField.TITLE, "Machine versus {Human} {Attention} in {Deep} {Reinforcement} {Learning} {Tasks}");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/2010.15942v3")), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByTitleWithColonAndJournalWithoutEprint() throws IOException {
        entry.setField(StandardField.TITLE, "Bayes-TrEx: a Bayesian Sampling Approach to Model Transparency by Example");
        entry.setField(StandardField.JOURNAL, "arXiv:2002.10248v4 [cs]");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/2002.10248v4")), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByTitleWithColonAndUrlWithoutEprint() throws IOException {
        entry.setField(StandardField.TITLE, "Bayes-TrEx: a Bayesian Sampling Approach to Model Transparency by Example");
        entry.setField(StandardField.URL, "http://arxiv.org/abs/2002.10248v4");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/2002.10248v4")), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByTitleAndPartOfAuthor() throws IOException {
        entry.setField(StandardField.TITLE, "Pause Point Spectra in DNA Constant-Force Unzipping");
        entry.setField(StandardField.AUTHOR, "Weeks and Lucks");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/cond-mat/0406246v1")), fetcher.findFullText(entry));
    }

    @Test
    void findFullTextByTitleWithCurlyBracketAndPartOfAuthor() throws IOException {
        entry.setField(StandardField.TITLE, "Machine versus {Human} {Attention} in {Deep} {Reinforcement} {Learning} {Tasks}");
        entry.setField(StandardField.AUTHOR, "Zhang, Ruohan and Guo");

        assertEquals(Optional.of(new URL("http://arxiv.org/pdf/2010.15942v3")), fetcher.findFullText(entry));
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
        assertEquals(Collections.singletonList(mainResultPaper),
                fetcher.performSearch("title:\"the architecture of mr. dLib's\""));
    }

    @Test
    void searchEntryByPartOfTitleWithAcuteAccent() throws Exception {
        assertEquals(Collections.singletonList(sliceTheoremPaper),
                fetcher.performSearch("title:\"slice theorem for Fréchet\""));
    }

    @Test
    void searchEntryByOldId() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "{H1 Collaboration}")
                .withField(StandardField.TITLE, "Multi-Electron Production at High Transverse Momenta in ep Collisions at HERA")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.VOLUME, "31")
                .withField(StandardField.PAGES, "17--29")
                .withField(StandardField.DATE, "2003-07-07")
                .withField(StandardField.YEAR, "2003")
                .withField(StandardField.MONTH, "oct")
                .withField(StandardField.ABSTRACT, "Multi-electron production is studied at high electron transverse momentum in positron- and electron-proton collisions using the H1 detector at HERA. The data correspond to an integrated luminosity of 115 pb-1. Di-electron and tri-electron event yields are measured. Cross sections are derived in a restricted phase space region dominated by photon-photon collisions. In general good agreement is found with the Standard Model predictions. However, for electron pair invariant masses above 100 GeV, three di-electron events and three tri-electron events are observed, compared to Standard Model expectations of 0.30 \\pm 0.04 and 0.23 \\pm 0.04, respectively.")
                .withField(StandardField.PUBLISHER, "Springer Science and Business Media {LLC}")
                .withField(StandardField.EPRINT, "hep-ex/0307015")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/hep-ex/0307015v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "hep-ex")
                .withField(StandardField.KEYWORDS, "High Energy Physics - Experiment (hep-ex), FOS: Physical sciences")
                .withField(StandardField.DOI, "10.1140/epjc/s2003-01326-x")
                .withField(StandardField.JOURNAL, "Eur.Phys.J.C31:17-29,2003")
                .withField(InternalField.KEY_FIELD, "2003")
                .withField(new UnknownField("copyright"), "Assumed arXiv.org perpetual, non-exclusive license to distribute this article for submissions made before January 2004");

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
    void searchWithMalformedIdReturnsEmpty() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById("123412345"));
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

    /**
     * A phrase is a sequence of terms wrapped in quotes.
     * Only documents that contain exactly this sequence are returned.
     */
    @Test
    public void supportsPhraseSearch() throws Exception {
        List<BibEntry> resultWithPhraseSearch = fetcher.performSearch("title:\"Taxonomy of Distributed\"");
        List<BibEntry> resultWithOutPhraseSearch = fetcher.performSearch("title:Taxonomy AND title:of AND title:Distributed");
        // Phrase search result has to be subset of the default search result
        assertTrue(resultWithOutPhraseSearch.containsAll(resultWithPhraseSearch));
    }

    /**
     * A phrase is a sequence of terms wrapped in quotes.
     * Only documents that contain exactly this sequence are returned.
     */
    @Test
    public void supportsPhraseSearchAndMatchesExact() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Rafrastara, Fauzi Adi and Deyu, Qi")
                .withField(StandardField.TITLE, "A Survey and Taxonomy of Distributed Data Mining Research Studies: A Systematic Literature Review")
                .withField(StandardField.DATE, "2020-09-14")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.PUBLISHER, "arXiv")
                .withField(StandardField.ABSTRACT, "Context: Data Mining (DM) method has been evolving year by year and as of today there is also the enhancement of DM technique that can be run several times faster than the traditional one, called Distributed Data Mining (DDM). It is not a new field in data processing actually, but in the recent years many researchers have been paying more attention on this area. Problems: The number of publication regarding DDM in high reputation journals and conferences has increased significantly. It makes difficult for researchers to gain a comprehensive view of DDM that require further research. Solution: We conducted a systematic literature review to map the previous research in DDM field. Our objective is to provide the motivation for new research by identifying the gap in DDM field as well as the hot area itself. Result: Our analysis came up with some conclusions by answering 7 research questions proposed in this literature review. In addition, the taxonomy of DDM research area is presented in this paper. Finally, this systematic literature review provides the statistic of development of DDM since 2000 to 2015, in which this will help the future researchers to have a comprehensive overview of current situation of DDM.")
                .withField(StandardField.EPRINT, "2009.10618")
                .withField(StandardField.DOI, "10.48550/ARXIV.2009.10618")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/2009.10618v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "cs.DC")
                .withField(StandardField.KEYWORDS, "Distributed / Parallel / Cluster Computing (cs.DC), Machine Learning (cs.LG), FOS: Computer and information sciences")
                .withField(InternalField.KEY_FIELD, "https://doi.org/10.48550/arxiv.2009.10618")
                .withField(new UnknownField("copyright"), "arXiv.org perpetual, non-exclusive license");

        List<BibEntry> resultWithPhraseSearch = fetcher.performSearch("title:\"Taxonomy of Distributed\"");

        // There is only a single paper found by searching that contains the exact sequence "Taxonomy of Distributed" in the title.
        assertEquals(Collections.singletonList(expected), resultWithPhraseSearch);
    }

    @Test
    public void supportsBooleanANDSearch() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Büscher, Tobias and Diez, Angel L. and Gompper, Gerhard and Elgeti, Jens")
                .withField(StandardField.TITLE, "Instability and fingering of interfaces in growing tissue")
                .withField(StandardField.DATE, "2020-03-10")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.MONTH, "aug")
                .withField(StandardField.NUMBER, "8")
                .withField(StandardField.VOLUME, "22")
                .withField(StandardField.PAGES, "083005")
                .withField(StandardField.PUBLISHER, "{IOP} Publishing")
                .withField(StandardField.JOURNAL, "New Journal of Physics")
                .withField(StandardField.ABSTRACT, "Interfaces in tissues are ubiquitous, both between tissue and environment as well as between populations of different cell types. The propagation of an interface can be driven mechanically. % e.g. by a difference in the respective homeostatic stress of the different cell types. Computer simulations of growing tissues are employed to study the stability of the interface between two tissues on a substrate. From a mechanical perspective, the dynamics and stability of this system is controlled mainly by four parameters of the respective tissues: (i) the homeostatic stress (ii) cell motility (iii) tissue viscosity and (iv) substrate friction. For propagation driven by a difference in homeostatic stress, the interface is stable for tissue-specific substrate friction even for very large differences of homeostatic stress; however, it becomes unstable above a critical stress difference when the tissue with the larger homeostatic stress has a higher viscosity. A small difference in directed bulk motility between the two tissues suffices to result in propagation with a stable interface, even for otherwise identical tissues. Larger differences in motility force, however, result in a finite-wavelength instability of the interface. Interestingly, the instability is apparently bound by nonlinear effects and the amplitude of the interface undulations only grows to a finite value in time.")
                .withField(StandardField.DOI, "10.1088/1367-2630/ab9e88")
                .withField(StandardField.EPRINT, "2003.04601")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/2003.04601v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "q-bio.TO")
                .withField(StandardField.KEYWORDS, "Tissues and Organs (q-bio.TO), FOS: Biological sciences")
                .withField(InternalField.KEY_FIELD, "B_scher_2020")
                .withField(new UnknownField("copyright"), "arXiv.org perpetual, non-exclusive license");

        List<BibEntry> result = fetcher.performSearch("author:\"Tobias Büscher\" AND title:\"Instability and fingering of interfaces\"");

        // There is only one paper authored by Tobias Büscher with that phrase in the title
        assertEquals(Collections.singletonList(expected), result);
    }

    @Test
    public void retrievePureArxivEntryWhenAllDOIFetchingFails() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Hai Zheng and Po-Yi Ho and Meiling Jiang and Bin Tang and Weirong Liu and Dengjin Li and Xuefeng Yu and Nancy E. Kleckner and Ariel Amir and Chenli Liu")
                .withField(StandardField.TITLE, "Interrogating the Escherichia coli cell cycle by cell dimension perturbations")
                .withField(StandardField.DATE, "2017-01-03")
                .withField(StandardField.JOURNAL, "PNAS December 27, 2016 vol. 113 no. 52 15000-15005")
                .withField(StandardField.ABSTRACT, "Bacteria tightly regulate and coordinate the various events in their cell cycles to duplicate themselves accurately and to control their cell sizes. Growth of Escherichia coli, in particular, follows a relation known as Schaechter 's growth law. This law says that the average cell volume scales exponentially with growth rate, with a scaling exponent equal to the time from initiation of a round of DNA replication to the cell division at which the corresponding sister chromosomes segregate. Here, we sought to test the robustness of the growth law to systematic perturbations in cell dimensions achieved by varying the expression levels of mreB and ftsZ. We found that decreasing the mreB level resulted in increased cell width, with little change in cell length, whereas decreasing the ftsZ level resulted in increased cell length. Furthermore, the time from replication termination to cell division increased with the perturbed dimension in both cases. Moreover, the growth law remained valid over a range of growth conditions and dimension perturbations. The growth law can be quantitatively interpreted as a consequence of a tight coupling of cell division to replication initiation. Thus, its robustness to perturbations in cell dimensions strongly supports models in which the timing of replication initiation governs that of cell division, and cell volume is the key phenomenological variable governing the timing of replication initiation. These conclusions are discussed in the context of our recently proposed adder-per-origin model, in which cells add a constant volume per origin between initiations and divide a constant time after initiation.")
                .withField(StandardField.DOI, "10.1073/pnas.1617932114")
                .withField(StandardField.EPRINT, "1701.00587")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1701.00587v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "q-bio.CB")
                .withField(StandardField.KEYWORDS, "q-bio.CB");

        DoiFetcher modifiedDoiFetcher = Mockito.spy(new DoiFetcher(importFormatPreferences));
        when(modifiedDoiFetcher.performSearchById("10.1073/pnas.1617932114")).thenThrow(new FetcherException("Could not fetch user-assigned DOI"));
        when(modifiedDoiFetcher.performSearchById("10.48550/arXiv.1701.00587")).thenThrow(new FetcherException("Could not fetch ArXiv-assigned DOI"));

        ArXivFetcher modifiedArXivFetcher = Mockito.spy(new ArXivFetcher(importFormatPreferences, modifiedDoiFetcher));
        assertEquals(Optional.of(expected), modifiedArXivFetcher.performSearchById("1701.00587"));
    }

    @Test
    public void canReplicateArXivOnlySearchByPassingNullParameter() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Hai Zheng and Po-Yi Ho and Meiling Jiang and Bin Tang and Weirong Liu and Dengjin Li and Xuefeng Yu and Nancy E. Kleckner and Ariel Amir and Chenli Liu")
                .withField(StandardField.TITLE, "Interrogating the Escherichia coli cell cycle by cell dimension perturbations")
                .withField(StandardField.DATE, "2017-01-03")
                .withField(StandardField.JOURNAL, "PNAS December 27, 2016 vol. 113 no. 52 15000-15005")
                .withField(StandardField.ABSTRACT, "Bacteria tightly regulate and coordinate the various events in their cell cycles to duplicate themselves accurately and to control their cell sizes. Growth of Escherichia coli, in particular, follows a relation known as Schaechter 's growth law. This law says that the average cell volume scales exponentially with growth rate, with a scaling exponent equal to the time from initiation of a round of DNA replication to the cell division at which the corresponding sister chromosomes segregate. Here, we sought to test the robustness of the growth law to systematic perturbations in cell dimensions achieved by varying the expression levels of mreB and ftsZ. We found that decreasing the mreB level resulted in increased cell width, with little change in cell length, whereas decreasing the ftsZ level resulted in increased cell length. Furthermore, the time from replication termination to cell division increased with the perturbed dimension in both cases. Moreover, the growth law remained valid over a range of growth conditions and dimension perturbations. The growth law can be quantitatively interpreted as a consequence of a tight coupling of cell division to replication initiation. Thus, its robustness to perturbations in cell dimensions strongly supports models in which the timing of replication initiation governs that of cell division, and cell volume is the key phenomenological variable governing the timing of replication initiation. These conclusions are discussed in the context of our recently proposed adder-per-origin model, in which cells add a constant volume per origin between initiations and divide a constant time after initiation.")
                .withField(StandardField.DOI, "10.1073/pnas.1617932114")
                .withField(StandardField.EPRINT, "1701.00587")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1701.00587v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "q-bio.CB")
                .withField(StandardField.KEYWORDS, "q-bio.CB");

        ArXivFetcher modifiedArXivFetcher = new ArXivFetcher(importFormatPreferences, null);
        assertEquals(Optional.of(expected), modifiedArXivFetcher.performSearchById("1701.00587"));
    }

    @Test
    public void retrievePartialResultWhenCannotGetInformationFromUserAssignedDOI() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Zheng, Hai and Ho, Po-Yi and Jiang, Meiling and Tang, Bin and Liu, Weirong and Li, Dengjin and Yu, Xuefeng and Kleckner, Nancy E. and Amir, Ariel and Liu, Chenli")
                .withField(StandardField.TITLE, "Interrogating the Escherichia coli cell cycle by cell dimension perturbations")
                .withField(StandardField.DATE, "2017-01-03")
                .withField(StandardField.JOURNAL, "PNAS December 27, 2016 vol. 113 no. 52 15000-15005")
                .withField(StandardField.ABSTRACT, "Bacteria tightly regulate and coordinate the various events in their cell cycles to duplicate themselves accurately and to control their cell sizes. Growth of Escherichia coli, in particular, follows a relation known as Schaechter 's growth law. This law says that the average cell volume scales exponentially with growth rate, with a scaling exponent equal to the time from initiation of a round of DNA replication to the cell division at which the corresponding sister chromosomes segregate. Here, we sought to test the robustness of the growth law to systematic perturbations in cell dimensions achieved by varying the expression levels of mreB and ftsZ. We found that decreasing the mreB level resulted in increased cell width, with little change in cell length, whereas decreasing the ftsZ level resulted in increased cell length. Furthermore, the time from replication termination to cell division increased with the perturbed dimension in both cases. Moreover, the growth law remained valid over a range of growth conditions and dimension perturbations. The growth law can be quantitatively interpreted as a consequence of a tight coupling of cell division to replication initiation. Thus, its robustness to perturbations in cell dimensions strongly supports models in which the timing of replication initiation governs that of cell division, and cell volume is the key phenomenological variable governing the timing of replication initiation. These conclusions are discussed in the context of our recently proposed adder-per-origin model, in which cells add a constant volume per origin between initiations and divide a constant time after initiation.")
                .withField(StandardField.DOI, "10.1073/pnas.1617932114")
                .withField(StandardField.EPRINT, "1701.00587")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1701.00587v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "q-bio.CB")
                .withField(StandardField.KEYWORDS, "Cell Behavior (q-bio.CB), FOS: Biological sciences")
                .withField(new UnknownField("copyright"), "arXiv.org perpetual, non-exclusive license")
                .withField(InternalField.KEY_FIELD, "https://doi.org/10.48550/arxiv.1701.00587")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.PUBLISHER, "arXiv");

        DoiFetcher modifiedDoiFetcher = Mockito.spy(new DoiFetcher(importFormatPreferences));
        when(modifiedDoiFetcher.performSearchById("10.1073/pnas.1617932114")).thenThrow(new FetcherException("Could not fetch user-assigned DOI"));

        ArXivFetcher modifiedArXivFetcher = Mockito.spy(new ArXivFetcher(importFormatPreferences, modifiedDoiFetcher));
        assertEquals(Optional.of(expected), modifiedArXivFetcher.performSearchById("1701.00587"));
    }

    @Test
    public void retrievePartialResultWhenCannotGetInformationFromArXivAssignedDOI() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Hai Zheng and Po-Yi Ho and Meiling Jiang and Bin Tang and Weirong Liu and Dengjin Li and Xuefeng Yu and Nancy E. Kleckner and Ariel Amir and Chenli Liu")
                .withField(StandardField.TITLE, "Interrogating the Escherichia coli cell cycle by cell dimension perturbations")
                .withField(StandardField.DATE, "2017-01-03")
                .withField(StandardField.JOURNAL, "PNAS December 27, 2016 vol. 113 no. 52 15000-15005")
                .withField(StandardField.ABSTRACT, "Bacteria tightly regulate and coordinate the various events in their cell cycles to duplicate themselves accurately and to control their cell sizes. Growth of Escherichia coli, in particular, follows a relation known as Schaechter 's growth law. This law says that the average cell volume scales exponentially with growth rate, with a scaling exponent equal to the time from initiation of a round of DNA replication to the cell division at which the corresponding sister chromosomes segregate. Here, we sought to test the robustness of the growth law to systematic perturbations in cell dimensions achieved by varying the expression levels of mreB and ftsZ. We found that decreasing the mreB level resulted in increased cell width, with little change in cell length, whereas decreasing the ftsZ level resulted in increased cell length. Furthermore, the time from replication termination to cell division increased with the perturbed dimension in both cases. Moreover, the growth law remained valid over a range of growth conditions and dimension perturbations. The growth law can be quantitatively interpreted as a consequence of a tight coupling of cell division to replication initiation. Thus, its robustness to perturbations in cell dimensions strongly supports models in which the timing of replication initiation governs that of cell division, and cell volume is the key phenomenological variable governing the timing of replication initiation. These conclusions are discussed in the context of our recently proposed adder-per-origin model, in which cells add a constant volume per origin between initiations and divide a constant time after initiation.")
                .withField(StandardField.DOI, "10.1073/pnas.1617932114")
                .withField(StandardField.EPRINT, "1701.00587")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1701.00587v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "q-bio.CB")
                .withField(StandardField.KEYWORDS, "q-bio.CB")
                .withField(StandardField.MONTH, "dec")
                .withField(StandardField.YEAR, "2016")
                .withField(StandardField.VOLUME, "113")
                .withField(InternalField.KEY_FIELD, "Zheng_2016")
                .withField(StandardField.PUBLISHER, "Proceedings of the National Academy of Sciences")
                .withField(StandardField.PAGES, "15000--15005")
                .withField(StandardField.NUMBER, "52");

        DoiFetcher modifiedDoiFetcher = Mockito.spy(new DoiFetcher(importFormatPreferences));
        when(modifiedDoiFetcher.performSearchById("10.48550/arXiv.1701.00587")).thenThrow(new FetcherException("Could not fetch ArXiv-assigned DOI"));

        ArXivFetcher modifiedArXivFetcher = Mockito.spy(new ArXivFetcher(importFormatPreferences, modifiedDoiFetcher));
        assertEquals(Optional.of(expected), modifiedArXivFetcher.performSearchById("1701.00587"));
    }
}
