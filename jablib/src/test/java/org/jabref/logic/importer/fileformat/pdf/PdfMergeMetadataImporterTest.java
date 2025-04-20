package org.jabref.logic.importer.fileformat.pdf;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class PdfMergeMetadataImporterTest {

    private PdfMergeMetadataImporter importer;

    @BeforeEach
    void setUp() {
        GrobidPreferences grobidPreferences = mock(GrobidPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(grobidPreferences.isGrobidEnabled()).thenReturn(true);
        when(grobidPreferences.getGrobidURL()).thenReturn("http://grobid.jabref.org:8070");

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(importFormatPreferences.grobidPreferences()).thenReturn(grobidPreferences);

        importer = new PdfMergeMetadataImporter(importFormatPreferences);
    }

    @Test
    void doesNotHandleEncryptedPdfs() throws Exception {
        Path file = Path.of(PdfMergeMetadataImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    @Disabled("Switch from ottobib to OpenLibraryFetcher changed the results")
    void importWorksAsExpected() throws Exception {
        Path file = Path.of(PdfMergeMetadataImporterTest.class.getResource("mixedMetadata.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        // From DOI (contained in embedded bib file)
        BibEntry expected = new BibEntry(StandardEntryType.Book)
                .withCitationKey("9780134685991")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.PUBLISHER, "Addison Wesley")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.MONTH, "jul")
                .withField(StandardField.DOI, "10.1002/9781118257517");

        // From ISBN (contained on first page verbatim bib entry)
        expected.setField(StandardField.DATE, "2018-01-31");
        expected.setField(new UnknownField("ean"), "9780134685991");
        expected.setField(StandardField.ISBN, "0134685997");
        expected.setField(StandardField.URL, "https://www.ebook.de/de/product/28983211/joshua_bloch_effective_java.html");

        // From embedded bib file
        expected.setField(StandardField.COMMENT, "From embedded bib");

        // From first page verbatim bib entry
        expected.setField(StandardField.JOURNAL, "Some Journal");
        expected.setField(StandardField.VOLUME, "1");

        // From merge
        expected.setFiles(List.of(new LinkedFile("", file.toAbsolutePath(), StandardFileType.PDF.getName())));

        assertEquals(List.of(expected), result);
    }

    @Test
    void pdfMetadataExtractedFrom2024SPLCBecker() throws Exception {
        Path file = Path.of(PdfMergeMetadataImporterTest.class.getResource("2024_SPLC_Becker.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Becker_2024")
                .withField(StandardField.AUTHOR, "Becker, Martin and Rabiser, Rick and Botterweck, Goetz")
                .withField(StandardField.TITLE, "Not Quite There Yet: Remaining Challenges in Systems and Software Product Line Engineering as Perceived by Industry Practitioners")
                .withField(StandardField.BOOKTITLE, "28th ACM International Systems and Software Product Line Conference")
                .withField(StandardField.SERIES, "SPLC ’24")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.YEAR, "2024")
                .withField(StandardField.MONTH, "#sep#")
                .withField(StandardField.PAGES, "179--190")
                .withField(StandardField.DOI, "10.1145/3646548.3672587")
                .withField(StandardField.KEYWORDS, "case studies about companies adopting product line engineering")
                .withField(FieldFactory.parseField("collection"), "SPLC ’24")
                .withField(StandardField.FILE, ":" + file.toString().replace("\\", "/").replace(":", "\\:") + ":PDF")

                // TODO: Abstract not correct yet --> parsing logic needs to be improved
                // Abstract is CC-BY, thus no issue to include it here.
                .withField(StandardField.ABSTRACT, "Research on system and software product line engineering (SPLE) and the community around it have been inspired by industrial applications. However, despite decades of research, industry is still struggling with adopting product line approaches and more generally with managing system variability. We argue that it is essential to better understand why this is the case. Particularly, we need to understand the current challenges industry is facing wrt. adopting SPLE practices, how far existing research helps industry practitioners to cope with their challenges, and where additional research would be required. We conducted a hybrid workshop at the 2023 Systems and Software Product Line Conference (SPLC) with over 30 participants from industry and academia. 9 companies from diverse domains and in different phases of SPLE adoption presented their context and perceived challenges. We grouped, discussed, and rated the relevance of the articulated challenges. We then formed clusters of relevant research topics to discuss existing literature as well as research opportunities. In this paper, we report the industry cases, the identified challenges and clusters of research topics, provide pointers to existing work, and discuss research opportunities. With this, we want to enable industry practitioners to become aware of typical challenges and find their way into the existing body of knowledge and to relevant fields of research. CCS CONCEPTS • Software and its engineering → Software product lines.");

        assertEquals(List.of(expected), result);
    }

    @Test
    void importRelativizesFilePath() throws Exception {
        // Initialize database and preferences
        FilePreferences preferences = mock(FilePreferences.class);
        BibDatabaseContext database = new BibDatabaseContext();

        // Initialize file and working directory
        Path file = Path.of(PdfMergeMetadataImporter.class.getResource("/pdfs/minimal.pdf").toURI());
        Path directory = Path.of(PdfMergeMetadataImporter.class.getResource("/pdfs/").toURI());
        when(preferences.getMainFileDirectory()).thenReturn(Optional.of(directory));

        List<BibEntry> result = importer.importDatabase(file, database, preferences).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "1 ")
                .withField(StandardField.TITLE, "Hello World")
                // Expecting relative path
                .withField(StandardField.FILE, ":minimal.pdf:PDF");

        assertEquals(List.of(expected), result);
    }
}
