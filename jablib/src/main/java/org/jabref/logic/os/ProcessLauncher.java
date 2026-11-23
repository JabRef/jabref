package org.jabref.logic.os;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Starts external programs that run independently of JabRef ("fire and forget").
///
/// For programs whose output JabRef reads or waits for, use [ProcessBuilder] directly.
@NullMarked
public class ProcessLauncher {

    /// The same device [ProcessBuilder.Redirect#DISCARD] writes to; Java offers no such constant for input
    @VisibleForTesting
    static final Path NULL_DEVICE = Path.of(OS.WINDOWS ? "NUL" : "/dev/null");

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessLauncher.class);

    private ProcessLauncher() {
    }

    /// Starts a program that never gets input and whose output JabRef does not need.
    ///
    /// The program reads from and writes to the null device, so no pipe connects it to JabRef:
    /// none stays open until garbage collection, no program blocks on a full pipe,
    /// and no program is terminated by SIGPIPE when it writes after JabRef has exited (editors, readers, terminals and browsers outlive JabRef).
    /// The process is neither waited for nor closed, as [Process#close()] waits for its termination.
    public static void startDetached(ProcessBuilder processBuilder) throws IOException {
        // The program's output is discarded, so this is the only trace of a failing launch.
        // The arguments are not logged: they may carry mail contents or login URLs.
        List<String> command = processBuilder.command();
        LOGGER.debug("Starting {} with {} argument(s)", command.isEmpty() ? "" : command.getFirst(), Math.max(0, command.size() - 1));
        processBuilder.redirectInput(ProcessBuilder.Redirect.from(NULL_DEVICE.toFile()));
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        processBuilder.start();
    }
}
