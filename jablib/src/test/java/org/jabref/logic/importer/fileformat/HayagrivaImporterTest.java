package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                                .withField(StandardField.AUTHOR, "Author, Test")
                                .withField(StandardField.TITLE, "Test Title")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")),
                Arguments.of("multipleAuthors",
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Author, Test and One, Other")
                                .withField(StandardField.TITLE, "Test Title")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")),
                Arguments.of("doi",
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Author, Test")
                                .withField(StandardField.TITLE, "Test Title")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")
                                .withField(StandardField.DOI, "10.1109/EDOC.2018.00030")),
                Arguments.of("isbn",
                        new BibEntry(StandardEntryType.Book)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Author, Test")
                                .withField(StandardField.TITLE, "Test Book")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")
                                .withField(StandardField.ISBN, "978-3-16-148410-0")),
                Arguments.of("issn",
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Author, Test")
                                .withField(StandardField.TITLE, "Test Article")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")
                                .withField(StandardField.ISSN, "0896-3207")),
                Arguments.of("journalAsParentTitle",
                        new BibEntry(StandardEntryType.Article)
                                .withCitationKey("test")
                                .withField(StandardField.AUTHOR, "Author, Test")
                                .withField(StandardField.TITLE, "Test Title")
                                .withField(StandardField.JOURNAL, "Test Publisher")
                                .withField(StandardField.URL, "http://example.com")
                                .withField(StandardField.DATE, "2020-10-14")),
                Arguments.of("affiliatedRuntimeAndTotals",
                        new BibEntry(StandardEntryType.Misc)
                                .withCitationKey("test")
                                .withField(StandardField.TITLE, "Test Film")
                                .withField(StandardField.DATE, "1991-07-01")
                                .withField(StandardField.VOLUME, "4")
                                .withField(StandardField.CHAPTER, "3")
                                .withField(StandardField.LANGUAGE, "de-DE")
                                .withField(StandardField.PAGETOTAL, "137")
                                .withField(StandardField.VOLUMES, "5")
                                .withField(StandardField.PMID, "12345678")
                                .withField(StandardField.TRANSLATOR, "Translator, Test")
                                .withField(new UnknownField("director"), "Cameron, James")
                                .withField(new UnknownField("cast-member"), "Schwarzenegger, Arnold and Hamilton, Linda")
                                .withField(new UnknownField("runtime"), "137:00")
                                .withField(new UnknownField("time-range"), "17:05-17:48"))
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
                .withField(StandardField.AUTHOR, "Author, Test")
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
    void isRecognizedFormatReturnsTrueForUpstreamBasicYml() throws Exception {
        try (Reader reader = new InputStreamReader(openBasicYml(), StandardCharsets.UTF_8)) {
            assertTrue(hayagrivaImporter.isRecognizedFormat(reader));
        }
    }

    @Test
    void isRecognizedFormatReturnsFalseForOtherYaml() throws Exception {
        String cff = """
                cff-version: 1.2.0
                message: If you use this software, please cite it as below.
                title: Some software
                """;
        assertFalse(hayagrivaImporter.isRecognizedFormat(cff));
        try (Reader reader = Reader.of(cff)) {
            assertFalse(hayagrivaImporter.isRecognizedFormat(reader));
        }
    }

    @Test
    void recognitionResetsReaderSoImportStillWorks() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openBasicYml(), StandardCharsets.UTF_8))) {
            assertTrue(hayagrivaImporter.isRecognizedFormat(reader));
            ParserResult result = hayagrivaImporter.importDatabase(reader);
            assertEquals(38, result.getDatabase().getEntryCount());
        }
    }

    @Test
    void importsAllEntriesOfUpstreamBasicYml() throws Exception {
        ParserResult result = importBasicYml();
        assertFalse(result.isInvalid());
        assertEquals(38, result.getDatabase().getEntryCount());
    }

    @Test
    void importsArticleWithFormattableTitleAndProceedingsParent() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("zygos")
                .withField(StandardField.AUTHOR, "Prekas, George and Kogias, Marios and Bugnion, Edouard")
                .withField(StandardField.TITLE, "ZygOS: Achieving Low Tail Latency for Microsecond-Scale Networked Tasks")
                .withField(StandardField.DATE, "2017")
                .withField(StandardField.PAGES, "325-341")
                .withField(StandardField.DOI, "10.1145/3132747.3132780")
                .withField(StandardField.BOOKTITLE, "Proceedings of the 26th Symposium on Operating Systems Principles");
        assertEquals(Optional.of(expected), importBasicYml().getDatabase().getEntryByCitationKey("zygos"));
    }

    @Test
    void importsArticleWithUntypedPeriodicalParent() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("kinetics")
                .withField(StandardField.AUTHOR, "Doan, T. D. and Tran Thoai, D. B. and Haug, Hartmut")
                .withField(StandardField.TITLE, "Kinetics and luminescence of the excitations of a nonequilibrium polariton condensate")
                .withField(StandardField.DATE, "2020-10-14")
                .withField(StandardField.PAGES, "165126-165139")
                .withField(StandardField.PAGETOTAL, "13")
                .withField(StandardField.DOI, "10.1103/PhysRevB.102.165126")
                .withField(StandardField.JOURNAL, "Physical Review B")
                .withField(StandardField.VOLUME, "102")
                .withField(StandardField.NUMBER, "16")
                .withField(StandardField.PUBLISHER, "American Physical Society");
        assertEquals(Optional.of(expected), importBasicYml().getDatabase().getEntryByCitationKey("kinetics"));
    }

    @Test
    void importsChapterWithBookParent() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.InBook)
                .withCitationKey("harry")
                .withField(StandardField.PAGES, "135-139")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.BOOKTITLE, "Harry Potter and the Order of the Phoenix");
        assertEquals(Optional.of(expected), importBasicYml().getDatabase().getEntryByCitationKey("harry"));
    }

    @Test
    void importsWebEntryWithStructuredAuthor() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Online)
                .withCitationKey("science-e-issue")
                .withField(StandardField.AUTHOR, "Mädje, Laurenz")
                .withField(StandardField.TITLE, "Tokenization of + and - with scientific notation")
                .withField(StandardField.URL, "https://github.com/typst/typstc/issues/3")
                .withField(StandardField.DATE, "2020-07-18")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.BOOKTITLE, "Typst");
        assertEquals(Optional.of(expected), importBasicYml().getDatabase().getEntryByCitationKey("science-e-issue"));
    }

    @Test
    void importsCapitalizedAndUnknownTypes() throws Exception {
        ParserResult result = importBasicYml();
        assertEquals(Optional.of(StandardEntryType.Book), result.getDatabase().getEntryByCitationKey("donne").map(BibEntry::getType));
        assertEquals(Optional.of(StandardEntryType.Misc), result.getDatabase().getEntryByCitationKey("georgia").map(BibEntry::getType));
    }

    @Test
    void importsAffiliatedPersonsAndRuntimeAsCustomFields() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("terminator-2")
                .withField(StandardField.TITLE, "Terminator 2: Judgment Day")
                .withField(StandardField.PUBLISHER, "Carolco Pictures; Pacific Western Productions; Lightstorm Entertainment; Le Studio Canal+ S.A.")
                .withField(StandardField.DATE, "1991-07-01")
                .withField(new UnknownField("director"), "Cameron, James")
                .withField(new UnknownField("cast-member"), "Schwarzenegger, Arnold and Hamilton, Linda and Patrick, Robert")
                .withField(new UnknownField("composer"), "Fiedel, Brad")
                .withField(new UnknownField("runtime"), "137:00")
                .withField(new UnknownField("time-range"), "17:05-17:48");
        assertEquals(Optional.of(expected), importBasicYml().getDatabase().getEntryByCitationKey("terminator-2"));
    }

    @Test
    void importsVolumeTotalAsVolumes() throws Exception {
        assertEquals(Optional.of("5"),
                importBasicYml().getDatabase().getEntryByCitationKey("wire").flatMap(entry -> entry.getField(StandardField.VOLUMES)));
    }

    @Test
    void importDatabaseReturnsErrorResultForMalformedYaml() throws Exception {
        try (BufferedReader reader = new BufferedReader(Reader.of("a: [unclosed"))) {
            ParserResult result = hayagrivaImporter.importDatabase(reader);
            assertTrue(result.isInvalid());
            assertEquals(List.of(), result.getDatabase().getEntries());
        }
    }

    private ParserResult importBasicYml() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openBasicYml(), StandardCharsets.UTF_8))) {
            return hayagrivaImporter.importDatabase(reader);
        }
    }

    private InputStream openBasicYml() {
        // Resolved relative to this class' package: src/test/resources/org/jabref/logic/importer/fileformat/basic.yml
        return getClass().getResourceAsStream("basic.yml");
    }
}
