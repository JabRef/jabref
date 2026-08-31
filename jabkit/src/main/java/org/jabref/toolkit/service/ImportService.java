package org.jabref.toolkit.service;

import java.nio.file.Path;
import java.util.List;

import javafx.util.Pair;

import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.toolkit.exception.ImportServiceException;

import org.jspecify.annotations.NullMarked;
import picocli.CommandLine;

@NullMarked
public class ImportService {

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

    public static ParserResult importBibTexFile(
            Path inputFile,
            CliPreferences cliPreferences,
            boolean porcelain) throws ImportServiceException {

        return importFile(inputFile, "bibtex", cliPreferences, porcelain);
    }

    public static ParserResult importFile(
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
                    Localization.lang("Unable to open file '%0'.", inputFile), ex,
                    CommandLine.ExitCode.USAGE);
        }
    }
}
