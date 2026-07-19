package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VelocityTemplateExporterTest {

    private static final List<String> HTML_HEAD = List.of(
            "<!DOCTYPE HTML>",
            "<html>",
            "<head>",
            "<title>JabRef references</title>",
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">",
            "<style type=\"text/css\">",
            "body { font-size: 12px; font-family: Arial, sans-serif; }",
            "dt { margin-top: 1em; font-weight: bold; }",
            "@media print {",
            "\tdt { page-break-after: avoid; }",
            "\tdd { page-break-before: avoid; }",
            "}",
            "</style>",
            "</head>",
            "<body>",
            "<dl>");

    private static final List<String> HTML_FOOT = List.of(
            "</dl>",
            "</body>",
            "</html>");

    private static VelocityTemplateExporter exporter;
    private static BibDatabaseContext databaseContext;

    @BeforeAll
    static void setUp() {
        exporter = new VelocityTemplateExporter(
                "Simple HTML (Velocity)",
                "simplehtml-velocity",
                "simplehtml.vm",
                StandardFileType.HTML,
                SaveOrder.getDefaultSaveOrder());
        databaseContext = new BibDatabaseContext();
    }

    private static List<String> expectedOutput(String... entryLines) {
        List<String> result = new ArrayList<>(HTML_HEAD);
        result.addAll(List.of(entryLines));
        result.addAll(HTML_FOOT);
        return result;
    }

    @Test
    void exportForNoEntriesWritesNothing(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        exporter.export(databaseContext, file, List.of());
        assertEquals(List.of(), Files.readAllLines(file));
    }

    @Test
    void exportsCorrectContent(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.JOURNAL, "Test Journal")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.VOLUME, "1")
                .withField(StandardField.NUMBER, "2")
                .withField(StandardField.PAGES, "11--22");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        exporter.export(databaseContext, file, List.of(entry));

        assertEquals(expectedOutput(
                        "<dt>article <a name=\"test\">(test)</a></dt>",
                        "<dd>Test Author</dd>",
                        "<dd><i>Test Title</i></dd>",
                        "<dd>Test Journal, <b>2020</b>, Vol. 1(2), pp. 11--22</dd>"),
                Files.readAllLines(file));
    }

    @Test
    void convertsLatexMarkupToHtml(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("goedel")
                .withField(StandardField.AUTHOR, "Kurt G{\\\"o}del")
                .withField(StandardField.TITLE, "Some {\\emph{Deep}} Thoughts")
                .withField(StandardField.PUBLISHER, "Test Publisher")
                .withField(StandardField.YEAR, "1931");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        exporter.export(databaseContext, file, List.of(entry));

        assertEquals(expectedOutput(
                        "<dt>book <a name=\"goedel\">(goedel)</a></dt>",
                        "<dd>Kurt G&ouml;del</dd>",
                        "<dd><i>Some <em>Deep</em> Thoughts</i></dd>",
                        "<dd>Test Publisher, <b>1931</b></dd>"),
                Files.readAllLines(file));
    }

    @Test
    void exportsMultipleEntries(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article)
                .withCitationKey("first")
                .withField(StandardField.TITLE, "First Title")
                .withField(StandardField.YEAR, "2020");
        BibEntry second = new BibEntry(StandardEntryType.Article)
                .withCitationKey("second")
                .withField(StandardField.TITLE, "Second Title")
                .withField(StandardField.YEAR, "2021");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        exporter.export(databaseContext, file, List.of(first, second));

        assertEquals(expectedOutput(
                        "<dt>article <a name=\"first\">(first)</a></dt>",
                        "<dd><i>First Title</i></dd>",
                        "<dd><b>2020</b></dd>",
                        "<dt>article <a name=\"second\">(second)</a></dt>",
                        "<dd><i>Second Title</i></dd>",
                        "<dd><b>2021</b></dd>"),
                Files.readAllLines(file));
    }
}
