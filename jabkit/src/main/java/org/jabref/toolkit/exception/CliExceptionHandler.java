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
                return (cliException.getExitCode() == CommandLine.ExitCode.SOFTWARE)
                       ? softwareErrorExitCode(commandLine)
                       : cliException.getExitCode();
            }
            case JabRefException jabRefException -> {
                printErrorMessage(jabRefException, true);
                return softwareErrorExitCode(commandLine);
            }
            default -> {
                return delegate.handleExecutionException(ex, commandLine, parseResult);
            }
        }
    }

    /// What the failed command exits with on a software error: [CommandLine.ExitCode#SOFTWARE], unless
    /// the command declares another `exitCodeOnExecutionException` because it uses that code for
    /// something else. Picocli exits with the same code for an exception that no handler catches.
    private static int softwareErrorExitCode(CommandLine commandLine) {
        return commandLine.getCommandSpec().exitCodeOnExecutionException();
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
