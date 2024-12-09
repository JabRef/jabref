package org.jabref.logic.importer.fileformat;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfContentImporterTest {

    private final PdfContentImporter importer = new PdfContentImporter();

    @Test
    void doesNotHandleEncryptedPdfs() throws Exception {
        Path file = Path.of(PdfContentImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(List.of(), result);
    }

    @Test
    void importTwiceWorksAsExpected() throws Exception {
        Path file = Path.of(PdfContentImporter.class.getResource("/pdfs/minimal.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "1 ")
                .withField(StandardField.TITLE, "Hello World")
                .withFiles(List.of(new LinkedFile("", file.toAbsolutePath(), "PDF")));
        assertEquals(List.of(expected), result);

        List<BibEntry> resultSecondImport = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(List.of(expected), resultSecondImport);
    }

    @Test
    void parsingEditorWithoutPagesorSeriesInformation() {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Anke Lüdeling and Merja Kytö (Eds.)")
                .withField(StandardField.EDITOR, "Anke Lüdeling and Merja Kytö")
                .withField(StandardField.PUBLISHER, "Springer")
                .withField(StandardField.TITLE, "Corpus Linguistics – An International Handbook – Lüdeling, Anke, Kytö, Merja (Eds.)");

        String firstPageContents = """
                Corpus Linguistics – An International Handbook – Lüdeling, Anke,
                Kytö, Merja (Eds.)

                Anke Lüdeling, Merja Kytö (Eds.)

                VOLUME 2

                This handbook provides an up-to-date survey of the field of corpus linguistics, a Handbücher zur Sprach- und
                field whose methodology has revolutionized much of the empirical work done in Kommunikationswissenschaft / Handbooks

                of Linguistics and Communication Science
                most fields of linguistic study over the past decade. (HSK) 29/2

                vii, 578 pages
                Corpus linguistics investigates human language by starting out from large
                """;

        assertEquals(Optional.of(entry), importer.getEntryFromPDFContent(firstPageContents, "\n", ""));
    }

    @Test
    void parsingWithoutActualDOINumber() {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Link to record in KAR and http://kar.kent.ac.uk/51043/  and Document Version and UNSPECIFIED  and Master of Research (MRes) thesis and University of Kent")
                .withField(StandardField.TITLE, "Kent Academic Repository Full text document (pdf) Citation for published version Smith, Lucy Anna (2014) Mortality in the Ornamental Fish Retail Sector: an Analysis of Stock Losses and Stakeholder Opinions. DOI")
                .withField(StandardField.YEAR, "5104");

        String firstPageContents = """
                Kent Academic Repository Full text document (pdf)
                Citation for published version
                Smith, Lucy Anna (2014) Mortality in the Ornamental Fish Retail Sector: an Analysis of Stock
                Losses and Stakeholder Opinions.
                DOI

                Link to record in KAR
                http://kar.kent.ac.uk/51043/
                Document Version
                UNSPECIFIED
                Master of Research (MRes) thesis, University of Kent,.""";

        assertEquals(Optional.of(entry), importer.getEntryFromPDFContent(firstPageContents, "\n", ""));
    }

    @Test
    void extractDOIFromPage1() {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.DOI, "10.1017/S0007114507795296")
                .withField(StandardField.AUTHOR, "Review Article")
                .withField(StandardField.TITLE, "British Journal of Nutrition (2008), 99, 1–11 doi: 10.1017/S0007114507795296 q The Authors")
                .withField(StandardField.YEAR, "2008");

        String firstPageContent = """
                British Journal of Nutrition (2008), 99, 1–11 doi: 10.1017/S0007114507795296
                q The Authors 2008

                Review Article

                Cocoa and health: a decade of research

                Karen A. Cooper1, Jennifer L. Donovan2, Andrew L. Waterhouse3 and Gary Williamson1*
                1Nestlé Research Center, Vers-Chez-les-Blanc, PO Box 44, CH-1000 Lausanne 26, Switzerland
                2Department of Psychiatry and Behavioural Sciences, Medical University of South Carolina, Charleston, SC 29425, USA
                3Department of Viticulture & Enology, University of California, Davis, CA 95616, USA

                (Received 5 December 2006 – Revised 29 May 2007 – Accepted 31 May 2007)

                Abbreviations: FMD, flow-mediated dilation; NO, nitirc oxide.

                *Corresponding author: Dr Gary Williamson, fax þ41 21 785 8544, email gary.williamson@rdls.nestle.com

                British Journal of Nutrition
                https://doi.org/10.1017/S0007114507795296 Published online by Cambridge University Press""";

        assertEquals(Optional.of(entry), importer.getEntryFromPDFContent(firstPageContent, "\n", ""));
    }

    @ParameterizedTest
    @MethodSource("providePdfData")
    void pdfTitleExtraction(String expectedTitle, String filePath) throws Exception {
        Path file = Path.of(Objects.requireNonNull(PdfContentImporter.class.getResource(filePath)).toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(Optional.of(expectedTitle), result.getFirst().getTitle());
    }

    private static Stream<Arguments> providePdfData() {
        return Stream.of(
                Arguments.of("Fundamentals of Distributed Computing: A Practical Tour of Vector Clock Systems", "/pdfs/PdfContentImporter/Baldoni2002.pdf"),
                Arguments.of("JabRef Example for Reference Parsing", "/pdfs/IEEE/ieee-paper-cover.pdf"),
                Arguments.of("On How We Can Teach – Exploring New Ways in Professional Software Development for Students", "/pdfs/PdfContentImporter/Kriha2018.pdf"),
                Arguments.of("JabRef Example for Reference Parsing", "/pdfs/IEEE/ieee-paper.pdf"),
                Arguments.of("Paper Title", "/org/jabref/logic/importer/util/LNCS-minimal.pdf"),
                Arguments.of("Is Oil the future?", "/pdfs/example-scientificThesisTemplate.pdf"),
                Arguments.of("Thesis Title", "/pdfs/thesis-example.pdf"),
                Arguments.of("Recovering Trace Links Between Software Documentation And Code", "/pdfs/PdfContentImporter/Keim2024.pdf"),
                Arguments.of("On the impact of service-oriented patterns on software evolvability: a controlled experiment and metric-based analysis", "/pdfs/PdfContentImporter/Bogner2019.pdf"),
                Arguments.of("Pandemic programming", "/pdfs/PdfContentImporter/Ralph2020.pdf"),
                Arguments.of("Do RESTful API design rules have an impact on the understandability of Web APIs?", "/pdfs/PdfContentImporter/Bogner2023.pdf"),
                Arguments.of("Adopting microservices and DevOps in the cyber-physical systems domain: A rapid review and case study", "/pdfs/PdfContentImporter/Fritzsch2022.pdf")
        );
    }
}
