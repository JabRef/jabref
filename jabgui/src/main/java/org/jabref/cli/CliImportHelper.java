package org.jabref.cli;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///  CLI import helper that are needed for importing stuff from the browser extension
///
/// @deprecated used by the browser extension only
@Deprecated
public class CliImportHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(CliImportHelper.class);

    /**
     * Reads URIs as input
     *
     * @param location URL or file path to import
     */
    public static Optional<ParserResult> importFile(String location,
                                                    CliPreferences cliPreferences,
                                                    boolean porcelain) {
        LOGGER.debug("Importing file from locaiton {}", location);
        String[] data = location.split(",");

        String address = data[0];
        Path file;
        if (address.startsWith("http://") || address.startsWith("https://") || address.startsWith("ftp://")) {
            // Download web resource to temporary file
            try {
                file = new URLDownload(address).toTemporaryFile();
            } catch (FetcherException |
                     MalformedURLException e) {
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

        Optional<ParserResult> importResult = importFile(file, cliPreferences, porcelain);
        importResult.ifPresent(result -> {
            if (result.hasWarnings()) {
                System.out.println(result.getErrorMessage());
            }
        });
        return importResult;
    }

    public static Optional<ParserResult> importFile(Path file,
                                                    CliPreferences cliPreferences,
                                                    boolean porcelain) {
        try {
            ImportFormatReader importFormatReader = new ImportFormatReader(
                    cliPreferences.getImporterPreferences(),
                    cliPreferences.getImportFormatPreferences(),
                    cliPreferences.getCitationKeyPatternPreferences(),
                    new DummyFileUpdateMonitor()
            );

            if (!porcelain) {
                System.out.println(Localization.lang("Importing %0", file));
            }
            ParserResult result = importFormatReader.importFromFile("bibtex", file);
            return Optional.of(result);
        } catch (ImportException ex) {
            LOGGER.error("Error opening file '{}'", file, ex);
            return Optional.empty();
        }
    }
}
