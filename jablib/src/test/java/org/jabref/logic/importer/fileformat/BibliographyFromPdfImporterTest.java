package org.jabref.logic.importer.fileformat;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibliographyFromPdfImporterTest {

    private static final BibEntry KNASTER_2017 = new BibEntry(StandardEntryType.Article)
            .withCitationKey("1")
            .withField(StandardField.AUTHOR, "J. Knaster and others")
            .withField(StandardField.TITLE, "Overview of the IFMIF/EVEDA project")
            .withField(StandardField.JOURNAL, "Nucl. Fusion")
            .withField(StandardField.VOLUME, "57")
            .withField(StandardField.PAGES, "102016")
            .withField(StandardField.YEAR, "2017")
            .withField(StandardField.DOI, "10.1088/1741-4326/aa6a6a")
            .withField(StandardField.COMMENT, "[1] J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57, p. 102016, 2017. doi:10.1088/ 1741-4326/aa6a6a");
    private static final BibEntry SHIMOSAKI_2019 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("3")
            .withField(StandardField.AUTHOR, "Y. Shimosaki and others")
            .withField(StandardField.TITLE, "Lattice design for 5 MeV - 125 mA CW RFQ operation in LIPAc")
            .withField(StandardField.BOOKTITLE, "Proc. IPAC’19, Melbourne, Australia")
            .withField(StandardField.MONTH, "#may#")
            .withField(StandardField.YEAR, "2019")
            .withField(StandardField.PAGES, "977-979")
            .withField(StandardField.DOI, "10.18429/JACoW-IPAC2019-MOPTS051")
            .withField(StandardField.COMMENT, "[3] Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia, May 2019, pp. 977-979. doi:10.18429/ JACoW-IPAC2019-MOPTS051");
    private static final BibEntry BELLAN_2021 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("6")
            .withField(StandardField.AUTHOR, "L. Bellan and others")
            .withField(StandardField.TITLE, "Acceleration of the high current deuteron beam through the IFMIF-EVEDA beam dynamics performances")
            .withField(StandardField.BOOKTITLE, "Proc. HB’21, Batavia, IL, USA")
            .withField(StandardField.MONTH, "#oct#")
            .withField(StandardField.YEAR, "2021")
            .withField(StandardField.PAGES, "197-202")
            .withField(StandardField.DOI, "10.18429/JACoW-HB2021-WEDC2")
            .withField(StandardField.COMMENT, "[6] L. Bellan et al., “Acceleration of the high current deuteron beam through the IFMIF-EVEDA beam dynamics perfor- mances”, in Proc. HB’21, Batavia, IL, USA, Oct. 2021, pp. 197-202. doi:10.18429/JACoW-HB2021-WEDC2");
    private static final BibEntry MASUDA_2022 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("7")
            .withField(StandardField.AUTHOR, "K. Masuda and others")
            .withField(StandardField.TITLE, "Commissioning of IFMIF Prototype Accelerator towards CW operation")
            .withField(StandardField.BOOKTITLE, "Proc. LINAC’22, Liverpool, UK")
            .withField(StandardField.MONTH, "#aug#")
            .withField(StandardField.YEAR, "2022")
            .withField(StandardField.PAGES, "319-323")
            .withField(StandardField.DOI, "10.18429/JACoW-LINAC2022-TU2AA04")
            .withField(StandardField.COMMENT, "[7] K. Masuda et al., “Commissioning of IFMIF Prototype Ac- celerator towards CW operation”, in Proc. LINAC’22, Liv- erpool, UK, Aug.-Sep. 2022, pp. 319-323. doi:10.18429/ JACoW-LINAC2022-TU2AA04");
    private static final BibEntry PODADERA_2012 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("11")
            .withField(StandardField.AUTHOR, "I. Podadera and J. M. Carmona and A. Ibarra and J. Molla")
            .withField(StandardField.TITLE, "Beam position monitor development for LIPAc")
            .withField(StandardField.BOOKTITLE, "th 8th DITANET Topical Workshop on Beam Position Monitors, CERN, Geneva, Switzreland")
            .withField(StandardField.MONTH, "#jan#")
            .withField(StandardField.YEAR, "2012")
            .withField(StandardField.COMMENT, "[11] I. Podadera, J. M. Carmona, A. Ibarra, and J. Molla, “Beam position monitor development for LIPAc”, presented at th 8th DITANET Topical Workshop on Beam Position Monitors, CERN, Geneva, Switzreland, Jan. 2012.");
    private static final BibEntry AKAGI_2023 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("15")
            .withField(StandardField.AUTHOR, "T. Akagi and others")
            .withField(StandardField.TITLE, "Achievement of high-current continuouswave deuteron injector for Linear IFMIF Prototype Accelerator (LIPAc)")
            .withField(StandardField.BOOKTITLE, "IAEA FEC’23, London, UK")
            .withField(StandardField.MONTH, "#oct#")
            .withField(StandardField.URL, "https://www.iaea.org/events/fec2023")
            .withField(StandardField.YEAR, "2023")
            .withField(StandardField.COMMENT, "[15] T. Akagi et al., “Achievement of high-current continuous- wave deuteron injector for Linear IFMIF Prototype Accelera- tor (LIPAc)”, to be presented at IAEA FEC’23, London, UK, Oct. 2023. https://www.iaea.org/events/fec2023");
    private static final BibEntry INTERNAL_NOTE = new BibEntry(StandardEntryType.TechReport)
            .withCitationKey("16")
            .withField(StandardField.TITLE, "AF4.1.1 SRF Linac Engineering Design Report")
            .withField(StandardField.NOTE, "Internal note")
            .withField(StandardField.COMMENT, "[16] “AF4.1.1 SRF Linac Engineering Design Report”, Internal note.");
    private static final BibEntry KWON_2023 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("14")
            .withField(StandardField.AUTHOR, "S. Kwon and others")
            .withField(StandardField.TITLE, "High beam current operation with beam di-agnostics at LIPAc")
            .withField(StandardField.BOOKTITLE, "HB’23, Geneva, Switzerland, paper FRC1I2, this conference")
            .withField(StandardField.MONTH, "#oct#")
            .withField(StandardField.YEAR, "2023")
            .withField(StandardField.COMMENT, "[14] S. Kwon et al., “High beam current operation with beam di-agnostics at LIPAc”, presented at HB’23, Geneva, Switzer- land, Oct. 2023, paper FRC1I2, this conference.");
    private static final BibEntry ALVER2007 = new BibEntry(StandardEntryType.Article)
            .withCitationKey("1")
            .withField(StandardField.AUTHOR, "M. O. Alver and T. Tennøy and J. A. Alfredsen and G. Øie")
            .withField(StandardField.TITLE, "Automatic measurement of rotifer brachionus plicatilis densities in first feeding tanks")
            .withField(StandardField.JOURNAL, "Aquacultural engineering")
            .withField(StandardField.VOLUME, "36")
            .withField(StandardField.NUMBER, "2")
            .withField(StandardField.YEAR, "2007")
            .withField(StandardField.PAGES, "115-121")
            .withField(StandardField.COMMENT, "[1] M. O. Alver, T. Tennøy, J. A. Alfredsen, and G. Øie, “Automatic measurement of rotifer brachionus plicatilis densities in first feeding tanks,” Aquacultural engineering, vol. 36, no. 2, pp. 115–121, 2007.");
    private static final BibEntry ALVER2007A = new BibEntry(StandardEntryType.Article)
            .withCitationKey("2")
            .withField(StandardField.AUTHOR, "M. O. Alver and others")
            .withField(StandardField.TITLE, "Estimating larval density in cod (gadus morhua) first feeding tanks using measurements of feed density and larval growth rates")
            .withField(StandardField.JOURNAL, "Aquaculture")
            .withField(StandardField.VOLUME, "268")
            .withField(StandardField.NUMBER, "1")
            .withField(StandardField.YEAR, "2007")
            .withField(StandardField.PAGES, "216-226")
            .withField(StandardField.COMMENT, "[2] M. O. Alver et al., “Estimating larval density in cod (gadus morhua) first feeding tanks using measurements of feed density and larval growth rates,” Aquaculture, vol. 268, no. 1, pp. 216–226, 2007.");
    private static final BibEntry KOPP2012 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("3")
            .withField(StandardField.AUTHOR, "O. Kopp and others")
            .withField(StandardField.TITLE, "BPMN4TOSCA: A domain-specific language to model management plans for composite applications")
            .withField(StandardField.BOOKTITLE, "Business Process Model and Notation")
            .withField(StandardField.SERIES, "LNCS")
            .withField(StandardField.VOLUME, "125")
            .withField(StandardField.YEAR, "2012")
            .withField(StandardField.PUBLISHER, "Springer")
            .withField(StandardField.COMMENT, "[3] O. Kopp et al., “BPMN4TOSCA: A domain-specific language to model management plans for composite applications,” in Business Process Model and Notation, ser. LNCS, vol. 125. Springer, 2012.");
    private static final BibEntry KOPPP2018 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("4")
            .withField(StandardField.AUTHOR, "O. Kopp and A. Armbruster and O. Zimmermann")
            .withField(StandardField.TITLE, "Markdown architectural decision records: Format and tool support")
            .withField(StandardField.BOOKTITLE, "ZEUS")
            .withField(StandardField.YEAR, "2018")
            .withField(StandardField.PUBLISHER, "CEUR-WS.org")
            .withField(StandardField.COMMENT, "[4] O. Kopp, A. Armbruster, and O. Zimmermann, “Markdown architectural decision records: Format and tool support,” in ZEUS. CEUR-WS.org, 2018.");
    private static final BibEntry KOENIG2023 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("5")
            .withField(StandardField.AUTHOR, "S. König and others")
            .withField(StandardField.TITLE, "BPMN4Cars: A car-tailored workflow engine")
            .withField(StandardField.BOOKTITLE, "INDIN")
            .withField(StandardField.PUBLISHER, "IEEE")
            .withField(StandardField.YEAR, "2023")
            .withField(StandardField.COMMENT, "[5] S. König et al., “BPMN4Cars: A car-tailored workflow engine,” in INDIN. IEEE, 2023.");

    private BibliographyFromPdfImporter bibliographyFromPdfImporter;

    @BeforeEach
    void setup() {
        bibliographyFromPdfImporter = new BibliographyFromPdfImporter();
    }

    @Test
    void tua3i2refpage() throws URISyntaxException {
        Path file = Path.of(BibliographyFromPdfImporterTest.class.getResource("/pdfs/IEEE/tua3i2refpage.pdf").toURI());
        ParserResult parserResult = bibliographyFromPdfImporter.importDatabase(file);
        BibEntry entry02 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("2")
                .withField(StandardField.AUTHOR, "K. Kondo and others")
                .withField(StandardField.TITLE, "Validation of the Linear IFMIF Prototype Accelerator (LIPAc) in Rokkasho")
                .withField(StandardField.JOURNAL, "Fusion Eng. Des") // TODO: Final dot should be kept
                .withField(StandardField.VOLUME, "153")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.PAGES, "111503")
                .withField(StandardField.DOI, "10.1016/j.fusengdes.2020.111503")
                .withField(StandardField.COMMENT, "[2] K. Kondo et al., “Validation of the Linear IFMIF Prototype Accelerator (LIPAc) in Rokkasho”, Fusion Eng. Des., vol. 153, p. 111503, 2020. doi:10.1016/j.fusengdes.2020. 111503");

        BibEntry entry04 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("4")
                .withField(StandardField.AUTHOR, "G. Devanz and others")
                .withField(StandardField.TITLE, "Manufacturing and validation tests of IFMIF low-beta HWRs")
                .withField(StandardField.BOOKTITLE, "Proc. IPAC’17, Copenhagen, Denmark")
                .withField(StandardField.MONTH, "#may#")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.PAGES, "942-944")
                .withField(StandardField.DOI, "10.18429/JACoW-IPAC2017-MOPVA039")
                .withField(StandardField.COMMENT, "[4] G. Devanz et al., “Manufacturing and validation tests of IFMIF low-beta HWRs”, in Proc. IPAC’17, Copen- hagen, Denmark, May 2017, pp. 942-944. doi:10.18429/ JACoW-IPAC2017-MOPVA039");

        BibEntry entry05 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("5")
                .withField(StandardField.AUTHOR, "B. Brañas and others")
                .withField(StandardField.TITLE, "The LIPAc Beam Dump")
                .withField(StandardField.JOURNAL, "Fusion Eng. Des")
                .withField(StandardField.VOLUME, "127")
                .withField(StandardField.PAGES, "127-138")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.DOI, "10.1016/j.fusengdes.2017.12.018")
                .withField(StandardField.COMMENT, "[5] B. Brañas et al., “The LIPAc Beam Dump”, Fusion Eng. Des., vol. 127, pp. 127-138, 2018. doi:10.1016/j.fusengdes. 2017.12.018");

        BibEntry entry08 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("8")
                .withField(StandardField.AUTHOR, "F. Scantamburlo and others")
                .withField(StandardField.TITLE, "Linear IFMIF Prototype Accelera-tor (LIPAc) Radio Frequency Quadrupole’s (RFQ) RF couplers enhancement towards CW operation at nominal voltage")
                .withField(StandardField.BOOKTITLE, "Proc. ISFNT’23, Las Palmas de Gran Canaria, Spain")
                .withField(StandardField.MONTH, "#sep#")
                .withField(StandardField.YEAR, "2023")
                .withField(StandardField.COMMENT, "[8] F. Scantamburlo et al., “Linear IFMIF Prototype Accelera-tor (LIPAc) Radio Frequency Quadrupole’s (RFQ) RF couplers enhancement towards CW operation at nominal voltage”, in Proc. ISFNT’23, Sep. 2023, Las Palmas de Gran Canaria, Spain.");

        BibEntry entry09 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("9")
                .withField(StandardField.AUTHOR, "A. De Franco and others")
                .withField(StandardField.BOOKTITLE, "Proc. IPAC’23, Venice, Italy")
                .withField(StandardField.TITLE, "RF conditioning towards continuous wave of the FRQ of the Linear IFMIF Prototype Accelerator")
                .withField(StandardField.PAGES, "2345-2348")
                .withField(StandardField.MONTH, "#may#")
                .withField(StandardField.YEAR, "2023")
                .withField(StandardField.DOI, "10.18429/JACoW-IPAC2023-TUPM065")
                .withField(StandardField.COMMENT, "[9] A. De Franco et al., “RF conditioning towards continuous wave of the FRQ of the Linear IFMIF Prototype Accelerator”, in Proc. IPAC’23, Venice, Italy, May 2023, pp. 2345-2348. doi:10.18429/JACoW-IPAC2023-TUPM065");

        BibEntry entry10 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("10")
                .withField(StandardField.AUTHOR, "K. Hirosawa and others")
                .withField(StandardField.BOOKTITLE, "Proc. PASJ’23, Japan")
                .withField(StandardField.TITLE, "High-Power RF tests of repaired circulator for LIPAc RFQ")
                .withField(StandardField.YEAR, "2023")
                .withField(StandardField.COMMENT, "[10] K. Hirosawa et al., “High-Power RF tests of repaired circu- lator for LIPAc RFQ”, in Proc. PASJ’23, 2023, Japan.");

        BibEntry entry12 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("12")
                .withField(StandardField.AUTHOR, "I. Podadera and others")
                .withField(StandardField.TITLE, "Beam commissioning of beam position and phase monitors for LIPAc")
                .withField(StandardField.BOOKTITLE, "Proc. IBIC’19, Malmö, Sweden")
                .withField(StandardField.PAGES, "534-538")
                .withField(StandardField.MONTH, "#sep#")
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.DOI, "10.18429/JACoW-IBIC2019-WEPP013")
                .withField(StandardField.COMMENT, "[12] I. Podadera et al., “Beam commissioning of beam posi- tion and phase monitors for LIPAc”, in Proc. IBIC’19, Malmö, Sweden, Sep. 2019, pp. 534-538. doi:10.18429/ JACoW-IBIC2019-WEPP013");

        BibEntry entry13 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("13")
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
                .withCitationKey("17")
                .withField(StandardField.AUTHOR, "L. Bellan and others")
                .withField(StandardField.BOOKTITLE, "Proc. ICIS’21, TRIUMF, Vancouver, BC, Canada")
                .withField(StandardField.COMMENT, "[17] L. Bellan et al., “Extraction and low energy beam transport models used for the IFMIF/EVEDA RFQ commissioning”, in Proc. ICIS’21, TRIUMF, Vancouver, BC, Canada, Sep. 2021. https://indico.cern.ch/event/1027296/")
                .withField(StandardField.MONTH, "#sep#")
                .withField(StandardField.TITLE, "Extraction and low energy beam transport models used for the IFMIF/EVEDA RFQ commissioning")
                .withField(StandardField.URL, "https://indico.cern.ch/event/1027296/")
                .withField(StandardField.YEAR, "2021");

        assertEquals(List.of(
                        KNASTER_2017,
                        entry02,
                        SHIMOSAKI_2019,
                        entry04,
                        entry05,
                        BELLAN_2021,
                        MASUDA_2022,
                        entry08,
                        entry09,
                        entry10,
                        PODADERA_2012,
                        entry12,
                        entry13,
                        KWON_2023,
                        AKAGI_2023,
                        INTERNAL_NOTE,
                        entry17),
                parserResult.getDatabase().getEntries());
    }

    @Test
    void ieeePaper() throws URISyntaxException {
        Path file = Path.of(BibliographyFromPdfImporterTest.class.getResource("/pdfs/IEEE/ieee-paper.pdf").toURI());
        ParserResult parserResult = bibliographyFromPdfImporter.importDatabase(file);
        assertEquals(List.of(ALVER2007, ALVER2007A, KOPP2012, KOPPP2018, KOENIG2023), parserResult.getDatabase().getEntries());
    }

    static Stream<BibEntry> references() {
        return Stream.of(
                KOENIG2023,
                KNASTER_2017,
                SHIMOSAKI_2019,
                BELLAN_2021,
                MASUDA_2022,
                PODADERA_2012,
                KWON_2023,
                AKAGI_2023,
                INTERNAL_NOTE,
                new BibEntry(StandardEntryType.InProceedings)
                        .withCitationKey("18")
                        .withField(StandardField.AUTHOR, "Z. Yao and D. S. Weld and W-P. Chen and H. Sun")
                        .withField(StandardField.BOOKTITLE, "Proceedings of the 2018 World Wide Web Conference")
                        .withField(StandardField.COMMENT, "[18] Z. Yao, D. S. Weld, W.-P. Chen, and H. Sun, “Staqc: A systematically mined question-code dataset from stack overflow,” in Proceedings of the 2018 World Wide Web Conference, 2018, pp. 1693–1703.")
                        .withField(StandardField.TITLE, "Staqc: A systematically mined question-code dataset from stack overflow")
                        .withField(StandardField.PAGES, "1693-1703")
                        .withField(StandardField.YEAR, "2018")
        );
    }

    @ParameterizedTest
    @MethodSource
    void references(BibEntry expectedEntry) {
        List<BibliographyFromPdfImporter.IntermediateData> intermediateDataList = BibliographyFromPdfImporter.getIntermediateData(expectedEntry.getField(StandardField.COMMENT).get());
        assertEquals(1, intermediateDataList.size());
        BibliographyFromPdfImporter.IntermediateData intermediateData = intermediateDataList.getFirst();
        assertEquals(expectedEntry, bibliographyFromPdfImporter.parseReference(intermediateData.number(), intermediateData.reference()));
    }
}
