package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ResourceLock("Localization.lang")
class BibliographyConsistencyCheckResultGitHubActionsWriterTest {

    private final BibtexImporter importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

    @Test
    void deviatingFieldsAreReportedAsGitHubActionsAnnotations(@TempDir Path tempDir) throws IOException {
        Path bibFile = tempDir.resolve("library.bib");
        Files.writeString(bibFile, """
                @Article{first,
                  author = {Author One},
                  pages = {1--2},
                }
                @Article{second,
                  author = {Author One},
                  publisher = {A Publisher},
                }
                """);

        ParserResult parserResult = importer.importDatabase(bibFile);
        BibDatabaseContext bibContext = parserResult.getDatabaseContext();
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(bibContext, new BibEntryTypesManager(), (_, _) -> {
                });

        Path outFile = tempDir.resolve("result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(outFile));
             BibliographyConsistencyCheckResultGitHubActionsWriter ghWriter =
                     new BibliographyConsistencyCheckResultGitHubActionsWriter(result, writer, false,
                             new BibEntryTypesManager(), bibContext.getMode(), parserResult, bibFile)) {
            ghWriter.writeFindings();
        }

        List<String> lines = Files.readString(outFile).replace("\r\n", "\n").lines().toList();
        assertEquals(3, lines.size(), "Expected one annotation per deviating field, but got: " + lines);
        // Every line starts with the GitHub Actions error workflow command.
        assertTrue(lines.stream().allMatch(line -> line.startsWith("::error file=")), lines.toString());
        // Each annotation carries line, column, title (citation key + field), and the message.
        assertTrue(lines.stream().allMatch(line -> line.contains(",line=") && line.contains(",col=") && line.contains(",title=")), lines.toString());
        // The message embeds the citation key and field name so the plain log line is self-describing.
        assertTrue(lines.stream().anyMatch(line -> line.endsWith("::first: field 'publisher' is absent but used by other entries of entry type Article")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.endsWith("::second: field 'pages' is absent but used by other entries of entry type Article")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.endsWith("::second: unknown field 'publisher' for entry type Article")), lines.toString());
        // Windows drive-letter colon and the ":" between citation key and field name must be URL-encoded inside properties.
        assertTrue(lines.stream().noneMatch(line -> line.matches(".*title=[^:]+:[^:]+::.*")), "title must not contain raw ':' separator: " + lines);
    }
}
