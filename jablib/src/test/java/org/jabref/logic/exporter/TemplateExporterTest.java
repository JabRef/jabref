package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("exporter")
class TemplateExporterTest {

    private static final byte[] ORIGINAL_CONTENT = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'o', 'l', 'd', '\r', '\n'};

    @TempDir
    Path directory;

    private BibDatabaseContext databaseContext;
    private List<BibEntry> entries;
    private TemplateExporter exporter;
    private Path layoutDirectory;
    private Path output;
    private Path outputDirectory;

    @BeforeEach
    void setUp() throws IOException {
        layoutDirectory = Files.createDirectory(directory.resolve("layouts"));
        outputDirectory = Files.createDirectory(directory.resolve("output"));
        output = outputDirectory.resolve("references.txt");
        entries = List.of(new BibEntry().withField(StandardField.AUTHOR, "Doe, Jane").withField(StandardField.TITLE, "Exported title"));
        databaseContext = new BibDatabaseContext(new BibDatabase(entries));
        exporter = new TemplateExporter("Test", layoutDirectory.resolve("custom.layout").toString(), "txt",
                LayoutFormatterPreferences.getDefault(), SaveOrder.getDefaultSaveOrder());
        exporter.setCustomExport(true);
    }

    // [utest->req~logic.exporter.preserve-output-on-template-failure~1]
    @ParameterizedTest
    @CsvSource(textBlock = """
            true,  false
            true,  true
            false, false
            false, true
            """)
    void missingLayoutDoesNotCommitOutput(boolean existingOutput, boolean writeHeader) throws IOException {
        if (existingOutput) {
            Files.write(output, ORIGINAL_CONTENT);
        }
        if (writeHeader) {
            Files.writeString(layoutDirectory.resolve("custom.begin.layout"), "Partial header\n".repeat(2048));
        }

        IOException exception = assertThrows(IOException.class, () -> exporter.export(databaseContext, output, entries));

        assertEquals("Cannot find layout file: '" + layoutDirectory.resolve("custom.layout") + "'.", exception.getMessage());
        if (existingOutput) {
            assertArrayEquals(ORIGINAL_CONTENT, Files.readAllBytes(output));
        }
        try (var files = Files.list(outputDirectory)) {
            assertEquals(existingOutput ? List.of(output) : List.of(), files.toList());
        }
    }

    // [utest->req~logic.exporter.preserve-output-on-template-failure~1]
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void invalidNameFormatterDoesNotCommitOutput(boolean existingOutput) throws IOException {
        if (existingOutput) {
            Files.write(output, ORIGINAL_CONTENT);
        }
        Files.writeString(layoutDirectory.resolve("custom.begin.layout"), "Partial header\n".repeat(2048));
        Files.writeString(layoutDirectory.resolve("custom.formatters"), "InvalidName: not-a-number@*@{ll}");
        Files.writeString(layoutDirectory.resolve("custom.layout"), "\\format[InvalidName]{\\author}");

        assertThrows(NumberFormatException.class, () -> exporter.export(databaseContext, output, entries));

        if (existingOutput) {
            assertArrayEquals(ORIGINAL_CONTENT, Files.readAllBytes(output));
        }
        try (var files = Files.list(outputDirectory)) {
            assertEquals(existingOutput ? List.of(output) : List.of(), files.toList());
        }
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            true,  false
            true,  true
            false, false
            false, true
            """)
    void successfulExportCommitsCompleteOutput(boolean existingOutput, boolean optionalLayouts) throws IOException {
        if (existingOutput) {
            Files.write(output, ORIGINAL_CONTENT);
        }
        Files.writeString(layoutDirectory.resolve("custom.layout"), "\\title\n");
        if (optionalLayouts) {
            Files.writeString(layoutDirectory.resolve("custom.begin.layout"), "Header\n");
            Files.writeString(layoutDirectory.resolve("custom.end.layout"), "Footer\n");
        }

        exporter.export(databaseContext, output, entries);

        assertEquals(optionalLayouts ? "Header\nExported title\nFooter\n" : "Exported title\n", Files.readString(output));
        try (var files = Files.list(outputDirectory)) {
            assertEquals(List.of(output), files.toList());
        }
    }

    @Test
    void emptySelectionPreservesExistingOutput() throws IOException {
        Files.write(output, ORIGINAL_CONTENT);

        exporter.export(databaseContext, output, List.of());

        assertArrayEquals(ORIGINAL_CONTENT, Files.readAllBytes(output));
    }
}
