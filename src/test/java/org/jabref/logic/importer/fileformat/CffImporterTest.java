package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.BiblatexSoftwareField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CffImporterTest {

    private CffImporter importer;

    @BeforeEach
    public void setUp() {
        CitationKeyPatternPreferences citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class, Answers.RETURNS_SMART_NULLS);
        when(citationKeyPatternPreferences.getKeyPattern()).thenReturn(GlobalCitationKeyPattern.fromPattern("[auth][year]"));
        importer = new CffImporter(citationKeyPatternPreferences);
    }

    @Test
    public void getFormatName() {
        assertEquals("CFF", importer.getName());
    }

    @Test
    public void getCLIId() {
        assertEquals("cff", importer.getId());
    }

    @Test
    public void sGetExtensions() {
        assertEquals(StandardFileType.CFF, importer.getFileType());
    }

    @Test
    public void getDescription() {
        assertEquals("Importer for the CFF format, which is intended to make software and datasets citable.",
                importer.getDescription());
    }

    @Test
    public void isRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValid.cff").toURI());
        assertTrue(importer.isRecognizedFormat(file));
    }

    @Test
    public void isRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("CffImporterTestInvalid1.cff", "CffImporterTestInvalid2.cff");

        for (String string : list) {
            Path file = Path.of(CffImporterTest.class.getResource(string).toURI());
            assertFalse(importer.isRecognizedFormat(file));
        }
    }

    @Test
    public void importEntriesBasic() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValid.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();

        BibEntry expected = getPopulatedEntry().withField(StandardField.AUTHOR, "Joe van Smith");

        assertEquals(entry, expected);
    }

    @Test
    public void importEntriesMultipleAuthors() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValidMultAuthors.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();

        BibEntry expected = getPopulatedEntry();

        assertEquals(entry, expected);
    }

    @Test
    public void importEntriesSwhIdSelect1() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValidSwhIdSelect1.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();

        BibEntry expected = getPopulatedEntry().withField(BiblatexSoftwareField.SWHID, "swh:1:rel:22ece559cc7cc2364edc5e5593d63ae8bd229f9f");

        assertEquals(entry, expected);
    }

    @Test
    public void importEntriesSwhIdSelect2() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValidSwhIdSelect2.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();

        BibEntry expected = getPopulatedEntry().withField(BiblatexSoftwareField.SWHID, "swh:1:cnt:94a9ed024d3859793618152ea559a168bbcbb5e2");

        assertEquals(entry, expected);
    }

    @Test
    public void importEntriesDataset() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestDataset.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();

        BibEntry expected = getPopulatedEntry();
        expected.setType(StandardEntryType.Dataset);

        assertEquals(entry, expected);
    }

    @Test
    public void importEntriesDoiSelect() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestDoiSelect.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();

        BibEntry expected = getPopulatedEntry();

        assertEquals(entry, expected);
    }

    @Test
    public void importEntriesUnknownFields() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestUnknownFields.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();

        BibEntry expected = getPopulatedEntry().withField(new UnknownField("commit"), "10ad");

        assertEquals(entry, expected);
    }

    @Test
    public void importEntriesPreferredCitation() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterPreferredCitation.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry mainEntry = bibEntries.getFirst();
        BibEntry preferredEntry = bibEntries.getLast();
        String citeKey = preferredEntry.getCitationKey().orElse("");

        BibEntry expectedMain = getPopulatedEntry();
        expectedMain.setField(StandardField.CITES, citeKey);

        BibEntry expectedPreferred = new BibEntry(StandardEntryType.InProceedings).withCitationKey(citeKey);
        expectedPreferred.setField(StandardField.AUTHOR, "Jonathan von Duke and Jim Kingston, Jr.");
        expectedPreferred.setField(StandardField.DOI, "10.0001/TEST");
        expectedPreferred.setField(StandardField.URL, "www.github.com");

        assertEquals(mainEntry, expectedMain);
        assertEquals(preferredEntry, expectedPreferred);
    }

    @Test
    public void importEntriesReferences() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterReferences.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry mainEntry = bibEntries.getFirst();
        BibEntry referenceEntry1 = bibEntries.get(1);
        BibEntry referenceEntry2 = bibEntries.getLast();
        String citeKey1 = referenceEntry1.getCitationKey().orElse("");
        String citeKey2 = referenceEntry2.getCitationKey().orElse("");

        BibEntry expectedMain = getPopulatedEntry();
        expectedMain.setField(StandardField.RELATED, citeKey1 + ", " + citeKey2);

        BibEntry expectedReference1 = new BibEntry(StandardEntryType.InProceedings).withCitationKey(citeKey1);
        expectedReference1.setField(StandardField.AUTHOR, "Jonathan von Duke and Jim Kingston, Jr.");
        expectedReference1.setField(StandardField.YEAR, "2007");
        expectedReference1.setField(StandardField.DOI, "10.0001/TEST");
        expectedReference1.setField(StandardField.URL, "www.example.com");

        BibEntry expectedReference2 = new BibEntry(StandardEntryType.Manual).withCitationKey(citeKey2);
        expectedReference2.setField(StandardField.AUTHOR, "Arthur Clark, Jr. and Luca von Diamond");
        expectedReference2.setField(StandardField.DOI, "10.0002/TEST");
        expectedReference2.setField(StandardField.URL, "www.facebook.com");

        assertEquals(mainEntry, expectedMain);
        assertEquals(referenceEntry1, expectedReference1);
        assertEquals(referenceEntry2, expectedReference2);
    }

    public BibEntry getPopulatedEntry() {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Software);
        entry.setField(StandardField.AUTHOR, "Joe van Smith and Bob Jones, Jr.");
        entry.setField(StandardField.TITLE, "Test");
        entry.setField(StandardField.URL, "www.google.com");
        entry.setField(BiblatexSoftwareField.REPOSITORY, "www.github.com");
        entry.setField(StandardField.DOI, "10.0000/TEST");
        entry.setField(StandardField.DATE, "2000-07-02");
        entry.setField(StandardField.COMMENT, "Test entry.");
        entry.setField(StandardField.ABSTRACT, "Test abstract.");
        entry.setField(BiblatexSoftwareField.LICENSE, "MIT");
        entry.setField(StandardField.VERSION, "1.0");

        return entry;
    }
}
