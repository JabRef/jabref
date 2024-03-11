
package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

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
    public final void exportForNoEntriesWritesNothing(@TempDir Path tempFile) throws Exception {
        Path file = tempFile.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        cffExporter.export(databaseContext, tempFile, Collections.emptyList());
        assertEquals(Collections.emptyList(), Files.readAllLines(file));
    }

    @Test
    public final void exportsCorrectContent(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.URL, "http://example.com");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "# YAML 1.2",
                "---",
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: \"Test Title\"",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "preferred-citation:",
                "  type: article",
                "  authors:",
                "    - family-names: Author",
                "      given-names: Test",
                "  title: \"Test Title\"",
                "  url: \"http://example.com\"");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void usesCorrectType(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DOI, "random_doi_value");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "# YAML 1.2",
                "---",
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: \"Test Title\"",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "preferred-citation:",
                "  type: conference-paper",
                "  authors:",
                "    - family-names: Author",
                "      given-names: Test",
                "  title: \"Test Title\"",
                "  doi: \"random_doi_value\"");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void usesCorrectDefaultValues(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Thesis);

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "# YAML 1.2",
                "---",
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: \"No title specified.\"",
                "authors: No author specified."
        );

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void exportsSoftwareCorrectly(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Software)
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DOI, "random_doi_value");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "# YAML 1.2",
                "---",
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: \"Test Title\"",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "type: software",
                "doi: \"random_doi_value\"");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void exportsSoftwareDateCorrectly(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Software)
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DATE, "2003-11-06");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "# YAML 1.2",
                "---",
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: \"Test Title\"",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "type: software",
                "date-released: 2003-11-06");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void exportsArticleDateCorrectly(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DATE, "2003-11");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "# YAML 1.2",
                "---",
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: \"Test Title\"",
                "authors:",
                "  - family-names: Author",
                "    given-names: Test",
                "preferred-citation:",
                "  type: article",
                "  authors:",
                "    - family-names: Author",
                "      given-names: Test",
                "  title: \"Test Title\"",
                "  month: 11",
                "  year: 2003");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void passesModifiedCharset(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "谷崎 潤一郎")
                .withField(StandardField.TITLE, "細雪")
                .withField(StandardField.URL, "http://example.com");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "# YAML 1.2",
                "---",
                "cff-version: 1.2.0",
                "message: If you use this software, please cite it using the metadata from this file.",
                "title: \"細雪\"",
                "authors:",
                "  - family-names: 潤一郎",
                "    given-names: 谷崎",
                "preferred-citation:",
                "  type: article",
                "  authors:",
                "    - family-names: 潤一郎",
                "      given-names: 谷崎",
                "  title: \"細雪\"",
                "  url: \"http://example.com\"");

        assertEquals(expected, Files.readAllLines(file));
    }
}

