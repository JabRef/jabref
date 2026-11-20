package org.jabref.logic.os;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.jabref.logic.util.StreamGobbler;

import com.google.common.annotations.VisibleForTesting;
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
    /// Its stdin is closed. Its output goes to the debug log when debug logging is enabled, as many programs report problems only there;
    /// otherwise it is discarded. Either way, no pipe stays open until garbage collection and no program blocks on a full pipe.
    /// The process is neither waited for nor closed, as [Process#close()] waits for its termination.
    public static void startDetached(ProcessBuilder processBuilder) throws IOException {
        List<String> command = processBuilder.command();
        String program = command.isEmpty() ? "" : command.getFirst();
        // The arguments are not logged: they may carry mail contents or login URLs.
        LOGGER.debug("Starting {} with {} argument(s)", program, Math.max(0, command.size() - 1));
        if (LOGGER.isDebugEnabled()) {
            startForwardingOutput(processBuilder, line -> LOGGER.debug("{}: {}", program, line));
        } else {
            startDiscardingOutput(processBuilder);
        }
    }

    @VisibleForTesting
    static void startDiscardingOutput(ProcessBuilder processBuilder) throws IOException {
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        startWithoutInput(processBuilder);
    }

    /// @param outputConsumer receives stdout and stderr line by line
    @VisibleForTesting
    static void startForwardingOutput(ProcessBuilder processBuilder, Consumer<String> outputConsumer) throws IOException {
        processBuilder.redirectErrorStream(true);
        Process process = startWithoutInput(processBuilder);
        // A virtual thread does not keep the JVM alive, so a long-running program does not delay JabRef's shutdown,
        // unlike a reader on HeadlessExecutorService, which is awaited on shutdown.
        Thread.ofVirtual()
              .name("program output")
              .start(new StreamGobbler(process.getInputStream(), outputConsumer));
    }

    private static Process startWithoutInput(ProcessBuilder processBuilder) throws IOException {
        Process process = processBuilder.start();
        process.getOutputStream().close();
        return process;
    }
}
