package org.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PdfContentImporterTest {

    private PdfContentImporter importer;

    @BeforeEach
    void setUp() {
        importer = new PdfContentImporter(mock(ImportFormatPreferences.class));
    }

    @Test
    void testsGetExtensions() {
        assertEquals(StandardFileType.PDF, importer.getFileType());
    }

    @Test
    void testGetDescription() {
        assertEquals("PdfContentImporter parses data of the first page of the PDF and creates a BibTeX entry. Currently, Springer and IEEE formats are supported.",
                     importer.getDescription());
    }

    @Test
    void doesNotHandleEncryptedPdfs() throws Exception {
        Path file = Path.of(PdfContentImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void importTwiceWorksAsExpected() throws Exception {
        Path file = Path.of(PdfContentImporter.class.getResource("/pdfs/minimal.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.InProceedings);
        expected.setField(StandardField.AUTHOR, "1 ");
        expected.setField(StandardField.TITLE, "Hello World");
        expected.setFiles(Collections.singletonList(new LinkedFile("", file.toAbsolutePath(), "PDF")));

        List<BibEntry> resultSecondImport = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        assertEquals(Collections.singletonList(expected), result);
        assertEquals(Collections.singletonList(expected), resultSecondImport);
    }

    @Test
    void testParsingEditorWithoutPagesorSeriesInformation() {

        BibEntry entry = new BibEntry(StandardEntryType.InProceedings);
        entry.setField(StandardField.AUTHOR, "Anke Lüdeling and Merja Kytö (Eds.)");
        entry.setField(StandardField.EDITOR, "Anke Lüdeling and Merja Kytö");
        entry.setField(StandardField.PUBLISHER, "Springer");
        entry.setField(StandardField.TITLE, "Corpus Linguistics – An International Handbook – Lüdeling, Anke, Kytö, Merja (Eds.)");

        String firstPageContents = "Corpus Linguistics – An International Handbook – Lüdeling, Anke,\n" +
                                   "Kytö, Merja (Eds.)\n" +
                                   "\n" +
                                   "Anke Lüdeling, Merja Kytö (Eds.)\n" +
                                   "\n" +
                                   "VOLUME 2\n" +
                                   "\n" +
                                   "This handbook provides an up-to-date survey of the field of corpus linguistics, a Handbücher zur Sprach- und\n" +
                                   "field whose methodology has revolutionized much of the empirical work done in Kommunikationswissenschaft / Handbooks\n" +
                                   "\n" +
                                   "of Linguistics and Communication Science\n" +
                                   "most fields of linguistic study over the past decade. (HSK) 29/2\n" +
                                   "\n" +
                                   "vii, 578 pages\n" +
                                   "Corpus linguistics investigates human language by starting out from large\n";

        assertEquals(Optional.of(entry), importer.getEntryFromPDFContent(firstPageContents, "\n"));
    }

    @Test
    void testParsingWithoutActualDOINumber() {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings);
        entry.withField(StandardField.AUTHOR, "Link to record in KAR and http://kar.kent.ac.uk/51043/  and Document Version and UNSPECIFIED  and Master of Research (MRes) thesis and University of Kent")
             .withField(StandardField.TITLE, "Kent Academic Repository Full text document (pdf) Citation for published version Smith, Lucy Anna (2014) Mortality in the Ornamental Fish Retail Sector: an Analysis of Stock Losses and Stakeholder Opinions. DOI")
             .withField(StandardField.YEAR, "5104");

        String firstPageContents = "Kent Academic Repository Full text document (pdf)\n"
                                   + "Citation for published version\n"
                                   + "Smith, Lucy Anna (2014) Mortality in the Ornamental Fish Retail Sector: an Analysis of Stock\n"
                                   + "Losses and Stakeholder Opinions.\n"
                                   + "DOI\n\n\n"
                                   + "Link to record in KAR\n"
                                   + "http://kar.kent.ac.uk/51043/\n"
                                   + "Document Version\n"
                                   + "UNSPECIFIED\n"
                                   + "Master of Research (MRes) thesis, University of Kent,.";

        assertEquals(Optional.of(entry), importer.getEntryFromPDFContent(firstPageContents, "\n"));

    }
}
