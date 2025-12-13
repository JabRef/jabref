package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("exporter")
class MarkdownTitleExporterTest {

    private static Exporter htmlWebsiteExporter;
    private static BibDatabaseContext databaseContext;
    private static final SelfContainedSaveOrder SAVE_MOST_RECENT_FIRST_SAVE_ORDER = new SelfContainedSaveOrder(SaveOrder.OrderType.SPECIFIED, List.of(new SaveOrder.SortCriterion(StandardField.YEAR, true)));

    @BeforeAll
    static void setUp() {
        htmlWebsiteExporter = new TemplateExporter(
                "Title-Markdown",
                "title-md",
                "title-md",
                "title-markdown",
                StandardFileType.MARKDOWN,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                SAVE_MOST_RECENT_FIRST_SAVE_ORDER,
                BlankLineBehaviour.DELETE_BLANKS);

        databaseContext = new BibDatabaseContext();
    }

    @Test
    final void exportForNoEntriesWritesNothing(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path file = tempDir.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        htmlWebsiteExporter.export(databaseContext, tempDir, List.of());
        assertEquals(List.of(), Files.readAllLines(file));
    }

    @Test
    final void exportsCorrectContentArticle(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.JOURNAL, "Journal of this \\& that")
                .withField(StandardField.PUBLISHER, "THE PRESS")
                .withField(StandardField.YEAR, "2020");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        htmlWebsiteExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "* Test Title. Journal of this &amp; that 2020");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void exportsCorrectContentInCollection(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.InCollection)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.BOOKTITLE, "Test book")
                .withField(StandardField.PUBLISHER, "PRESS")
                .withField(StandardField.YEAR, "2020");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        htmlWebsiteExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "* Test Title. Test book, PRESS 2020");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void exportsCorrectContentBook(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.BOOKTITLE, "Test book")
                .withField(StandardField.PUBLISHER, "PRESS")
                .withField(StandardField.YEAR, "2020");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        htmlWebsiteExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "* Test Title. PRESS 2020");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void exportsCorrectContentInProceeedingsPublisher(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.BOOKTITLE, "Test Conference")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.SERIES, "CONF'20")
                .withField(StandardField.YEAR, "2020");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        htmlWebsiteExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "* Test Title. ACM CONF'20");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void exportsCorrectContentInProceeedingsNoPublisher(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.BOOKTITLE, "Test Conference")
                .withField(StandardField.SERIES, "CONF'20")
                .withField(StandardField.YEAR, "2020");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        htmlWebsiteExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "* Test Title. CONF'20");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void exportsCorrectContentInProceeedingsNoSeries(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.BOOKTITLE, "Test Conference")
                .withField(StandardField.YEAR, "2020");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        htmlWebsiteExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "* Test Title. Test Conference 2020");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    final void exportsCorrectContentBracketsInTitle(@TempDir Path tempDir) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "This is {JabRef}")
                .withField(StandardField.JOURNAL, "Journal of this \\& that")
                .withField(StandardField.YEAR, "2020");

        Path file = tempDir.resolve("RandomFileName");
        Files.createFile(file);
        htmlWebsiteExporter.export(databaseContext, file, List.of(entry));

        List<String> expected = List.of(
                "* This is JabRef. Journal of this &amp; that 2020");

        assertEquals(expected, Files.readAllLines(file));
    }
}
