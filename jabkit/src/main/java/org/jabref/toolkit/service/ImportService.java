package org.jabref.toolkit.service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

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
import org.jabref.toolkit.exception.ImportServiceException;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class ImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportService.class);

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

    public static @NonNull ParserResult importBibTexFile(
            Path inputFile,
            CliPreferences cliPreferences,
            boolean porcelain) throws ImportServiceException {

        return importFile(inputFile, "bibtex", cliPreferences, porcelain);
    }

    public static @NonNull ParserResult importFile(
            Path inputFile,
            String importFormat,
            CliPreferences cliPreferences,
            boolean porcelain) throws ImportServiceException {

        try {
            ImportFormatReader importFormatReader = new ImportFormatReader(
                    cliPreferences.getImporterPreferences(),
                    cliPreferences.getImportFormatPreferences(),
                    cliPreferences.getCitationKeyPatternPreferences(),
                    new DummyFileUpdateMonitor()
            );

            ParserResult result;
            if (!"*".equals(importFormat)) {
                if (!porcelain) {
                    System.out.println(Localization.lang("Importing %0", inputFile));
                }
                result = importFormatReader.importFromFile(importFormat, inputFile);
            } else {
                // * means "guess the format":
                if (!porcelain) {
                    System.out.println(Localization.lang("Importing file %0 as unknown format", inputFile));
                }

                ImportFormatReader.ImportResult importResult = importFormatReader.importWithAutoDetection(inputFile);

                if (!porcelain) {
                    System.out.println(Localization.lang("Format used: %0", importResult.format()));
                }
                result = importResult.parserResult();
            }

            if (result.isInvalid()) {
                throw new ImportServiceException("Input file '" + inputFile + "' is invalid and could not be parsed.",
                        Localization.lang("Input file '%0' is invalid and could not be parsed.", inputFile),
                        CommandLine.ExitCode.USAGE);
            }
            return result;
        } catch (ImportException ex) {
            throw new ImportServiceException("Unable to open file '" + inputFile + "'.",
                    Localization.lang("Unable to open file '%0'.", inputFile), ex, CommandLine.ExitCode.USAGE);
        }
    }

    /// Reads URIs as input
    public static ParserResult importFile(String importArguments,
                                          String importFormat,
                                          CliPreferences cliPreferences,
                                          boolean porcelain) throws ImportServiceException {
        LOGGER.debug("Importing file {}", importArguments);
        String[] data = importArguments.split(",");

        String address = data[0];
        Path file;
        if (address.startsWith("http://") || address.startsWith("https://") || address.startsWith("ftp://")) {
            // Download web resource to temporary file
            try {
                file = new URLDownload(address).toTemporaryFile();
            } catch (FetcherException | MalformedURLException e) {
                throw new ImportServiceException("Problem downloading from " + address + ": " + e.getLocalizedMessage(),
                        Localization.lang("Problem downloading from %0: %1", address, e.getLocalizedMessage()),
                        CommandLine.ExitCode.SOFTWARE);
            }
        } else {
            if (OS.WINDOWS) {
                file = Path.of(address);
            } else {
                file = Path.of(address.replace("~", System.getProperty("user.home")));
            }
        }

        ParserResult result = importFile(file, importFormat, cliPreferences, porcelain);
        if (result.hasWarnings()) {
            System.out.println(result.getErrorMessage());
        }
        return result;
    }
}
