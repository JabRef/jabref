package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
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
    void setUp() {
        CitationKeyPatternPreferences citationKeyPatternPreferences = mock(
                CitationKeyPatternPreferences.class,
                Answers.RETURNS_SMART_NULLS
        );
        when(citationKeyPatternPreferences.getKeyPatterns())
                .thenReturn(GlobalCitationKeyPatterns.fromPattern("[auth][year]"));
        importer = new CffImporter(citationKeyPatternPreferences);
    }

    @Test
    void getFormatName() {
        assertEquals("CFF", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("cff", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.CFF, importer.getFileType());
    }

    @Test
    void getDescription() {
        assertEquals("Importer for the CFF format, which is intended to make software and datasets citable.",
                importer.getDescription());
    }

    @Test
    void isRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValid.cff").toURI());
        assertTrue(importer.isRecognizedFormat(file));
    }

    @Test
    void isRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("CffImporterTestInvalid1.cff", "CffImporterTestInvalid2.cff");
        for (String string : list) {
            Path file = Path.of(CffImporterTest.class.getResource(string).toURI());
            assertFalse(importer.isRecognizedFormat(file));
        }
    }

    @Test
    void importEntriesBasic() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValid.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();
        BibEntry expected = getPopulatedEntry().withField(StandardField.AUTHOR, "Joe van Smith");
        assertEquals(entry, expected);
    }

    @Test
    void importEntriesMultipleAuthors() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValidMultAuthors.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();
        BibEntry expected = getPopulatedEntry();
        assertEquals(entry, expected);
    }

    @Test
    void importEntriesSwhIdSelect1() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValidSwhIdSelect1.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();
        BibEntry expected = getPopulatedEntry()
                .withField(BiblatexSoftwareField.SWHID, "swh:1:rel:22ece559cc7cc2364edc5e5593d63ae8bd229f9f");
        assertEquals(entry, expected);
    }

    @Test
    void importEntriesSwhIdSelect2() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestValidSwhIdSelect2.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = bibEntries.getFirst();
        BibEntry expected = getPopulatedEntry()
                .withField(BiblatexSoftwareField.SWHID, "swh:1:cnt:94a9ed024d3859793618152ea559a168bbcbb5e2");
        assertEquals(entry, expected);
    }

    @Test
    void importEntriesDataset() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestDataset.cff").toURI());
        BibEntry entry = importer.importDatabase(file).getDatabase().getEntries().getFirst();
        BibEntry expected = getPopulatedEntry();
        expected.setType(StandardEntryType.Dataset);
        assertEquals(entry, expected);
    }

    @Test
    void importEntriesDoiSelect() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestDoiSelect.cff").toURI());
        BibEntry entry = importer.importDatabase(file).getDatabase().getEntries().getFirst();
        BibEntry expected = getPopulatedEntry();
        assertEquals(entry, expected);
    }

    @Test
    void importEntriesUnknownFields() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestUnknownFields.cff").toURI());
        BibEntry entry = importer.importDatabase(file).getDatabase().getEntries().getFirst();
        BibEntry expected = getPopulatedEntry().withField(new UnknownField("commit"), "10ad");
        assertEquals(entry, expected);
    }

    @Test
    void importEntriesMultilineAbstract() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterTestMultilineAbstract.cff").toURI());
        BibEntry entry = importer.importDatabase(file).getDatabase().getEntries().getFirst();
        BibEntry expected = getPopulatedEntry().withField(StandardField.ABSTRACT,
                """
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                        Morbi vel tortor sem. Suspendisse posuere nibh commodo nunc iaculis,
                        sed eleifend justo malesuada. Curabitur sodales auctor cursus.
                        Fusce non elit elit. Mauris sollicitudin lobortis pulvinar.
                        Nullam vel enim quis tellus pellentesque sagittis non at justo.
                        Nam convallis et velit non auctor. Praesent id ex eros. Nullam
                        ullamcorper leo vitae leo rhoncus porta. In lobortis rhoncus nisl,
                        sit amet aliquet elit cursus ut. Cras laoreet justo in tortor vehicula,
                        quis semper tortor maximus. Nulla vitae ante ullamcorper, viverra
                        est at, laoreet tortor. Suspendisse rutrum hendrerit est in commodo.
                        Aenean urna purus, lobortis a condimentum et, varius ut augue.
                        Praesent ac lectus id mi posuere elementum.
                        """);
        assertEquals(entry, expected);
    }

    @Test
    void importEntriesPreferredCitation() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterPreferredCitation.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry mainEntry = bibEntries.getFirst();
        BibEntry preferredEntry = bibEntries.getLast();
        String citeKey = preferredEntry.getCitationKey().orElse("");

        BibEntry expectedMain = getPopulatedEntry().withField(StandardField.CITES, citeKey);

        BibEntry expectedPreferred = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey(citeKey)
                .withField(StandardField.AUTHOR, "Jonathan von Duke and Jim Kingston, Jr.")
                .withField(StandardField.DOI, "10.0001/TEST")
                .withField(StandardField.URL, "www.github.com");

        assertEquals(mainEntry, expectedMain);
        assertEquals(preferredEntry, expectedPreferred);
    }

    @Test
    void importEntriesReferences() throws IOException, URISyntaxException {
        Path file = Path.of(CffImporterTest.class.getResource("CffImporterReferences.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry mainEntry = bibEntries.getFirst();
        BibEntry referenceEntry1 = bibEntries.get(1);
        BibEntry referenceEntry2 = bibEntries.getLast();
        String citeKey1 = referenceEntry1.getCitationKey().orElse("");
        String citeKey2 = referenceEntry2.getCitationKey().orElse("");

        BibEntry expectedMain = getPopulatedEntry().withField(StandardField.RELATED, citeKey1 + "," + citeKey2);

        BibEntry expectedReference1 = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey(citeKey1)
                .withField(StandardField.AUTHOR, "Jonathan von Duke and Jim Kingston, Jr.")
                .withField(StandardField.YEAR, "2007")
                .withField(StandardField.DOI, "10.0001/TEST")
                .withField(StandardField.URL, "www.example.com");

        BibEntry expectedReference2 = new BibEntry(StandardEntryType.Manual)
                .withCitationKey(citeKey2)
                .withField(StandardField.AUTHOR, "Arthur Clark, Jr. and Luca von Diamond")
                .withField(StandardField.DOI, "10.0002/TEST")
                .withField(StandardField.URL, "www.facebook.com");

        assertEquals(mainEntry, expectedMain);
        assertEquals(referenceEntry1, expectedReference1);
        assertEquals(referenceEntry2, expectedReference2);
    }

    public BibEntry getPopulatedEntry() {
        return new BibEntry(StandardEntryType.Software)
                .withField(StandardField.AUTHOR, "Joe van Smith and Bob Jones, Jr.")
                .withField(StandardField.TITLE, "Test")
                .withField(StandardField.URL, "www.google.com")
                .withField(BiblatexSoftwareField.REPOSITORY, "www.github.com")
                .withField(StandardField.DOI, "10.0000/TEST")
                .withField(StandardField.DATE, "2000-07-02")
                .withField(StandardField.COMMENT, "Test entry.")
                .withField(StandardField.ABSTRACT, "Test abstract.")
                .withField(BiblatexSoftwareField.LICENSE, "MIT")
                .withField(StandardField.VERSION, "1.0");
    }
}
