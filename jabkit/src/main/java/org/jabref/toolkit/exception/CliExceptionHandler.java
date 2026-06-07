package org.jabref.toolkit.exception;

import org.jabref.logic.JabRefException;

import org.jspecify.annotations.NullMarked;
import picocli.CommandLine;

@NullMarked
public class CliExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    private final CommandLine.IExecutionExceptionHandler delegate;

    public CliExceptionHandler(CommandLine.IExecutionExceptionHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult parseResult) throws Exception {
        switch (ex) {
            case CliException cliException -> {
                printErrorMessage(cliException, true);
                return cliException.getExitCode();
            }
            case JabRefException jabRefException -> {
                printErrorMessage(jabRefException, true);
                return CommandLine.ExitCode.SOFTWARE;
            }
            default -> {
                return delegate.handleExecutionException(ex, commandLine, parseResult);
            }
        }
    }

    private static void printErrorMessage(Exception ex, boolean withCauseIfPresent) {
        String errorMessage = (withCauseIfPresent && ex.getCause() != null)
                              ? ex.getLocalizedMessage() + buildCauseSuffix(ex.getCause())
                              : ex.getLocalizedMessage();
        System.err.println(errorMessage);
    }

    private static String buildCauseSuffix(Throwable cause) {
        return " (" + cause.getClass().getSimpleName() + ": " + cause.getLocalizedMessage() + ")";
    }
}
