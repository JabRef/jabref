package org.jabref.logic.exporter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.importer.fileformat.CffImporter;
import org.jabref.logic.importer.fileformat.CffImporterTest;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CffExporterTest {

    private static Exporter cffExporter;
    private static BibDatabaseContext databaseContext;

    @BeforeAll
    static void setUp() {
        cffExporter = new CffExporter();
        databaseContext = BibDatabaseContext.empty();
    }

    @Test
    final void exportForNoEntriesWritesNothing(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path file = tempDir.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        cffExporter.export(databaseContext, tempDir, List.of());
        assertEquals(List.of(), Files.readAllLines(file));
    }

    @Test
    final void exportsCorrectContent(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.URL, "http://example.com");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: No title specified.",
                "authors:",
                "  - name: /",
                "references:",
                "  - title: Test Title",
                "    authors:",
                "      - family-names: Author",
                "        given-names: Test",
                "    type: article",
                "    url: http://example.com");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void usesCorrectType(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DOI, "random_doi_value");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: No title specified.",
                "authors:",
                "  - name: /",
                "references:",
                "  - title: Test Title",
                "    authors:",
                "      - family-names: Author",
                "        given-names: Test",
                "    type: conference-paper",
                "    doi: random_doi_value");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void usesCorrectDefaultValues(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.Thesis).withCitationKey("test");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: No title specified.",
                "authors:",
                "  - name: /",
                "references:",
                "  - title: No title specified.",
                "    authors:",
                "      - name: /",
                "    type: misc");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void exportsSoftwareCorrectly(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.Software)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DOI, "random_doi_value");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: Test Title",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "type: software",
                "doi: random_doi_value");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void exportsSoftwareDateCorrectly(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.Software)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DATE, "2003-11-06");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: Test Title",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "type: software",
                "date-released: '2003-11-06'");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void exportsArticleDateCorrectly(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DATE, "2003-11");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: No title specified.",
                "authors:",
                "  - name: /",
                "references:",
                "  - title: Test Title",
                "    authors:",
                "      - family-names: Author",
                "        given-names: Test",
                "    type: article",
                "    month: 11",
                "    year: 2003");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void passesModifiedCharset(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "谷崎 潤一郎")
                .withField(StandardField.TITLE, "細雪")
                .withField(StandardField.URL, "http://example.com");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: No title specified.",
                "authors:",
                "  - name: /",
                "references:",
                "  - title: 細雪",
                "    authors:",
                "      - family-names: 潤一郎",
                "        given-names: 谷崎",
                "    type: article",
                "    url: http://example.com");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void roundTripTest(@TempDir Path tempDir) throws URISyntaxException, IOException, SaveException, ParserConfigurationException, TransformerException {
        CitationKeyPatternPreferences citationKeyPatternPreferences = mock(
                CitationKeyPatternPreferences.class,
                Answers.RETURNS_SMART_NULLS
        );
        when(citationKeyPatternPreferences.getKeyPatterns())
                .thenReturn(GlobalCitationKeyPatterns.fromPattern("[auth][year]"));

        // First, import the file which will be parsed as two entries
        CffImporter importer = new CffImporter(citationKeyPatternPreferences);
        Path file = Path.of(CffImporterTest.class.getResource("CITATION.cff").toURI());
        BibDatabase db = importer.importDatabase(file).getDatabase();
        BibDatabaseContext dbc = new BibDatabaseContext(db);

        // Then, export both entries that will be exported as one file
        Path out = tempDir.resolve("OUT.cff");
        Files.createFile(out);
        cffExporter.export(dbc, out, db.getEntries());

        Set<String> expectedSoftware = Set.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: JabRef",
                "authors:",
                "  - family-names: Kopp",
                "    given-names: Oliver",
                "  - family-names: Diez",
                "    given-names: Tobias",
                "  - family-names: Schwentker",
                "    given-names: Christoph",
                "  - family-names: Snethlage",
                "    given-names: Carl Christian",
                "  - family-names: Asketorp",
                "    given-names: Jonatan",
                "  - family-names: Tutzer",
                "    given-names: Benedikt",
                "  - family-names: Ertel",
                "    given-names: Thilo",
                "  - family-names: Nasri",
                "    given-names: Houssem",
                "type: software",
                "keywords:",
                "  - reference manager",
                "  - bibtex",
                "  - biblatex",
                "license: MIT",
                "repository-code: https://github.com/jabref/jabref/",
                "abstract: JabRef is an open-source, cross-platform citation and reference management tool.",
                "url: https://www.jabref.org",
                "preferred-citation:",
                "  title: 'JabRef: BibTeX-based literature management software'",
                "  authors:",
                "    - family-names: Kopp",
                "      given-names: Oliver",
                "    - family-names: Snethlage",
                "      given-names: Carl Christian",
                "    - family-names: Schwentker",
                "      given-names: Christoph",
                "  type: article",
                "  month: '11'",
                "  issue: '138'",
                "  volume: '44'",
                "  year: '2023'",
                "  doi: 10.47397/tb/44-3/tb138kopp-jabref",
                "  journal: TUGboat",
                "  number: '3'",
                "  start: '441'",
                "  end: '447'");

        // Tests equality of sets since last lines order is random and relies on entries internal order
        try (Stream<String> st = Files.lines(out)) {
            assertEquals(expectedSoftware, st.collect(Collectors.toSet()));
        }
    }
}

