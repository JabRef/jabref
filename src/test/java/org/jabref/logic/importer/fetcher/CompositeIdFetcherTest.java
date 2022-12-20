package org.jabref.logic.importer.fetcher;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the CompositeIdFetcher, for which Fetchers implementing the
 * IdBasedFetcher interface are a prerequisite. Excluding TitleFetcher.
 */
@FetcherTest
class CompositeIdFetcherTest {

    private CompositeIdFetcher compositeIdFetcher;

    public static Stream<Arguments> performSearchByIdReturnsCorrectEntryForIdentifier() {
        return Stream.of(
                Arguments.arguments(
                        "performSearchByIdReturnsCorrectEntryForArXivId",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Cunningham, Emily C. and Sanderson, Robyn E. and Johnston, Kathryn V. and Panithanpaisal, Nondh and Ness, Melissa K. and Wetzel, Andrew and Loebman, Sarah R. and Escala, Ivanna and Horta, Danny and Faucher-Giguère, Claude-André")
                                .withField(StandardField.TITLE, "Reading the CARDs: the Imprint of Accretion History in the Chemical Abundances of the Milky Way's Stellar Halo")
                                .withField(StandardField.DATE, "2021-10-06")
                                .withField(StandardField.YEAR, "2021")
                                .withField(StandardField.MONTH, "aug")
                                .withField(StandardField.NUMBER, "2")
                                .withField(StandardField.VOLUME, "934")
                                .withField(StandardField.PUBLISHER, "American Astronomical Society")
                                .withField(StandardField.JOURNAL, "The Astrophysical Journal")
                                .withField(StandardField.PAGES, "172")
                                .withField(StandardField.ABSTRACT, "In the era of large-scale spectroscopic surveys in the Local Group (LG), we can explore using chemical abundances of halo stars to study the star formation and chemical enrichment histories of the dwarf galaxy progenitors of the Milky Way (MW) and M31 stellar halos. In this paper, we investigate using the Chemical Abundance Ratio Distributions (CARDs) of seven stellar halos from the Latte suite of FIRE-2 simulations. We attempt to infer galaxies' assembly histories by modelling the CARDs of the stellar halos of the Latte galaxies as a linear combination of template CARDs from disrupted dwarfs, with different stellar masses $M_{\\star}$ and quenching times $t_{100}$. We present a method for constructing these templates using present-day dwarf galaxies. For four of the seven Latte halos studied in this work, we recover the mass spectrum of accreted dwarfs to a precision of $<10\\%$. For the fraction of mass accreted as a function of $t_{100}$, we find residuals of $20-30\\%$ for five of the seven simulations. We discuss the failure modes of this method, which arise from the diversity of star formation and chemical enrichment histories dwarf galaxies can take. These failure cases can be robustly identified by the high model residuals. Though the CARDs modeling method does not successfully infer the assembly histories in these cases, the CARDs of these disrupted dwarfs contain signatures of their unusual formation histories. Our results are promising for using CARDs to learn more about the histories of the progenitors of the MW and M31 stellar halos.")
                                .withField(StandardField.DOI, "10.3847/1538-4357/ac78ea")
                                .withField(StandardField.EPRINT, "2110.02957")
                                .withField(StandardField.DOI, "10.3847/1538-4357/ac78ea")
                                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/2110.02957v1:PDF")
                                .withField(StandardField.EPRINTTYPE, "arXiv")
                                .withField(StandardField.EPRINTCLASS, "astro-ph.GA")
                                .withField(StandardField.KEYWORDS, "Astrophysics of Galaxies (astro-ph.GA), FOS: Physical sciences")
                                .withField(InternalField.KEY_FIELD, "Cunningham_2022")
                                .withField(new UnknownField("copyright"), "Creative Commons Attribution 4.0 International"),
                        "arXiv:2110.02957"
                ),
                /* disabled, because Iacr does not work
                Arguments.arguments(
                        "performSearchByIdReturnsCorrectEntryForIacrEprintId",
                        new BibEntry(StandardEntryType.Misc)
                                .withField(StandardField.ABSTRACT, "The decentralized cryptocurrency Bitcoin has experienced great success but also encountered many challenges. One of the challenges has been the long confirmation time. Another challenge is the lack of incentives at certain steps of the protocol, raising concerns for transaction withholding, selfish mining, etc. To address these challenges, we propose Solida, a decentralized blockchain protocol based on reconfigurable Byzantine consensus augmented by proof-of-work. Solida improves on Bitcoin in confirmation time,  and provides safety  and liveness assuming the adversary control less than (roughly) one-third of the total mining power.\n")
                                .withField(StandardField.AUTHOR, "Ittai Abraham and Dahlia Malkhi and Kartik Nayak and Ling Ren and Alexander Spiegelman")
                                .withField(StandardField.DATE, "2017-11-18")
                                .withField(StandardField.HOWPUBLISHED, "Cryptology ePrint Archive, Report 2017/1118")
                                .withField(StandardField.NOTE, "\\url{https://ia.cr/2017/1118}")
                                .withField(StandardField.TITLE, "Solida: A Blockchain Protocol Based on Reconfigurable Byzantine Consensus")
                                .withField(StandardField.URL, "https://eprint.iacr.org/2017/1118/20171124:064527")
                                .withField(StandardField.VERSION, "20171124:064527")
                                .withField(StandardField.YEAR, "2017")
                                .withCitationKey("cryptoeprint:2017:1118"),
                        "2017/1118"
                ), */
                Arguments.arguments(
                        "performSearchByIdReturnsCorrectEntryForIsbnId",
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.TITLE, "Effective Java")
                                .withField(StandardField.PUBLISHER, "Addison-Wesley Professional")
                                .withField(StandardField.YEAR, "2017")
                                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                                .withField(StandardField.PAGES, "416")
                                .withField(StandardField.ISBN, "9780134685991"),
                        "9780134685991"
                ),
                Arguments.arguments(
                        "performSearchByIdReturnsCorrectEntryForDoiId",
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.TITLE, "Java{\\textregistered} For Dummies{\\textregistered}")
                                .withField(StandardField.PUBLISHER, "Wiley")
                                .withField(StandardField.YEAR, "2011")
                                .withField(StandardField.AUTHOR, "Barry Burd")
                                .withField(StandardField.MONTH, "jul")
                                .withField(StandardField.DOI, "10.1002/9781118257517")
                                .withCitationKey("Burd_2011"),
                        "10.1002/9781118257517"
                )
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        // Needed for ArXiv Fetcher keyword processing
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        compositeIdFetcher = new CompositeIdFetcher(importFormatPreferences);
    }

    @ParameterizedTest
    @ValueSource(strings = "arZiv:2110.02957")
    void performSearchByIdReturnsEmptyForInvalidId(String groundInvalidArXivId) throws FetcherException {
        assertEquals(Optional.empty(), compositeIdFetcher.performSearchById(groundInvalidArXivId));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource
    void performSearchByIdReturnsCorrectEntryForIdentifier(String name, BibEntry bibEntry, String identifier) throws FetcherException {
        assertEquals(Optional.of(bibEntry), compositeIdFetcher.performSearchById(identifier));
    }
}
