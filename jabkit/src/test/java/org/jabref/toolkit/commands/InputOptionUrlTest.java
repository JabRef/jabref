package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.toolkit.exception.CliExceptionHandler;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that the `FILE`/`--input` argument shared by all `jabkit` commands (see [InputOption])
/// also accepts an http(s)/ftp URL, downloading it to a local temporary file first.
///
/// [org.jabref.toolkit.commands.Convert] is used as a representative command, since the resolution
/// logic itself lives in the shared [InputOption] mixin used by every input-taking command.
class InputOptionUrlTest extends AbstractJabKitTest {

    private static final String BIBTEX_CONTENT = """
            @Article{Darwin1888,
              author = {Darwin, Charles},
              title  = {Origin of Species},
              year   = {1888},
            }""";

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start(0);
        commandLine.setExecutionExceptionHandler(new CliExceptionHandler(commandLine.getExecutionExceptionHandler()));
    }

    @AfterEach
    void stopServer() throws IOException {
        server.close();
    }

    @Test
    void inputOptionDownloadsBibtexFromUrl(@TempDir Path tempDir) throws IOException {
        server.enqueue(new MockResponse.Builder().code(200).body(BIBTEX_CONTENT).build());
        HttpUrl url = server.url("/references.bib");
        Path outputPath = tempDir.resolve("output");

        int exitCode = commandLine.executeToLog("convert",
                "--input=" + url,
                "--input-format=bibtex",
                "--output-format=bibtex",
                "--output=" + outputPath);

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        assertTrue(Files.readString(outputPath).contains("Darwin1888"));
    }

    @Test
    void positionalFileArgumentAlsoDownloadsFromUrl(@TempDir Path tempDir) throws IOException {
        server.enqueue(new MockResponse.Builder().code(200).body(BIBTEX_CONTENT).build());
        HttpUrl url = server.url("/references.bib");
        Path outputPath = tempDir.resolve("output");

        int exitCode = commandLine.executeToLog("convert",
                url.toString(),
                "--input-format=bibtex",
                "--output-format=bibtex",
                "--output=" + outputPath);

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        assertTrue(Files.readString(outputPath).contains("Darwin1888"));
    }

    @Test
    void failedDownloadExitsWithSoftwareError() {
        server.enqueue(new MockResponse.Builder().code(404).build());
        HttpUrl url = server.url("/missing.bib");

        int exitCode = commandLine.executeToLog("convert",
                "--input=" + url,
                "--input-format=bibtex",
                "--output-format=bibtex");

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Problem downloading"));
    }
}
