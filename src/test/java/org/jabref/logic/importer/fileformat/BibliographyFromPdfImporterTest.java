package org.jabref.logic.importer.fileformat;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.logic.citationkeypattern.CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BibliographyFromPdfImporterTest {

    private static final BibEntry KNASTER_2017 = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.AUTHOR, "J. Knaster and others")
            .withField(StandardField.TITLE, "Overview of the IFMIF/EVEDA project")
            .withField(StandardField.JOURNAL, "Nucl. Fusion")
            .withField(StandardField.VOLUME, "57")
            .withField(StandardField.PAGES, "102016")
            .withField(StandardField.YEAR, "2017")
            .withField(StandardField.DOI, "10.1088/1741-4326/aa6a6a")
            .withField(StandardField.COMMENT, "[1] J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57, p. 102016, 2017. doi:10.1088/ 1741-4326/aa6a6a");
    private static final BibEntry SHIMOSAKI_2019 = new BibEntry(StandardEntryType.InProceedings)
            .withField(StandardField.AUTHOR, "Y. Shimosaki and others")
            .withField(StandardField.TITLE, "Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc")
            .withField(StandardField.BOOKTITLE, "Proc. IPAC’19, Melbourne, Australia")
            .withField(StandardField.MONTH, "#may#")
            .withField(StandardField.YEAR, "2019")
            .withField(StandardField.PAGES, "977-979")
            .withField(StandardField.DOI, "10.18429/JACoW-IPAC2019-MOPTS051")
            .withField(StandardField.COMMENT, "[3] Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia, May 2019, pp. 977-979. doi:10.18429/ JACoW-IPAC2019-MOPTS051");
    private static final BibEntry BELLAN_2021 = new BibEntry(StandardEntryType.InProceedings)
            .withField(StandardField.AUTHOR, "L. Bellan and others")
            .withField(StandardField.TITLE, "Acceleration of the high current deuteron beam through the IFMIF-EVEDA beam dynamics performances")
            .withField(StandardField.BOOKTITLE, "Proc. HB’21, Batavia, IL, USA")
            .withField(StandardField.MONTH, "#oct#")
            .withField(StandardField.YEAR, "2021")
            .withField(StandardField.PAGES, "197-202")
            .withField(StandardField.DOI, "10.18429/JACoW-HB2021-WEDC2")
            .withField(StandardField.COMMENT, "[6] L. Bellan et al., “Acceleration of the high current deuteron beam through the IFMIF-EVEDA beam dynamics perfor- mances”, in Proc. HB’21, Batavia, IL, USA, Oct. 2021, pp. 197-202. doi:10.18429/JACoW-HB2021-WEDC2");
    private static final BibEntry MASUDA_2022 = new BibEntry(StandardEntryType.InProceedings)
            .withField(StandardField.AUTHOR, "K. Masuda and others")
            .withField(StandardField.TITLE, "Commissioning of IFMIF Prototype Accelerator towards CW operation")
            .withField(StandardField.BOOKTITLE, "Proc. LINAC’22, Liverpool, UK")
            .withField(StandardField.MONTH, "#aug#")
            .withField(StandardField.YEAR, "2022")
            .withField(StandardField.PAGES, "319-323")
            .withField(StandardField.DOI, "10.18429/JACoW-LINAC2022-TU2AA04")
            .withField(StandardField.COMMENT, "[7] K. Masuda et al., “Commissioning of IFMIF Prototype Ac- celerator towards CW operation”, in Proc. LINAC’22, Liv- erpool, UK, Aug.-Sep. 2022, pp. 319-323. doi:10.18429/ JACoW-LINAC2022-TU2AA04");
    private static final BibEntry PODADERA_2012 = new BibEntry(StandardEntryType.InProceedings)
            .withField(StandardField.AUTHOR, "I. Podadera and J. M. Carmona and A. Ibarra and J. Molla")
            .withField(StandardField.TITLE, "Beam position monitor development for LIPAc")
            .withField(StandardField.BOOKTITLE, "th 8th DITANET Topical Workshop on Beam Position Monitors, CERN, Geneva, Switzreland")
            .withField(StandardField.MONTH, "#jan#")
            .withField(StandardField.YEAR, "2012")
            .withField(StandardField.COMMENT, "[11] I. Podadera, J. M. Carmona, A. Ibarra, and J. Molla, “Beam position monitor development for LIPAc”, presented at th 8th DITANET Topical Workshop on Beam Position Monitors, CERN, Geneva, Switzreland, Jan. 2012.");
    private static final BibEntry AKAGI_2023 = new BibEntry(StandardEntryType.InProceedings)
            .withField(StandardField.AUTHOR, "T. Akagi and others")
            .withField(StandardField.TITLE, "Achievement of high-current continuouswave deuteron injector for Linear IFMIF Prototype Accelerator (LIPAc)")
            .withField(StandardField.BOOKTITLE, "IAEA FEC’23, London, UK, https://www.iaea.org/events/fec2023")
            .withField(StandardField.MONTH, "#oct#")
            .withField(StandardField.YEAR, "2023")
            .withField(StandardField.COMMENT, "[15] T. Akagi et al., “Achievement of high-current continuous- wave deuteron injector for Linear IFMIF Prototype Accelera- tor (LIPAc)”, to be presented at IAEA FEC’23, London, UK, Oct. 2023. https://www.iaea.org/events/fec2023");
    private static final BibEntry INTERNAL_NOTE = new BibEntry(StandardEntryType.TechReport)
            .withField(StandardField.TITLE, "AF4.1.1 SRF Linac Engineering Design Report")
            .withField(StandardField.NOTE, "Internal note")
            .withField(StandardField.COMMENT, "[16] “AF4.1.1 SRF Linac Engineering Design Report”, Internal note.");
    private static final BibEntry KWON_2023 = new BibEntry(StandardEntryType.InProceedings)
            .withField(StandardField.AUTHOR, "S. Kwon and others")
            .withField(StandardField.TITLE, "High beam current operation with beam di-agnostics at LIPAc")
            .withField(StandardField.BOOKTITLE, "HB’23, Geneva, Switzerland, paper FRC1I2, this conference")
            .withField(StandardField.MONTH, "#oct#")
            .withField(StandardField.YEAR, "2023")
            .withField(StandardField.COMMENT, "[14] S. Kwon et al., “High beam current operation with beam di-agnostics at LIPAc”, presented at HB’23, Geneva, Switzer- land, Oct. 2023, paper FRC1I2, this conference.");
    private BibliographyFromPdfImporter bibliographyFromPdfImporter;

    @BeforeEach
    void setup() {
        GlobalCitationKeyPatterns globalCitationKeyPattern = GlobalCitationKeyPatterns.fromPattern("[auth][year]");
        CitationKeyPatternPreferences citationKeyPatternPreferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                globalCitationKeyPattern,
                "",
                ',');
        bibliographyFromPdfImporter = new BibliographyFromPdfImporter(citationKeyPatternPreferences);
    }

    @Test
    void tua3i2refpage() throws Exception {
        Path file = Path.of(BibliographyFromPdfImporterTest.class.getResource("tua3i2refpage.pdf").toURI());
        ParserResult parserResult = bibliographyFromPdfImporter.importDatabase(file);
        BibEntry entry02 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Kondo2020")
                .withField(StandardField.AUTHOR, "K. Kondo and others")
                .withField(StandardField.TITLE, "Validation of the Linear IFMIF Prototype Accelerator (LIPAc) in Rokkasho")
                .withField(StandardField.JOURNAL, "Fusion Eng. Des") // TODO: Final dot should be kept
                .withField(StandardField.VOLUME, "153")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.PAGES, "111503")
                .withField(StandardField.DOI, "10.1016/j.fusengdes.2020.111503")
                .withField(StandardField.COMMENT, "[2] K. Kondo et al., “Validation of the Linear IFMIF Prototype Accelerator (LIPAc) in Rokkasho”, Fusion Eng. Des., vol. 153, p. 111503, 2020. doi:10.1016/j.fusengdes.2020. 111503");

        BibEntry entry04 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Devanz2017")
                .withField(StandardField.AUTHOR, "G. Devanz and others")
                .withField(StandardField.TITLE, "Manufacturing and validation tests of IFMIF low-beta HWRs")
                .withField(StandardField.BOOKTITLE, "Proc. IPAC’17, Copenhagen, Denmark")
                .withField(StandardField.MONTH, "#may#")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.PAGES, "942-944")
                .withField(StandardField.DOI, "10.18429/JACoW-IPAC2017-MOPVA039")
                .withField(StandardField.COMMENT, "[4] G. Devanz et al., “Manufacturing and validation tests of IFMIF low-beta HWRs”, in Proc. IPAC’17, Copen- hagen, Denmark, May 2017, pp. 942-944. doi:10.18429/ JACoW-IPAC2017-MOPVA039");

        BibEntry entry05 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Branas2018")
                .withField(StandardField.AUTHOR, "B. Brañas and others")
                .withField(StandardField.TITLE, "The LIPAc Beam Dump")
                .withField(StandardField.JOURNAL, "Fusion Eng. Des")
                .withField(StandardField.VOLUME, "127")
                .withField(StandardField.PAGES, "127-138")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.DOI, "10.1016/j.fusengdes.2017.12.018")
                .withField(StandardField.COMMENT, "[5] B. Brañas et al., “The LIPAc Beam Dump”, Fusion Eng. Des., vol. 127, pp. 127-138, 2018. doi:10.1016/j.fusengdes. 2017.12.018");

        BibEntry entry08 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Scantamburlo2023")
                .withField(StandardField.AUTHOR, "F. Scantamburlo and others")
                .withField(StandardField.TITLE, "Linear IFMIF Prototype Accelera-tor (LIPAc) Radio Frequency Quadrupole’s (RFQ) RF couplers enhancement towards CW operation at nominal voltage")
                .withField(StandardField.BOOKTITLE, "Proc. ISFNT’23, Las Palmas de Gran Canaria, Spain.")
                .withField(StandardField.MONTH, "#sep#")
                .withField(StandardField.YEAR, "2023")
                .withField(StandardField.COMMENT, "[8] F. Scantamburlo et al., “Linear IFMIF Prototype Accelera-tor (LIPAc) Radio Frequency Quadrupole’s (RFQ) RF couplers enhancement towards CW operation at nominal voltage”, in Proc. ISFNT’23, Sep. 2023, Las Palmas de Gran Canaria, Spain.");

        BibEntry entry09 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Franco2023")
                .withField(StandardField.AUTHOR, "A. De Franco and others")
                        .withField(StandardField.BOOKTITLE, "Proc. IPAC’23, Venice, Italy")
                .withField(StandardField.TITLE, "RF conditioning towards continuous wave of the FRQ of the Linear IFMIF Prototype Accelerator")
                .withField(StandardField.PAGES, "2345-2348")
                .withField(StandardField.MONTH, "#may#")
                .withField(StandardField.YEAR, "2023")
                        .withField(StandardField.DOI, "10.18429/JACoW-IPAC2023-TUPM065")
                                .withField(StandardField.COMMENT, "[9] A. De Franco et al., “RF conditioning towards continuous wave of the FRQ of the Linear IFMIF Prototype Accelerator”, in Proc. IPAC’23, Venice, Italy, May 2023, pp. 2345-2348. doi:10.18429/JACoW-IPAC2023-TUPM065");

        BibEntry entry10 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Hirosawa")
                .withField(StandardField.AUTHOR, "K. Hirosawa and others")
                .withField(StandardField.BOOKTITLE, "Proc. PASJ’23, 2023, Japan.")
                .withField(StandardField.TITLE, "High-Power RF tests of repaired circulator for LIPAc RFQ")
                .withField(StandardField.COMMENT, "[10] K. Hirosawa et al., “High-Power RF tests of repaired circu- lator for LIPAc RFQ”, in Proc. PASJ’23, 2023, Japan.");

        BibEntry entry12 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Podadera2019")
                .withField(StandardField.AUTHOR, "I. Podadera and others")
                .withField(StandardField.TITLE, "Beam commissioning of beam position and phase monitors for LIPAc")
                .withField(StandardField.BOOKTITLE, "Proc. IBIC’19, Malmö, Sweden")
                .withField(StandardField.PAGES, "534-538")
                .withField(StandardField.MONTH, "#sep#")
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.DOI, "10.18429/JACoW-IBIC2019-WEPP013")
                .withField(StandardField.COMMENT, "[12] I. Podadera et al., “Beam commissioning of beam posi- tion and phase monitors for LIPAc”, in Proc. IBIC’19, Malmö, Sweden, Sep. 2019, pp. 534-538. doi:10.18429/ JACoW-IBIC2019-WEPP013");

        BibEntry entry13 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Kondo2021")
                .withField(StandardField.AUTHOR, "K. Kondo and others")
                .withField(StandardField.TITLE, "Neutron production measurement in the 125 mA 5 MeV Deuteron beam commissioning of Linear IFMIF Prototype Accelerator (LIPAc) RFQ")
                .withField(StandardField.JOURNAL, "Nucl. Fusion")
                .withField(StandardField.VOLUME, "61")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "116002")
                .withField(StandardField.YEAR, "2021")
                .withField(StandardField.DOI, "82310.1088/1741-4326/ac233c")
                .withField(StandardField.COMMENT, "[13] K. Kondo et al., “Neutron production measurement in the 125 mA 5 MeV Deuteron beam commissioning of Linear IFMIF Prototype Accelerator (LIPAc) RFQ”, Nucl. Fusion, vol. 61, no. 1, p. 116002, 2021. doi:82310.1088/1741-4326/ ac233c");

        BibEntry entry17 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Bellan2021a")
                .withField(StandardField.AUTHOR, "L. Bellan and others")
                .withField(StandardField.BOOKTITLE, "Proc. ICIS’21, TRIUMF, Vancouver, BC, Canada, https://indico.cern.ch/event/1027296/")
                .withField(StandardField.COMMENT, "[17] L. Bellan et al., “Extraction and low energy beam transport models used for the IFMIF/EVEDA RFQ commissioning”, in Proc. ICIS’21, TRIUMF, Vancouver, BC, Canada, Sep. 2021. https://indico.cern.ch/event/1027296/")
                .withField(StandardField.MONTH, "#sep#")
                .withField(StandardField.TITLE, "Extraction and low energy beam transport models used for the IFMIF/EVEDA RFQ commissioning")
                .withField(StandardField.YEAR, "2021");

        // We use the existing test entries, but add a citation key (which is added by the importer)
        // We need to clone to keep the static entries unmodified
        assertEquals(List.of(
                        ((BibEntry) KNASTER_2017.clone()).withCitationKey("Knaster2017"),
                        entry02,
                        ((BibEntry) SHIMOSAKI_2019.clone()).withCitationKey("Shimosaki2019"),
                        entry04,
                        entry05,
                        ((BibEntry) BELLAN_2021.clone()).withCitationKey("Bellan2021"),
                        ((BibEntry) MASUDA_2022.clone()).withCitationKey("Masuda2022"),
                        entry08,
                        entry09,
                        entry10,
                        ((BibEntry) PODADERA_2012.clone()).withCitationKey("Podadera2012"),
                        entry12,
                        entry13,
                        ((BibEntry) KWON_2023.clone()).withCitationKey("Kwon2023"),
                        ((BibEntry) AKAGI_2023.clone()).withCitationKey("Akagi2023"),
                        ((BibEntry) INTERNAL_NOTE.clone()),
                        entry17),
                parserResult.getDatabase().getEntries());
    }

    static Stream<Arguments> references() {
        return Stream.of(
                Arguments.of(
                        KNASTER_2017,
                        "1",
                        "J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57, p. 102016, 2017. doi:10.1088/ 1741-4326/aa6a6a"
                ),
                Arguments.of(
                        SHIMOSAKI_2019,
                        "3",
                        "Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia, May 2019, pp. 977-979. doi:10.18429/ JACoW-IPAC2019-MOPTS051"
                ),
                Arguments.of(
                        BELLAN_2021,
                        "6",
                        "L. Bellan et al., “Acceleration of the high current deuteron beam through the IFMIF-EVEDA beam dynamics perfor- mances”, in Proc. HB’21, Batavia, IL, USA, Oct. 2021, pp. 197-202. doi:10.18429/JACoW-HB2021-WEDC2"
                ),
                Arguments.of(
                        MASUDA_2022,
                        "7",
                        "K. Masuda et al., “Commissioning of IFMIF Prototype Ac- celerator towards CW operation”, in Proc. LINAC’22, Liv- erpool, UK, Aug.-Sep. 2022, pp. 319-323. doi:10.18429/ JACoW-LINAC2022-TU2AA04"
                ),
                Arguments.of(
                        PODADERA_2012,
                        "11",
                        "I. Podadera, J. M. Carmona, A. Ibarra, and J. Molla, “Beam position monitor development for LIPAc”, presented at th 8th DITANET Topical Workshop on Beam Position Monitors, CERN, Geneva, Switzreland, Jan. 2012."
                ),
                Arguments.of(
                        KWON_2023,
                        "14",
                        "S. Kwon et al., “High beam current operation with beam di-agnostics at LIPAc”, presented at HB’23, Geneva, Switzer- land, Oct. 2023, paper FRC1I2, this conference."
                ),
                Arguments.of(
                        AKAGI_2023,
                        "15",
                        "T. Akagi et al., “Achievement of high-current continuous- wave deuteron injector for Linear IFMIF Prototype Accelera- tor (LIPAc)”, to be presented at IAEA FEC’23, London, UK, Oct. 2023. https://www.iaea.org/events/fec2023"
                ),
                Arguments.of(
                        INTERNAL_NOTE,
                        "16",
                        "“AF4.1.1 SRF Linac Engineering Design Report”, Internal note."
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void references(BibEntry expectedEntry, String number, String reference) {
        assertEquals(expectedEntry, bibliographyFromPdfImporter.parseReference(number, reference));
    }
}
