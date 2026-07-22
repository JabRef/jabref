package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.util.CapturingCommandLine;
import org.jabref.toolkit.util.CommandFactory;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PdfExtractReferencesTest extends AbstractJabKitTest {

    @TempDir
    Path outputDir;

    private ExportService mockExportService;
    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start(0);
    }

    @AfterEach
    void stopServer() throws IOException {
        server.close();
    }

    @BeforeEach
    void setupMocks() {
        mockExportService = mock();

        PdfExtractReferences sut = new PdfExtractReferences() {
            @Override
            void initFields() {
                this.exportService = mockExportService;
            }
        };

        JabKit jabKit = new JabKit(preferences, entryTypesManager);
        CommandFactory factory = new CommandFactory(sut);
        this.commandLine = new CapturingCommandLine(jabKit, factory);
    }

    private String pdfPath(String name) {
        return getClassResourceAsFullyQualifiedString("/pdfs/" + name);
    }

    @Test
    void singleFileWithoutOutputPrintsToStdOut() throws Exception {
        ArgumentCaptor<BibDatabaseContext> captor = ArgumentCaptor.captor();

        int exitCode = commandLine.executeToLog("pdf", "extract-references", pdfPath("ieee-paper.pdf"));

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        verify(mockExportService).printDatabaseContextToStdOut(captor.capture());
        assertEquals(5, captor.getValue().getEntries().size());
    }

    @Test
    void multipleFilesWithOutputDirWriteOneBibPerFile() throws Exception {
        ArgumentCaptor<ParserResult> results = ArgumentCaptor.captor();
        ArgumentCaptor<Path> files = ArgumentCaptor.captor();

        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output-dir", outputDir.toString(),
                pdfPath("ieee-paper.pdf"), pdfPath("ieee-paper-2.pdf"));

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        verify(mockExportService, times(2))
                .exportParserResultToFile(results.capture(), files.capture(), eq("bibtex"));
        assertEquals(List.of(outputDir.resolve("ieee-paper.bib"), outputDir.resolve("ieee-paper-2.bib")), files.getAllValues());
    }

    @Test
    void sameFileNameInDifferentDirectoriesGetsItsOwnOutputFile(@TempDir Path inputRoot) throws Exception {
        Path first = Files.createDirectory(inputRoot.resolve("a")).resolve("paper.pdf");
        Path second = Files.createDirectory(inputRoot.resolve("b")).resolve("paper.pdf");
        Files.copy(Path.of(pdfPath("ieee-paper.pdf")), first);
        Files.copy(Path.of(pdfPath("ieee-paper.pdf")), second);
        ArgumentCaptor<Path> files = ArgumentCaptor.captor();

        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output-dir", outputDir.toString(),
                first.toString(), second.toString());

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        verify(mockExportService, times(2))
                .exportParserResultToFile(any(), files.capture(), eq("bibtex"));
        assertEquals(List.of(outputDir.resolve("paper.bib"), outputDir.resolve("paper-2.bib")), files.getAllValues());
    }

    @Test
    void missingOutputDirIsCreated() throws Exception {
        Path nestedOutputDir = outputDir.resolve("nested/sub");

        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output-dir", nestedOutputDir.toString(),
                pdfPath("ieee-paper.pdf"));

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        assertTrue(Files.isDirectory(nestedOutputDir));
        verify(mockExportService).exportParserResultToFile(
                any(), eq(nestedOutputDir.resolve("ieee-paper.bib")), eq("bibtex"));
    }

    @Test
    void uncreatableOutputDirExitsSoftwareError() throws Exception {
        // A regular file cannot double as the output directory.
        Path blockedByFile = outputDir.resolve("occupied");
        Files.createFile(blockedByFile);

        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output-dir", blockedByFile.toString(),
                pdfPath("ieee-paper.pdf"));

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Could not create output directory"));
        verifyNoInteractions(mockExportService);
    }

    @Test
    void nonexistentInputFileExitsSoftwareError() throws Exception {
        int exitCode = commandLine.executeToLog("pdf", "extract-references", "does-not-exist.pdf");

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
    }

    @Test
    void urlInputIsDownloadedAndProcessed() throws Exception {
        ArgumentCaptor<BibDatabaseContext> captor = ArgumentCaptor.captor();
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body(new Buffer().write(Files.readAllBytes(Path.of(pdfPath("ieee-paper.pdf")))))
                .build());

        int exitCode = commandLine.executeToLog("pdf", "extract-references", server.url("/ieee-paper.pdf").toString());

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        verify(mockExportService).printDatabaseContextToStdOut(captor.capture());
        assertEquals(5, captor.getValue().getEntries().size());
    }

    @Test
    void unreachableUrlIsSkippedWhileRemainingFilesAreStillProcessed() throws Exception {
        server.enqueue(new MockResponse.Builder().code(404).build());

        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output-dir", outputDir.toString(),
                server.url("/missing.pdf").toString(), pdfPath("ieee-paper.pdf"));

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
        // The download must have been attempted - a URL treated as a plain path never reaches the server.
        assertEquals(1, server.getRequestCount());
        verify(mockExportService).exportParserResultToFile(
                any(), eq(outputDir.resolve("ieee-paper.bib")), eq("bibtex"));
    }

    @Test
    void grobidUrlWithoutGrobidModeExitsUsageError() throws Exception {
        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--mode", "RULE_BASED",
                "--grobid-url", "http://localhost:1234",
                pdfPath("ieee-paper.pdf"));

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
    }
}
