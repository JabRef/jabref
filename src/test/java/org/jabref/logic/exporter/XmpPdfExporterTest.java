package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.FilePreferences;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XmpPdfExporterTest {

    @TempDir static Path tempDir;

    private static BibEntry olly2018 = new BibEntry(StandardEntryType.Article);
    private static BibEntry toral2006 = new BibEntry(StandardEntryType.Article);
    private static BibEntry vapnik2000 = new BibEntry(StandardEntryType.Article);

    private XmpPdfExporter exporter;
    private PdfXmpImporter importer;
    private XmpPreferences xmpPreferences;

    private BibDatabaseContext databaseContext;
    private JournalAbbreviationRepository abbreviationRepository;
    private FilePreferences filePreferences;

    private static void initBibEntries() throws IOException {
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

        LinkedFile linkedFile = createDefaultLinkedFile("existing.pdf", tempDir);
        olly2018.setFiles(List.of(linkedFile));

        toral2006.setField(StandardField.AUTHOR, "Toral, Antonio and Munoz, Rafael");
        toral2006.setField(StandardField.TITLE, "A proposal to automatically build and maintain gazetteers for Named Entity Recognition by using Wikipedia");
        toral2006.setField(StandardField.BOOKTITLE, "Proceedings of EACL");
        toral2006.setField(StandardField.PAGES, "56--61");
        toral2006.setField(StandardField.EPRINTTYPE, "asdf");
        toral2006.setField(StandardField.OWNER, "Ich");
        toral2006.setField(StandardField.URL, "www.url.de");

        toral2006.setFiles(List.of(new LinkedFile("non-existing", "path/to/nowhere.pdf", "PDF")));

        vapnik2000.setCitationKey("vapnik2000");
        vapnik2000.setField(StandardField.TITLE, "The Nature of Statistical Learning Theory");
        vapnik2000.setField(StandardField.PUBLISHER, "Springer Science + Business Media");
        vapnik2000.setField(StandardField.AUTHOR, "Vapnik, Vladimir N.");
        vapnik2000.setField(StandardField.DOI, "10.1007/978-1-4757-3264-1");
        vapnik2000.setField(StandardField.OWNER, "Ich");
    }

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @BeforeEach
    void setUp() throws IOException {
        abbreviationRepository = mock(JournalAbbreviationRepository.class);
        filePreferences = mock(FilePreferences.class);
        when(filePreferences.getUserAndHost()).thenReturn(tempDir.toAbsolutePath().toString());
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);

        xmpPreferences = new XmpPreferences(false, Collections.emptySet(), new SimpleObjectProperty<>(','));
        exporter = new XmpPdfExporter(xmpPreferences);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        importer = new PdfXmpImporter(xmpPreferences);

        databaseContext = new BibDatabaseContext();
        BibDatabase dataBase = databaseContext.getDatabase();

        initBibEntries();
        dataBase.insertEntry(olly2018);
        dataBase.insertEntry(toral2006);
        dataBase.insertEntry(vapnik2000);
    }

    @AfterEach
    void reset() throws IOException {
        List<BibEntry> expectedEntries = databaseContext.getEntries();
        for (BibEntry entry : expectedEntries) {
            entry.clearField(StandardField.FILE);
        }
        LinkedFile linkedFile = createDefaultLinkedFile("existing.pdf", tempDir);
        olly2018.setFiles(List.of(linkedFile));
        toral2006.setFiles(List.of(new LinkedFile("non-existing", "path/to/nowhere.pdf", "PDF")));
    }

    @ParameterizedTest
    @MethodSource("provideBibEntriesWithValidPdfFileLinks")
    void successfulExportToAllFilesOfEntry(BibEntry bibEntryWithValidPdfFileLink) throws Exception {
        assertTrue(exporter.exportToAllFilesOfEntry(databaseContext, filePreferences, bibEntryWithValidPdfFileLink, List.of(olly2018), abbreviationRepository));
    }

    @ParameterizedTest
    @MethodSource("provideBibEntriesWithInvalidPdfFileLinks")
    void unsuccessfulExportToAllFilesOfEntry(BibEntry bibEntryWithValidPdfFileLink) throws Exception {
        assertFalse(exporter.exportToAllFilesOfEntry(databaseContext, filePreferences, bibEntryWithValidPdfFileLink, List.of(olly2018), abbreviationRepository));
    }

    public static Stream<Arguments> provideBibEntriesWithValidPdfFileLinks() {
        return Stream.of(Arguments.of(olly2018));
    }

    public static Stream<Arguments> provideBibEntriesWithInvalidPdfFileLinks() {
        return Stream.of(Arguments.of(vapnik2000), Arguments.of(toral2006));
    }

    @ParameterizedTest
    @MethodSource("providePathsToValidPDFs")
    void successfulExportToFileByPath(Path path) throws Exception {
        assertTrue(exporter.exportToFileByPath(databaseContext, filePreferences, path, abbreviationRepository));
    }

    @ParameterizedTest
    @MethodSource("providePathsToInvalidPDFs")
    void unsuccessfulExportToFileByPath(Path path) throws Exception {
        assertFalse(exporter.exportToFileByPath(databaseContext, filePreferences, path, abbreviationRepository));
    }

    @ParameterizedTest
    @MethodSource("providePathToNewPDFs")
    public void testRoundtripExportImport(Path path) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.newLineAtOffset(25, 500);
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.showText("This PDF was created by JabRef. It demonstrates the embedding of XMP data in PDF files. Please open the file metadata view of your PDF viewer to see the attached files. Note that the normal usage is to embed the BibTeX data in an existing PDF.");
                contentStream.endText();
            }
            document.save(path.toString());
        }
        new XmpUtilWriter(xmpPreferences).writeXmp(path, databaseContext.getEntries(), databaseContext.getDatabase());

        List<BibEntry> importedEntries = importer.importDatabase(path).getDatabase().getEntries();
        importedEntries.forEach(bibEntry -> new FieldFormatterCleanup(StandardField.AUTHOR, new NormalizeNamesFormatter()).cleanup(bibEntry));

        List<BibEntry> expectedEntries = databaseContext.getEntries();
        for (BibEntry entry : expectedEntries) {
            entry.clearField(StandardField.FILE);
            entry.addFile(createDefaultLinkedFile("original.pdf", tempDir));
        }
        assertEquals(expectedEntries, importedEntries);
    }

    public static Stream<Arguments> providePathToNewPDFs() {
        return Stream.of(Arguments.of(tempDir.resolve("original.pdf").toAbsolutePath()));
    }

    public static Stream<Arguments> providePathsToValidPDFs() {
        return Stream.of(Arguments.of(tempDir.resolve("existing.pdf").toAbsolutePath()));
    }

    public static Stream<Arguments> providePathsToInvalidPDFs() throws IOException {
        LinkedFile existingFileThatIsNotLinked = createDefaultLinkedFile("notlinked.pdf", tempDir);
        return Stream.of(
                Arguments.of(Path.of("")),
                Arguments.of(tempDir.resolve("path/to/nowhere.pdf").toAbsolutePath()),
                Arguments.of(Path.of(existingFileThatIsNotLinked.getLink())));
    }

    private static LinkedFile createDefaultLinkedFile(String fileName, Path tempDir) throws IOException {
        return createDefaultLinkedFile("", fileName, tempDir);
    }

    private static LinkedFile createDefaultLinkedFile(String description, String fileName, Path tempDir) throws IOException {
        Path pdfFile = tempDir.resolve(fileName);
        try (PDDocument pdf = new PDDocument()) {
            pdf.addPage(new PDPage());
            pdf.save(pdfFile.toAbsolutePath().toString());
        }

        return new LinkedFile("", pdfFile, "PDF");
    }
}
