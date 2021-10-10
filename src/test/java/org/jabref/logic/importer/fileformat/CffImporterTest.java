package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CffImporterTest {

    private CffImporter importer;

    @BeforeEach
    public void setUp() {
        importer = new CffImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("CFF", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("cff", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(StandardFileType.CFF, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the CFF format. Is only used to cite software, one entry per file.",
                importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValid.cff").toURI());
        assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("CffImporterTestInvalid1.cff", "CffImporterTestInvalid2.cff");

        for (String string : list) {
            Path file = Path.of(CffImporterTest.class.getResource(string).toURI());
            assertFalse(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testImportEntriesBasic() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValid.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = bibEntries.get(0);

        BibEntry expected = getPopulatedEntry().withField(StandardField.AUTHOR, "Joe van Smith");

        assertEquals(entry, expected);
    }

    @Test
    public void testImportEntriesMultipleAuthors() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValidMultAuthors.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = bibEntries.get(0);

        BibEntry expected = getPopulatedEntry();

        assertEquals(entry, expected);

    }

    @Test
    public void testImportEntriesSwhIdSelect1() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValidSwhIdSelect1.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = bibEntries.get(0);

        BibEntry expected = getPopulatedEntry().withField(StandardField.SWHID, "swh:1:rel:22ece559cc7cc2364edc5e5593d63ae8bd229f9f");

        assertEquals(entry, expected);
    }

    @Test
    public void testImportEntriesSwhIdSelect2() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValidSwhIdSelect2.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = bibEntries.get(0);

        BibEntry expected = getPopulatedEntry().withField(StandardField.SWHID, "swh:1:cnt:94a9ed024d3859793618152ea559a168bbcbb5e2");

        assertEquals(entry, expected);
    }

    @Test
    public void testImportEntriesDataset() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestDataset.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = bibEntries.get(0);

        BibEntry expected = getPopulatedEntry();
        expected.setType(StandardEntryType.Dataset);

        assertEquals(entry, expected);
    }

    @Test
    public void testImportEntriesDoiSelect() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestDoiSelect.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = bibEntries.get(0);

        BibEntry expected = getPopulatedEntry();

        assertEquals(entry, expected);
    }

    @Test
    public void testImportEntriesUnknownFields() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestUnknownFields.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();
        BibEntry entry = bibEntries.get(0);

        BibEntry expected = getPopulatedEntry().withField(new UnknownField("commit"), "10ad");

        assertEquals(entry, expected);
    }

    public BibEntry getPopulatedEntry() {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Software);

        entry.setField(StandardField.AUTHOR, "Joe van Smith and Bob Jones, Jr.");
        entry.setField(StandardField.TITLE, "Test");
        entry.setField(StandardField.URL, "www.google.com");
        entry.setField(StandardField.REPOSITORY, "www.github.com");
        entry.setField(StandardField.DOI, "10.0000/TEST");
        entry.setField(StandardField.DATE, "2000-07-02");
        entry.setField(StandardField.COMMENT, "Test entry.");
        entry.setField(StandardField.ABSTRACT, "Test abstract.");
        entry.setField(StandardField.LICENSE, "MIT");
        entry.setField(StandardField.VERSION, "1.0");

        return entry;
    }
}
