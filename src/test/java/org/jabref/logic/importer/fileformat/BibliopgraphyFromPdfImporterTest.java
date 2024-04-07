package org.jabref.logic.importer.fileformat;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
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

class BibliopgraphyFromPdfImporterTest {

    private BibliopgraphyFromPdfImporter bibliopgraphyFromPdfImporter;

    @BeforeEach
    void setup() {
        GlobalCitationKeyPattern globalCitationKeyPattern = GlobalCitationKeyPattern.fromPattern("[auth][year]");
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
        bibliopgraphyFromPdfImporter = new BibliopgraphyFromPdfImporter(citationKeyPatternPreferences);
    }

    @Test
    void tua3i2refpage() throws Exception {
        Path file = Path.of(BibliopgraphyFromPdfImporterTest.class.getResource("tua3i2refpage.pdf").toURI());
        ParserResult parserResult = bibliopgraphyFromPdfImporter.importDatabase(file);
        BibEntry entry01 = new BibEntry();
        assertEquals(List.of(entry01), parserResult.getDatabase().getEntries());
    }

    static Stream<Arguments> references() {
        return Stream.of(
                Arguments.of(
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "J. Knaster and others")
                                .withField(StandardField.TITLE, "Overview of the IFMIF/EVEDA project")
                                .withField(StandardField.JOURNAL, "Nucl. Fusion")
                                .withField(StandardField.VOLUME, "57")
                                .withField(StandardField.PAGES, "102016")
                                .withField(StandardField.YEAR, "2017")
                                .withField(StandardField.DOI, "10.1088/1741-4326/aa6a6a")
                                .withField(StandardField.COMMENT, "[1] J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57, p. 102016, 2017. doi:10.1088/ 1741-4326/aa6a6a"),
                        "1",
                        "J. Knaster et al., “Overview of the IFMIF/EVEDA project”, Nucl. Fusion, vol. 57, p. 102016, 2017. doi:10.1088/ 1741-4326/aa6a6a"
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.InProceedings)
                                .withField(StandardField.AUTHOR, "Y. Shimosaki and others")
                                .withField(StandardField.TITLE, "Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc")
                                .withField(StandardField.BOOKTITLE, "Proc. IPAC’19, Melbourne, Australia")
                                .withField(StandardField.MONTH, "#may#")
                                .withField(StandardField.YEAR, "2019")
                                .withField(StandardField.PAGES, "977-979")
                                .withField(StandardField.DOI, "10.18429/JACoW-IPAC2019-MOPTS051")
                                .withField(StandardField.COMMENT, "[2] Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia, May 2019, pp. 977-979. doi:10.18429/ JACoW-IPAC2019-MOPTS051"),
                        "2",
                        "Y. Shimosaki et al., “Lattice design for 5 MeV – 125 mA CW RFQ operation in LIPAc”, in Proc. IPAC’19, Mel- bourne, Australia, May 2019, pp. 977-979. doi:10.18429/ JACoW-IPAC2019-MOPTS051"
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.InProceedings)
                                .withField(StandardField.AUTHOR, "L. Bellan and others")
                                .withField(StandardField.TITLE, "Acceleration of the high current deuteron beam through the IFMIF-EVEDA beam dynamics performances")
                                .withField(StandardField.BOOKTITLE, "Proc. HB’21, Batavia, IL, USA")
                                .withField(StandardField.MONTH, "#oct#")
                                .withField(StandardField.YEAR, "2021")
                                .withField(StandardField.PAGES, "197-202")
                                .withField(StandardField.DOI, "10.18429/JACoW-HB2021-WEDC2")
                                .withField(StandardField.COMMENT, "[5] L. Bellan et al., “Acceleration of the high current deuteron beam through the IFMIF-EVEDA beam dynamics perfor- mances”, in Proc. HB’21, Batavia, IL, USA, Oct. 2021, pp. 197-202. doi:10.18429/JACoW-HB2021-WEDC2"),
                        "5",
                        "L. Bellan et al., “Acceleration of the high current deuteron beam through the IFMIF-EVEDA beam dynamics perfor- mances”, in Proc. HB’21, Batavia, IL, USA, Oct. 2021, pp. 197-202. doi:10.18429/JACoW-HB2021-WEDC2"
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.InProceedings)
                                .withField(StandardField.AUTHOR, "K. Masuda and others")
                                .withField(StandardField.TITLE, "Commissioning of IFMIF Prototype Accelerator towards CW operation")
                                .withField(StandardField.BOOKTITLE, "Proc. LINAC’22, Liverpool, UK")
                                .withField(StandardField.MONTH, "#aug#")
                                .withField(StandardField.YEAR, "2022")
                                .withField(StandardField.PAGES, "319-323")
                                .withField(StandardField.DOI, "10.18429/JACoW-LINAC2022-TU2AA04")
                                .withField(StandardField.COMMENT, "[6] K. Masuda et al., “Commissioning of IFMIF Prototype Ac- celerator towards CW operation”, in Proc. LINAC’22, Liv- erpool, UK, Aug.-Sep. 2022, pp. 319-323. doi:10.18429/ JACoW-LINAC2022-TU2AA04"),

                        "6",
                        "K. Masuda et al., “Commissioning of IFMIF Prototype Ac- celerator towards CW operation”, in Proc. LINAC’22, Liv- erpool, UK, Aug.-Sep. 2022, pp. 319-323. doi:10.18429/ JACoW-LINAC2022-TU2AA04"
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.InProceedings)
                                .withField(StandardField.AUTHOR, "I. Podadera, J. M. Carmona, A. Ibarra, and J. Molla")
                                .withField(StandardField.TITLE, "Beam position monitor development for LIPAc")
                                .withField(StandardField.BOOKTITLE, "th 8th DITANET Topical Workshop on Beam Position Monitors, CERN, Geneva, Switzreland")
                                .withField(StandardField.MONTH, "#jan#")
                                .withField(StandardField.YEAR, "2012")
                                .withField(StandardField.COMMENT, "[10] I. Podadera, J. M. Carmona, A. Ibarra, and J. Molla, “Beam position monitor development for LIPAc”, presented at th 8th DITANET Topical Workshop on Beam Position Monitors, CERN, Geneva, Switzreland, Jan. 2012."),
                        "10",
                        "I. Podadera, J. M. Carmona, A. Ibarra, and J. Molla, “Beam position monitor development for LIPAc”, presented at th 8th DITANET Topical Workshop on Beam Position Monitors, CERN, Geneva, Switzreland, Jan. 2012."
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.InProceedings)
                                .withField(StandardField.AUTHOR, "S. Kwon and others")
                                .withField(StandardField.TITLE, "High beam current operation with beam di-agnostics at LIPAc")
                                .withField(StandardField.BOOKTITLE, "HB’23, Geneva, Switzerland, paper FRC1I2, this conference")
                                .withField(StandardField.MONTH, "#oct#")
                                .withField(StandardField.YEAR, "2023")
                                .withField(StandardField.COMMENT, "[14] S. Kwon et al., “High beam current operation with beam di-agnostics at LIPAc”, presented at HB’23, Geneva, Switzer- land, Oct. 2023, paper FRC1I2, this conference."),
                        "14",
                        "S. Kwon et al., “High beam current operation with beam di-agnostics at LIPAc”, presented at HB’23, Geneva, Switzer- land, Oct. 2023, paper FRC1I2, this conference."
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.InProceedings)
                                .withField(StandardField.AUTHOR, "T. Akagi and others")
                                .withField(StandardField.TITLE, "Achievement of high-current continuouswave deuteron injector for Linear IFMIF Prototype Accelerator (LIPAc)")
                                .withField(StandardField.BOOKTITLE, "IAEA FEC’23, London, UK, https://www.iaea.org/events/fec2023")
                                .withField(StandardField.MONTH, "#oct#")
                                .withField(StandardField.YEAR, "2023")
                                .withField(StandardField.COMMENT, "[15] T. Akagi et al., “Achievement of high-current continuous- wave deuteron injector for Linear IFMIF Prototype Accelera- tor (LIPAc)”, to be presented at IAEA FEC’23, London, UK, Oct. 2023. https://www.iaea.org/events/fec2023"),
                        "15",
                        "T. Akagi et al., “Achievement of high-current continuous- wave deuteron injector for Linear IFMIF Prototype Accelera- tor (LIPAc)”, to be presented at IAEA FEC’23, London, UK, Oct. 2023. https://www.iaea.org/events/fec2023"
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.TechReport)
                                .withField(StandardField.TITLE, "AF4.1.1 SRF Linac Engineering Design Report")
                                .withField(StandardField.NOTE, "Internal note")
                                .withField(StandardField.COMMENT, "[16] “AF4.1.1 SRF Linac Engineering Design Report”, Internal note."),
                        "16",
                        "“AF4.1.1 SRF Linac Engineering Design Report”, Internal note."
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void references(BibEntry expectedEntry, String number, String reference) {
        assertEquals(expectedEntry, bibliopgraphyFromPdfImporter.parseReference(number, reference));
    }
}
