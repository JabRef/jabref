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

    /// Starts a program that never gets input and whose output JabRef does not need.
    ///
    /// Its stdin is closed and its output is discarded, so no pipe stays open until garbage collection and no program blocks on a full pipe.
    /// The output is not read, not even for debugging: editors, readers, terminals and browsers outlive JabRef,
    /// and a program writing to a pipe whose reader has exited gets SIGPIPE and is terminated.
    /// The process is neither waited for nor closed, as [Process#close()] waits for its termination.
    public static void startDetached(ProcessBuilder processBuilder) throws IOException {
        // The program's output is discarded, so this is the only trace of a failing launch.
        // The arguments are not logged: they may carry mail contents or login URLs.
        List<String> command = processBuilder.command();
        LOGGER.debug("Starting {} with {} argument(s)", command.isEmpty() ? "" : command.getFirst(), Math.max(0, command.size() - 1));
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = processBuilder.start();
        process.getOutputStream().close();
    }
}
