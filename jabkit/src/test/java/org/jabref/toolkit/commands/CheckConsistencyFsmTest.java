package org.jabref.toolkit.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultCsvWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultTxtWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class CheckConsistencyFsmTest extends AbstractJabKitTest {

    private final PrintStream originalOut = System.out;

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    private static Path writeFile(Path dir, String fileName, String content) throws IOException {
        Path file = dir.resolve(fileName);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    /**
     * FSM: Start -> Importing (fails) -> Exit(2)
     * Input is a directory and not a valid BibTeX file to import.
     */
    @Test
    void importingFails_inputIsDirectory_goesToExit2(@TempDir Path tempDir) throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("not-a-file.bib"));

        int exitCode = commandLine.execute("check-consistency", "--input=" + dir);

        assertEquals(2, exitCode);
    }

    /**
     * FSM: Start -> Importing (ok) -> Validating (invalid) -> Exit(2)
     */
    @Test
    void validatingFails_invalidBibFile_goesToExit2() {
        ParserResult mockedParserResult = mock(ParserResult.class);
        when(mockedParserResult.isInvalid()).thenReturn(true);

        try (MockedStatic<JabKit> jabkit = mockStatic(JabKit.class)) {
            jabkit.when(() -> JabKit.importFile(anyString(), anyString(), any(), anyBoolean()))
                  .thenReturn(Optional.of(mockedParserResult));

            int exitCode = commandLine.execute("check-consistency", "--input=/does/not/matter.bib");

            assertEquals(2, exitCode);
        }
    }

    /**
     * FSM: Start -> Importing -> Validating -> Checking -> Writing(txt) -> Exit(0)
     */
    @Test
    void checkingFindsNoIssues_goesToExit0(@TempDir Path tempDir) throws IOException {
        Path clean = writeFile(tempDir, "clean.bib", """
                @article{ok,
                  author  = {John Doe},
                  title   = {A Valid Paper},
                  journal = {Journal of Testing},
                  year    = {2020}
                }
                """);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

        int exitCode = commandLine.execute("check-consistency", "--input=" + clean);

        assertEquals(0, exitCode);
    }

    /**
     * FSM: Start -> Importing -> Validating -> Checking -> Writing(csv) -> Exit(0)
     * This covers the writer branch "csv" while staying on the same path.
     */
    @Test
    void outputFormatCsv_stillRunsAndReturnsExit0(@TempDir Path tempDir) throws IOException {
        Path clean = writeFile(tempDir, "clean.bib", """
                @article{ok,
                  author  = {John Doe},
                  title   = {A Valid Paper},
                  journal = {Journal of Testing},
                  year    = {2020}
                }
                """);

        int exitCode = commandLine.execute("check-consistency", "--input=" + clean, "--output-format=csv");

        assertEquals(0, exitCode);
    }

    /**
     * FSM: Start -> Importing -> Validating -> Checking (findings) -> Writing -> Exit(1)
     */
    @Test
    void checkingFindsIssues_goesToExit1_usingMockito(@TempDir Path tempDir) throws IOException {
        Path anyValid = writeFile(tempDir, "valid.bib", """
                @article{ok,
                  author  = {John Doe},
                  title   = {A Valid Paper},
                  journal = {Journal of Testing},
                  year    = {2020}
                }
                """);

        // Mock Result to report findings (non-empty map)
        BibliographyConsistencyCheck.Result mockedResult = mock(BibliographyConsistencyCheck.Result.class);

        @SuppressWarnings({"rawtypes"})
        Map forcedNonEmpty = Map.of(mock(Object.class), mock(Object.class));

        when(mockedResult.entryTypeToResultMap()).thenReturn((Map) forcedNonEmpty);

        try (MockedConstruction<BibliographyConsistencyCheck> mockedCheck =
                     mockConstruction(BibliographyConsistencyCheck.class, (mock, context) -> {
                         when(mock.check(any(), any(), any())).thenReturn(mockedResult);
                     });
             MockedConstruction<BibliographyConsistencyCheckResultTxtWriter> mockedTxtWriter =
                     mockConstruction(BibliographyConsistencyCheckResultTxtWriter.class, (mock, context) -> {
                         doNothing().when(mock).writeFindings();
                     });
             MockedConstruction<BibliographyConsistencyCheckResultCsvWriter> mockedCsvWriter =
                     mockConstruction(BibliographyConsistencyCheckResultCsvWriter.class, (mock, context) -> {
                         doNothing().when(mock).writeFindings();
                     })) {

            int exitCode = commandLine.execute("check-consistency", "--input=" + anyValid);

            assertEquals(1, exitCode);
        }
    }
}
