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
import org.jabref.logic.util.GitHubActionsEscape;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        // The message embeds the citation key and field name so the plain log line is self-describing.
        // The ":" in the file name (Windows drive letter) and between citation key and field name in the title are URL-encoded.
        String file = GitHubActionsEscape.property(bibFile.toString());
        assertEquals(List.of(
                "::error file=" + file + ",line=1,col=0,title=first%3Apublisher::first: field 'publisher' is absent but used by other entries of entry type Article",
                "::error file=" + file + ",line=8,col=0,title=second%3Apages::second: field 'pages' is absent but used by other entries of entry type Article",
                "::error file=" + file + ",line=13,col=3,title=second%3Apublisher::second: unknown field 'publisher' for entry type Article"
        ), writeFindings(bibFile));
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

        String file = GitHubActionsEscape.property(bibFile.toString());
        assertEquals(List.of(
                "::error file=" + file + ",line=3,col=0,title=first%3Ajournal::first: required field 'journal' is missing",
                "::error file=" + file + ",line=3,col=0,title=first%3Atitle::first: required field 'title' is missing",
                "::error file=" + file + ",line=3,col=0,title=first%3Ayear::first: required field 'year' is missing",
                "::error file=" + file + ",line=7,col=0,title=second%3Ajournal::second: required field 'journal' is missing",
                "::error file=" + file + ",line=7,col=0,title=second%3Atitle::second: required field 'title' is missing",
                "::error file=" + file + ",line=7,col=0,title=second%3Ayear::second: required field 'year' is missing"
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
        String file = GitHubActionsEscape.property(bibFile.toString());
        assertEquals(List.of(
                "::error file=" + file + ",line=3,col=0,title=articleWithNumber%3Apages::articleWithNumber: field 'pages' is absent but used by other entries of entry type Article",
                "::error file=" + file + ",line=11,col=0,title=articleWithPages%3Anumber::articleWithPages: field 'number' is absent but used by other entries of entry type Article",
                "::error file=" + file + ",line=19,col=0,title=miscWithHowPublished%3Anote::miscWithHowPublished: field 'note' is absent but used by other entries of entry type Misc",
                "::error file=" + file + ",line=24,col=0,title=miscWithNote%3Ahowpublished::miscWithNote: field 'howpublished' is absent but used by other entries of entry type Misc"
        ), writeFindings(bibFile));
    }

    private List<String> writeFindings(Path bibFile) throws IOException {
        ParserResult parserResult = importer.importDatabase(bibFile);
        BibDatabaseContext bibContext = parserResult.getDatabaseContext();
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(bibContext, new BibEntryTypesManager(), (_, _) -> {
                });

        Path outFile = bibFile.resolveSibling("result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(outFile));
             BibliographyConsistencyCheckResultGitHubActionsWriter ghWriter =
                     new BibliographyConsistencyCheckResultGitHubActionsWriter(result, writer, false,
                             new BibEntryTypesManager(), bibContext.getMode(), parserResult, bibFile)) {
            ghWriter.writeFindings();
        }

        return Files.readString(outFile).replace("\r\n", "\n").lines().toList();
    }
}
