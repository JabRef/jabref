package org.jabref.logic.xmp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.FilePreferences;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.jabref.logic.exporter.EmbeddedBibFilePdfExporter.EMBEDDED_FILE_NAME;
import static org.jabref.logic.xmp.DublinCoreExtractor.DC_COVERAGE;
import static org.jabref.logic.xmp.DublinCoreExtractor.DC_RIGHTS;
import static org.jabref.logic.xmp.DublinCoreExtractor.DC_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XmpUtilWriterTest {

    private static BibEntry olly2018;
    private static BibEntry toral2006;
    private static BibEntry vapnik2000;
    private XmpPreferences xmpPreferences;

    private void initBibEntries() {
        olly2018 = new BibEntry(StandardEntryType.Article);
        olly2018.setCitationKey("Olly2018");
        olly2018.setField(StandardField.AUTHOR, "Olly and Johannes");
        olly2018.setField(StandardField.TITLE, "Stefan's palace");
        olly2018.setField(StandardField.JOURNAL, "Test Journal");
        olly2018.setField(StandardField.VOLUME, "1");
        olly2018.setField(StandardField.NUMBER, "1");
        olly2018.setField(StandardField.PAGES, "1-2");
        olly2018.setMonth(Month.MARCH);
        olly2018.setField(StandardField.ISSN, "978-123-123");
        olly2018.setField(StandardField.NOTE, "NOTE");
        olly2018.setField(StandardField.ABSTRACT, "ABSTRACT");
        olly2018.setField(StandardField.COMMENT, "COMMENT");
        olly2018.setField(StandardField.DOI, "10/3212.3123");
        olly2018.setField(StandardField.FILE, ":article_dublinCore.pdf:PDF");
        olly2018.setField(StandardField.GROUPS, "NO");
        olly2018.setField(StandardField.HOWPUBLISHED, "online");
        olly2018.setField(StandardField.KEYWORDS, "k1, k2");
        olly2018.setField(StandardField.OWNER, "me");
        olly2018.setField(StandardField.REVIEW, "review");
        olly2018.setField(StandardField.URL, "https://www.olly2018.edu");

        toral2006 = new BibEntry(StandardEntryType.InProceedings);
        toral2006.setField(StandardField.AUTHOR, "Toral, Antonio and Munoz, Rafael");
        toral2006.setField(StandardField.TITLE, "A proposal to automatically build and maintain gazetteers for Named Entity Recognition by using Wikipedia");
        toral2006.setField(StandardField.BOOKTITLE, "Proceedings of EACL");
        toral2006.setField(StandardField.PAGES, "56--61");
        toral2006.setField(StandardField.EPRINTTYPE, "asdf");
        toral2006.setField(StandardField.OWNER, "Ich");
        toral2006.setField(StandardField.URL, "www.url.de");

        vapnik2000 = new BibEntry(StandardEntryType.Book);
        vapnik2000.setCitationKey("vapnik2000");
        vapnik2000.setField(StandardField.TITLE, "The Nature of Statistical Learning Theory");
        vapnik2000.setField(StandardField.PUBLISHER, "Springer Science + Business Media");
        vapnik2000.setField(StandardField.AUTHOR, "Vladimir N. Vapnik");
        vapnik2000.setField(StandardField.DOI, "10.1007/978-1-4757-3264-1");
        vapnik2000.setField(StandardField.OWNER, "Ich");
        vapnik2000.setField(StandardField.LANGUAGE, "English, Japanese");
        vapnik2000.setDate(new Date(2000, 5));

        vapnik2000.setField(new UnknownField(DC_COVERAGE), "coverageField");
        vapnik2000.setField(new UnknownField((DC_SOURCE)), "JabRef");
        vapnik2000.setField(new UnknownField(DC_RIGHTS), "Right To X");
    }

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @BeforeEach
    void setUp() {
        xmpPreferences = mock(XmpPreferences.class);
        // The code assumes privacy filters to be off
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(false);
        when(xmpPreferences.shouldEnableEnclosingBracketsFilter()).thenReturn(false);

        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        this.initBibEntries();
    }

    /**
     * Test for writing a PDF file with a single DublinCore metadata entry.
     */
    @Test
    void testWriteXmp(@TempDir Path tempDir) throws Exception {
        BibDatabaseMode bibDatabaseMode = BibDatabaseMode.BIBTEX;
        BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();
        FieldWriterPreferences fieldWriterPreferences = new FieldWriterPreferences(true, List.of(StandardField.MONTH), new FieldContentFormatterPreferences());
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibDatabase database = databaseContext.getDatabase();

        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.getUser()).thenReturn(tempDir.toAbsolutePath().toString());
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);

//        Path pdfFile = this.createDefaultFile("JabRef_writeSingle.pdf", tempDir);
        Path pdfFile = tempDir.resolve("existing.pdf").toAbsolutePath();
        // read a bib entry from the tests before
        BibEntry entry = vapnik2000;
        LinkedFile linkedFile = createDefaultLinkedFile("existing.pdf", tempDir);
        entry.setFiles(Arrays.asList(linkedFile));
        database.insertEntry(entry);

        // write the changed bib entry to the PDF
        XmpUtilWriter.writeXmp(pdfFile.toAbsolutePath().toString(), entry, null, xmpPreferences);

        // export
        EmbeddedBibFilePdfExporter embeddedBibExporter = new EmbeddedBibFilePdfExporter(bibDatabaseMode, bibEntryTypesManager, fieldWriterPreferences, xmpPreferences);
        embeddedBibExporter.exportToFileByPath(databaseContext, database, filePreferences, pdfFile);

        // read entry again
        List<BibEntry> entriesWritten = XmpUtilReader.readXmp(pdfFile.toAbsolutePath().toString(), xmpPreferences);
        BibEntry entryWritten = entriesWritten.get(0);

        System.out.println(entryWritten);

        entryWritten.clearField(StandardField.FILE);

        // compare the two entries
        assertEquals(entry, entryWritten);
    }

    /**
     * Test, which writes multiple metadata entries to a PDF and reads them again to test the size.
     */
    @Test
    void testWriteMultipleBibEntries(@TempDir Path tempDir) throws IOException, TransformerException {
        Path pdfFile = this.createDefaultFile("JabRef_writeMultiple.pdf", tempDir);

        List<BibEntry> entries = Arrays.asList(olly2018, vapnik2000, toral2006);

        XmpUtilWriter.writeXmp(Path.of(pdfFile.toAbsolutePath().toString()), entries, null, xmpPreferences);

        List<BibEntry> entryList = XmpUtilReader.readXmp(Path.of(pdfFile.toAbsolutePath().toString()), xmpPreferences);
        assertEquals(3, entryList.size());
    }

    /**
     * Test for writing XMP metadata to PDF file and export without enclosing braces.
     */
    @Test
    void testWriteAndExportXmpWithoutEnclosingBraces(@TempDir Path tempDir) throws Exception {
        // turn filter on
        when(xmpPreferences.shouldEnableEnclosingBracketsFilter()).thenReturn(true);
        // some initialization
        BibDatabaseMode bibDatabaseMode = BibDatabaseMode.BIBTEX;
        BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();
        FieldWriterPreferences fieldWriterPreferences = new FieldWriterPreferences(true, List.of(StandardField.MONTH), new FieldContentFormatterPreferences());
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibDatabase database = databaseContext.getDatabase();

        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.getUser()).thenReturn(tempDir.toAbsolutePath().toString());
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);

        Path pdfFile = tempDir.resolve("existing.pdf").toAbsolutePath();
        // read a bib entry from the tests before
        BibEntry entry = vapnik2000;
        // link file to entry
        LinkedFile linkedFile = createDefaultLinkedFile("existing.pdf", tempDir);
        entry.setFiles(Arrays.asList(linkedFile));
        // insert entry to database
        database.insertEntry(entry);

        String expectBibTeX = "@Book{vapnik2000,\n" +
                "  author    = Vladimir N. Vapnik,\n" +
                "  publisher = Springer Science + Business Media,\n" +
                "  title     = The Nature of Statistical Learning Theory,\n" +
                "  year      = 2000,\n" +
                "  month     = may,\n" +
                "  coverage  = coverageField,\n" +
                "  doi       = 10.1007/978-1-4757-3264-1,\n" +
                "  language  = English, Japanese,\n" +
                "  owner     = Ich,\n" +
                "  rights    = Right To X,\n" +
                "  source    = JabRef,\n" +
                "}";

        // write the bib entry to the PDF and export
        XmpUtilWriter.writeXmp(pdfFile.toAbsolutePath().toString(), entry, null, xmpPreferences);
        EmbeddedBibFilePdfExporter embeddedBibExporter = new EmbeddedBibFilePdfExporter(bibDatabaseMode, bibEntryTypesManager, fieldWriterPreferences, xmpPreferences);
        embeddedBibExporter.exportToFileByPath(databaseContext, database, filePreferences, pdfFile);

        // extract bibTeX embedded in PDF
        String bibTeX = extractBibTeX(pdfFile);

        // remove FILE for now due to different separator
        // not sure why
        bibTeX = bibTeX.replaceAll("(?m)^  file.*(?:\\r?\\n)?", "").strip().replaceAll("\\r\\n?", "\n");

        assertEquals(expectBibTeX, bibTeX);
    }

    /**
     * Test for writing XMP metadata to PDF file and export with enclosing braces.
     */
    @Test
    void testWriteAndExportXmpWithEnclosingBraces(@TempDir Path tempDir) throws Exception {
        // turn filter on
        when(xmpPreferences.shouldEnableEnclosingBracketsFilter()).thenReturn(false);
        // some initialization
        BibDatabaseMode bibDatabaseMode = BibDatabaseMode.BIBTEX;
        BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();
        FieldWriterPreferences fieldWriterPreferences = new FieldWriterPreferences(true, List.of(StandardField.MONTH), new FieldContentFormatterPreferences());
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibDatabase database = databaseContext.getDatabase();

        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.getUser()).thenReturn(tempDir.toAbsolutePath().toString());
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);

        Path pdfFile = tempDir.resolve("existing.pdf").toAbsolutePath();
        // read a bib entry from the tests before
        BibEntry entry = vapnik2000;
        // link file to entry
        LinkedFile linkedFile = createDefaultLinkedFile("existing.pdf", tempDir);
        entry.setFiles(Arrays.asList(linkedFile));
        // insert entry to database
        database.insertEntry(entry);

        String expectBibTeX = "@Book{vapnik2000,\n" +
                "  author    = {Vladimir N. Vapnik},\n" +
                "  publisher = {Springer Science + Business Media},\n" +
                "  title     = {The Nature of Statistical Learning Theory},\n" +
                "  year      = {2000},\n" +
                "  month     = may,\n" +
                "  coverage  = {coverageField},\n" +
                "  doi       = {10.1007/978-1-4757-3264-1},\n" +
                "  language  = {English, Japanese},\n" +
                "  owner     = {Ich},\n" +
                "  rights    = {Right To X},\n" +
                "  source    = {JabRef},\n" +
                "}";

        // write the bib entry to the PDF and export
        XmpUtilWriter.writeXmp(pdfFile.toAbsolutePath().toString(), entry, null, xmpPreferences);
        EmbeddedBibFilePdfExporter embeddedBibExporter = new EmbeddedBibFilePdfExporter(bibDatabaseMode, bibEntryTypesManager, fieldWriterPreferences, xmpPreferences);
        embeddedBibExporter.exportToFileByPath(databaseContext, database, filePreferences, pdfFile);

        // extract bibTeX embedded in PDF
        String bibTeX = extractBibTeX(pdfFile);

        // remove FILE for now due to different separator
        // not sure why
        bibTeX = bibTeX.replaceAll("(?m)^  file.*(?:\\r?\\n)?", "").strip().replaceAll("\\r\\n?", "\n");

        assertEquals(expectBibTeX, bibTeX);
    }

    /**
     * Extract BibTeX that is embedded in the PDF file during export.
     */
    private String extractBibTeX(Path path) throws IOException {
        PDDocument document = Loader.loadPDF(path.toFile());
        PDDocumentCatalog documentCatalog = document.getDocumentCatalog();
        PDDocumentNameDictionary nameDictionary = documentCatalog.getNames();
        PDEmbeddedFilesNameTreeNode efTree = nameDictionary.getEmbeddedFiles();
        Map<String, PDComplexFileSpecification> names = efTree.getNames();
        PDComplexFileSpecification fileSpecification = names.get(EMBEDDED_FILE_NAME);
        PDEmbeddedFile embeddedFile = fileSpecification.getEmbeddedFile();
        InputStream inputStream = embeddedFile.createInputStream();

        String bibTeX = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        return bibTeX;
    }

    private Path createDefaultFile(String fileName, Path tempDir) throws IOException {
        // create a default PDF
        Path pdfFile = tempDir.resolve(fileName);
        try (PDDocument pdf = new PDDocument()) {
            // Need a single page to open in Acrobat
            pdf.addPage(new PDPage());
            pdf.save(pdfFile.toAbsolutePath().toString());
        }

        return pdfFile;
    }

    private static LinkedFile createDefaultLinkedFile(String fileName, Path tempDir) throws IOException {
        Path pdfFile = tempDir.resolve(fileName);
        try (PDDocument pdf = new PDDocument()) {
            pdf.addPage(new PDPage());
            pdf.save(pdfFile.toAbsolutePath().toString());
        }

        LinkedFile linkedFile = new LinkedFile("A linked pdf", pdfFile, "PDF");

        return linkedFile;
    }
}
