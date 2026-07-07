package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jabref.logic.exporter.BlankLineBehaviour;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import tools.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class HayagrivaImporterTest {

    private static Exporter hayagrivaYamlExporter;
    private static HayagrivaImporter hayagrivaImporter;
    private static BibDatabaseContext databaseContext;

    @BeforeAll
    static void setUp() {
        hayagrivaYamlExporter = new TemplateExporter(
                "Hayagriva YAML",
                "hayagrivayaml",
                "hayagrivayaml",
                null,
                StandardFileType.YAML,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                SaveOrder.getDefaultSaveOrder(),
                BlankLineBehaviour.DELETE_BLANKS);
        hayagrivaImporter = new HayagrivaImporter();
        databaseContext = new BibDatabaseContext();
    }

    private BibEntry roundTrip(BibEntry entry, Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path file = tempDir.resolve("roundtrip.yml");
        Files.createFile(file);
        hayagrivaYamlExporter.export(databaseContext, file, List.of(entry));

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ParserResult result = hayagrivaImporter.importDatabase(reader);
            List<BibEntry> imported = result.getDatabase().getEntries();
            assertEquals(1, imported.size(), "Expected exactly one imported entry");
            return imported.getFirst();
        }
    }

    private static Stream<Arguments> roundTripEntries() {
        return Stream.of(
                Arguments.of("basicFields",
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Test Author")
                                .withField(StandardField.TITLE, "Test Title")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")),
                Arguments.of("multipleAuthors",
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Test Author and Other One")
                                .withField(StandardField.TITLE, "Test Title")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")),
                Arguments.of("doi",
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Test Author")
                                .withField(StandardField.TITLE, "Test Title")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")
                                .withField(StandardField.DOI, "10.1109/EDOC.2018.00030")),
                Arguments.of("isbn",
                        new BibEntry(StandardEntryType.Book)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Test Author")
                                .withField(StandardField.TITLE, "Test Book")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")
                                .withField(StandardField.ISBN, "978-3-16-148410-0")),
                Arguments.of("issn",
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Test Author")
                                .withField(StandardField.TITLE, "Test Article")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")
                                .withField(StandardField.ISSN, "0896-3207")),
                Arguments.of("journalAsParentTitle",
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Test Author")
                                .withField(StandardField.TITLE, "Test Title")
                                .withField(StandardField.JOURNAL, "Test Publisher")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14"))
        );
    }

    @ParameterizedTest(name = "roundTripPreserves[{0}]")
    @MethodSource("roundTripEntries")
    void roundTripPreservesAllFields(String caseName, BibEntry original, @TempDir Path tempDir) throws Exception {
        BibEntry imported = roundTrip(original, tempDir);
        assertEquals(original, imported);
    }

    @Test
    void isRecognizedFormatReturnsTrueForValidHayagrivaYaml(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.URL, "http://example.com")
                .withField(StandardField.DATE, "2020-10-14");
        Path file = tempDir.resolve("valid.yml");
        Files.createFile(file);
        hayagrivaYamlExporter.export(databaseContext, file, List.of(entry));

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            assertTrue(hayagrivaImporter.isRecognizedFormat(reader));
        }
    }

    @Test
    void upstreamBasicYmlFixtureFailsDueToKnownPolymorphismLimitation() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/org/jabref/logic/importer/fileformat/basic.yml");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            assertThrows(MismatchedInputException.class, () -> hayagrivaImporter.importDatabase(reader));
        }
    }
}
