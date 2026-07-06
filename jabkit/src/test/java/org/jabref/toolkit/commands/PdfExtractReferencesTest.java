package org.jabref.toolkit.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.util.CapturingCommandLine;
import org.jabref.toolkit.util.CommandFactory;

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

class PdfExtractReferencesTest extends AbstractJabKitTest {

    @TempDir
    Path outputDir;

    private ExportService mockExportService;

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
    void nonexistentInputFileExitsSoftwareError() throws Exception {
        int exitCode = commandLine.executeToLog("pdf", "extract-references", "does-not-exist.pdf");

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
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
