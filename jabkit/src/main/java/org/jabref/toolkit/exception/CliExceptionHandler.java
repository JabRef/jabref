package org.jabref.toolkit.exception;

import org.jabref.logic.JabRefException;

import picocli.CommandLine;

public class CliExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    private final CommandLine.IExecutionExceptionHandler delegate;

    public CliExceptionHandler(CommandLine.IExecutionExceptionHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult parseResult) throws Exception {
        switch (ex) {
            case CliException cliException -> {
                System.err.println(buildErrorMessage(cliException, true));
                return cliException.getExitCode();
            }
            case JabRefException jabRefException -> {
                System.err.println(buildErrorMessage(jabRefException, true));
                return CommandLine.ExitCode.SOFTWARE;
            }
            default -> {
                return delegate.handleExecutionException(ex, commandLine, parseResult);
            }
        }
    }

    public static String buildErrorMessage(Exception exception, boolean withCauseIfPresent) {
        Throwable cause = exception.getCause();
        if (withCauseIfPresent && cause != null) {
            return exception.getLocalizedMessage()
                    + " (" + cause.getClass().getSimpleName() + ": " + cause.getLocalizedMessage() + ")";
        } else {
            return exception.getLocalizedMessage();
        }
    }
}
