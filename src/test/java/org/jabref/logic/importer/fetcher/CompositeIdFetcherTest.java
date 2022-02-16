package org.jabref.logic.importer.fetcher;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    public static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.arguments(
                        "performSearchByIdReturnsCorrectEntryForArXivId",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Emily C. Cunningham and Robyn E. Sanderson and Kathryn V. Johnston and Nondh Panithanpaisal and Melissa K. Ness and Andrew Wetzel and Sarah R. Loebman and Ivanna Escala and Danny Horta and Claude-André Faucher-Giguère")
                                .withField(StandardField.TITLE, "Reading the CARDs: the Imprint of Accretion History in the Chemical Abundances of the Milky Way's Stellar Halo")
                                .withField(StandardField.DATE, "2021-10-06")
                                .withField(StandardField.ABSTRACT, "In the era of large-scale spectroscopic surveys in the Local Group (LG), we can explore using chemical abundances of halo stars to study the star formation and chemical enrichment histories of the dwarf galaxy progenitors of the Milky Way (MW) and M31 stellar halos. In this paper, we investigate using the Chemical Abundance Ratio Distributions (CARDs) of seven stellar halos from the Latte suite of FIRE-2 simulations. We attempt to infer galaxies' assembly histories by modelling the CARDs of the stellar halos of the Latte galaxies as a linear combination of template CARDs from disrupted dwarfs, with different stellar masses $M_{\\star}$ and quenching times $t_{100}$. We present a method for constructing these templates using present-day dwarf galaxies. For four of the seven Latte halos studied in this work, we recover the mass spectrum of accreted dwarfs to a precision of $<10\\%$. For the fraction of mass accreted as a function of $t_{100}$, we find residuals of $20-30\\%$ for five of the seven simulations. We discuss the failure modes of this method, which arise from the diversity of star formation and chemical enrichment histories dwarf galaxies can take. These failure cases can be robustly identified by the high model residuals. Though the CARDs modeling method does not successfully infer the assembly histories in these cases, the CARDs of these disrupted dwarfs contain signatures of their unusual formation histories. Our results are promising for using CARDs to learn more about the histories of the progenitors of the MW and M31 stellar halos.")
                                .withField(StandardField.EPRINT, "2110.02957")
                                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/2110.02957v1:PDF")
                                .withField(StandardField.EPRINTTYPE, "arXiv")
                                .withField(StandardField.EPRINTCLASS, "astro-ph.GA")
                                .withField(StandardField.KEYWORDS, "astro-ph.GA"),
                        "arXiv:2110.02957"
                ),
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
                ),
                Arguments.arguments(
                        "performSearchByIdReturnsCorrectEntryForIsbnId",
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.TITLE, "Effective Java")
                                .withField(StandardField.PUBLISHER, "Addison Wesley")
                                .withField(StandardField.YEAR, "2018")
                                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                                .withField(StandardField.DATE, "2018-01-31")
                                .withField(new UnknownField("ean"), "9780134685991")
                                .withField(StandardField.ISBN, "0134685997")
                                .withField(StandardField.URL, "https://www.ebook.de/de/product/28983211/joshua_bloch_effective_java.html")
                                .withCitationKey("9780134685991"),
                        "9780134685991"
                ),
                Arguments.arguments(
                        "performSearchByIdReturnsCorrectEntryForDoiId",
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.TITLE, "Java{\\textregistered} For Dummies{\\textregistered}")
                                .withField(StandardField.PUBLISHER, "Wiley Publishing, Inc.")
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
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        FieldContentFormatterPreferences fieldContentFormatterPreferences = mock(FieldContentFormatterPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(fieldContentFormatterPreferences);
        compositeIdFetcher = new CompositeIdFetcher(importFormatPreferences);
    }

    @ParameterizedTest
    @ValueSource(strings = "arZiv:2110.02957")
    void performSearchByIdReturnsEmptyForInvalidId(String groundInvalidArXivId) throws FetcherException {
        assertEquals(Optional.empty(), compositeIdFetcher.performSearchById(groundInvalidArXivId));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideTestData")
    void performSearchByIdReturnsCorrectEntryForIdentifier(String name, BibEntry bibEntry, String identifier) throws FetcherException {
        assertEquals(Optional.of(bibEntry), compositeIdFetcher.performSearchById(identifier));
    }

}
