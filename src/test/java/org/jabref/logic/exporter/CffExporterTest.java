
package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.importer.fileformat.CffImporter;
import org.jabref.logic.importer.fileformat.CffImporterTest;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CffExporterTest {

    private static Exporter cffExporter;
    private static BibDatabaseContext databaseContext;

    @BeforeAll
    static void setUp() {
        cffExporter = new CffExporter();
        databaseContext = new BibDatabaseContext();
    }

    @Test
    public final void exportForNoEntriesWritesNothing(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        cffExporter.export(databaseContext, tempDir, Collections.emptyList());
        assertEquals(Collections.emptyList(), Files.readAllLines(file));
    }

    @Test
    public final void exportsCorrectContent(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.URL, "http://example.com");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: Test Title",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "references:",
                "  type: article",
                "  authors:",
                "    - family-names: Author",
                "      given-names: Test",
                "  title: Test Title",
                "  url: http://example.com");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void usesCorrectType(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DOI, "random_doi_value");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: Test Title",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "references:",
                "  type: conference-paper",
                "  authors:",
                "    - family-names: Author",
                "      given-names: Test",
                "  title: Test Title",
                "  doi: random_doi_value");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void usesCorrectDefaultValues(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Thesis).withCitationKey("test");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: No title specified.",
                "authors: No author specified."
        );

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void exportsSoftwareCorrectly(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Software)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DOI, "random_doi_value");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

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
    public final void exportsSoftwareDateCorrectly(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Software)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DATE, "2003-11-06");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

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
    public final void exportsArticleDateCorrectly(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DATE, "2003-11");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: Test Title",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "references:",
                "  type: article",
                "  authors:",
                "    - family-names: Author",
                "      given-names: Test",
                "  title: Test Title",
                "  month: 11",
                "  year: 2003");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void passesModifiedCharset(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "谷崎 潤一郎")
                .withField(StandardField.TITLE, "細雪")
                .withField(StandardField.URL, "http://example.com");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: 細雪",
                "authors:",
                "  - family-names: 潤一郎",
                "    given-names: 谷崎",
                "references:",
                "  type: article",
                "  authors:",
                "    - family-names: 潤一郎",
                "      given-names: 谷崎",
                "  title: 細雪",
                "  url: http://example.com");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void roundTripTest(@TempDir Path tempDir) throws Exception {

        // First, import the file which will be parsed as two entries
        CffImporter importer = new CffImporter();
        Path file = Path.of(CffImporterTest.class.getResource("CITATION.cff").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry softwareEntry = bibEntries.getFirst();
        BibEntry articleEntry = bibEntries.getLast();

        // Then, export them separately and check they have all required fields
        Path softwareFile = tempDir.resolve("CITATION_SOFTWARE.cff");
        Path articleFile = tempDir.resolve("CITATION_ARTICLE.cff");
        Files.createFile(softwareFile);
        Files.createFile(articleFile);

        cffExporter.export(databaseContext, softwareFile, Collections.singletonList(softwareEntry));
        cffExporter.export(databaseContext, articleFile, Collections.singletonList(articleEntry));

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
                "url: https://www.jabref.org");

        Set<String> expectedArticle = Set.of(
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: 'JabRef: BibTeX-based literature management software'",
                "authors:",
                "  - family-names: Kopp",
                "    given-names: Oliver",
                "  - family-names: Snethlage",
                "    given-names: Carl Christian",
                "  - family-names: Schwentker",
                "    given-names: Christoph",
                "references:",
                "  type: article",
                "  authors:",
                "    - family-names: Kopp",
                "      given-names: Oliver",
                "    - family-names: Snethlage",
                "      given-names: Carl Christian",
                "    - family-names: Schwentker",
                "      given-names: Christoph",
                "  title: 'JabRef: BibTeX-based literature management software'",
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
        try (Stream<String> st = Files.lines(softwareFile)) {
            assertEquals(expectedSoftware, st.collect(Collectors.toSet()));
        }

        try (Stream<String> st = Files.lines(articleFile)) {
            assertEquals(expectedArticle, st.collect(Collectors.toSet()));
        }
    }
}

