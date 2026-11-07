package org.jabref.toolkit.commands;

import java.nio.file.Path;

import org.jabref.toolkit.exception.CliExceptionHandler;
import org.jabref.toolkit.exception.ImportServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that the `FILE`/`--input` argument shared by all `jabkit` commands (see [InputOption])
/// routes a PostgreSQL connection URL to the shared-database export, and only such a URL.
///
/// The successful round trip is covered by `SharedDatabaseExportTest` (`jablib`) against an
/// embedded PostgreSQL; here only the routing, the exit code, and the error output are of interest,
/// so an unreachable port is enough.
class InputOptionSharedDatabaseTest extends AbstractJabKitTest {

    /// Port 1 is privileged and unbound, so connecting fails immediately instead of running into a timeout.
    private static final String UNREACHABLE_DATABASE = "postgresql://someone:secret@localhost:1/library";

    @BeforeEach
    void useCliExceptionHandler() {
        commandLine.setExecutionExceptionHandler(new CliExceptionHandler(commandLine.getExecutionExceptionHandler()));
    }

    @Test
    void connectionUrlIsRoutedToTheSharedDatabase() {
        int exitCode = commandLine.executeToLog("check", "integrity", UNREACHABLE_DATABASE);

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Problem reading the shared database"), commandLine.getErrorOutput());
    }

    @Test
    void failedConnectionKeepsThePasswordOutOfTheErrorOutput() {
        commandLine.executeToLog("check", "integrity", UNREACHABLE_DATABASE);

        assertFalse(commandLine.getErrorOutput().contains("secret"), commandLine.getErrorOutput());
    }

    @Test
    void uppercasePasswordParameterIsKeptOutOfTheErrorOutput() {
        commandLine.executeToLog("check", "integrity", "postgresql://localhost:1/library?PASSWORD=secret");

        assertFalse(commandLine.getErrorOutput().contains("secret"), commandLine.getErrorOutput());
    }

    /// [org.jabref.logic.shared.DBMSConnectionUrl#parse] is permissive for the GUI's sake: it also accepts
    /// libpq's `host=… dbname=…` keyword form. A relative file name of that shape must still be a file.
    @ParameterizedTest
    @ValueSource(strings = {"host=references.bib", "dbname=library.bib", "postgres-notes.bib"})
    void fileNameResemblingAConnectionUrlStaysALocalPath(String fileName) throws ImportServiceException {
        assertEquals(Path.of(fileName), InputOption.resolveInput(fileName, preferences));
    }
}
