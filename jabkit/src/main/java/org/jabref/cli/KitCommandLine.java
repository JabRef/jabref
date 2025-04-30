package org.jabref.cli;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Command(name = "jabkit",
        mixinStandardHelpOptions = true,
        subcommands = {
                GenerateCitationKeys.class,
                CheckConsistency.class,
                CheckIntegrity.class,
                Fetch.class,
                Search.class,
                Convert.class,
                GenerateBibFromAux.class,
                Preferences.class,
                Pdf.class
        })
public class KitCommandLine implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KitCommandLine.class);

    protected final CliPreferences cliPreferences;
    protected final BibEntryTypesManager entryTypesManager;

    @Option(names = "--debug", description = "Enable debug output")
    boolean debug;

    @Option(names = "--porcelain", description = "Enable script-friendly output")
    boolean porcelain;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested; // VersionProvider?

    @Option(names = {"?", "-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    public KitCommandLine(CliPreferences cliPreferences, BibEntryTypesManager entryTypesManager) {
        this.cliPreferences = cliPreferences;
        this.entryTypesManager = entryTypesManager;
    }

    @Override
    public Integer call() {
        // Todo: Implement
        return 0;
    }

    /**
     *
     * @param importArguments Format: <code>fileName[,format]</code>
     */
    protected Optional<ParserResult> importFile(String importArguments, String importFormat) {
        LOGGER.debug("Importing file {}", importArguments);
        String[] data = importArguments.split(",");

        String address = data[0];
        Path file;
        if (address.startsWith("http://") || address.startsWith("https://") || address.startsWith("ftp://")) {
            // Download web resource to temporary file
            try {
                file = new URLDownload(address).toTemporaryFile();
            } catch (FetcherException | MalformedURLException e) {
                System.err.println(Localization.lang("Problem downloading from %1", address) + e.getLocalizedMessage());
                return Optional.empty();
            }
        } else {
            if (OS.WINDOWS) {
                file = Path.of(address);
            } else {
                file = Path.of(address.replace("~", System.getProperty("user.home")));
            }
        }

        Optional<ParserResult> importResult = importFile(file, importFormat);
        importResult.ifPresent(result -> {
            if (result.hasWarnings()) {
                System.out.println(result.getErrorMessage());
            }
        });
        return importResult;
    }

    protected Optional<ParserResult> importFile(Path file, String importFormat) {
        try {
            ImportFormatReader importFormatReader = new ImportFormatReader(
                    cliPreferences.getImporterPreferences(),
                    cliPreferences.getImportFormatPreferences(),
                    cliPreferences.getCitationKeyPatternPreferences(),
                    new DummyFileUpdateMonitor()
            );

            if (!"*".equals(importFormat)) {
                System.out.println(Localization.lang("Importing %0", file));
                ParserResult result = importFormatReader.importFromFile(importFormat, file);
                return Optional.of(result);
            } else {
                // * means "guess the format":
                System.out.println(Localization.lang("Importing file %0 as unknown format", file));

                ImportFormatReader.UnknownFormatImport importResult =
                        importFormatReader.importUnknownFormat(file, new DummyFileUpdateMonitor());

                System.out.println(Localization.lang("Format used: %0", importResult.format()));
                return Optional.of(importResult.parserResult());
            }
        } catch (ImportException ex) {
            System.err.println(Localization.lang("Error opening file '%0'", file) + "\n" + ex.getLocalizedMessage());
            return Optional.empty();
        }
    }
}
