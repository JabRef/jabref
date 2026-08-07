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
class BibliographyConsistencyCheckResultErrorFormatWriterTest {

    private final BibtexImporter importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

    @Test
    void deviatingFieldsAreReportedOnePerLine(@TempDir Path tempDir) throws IOException {
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

        Path errorFormatFile = tempDir.resolve("result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(errorFormatFile));
             BibliographyConsistencyCheckResultErrorFormatWriter errorFormatWriter =
                     new BibliographyConsistencyCheckResultErrorFormatWriter(result, writer, false,
                             new BibEntryTypesManager(), bibContext.getMode(), parserResult, bibFile)) {
            errorFormatWriter.writeFindings();
        }

        List<String> lines = Files.readString(errorFormatFile).replace("\r\n", "\n").lines().toList();
        assertEquals(3, lines.size(), "Expected one line per deviating field, but got: " + lines);
        // The `pages` field is present in `first` but absent in `second`; `publisher` is an
        // unknown field present in `second` and absent in `first`.
        assertTrue(lines.stream().anyMatch(line -> line.endsWith(":first:publisher: field is absent but used by other entries of entry type Article")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.endsWith(":second:pages: field is absent but used by other entries of entry type Article")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.endsWith(":second:publisher: unknown field for entry type Article")), lines.toString());
        // Every line follows the `file:line:column:citationKey:field: message` shape.
        assertTrue(lines.stream().allMatch(line -> line.contains(".bib:")), lines.toString());
    }
}
