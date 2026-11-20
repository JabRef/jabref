package org.jabref.logic.os;

import java.io.IOException;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Starts external programs that run independently of JabRef ("fire and forget").
///
/// For programs whose output JabRef reads or waits for, use [ProcessBuilder] directly.
@NullMarked
public class ProcessLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessLauncher.class);

    private ProcessLauncher() {
    }

    /// Starts a program that never gets input and whose output is not needed.
    ///
    /// Its output is discarded and its stdin is closed: no pipe stays open until garbage collection,
    /// no program blocks on a full pipe, and no thread reads from a long-running program, which would delay JabRef's shutdown.
    /// The process is neither waited for nor closed, as [Process#close()] waits for its termination.
    public static void startDetached(ProcessBuilder processBuilder) throws IOException {
        // The program's error output is discarded, so this is the only trace of a failing launch.
        // The arguments are not logged: they may carry mail contents or login URLs.
        List<String> command = processBuilder.command();
        LOGGER.debug("Starting {} with {} argument(s)", command.isEmpty() ? "" : command.getFirst(), Math.max(0, command.size() - 1));
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = processBuilder.start();
        process.getOutputStream().close();
    }
}
