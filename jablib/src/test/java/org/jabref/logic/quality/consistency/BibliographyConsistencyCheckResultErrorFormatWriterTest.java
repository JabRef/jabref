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
                  title = {Title},
                  journal = {Journal},
                  year = {2024},
                  pages = {1--2},
                }
                @Article{second,
                  author = {Author One},
                  title = {Title},
                  journal = {Journal},
                  year = {2024},
                  publisher = {A Publisher},
                }
                """);

        List<String> lines = writeFindings(bibFile);
        assertEquals(3, lines.size(), "Expected one line per deviating field, but got: " + lines);
        // The `pages` field is present in `first` but absent in `second`; `publisher` is an
        // unknown field present in `second` and absent in `first`.
        assertTrue(lines.stream().anyMatch(line -> line.endsWith(":first:publisher: field is absent but used by other entries of entry type Article")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.endsWith(":second:pages: field is absent but used by other entries of entry type Article")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.endsWith(":second:publisher: unknown field for entry type Article")), lines.toString());
    }

    @Test
    void missingRequiredFieldsAreReported(@TempDir Path tempDir) throws IOException {
        Path bibFile = tempDir.resolve("library.bib");
        Files.writeString(bibFile, """
                @Comment{jabref-meta: databaseType:bibtex;}

                @Article{first,
                  author = {Author One},
                }

                @Article{second,
                  author = {Author Two},
                }
                """);

        assertEquals(List.of(
                "3:0:first:journal: required field 'journal' is missing",
                "3:0:first:title: required field 'title' is missing",
                "3:0:first:year: required field 'year' is missing",
                "7:0:second:journal: required field 'journal' is missing",
                "7:0:second:title: required field 'title' is missing",
                "7:0:second:year: required field 'year' is missing"
        ), writeFindings(bibFile));
    }

    @Test
    void onlyFieldsDifferingWithinTheEntryTypeAreReported(@TempDir Path tempDir) throws IOException {
        Path bibFile = tempDir.resolve("library.bib");
        Files.writeString(bibFile, """
                @Comment{jabref-meta: databaseType:bibtex;}

                @Article{articleWithNumber,
                  author = {Author One},
                  title = {Title},
                  journal = {Journal},
                  year = {2024},
                  number = {1},
                }

                @Article{articleWithPages,
                  author = {Author One},
                  title = {Title},
                  journal = {Journal},
                  year = {2024},
                  pages = {1--2},
                }

                @Misc{miscWithHowPublished,
                  title = {Title},
                  howpublished = {Online},
                }

                @Misc{miscWithNote,
                  title = {Title},
                  note = {A note},
                }
                """);

        // Fields differing within Misc must not be reported for the Article entries and vice versa
        assertEquals(List.of(
                "3:0:articleWithNumber:pages: field is absent but used by other entries of entry type Article",
                "11:0:articleWithPages:number: field is absent but used by other entries of entry type Article",
                "19:0:miscWithHowPublished:note: field is absent but used by other entries of entry type Misc",
                "24:0:miscWithNote:howpublished: field is absent but used by other entries of entry type Misc"
        ), writeFindings(bibFile));
    }

    /// Returns the written lines without the leading file name
    private List<String> writeFindings(Path bibFile) throws IOException {
        ParserResult parserResult = importer.importDatabase(bibFile);
        BibDatabaseContext bibContext = parserResult.getDatabaseContext();
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(bibContext, new BibEntryTypesManager(), (_, _) -> {
                });

        Path errorFormatFile = bibFile.resolveSibling("result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(errorFormatFile));
             BibliographyConsistencyCheckResultErrorFormatWriter errorFormatWriter =
                     new BibliographyConsistencyCheckResultErrorFormatWriter(result, writer, false,
                             new BibEntryTypesManager(), bibContext.getMode(), parserResult, bibFile)) {
            errorFormatWriter.writeFindings();
        }

        String filePrefix = bibFile + ":";
        List<String> lines = Files.readString(errorFormatFile).replace("\r\n", "\n").lines().toList();
        // Every line follows the `file:line:column:citationKey:field: message` shape.
        assertTrue(lines.stream().allMatch(line -> line.startsWith(filePrefix)), lines.toString());
        return lines.stream().map(line -> line.substring(filePrefix.length())).toList();
    }
}
