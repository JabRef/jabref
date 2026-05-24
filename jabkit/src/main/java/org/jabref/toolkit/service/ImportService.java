package org.jabref.toolkit.service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.util.Pair;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportService.class);

    /// Outcome of [#importBibtexLibrary]. On success, [#parserResult()] is non-null. On failure, it
    /// is `null` and [#exitCode()] holds the error code the command should return.
    public record ImportOutcome(@Nullable ParserResult parserResult, int exitCode) {
    }

    public static List<Pair<String, String>> getAvailableImportFormats(CliPreferences preferences) {
        ImportFormatReader importFormatReader = new ImportFormatReader(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getCitationKeyPatternPreferences(),
                new DummyFileUpdateMonitor()
        );
        return importFormatReader
                .getImporters().stream()
                .map(format -> new Pair<>(format.getName(), format.getId()))
                .toList();
    }

    public static Optional<ParserResult> importFile(Path file,
                                                    String importFormat,
                                                    CliPreferences cliPreferences,
                                                    boolean porcelain) {
        try {
            ImportFormatReader importFormatReader = new ImportFormatReader(
                    cliPreferences.getImporterPreferences(),
                    cliPreferences.getImportFormatPreferences(),
                    cliPreferences.getCitationKeyPatternPreferences(),
                    new DummyFileUpdateMonitor()
            );

            if (!"*".equals(importFormat)) {
                if (!porcelain) {
                    System.out.println(Localization.lang("Importing %0", file));
                }
                ParserResult result = importFormatReader.importFromFile(importFormat, file);
                return Optional.of(result);
            } else {
                // * means "guess the format":
                if (!porcelain) {
                    System.out.println(Localization.lang("Importing file %0 as unknown format", file));
                }

                ImportFormatReader.ImportResult importResult = importFormatReader.importWithAutoDetection(file);

                if (!porcelain) {
                    System.out.println(Localization.lang("Format used: %0", importResult.format()));
                }
                return Optional.of(importResult.parserResult());
            }
        } catch (ImportException ex) {
            LOGGER.error("Error opening file '{}'", file, ex);
            return Optional.empty();
        }
    }

    /// Imports `inputFile` as a BibTeX library and validates it. On failure, an error message is
    /// printed and the returned [ImportOutcome] carries a `null` parser result plus the exit code.
    public static ImportOutcome importBibtexLibrary(Path inputFile, CliPreferences cliPreferences, boolean porcelain) {
        Optional<ParserResult> parserResult = importFile(inputFile, "bibtex", cliPreferences, porcelain);
        if (parserResult.isEmpty()) {
            System.err.println(Localization.lang("Unable to open file '%0'.", inputFile));
            return new ImportOutcome(null, 2);
        }
        if (parserResult.get().isInvalid()) {
            System.err.println(Localization.lang("Input file '%0' is invalid and could not be parsed.", inputFile));
            return new ImportOutcome(null, 2);
        }
        return new ImportOutcome(parserResult.get(), 0);
    }

    /// Reads URIs as input
    public static Optional<ParserResult> importFile(String importArguments,
                                                    String importFormat,
                                                    CliPreferences cliPreferences,
                                                    boolean porcelain) {
        LOGGER.debug("Importing file {}", importArguments);
        String[] data = importArguments.split(",");

        String address = data[0];
        Path file;
        if (address.startsWith("http://") || address.startsWith("https://") || address.startsWith("ftp://")) {
            // Download web resource to temporary file
            try {
                file = new URLDownload(address).toTemporaryFile();
            } catch (FetcherException | MalformedURLException e) {
                System.err.println(Localization.lang("Problem downloading from %0: %1", address, e.getLocalizedMessage()));
                return Optional.empty();
            }
        } else {
            if (OS.WINDOWS) {
                file = Path.of(address);
            } else {
                file = Path.of(address.replace("~", System.getProperty("user.home")));
            }
        }

        Optional<ParserResult> importResult = importFile(file, importFormat, cliPreferences, porcelain);
        importResult.ifPresent(result -> {
            if (result.hasWarnings()) {
                System.out.println(result.getErrorMessage());
            }
        });
        return importResult;
    }

    public static Optional<ParserResult> importBibTexFile(Path inputPath, CliPreferences cliPreferences, boolean porcelain) {
        Optional<ParserResult> parserResult = importFile(
                inputPath,
                "bibtex",
                cliPreferences,
                porcelain);

        if (parserResult.isEmpty()) {
            System.err.println(Localization.lang("Unable to open file '%0'.", inputPath));
            return Optional.empty();
        }

        if (parserResult.get().isInvalid()) {
            System.err.println(Localization.lang("Input file '%0' is invalid and could not be parsed.", inputPath));
            return Optional.empty();
        }
        return parserResult;
    }
}
