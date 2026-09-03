package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class PdfExtractReferencesTest extends AbstractJabKitTest {

    @TempDir
    Path outputDir;

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
    void setupSaveConfiguration() {
        // The database writers need a real save configuration; the deep-stub mock's default answer is not usable.
        SelfContainedSaveOrder saveOrder = new SelfContainedSaveOrder(SaveOrder.OrderType.ORIGINAL, List.of());
        when(preferences.getSelfContainedExportConfiguration())
                .thenReturn(new SelfContainedSaveConfiguration(saveOrder, false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false));
    }

    private String pdfPath(String name) {
        return getClassResourceAsFullyQualifiedString("/pdfs/" + name);
    }

    private long entriesOnStdOut() {
        return commandLine.getStandardOutput().lines().filter(line -> line.startsWith("@")).count();
    }

    @Test
    void singleFileWithoutOutputPrintsToStdOut() throws Exception {
        int exitCode = commandLine.executeToLog("pdf", "extract-references", pdfPath("ieee-paper.pdf"));

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        assertEquals(5, entriesOnStdOut());
    }

    @Test
    void multipleFilesWithOutputDirWriteOneBibPerFile() throws Exception {
        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output-dir", outputDir.toString(),
                pdfPath("ieee-paper.pdf"), pdfPath("ieee-paper-2.pdf"));

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        assertFileExists(outputDir.resolve("ieee-paper.bib"));
        assertFileExists(outputDir.resolve("ieee-paper-2.bib"));
    }

    @Test
    void sameFileNameInDifferentDirectoriesGetsItsOwnOutputFile(@TempDir Path inputRoot) throws Exception {
        Path first = Files.createDirectory(inputRoot.resolve("a")).resolve("paper.pdf");
        Path second = Files.createDirectory(inputRoot.resolve("b")).resolve("paper.pdf");
        Files.copy(Path.of(pdfPath("ieee-paper.pdf")), first);
        Files.copy(Path.of(pdfPath("ieee-paper.pdf")), second);

        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output-dir", outputDir.toString(),
                first.toString(), second.toString());

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        assertFileExists(outputDir.resolve("paper.bib"));
        assertFileExists(outputDir.resolve("paper-2.bib"));
    }

    @Test
    void missingOutputDirIsCreated() throws Exception {
        Path nestedOutputDir = outputDir.resolve("nested/sub");

        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output-dir", nestedOutputDir.toString(),
                pdfPath("ieee-paper.pdf"));

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        assertFileExists(nestedOutputDir.resolve("ieee-paper.bib"));
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
    }

    @Test
    void outputAndOutputDirTogetherExitUsageError() throws Exception {
        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output", outputDir.resolve("out.bib").toString(),
                "--output-dir", outputDir.toString(),
                pdfPath("ieee-paper.pdf"));

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertFileDoesntExist(outputDir.resolve("out.bib"));
    }

    @Test
    void nonexistentInputFileExitsSoftwareError() throws Exception {
        int exitCode = commandLine.executeToLog("pdf", "extract-references", "does-not-exist.pdf");

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
    }

    @Test
    void urlInputIsDownloadedAndProcessed() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body(new Buffer().write(Files.readAllBytes(Path.of(pdfPath("ieee-paper.pdf")))))
                .build());

        int exitCode = commandLine.executeToLog("pdf", "extract-references", server.url("/ieee-paper.pdf").toString());

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        assertEquals(5, entriesOnStdOut());
    }

    @Test
    void urlInputOutputIsNamedAfterTheUrl() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body(new Buffer().write(Files.readAllBytes(Path.of(pdfPath("ieee-paper.pdf")))))
                .build());

        int exitCode = commandLine.executeToLog(
                "pdf", "extract-references",
                "--output-dir", outputDir.toString(),
                server.url("/papers/mypaper.pdf").toString() + "?version=2");

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        // Named after the URL, not after the randomly named temporary file the download went to.
        assertFileExists(outputDir.resolve("mypaper.bib"));
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
        assertFileExists(outputDir.resolve("ieee-paper.bib"));
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
