package org.jabref.toolkit.commands;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.DBMSConnectionUrl;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.SharedDatabaseExport;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.toolkit.exception.ImportServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
/// local temporary file before being used as the input, and a PostgreSQL connection URL such as
/// `postgresql://user:secret@host:5432/library`, whose shared library is exported to a temporary
/// `.bib` file before being used as the input (read-only).
///
/// See ADR-0057 (which supersedes ADR-0045) and ADR-0065 for the rationale.
class InputOption {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputOption.class);

    @ArgGroup(exclusive = true, multiplicity = "1")
    private InputSource inputSource;

    /// @return the resolved input file, downloading it to a temporary file first if a URL was supplied
    /// @throws ImportServiceException if a URL was supplied and could not be downloaded
    Path getInputFile(CliPreferences preferences) throws ImportServiceException {
        return resolveInput(inputSource.positionalInput != null
                            ? inputSource.positionalInput
                            : inputSource.optionInput,
                preferences);
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
    // [impl->req~jabkit.cli.input-shared-db~1]
    // [impl->adr~download-url-input-files~1]
    // [impl->adr~shared-database-url-as-jabkit-input~1]
    static Path resolveInput(String input, CliPreferences preferences) throws ImportServiceException {
        if (isPostgreSqlUrl(input)) {
            Optional<DBMSConnectionUrl> connectionUrl = DBMSConnectionUrl.parse(input);
            if (connectionUrl.isPresent()) {
                return pullSharedDatabase(connectionUrl.orElseThrow(), preferences);
            }
        }

        if (URLUtil.isURL(input)) {
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

    /// [DBMSConnectionUrl#parse] is deliberately permissive: it finds a PostgreSQL URL anywhere in a
    /// pasted text and also accepts libpq's `host=... dbname=...` keyword form, which the GUI needs.
    /// On the command line that would swallow local files whose name happens to contain such a
    /// fragment, so only an argument that starts with the scheme is offered to it.
    private static boolean isPostgreSqlUrl(String input) {
        return input.regionMatches(true, 0, "postgres", 0, "postgres".length())
                || input.regionMatches(true, 0, "jdbc:postgres", 0, "jdbc:postgres".length());
    }

    private static Path pullSharedDatabase(DBMSConnectionUrl connectionUrl, CliPreferences preferences) throws ImportServiceException {
        // The JDBC URL keeps host, port and database, but not the user and the password, so it is safe to print
        String redactedInput = connectionUrl.toJdbcUrl();
        try {
            return SharedDatabaseExport.pullToTemporaryBib(connectionUrl, preferences);
        } catch (SQLException | InvalidDBMSConnectionPropertiesException | DatabaseNotSupportedException | IOException e) {
            // The CLI prints the messages only; the stack trace of a failed connection is what makes it diagnosable
            LOGGER.error("Could not read the shared database {}", redactedInput, e);
            throw new ImportServiceException(
                    "Problem reading the shared database " + redactedInput + ": " + e.getLocalizedMessage(),
                    Localization.lang("Problem reading the shared database %0: %1", redactedInput, e.getLocalizedMessage()),
                    e,
                    CommandLine.ExitCode.SOFTWARE);
        }
    }

    /// `--input` is a backward-compatible alias here; the positional form and the alias both come from
    /// ADR 57, which superseded the `--input`-only ADR 45.
    // [impl->adr~allow-positional-input-file-argument~1]
    private static class InputSource {
        // [impl->req~jabkit.cli.input-flag~2]
        @Parameters(index = "0", paramLabel = "FILE",
                description = "Input file, an http(s)/ftp URL, or a PostgreSQL connection URL. Alternatively, pass it via --input.")
        private String positionalInput;

        @Option(names = {"--input"},
                description = "Input file, an http(s)/ftp URL, or a PostgreSQL connection URL (alias for the positional FILE argument).")
        private String optionInput;
    }
}
