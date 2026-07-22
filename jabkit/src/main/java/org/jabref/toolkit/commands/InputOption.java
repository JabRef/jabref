package org.jabref.toolkit.commands;

import java.net.MalformedURLException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.toolkit.exception.ImportServiceException;

import io.github.adr.linked.ADR;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/// Reusable input-file argument shared by all `jabkit` subcommands that read a single file.
///
/// The file can be supplied either as a positional argument
/// (`jabkit check integrity references.bib`) or via the `--input` option
/// (`jabkit check integrity --input references.bib`). The positional form is the preferred,
/// idiomatic style; `--input` is retained as a backward-compatible alias.
///
/// Exactly one of the two forms must be given; picocli enforces this at parse time via the
/// required, mutually exclusive [ArgGroup].
///
/// Both forms also accept an `http://`, `https://`, or `ftp://` URL, which is downloaded to a
/// local temporary file before being used as the input.
///
/// See ADR-0057 (which supersedes ADR-0045) and ADR-0065 for the rationale.
class InputOption {

    @ArgGroup(exclusive = true, multiplicity = "1")
    private InputSource inputSource;

    /// @return the resolved input file, downloading it to a temporary file first if a URL was supplied
    /// @throws ImportServiceException if a URL was supplied and could not be downloaded
    Path getInputFile() throws ImportServiceException {
        return resolveInput(inputSource.positionalInput != null
                            ? inputSource.positionalInput
                            : inputSource.optionInput);
    }

    /// @return `true` if the given input argument is an `http(s)`/`ftp` URL rather than a local path
    static boolean isUrl(String input) {
        String lowerCaseInput = input.toLowerCase(Locale.ROOT);
        return lowerCaseInput.startsWith("http://") || lowerCaseInput.startsWith("https://") || lowerCaseInput.startsWith("ftp://");
    }

    /// Resolves a single input argument to a local file: a URL is downloaded to a temporary file,
    /// anything else is taken as a (possibly Cygwin-style) local path.
    ///
    /// Commands reading more than one input cannot use this mixin — its [ArgGroup] binds a single
    /// argument — and call this method per input instead.
    ///
    /// @return the resolved input file, downloading it to a temporary file first if a URL was supplied
    /// @throws ImportServiceException if a URL was supplied and could not be downloaded
    // [impl->req~jabkit.cli.input-url~2]
    @ADR(65)
    static Path resolveInput(String input) throws ImportServiceException {
        if (isUrl(input)) {
            try {
                return new URLDownload(input).toTemporaryFile();
            } catch (FetcherException | MalformedURLException e) {
                String redactedInput = FetcherException.getRedactedUrl(input);
                throw new ImportServiceException(
                        "Problem downloading from " + redactedInput + ": " + e.getLocalizedMessage(),
                        Localization.lang("Problem downloading from %0: %1", redactedInput, e.getLocalizedMessage()),
                        e,
                        CommandLine.ExitCode.SOFTWARE);
            }
        }

        try {
            return FileUtil.convertCygwinPathToWindows(input);
        } catch (InvalidPathException e) {
            throw new ImportServiceException(
                    "Invalid input path '" + input + "': " + e.getLocalizedMessage(),
                    Localization.lang("Invalid input path '%0'.", input),
                    e,
                    CommandLine.ExitCode.USAGE);
        }
    }

    private static class InputSource {
        // [impl->req~jabkit.cli.input-flag~2]
        @Parameters(index = "0", paramLabel = "FILE",
                description = "Input file, or an http(s)/ftp URL. Alternatively, pass it via --input.")
        private String positionalInput;

        @ADR(45)
        @Option(names = {"--input"},
                description = "Input file, or an http(s)/ftp URL (alias for the positional FILE argument).")
        private String optionInput;
    }
}
