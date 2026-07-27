package org.jabref.toolkit.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import picocli.CommandLine;

/// A CommandLine subclass to decorate the original CommandLine that captures test relevant information.
public class CapturingCommandLine extends CommandLine {

    private final ByteArrayOutputStream outWriter = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errWriter = new ByteArrayOutputStream();

    public CapturingCommandLine(Object command) {
        super(command);
    }

    public CapturingCommandLine(Object command, IFactory factory) {
        super(command, factory);
    }

    /// Executes the configured {@link picocli.CommandLine} command while capturing its
    /// standard output and error streams.
    ///
    /// This method temporarily redirects `System.out` and `System.err` to
    /// internal buffers during the command execution, allowing the captured output to be
    /// retrieved later using {@link #getStandardOutput()} and {@link #getErrorOutput()}.
    ///
    /// NOTE: Running execute in parallel might lead to instructive and subtle false negatives.
    ///
    /// @param args the command line arguments to parse
    /// @return the error code
    public int executeToLog(String... args) {
        PrintStream or = System.out;
        PrintStream orErr = System.err;

        System.setOut(new PrintStream(outWriter, true));
        System.setErr(new PrintStream(errWriter, true));

        int result = execute(args);

        System.setOut(or);
        System.setErr(orErr);

        return result;
    }

    /// Returns the captured standard output from the command line execution.
    ///
    /// @return The captured stdout string.
    public String getStandardOutput() {
        return outWriter.toString().replace("\r\n", "\n");
    }

    /// Returns the captured error output from the command line execution.
    ///
    /// @return The captured stderr string.
    public String getErrorOutput() {
        return errWriter.toString().replace("\r\n", "\n");
    }
}
