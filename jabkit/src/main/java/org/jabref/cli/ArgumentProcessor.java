package org.jabref.cli;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.util.Pair;

import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;

@Command(name = "jabkit",
        mixinStandardHelpOptions = true,
        // sorted alphabetically
        subcommands = {
                CheckConsistency.class,
                CheckIntegrity.class,
                Convert.class,
                Fetch.class,
                GenerateBibFromAux.class,
                GenerateCitationKeys.class,
                Pdf.class,
                Preferences.class,
                Pseudonymize.class,
                Search.class
        })
public class ArgumentProcessor implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentProcessor.class);

    protected final CliPreferences cliPreferences;
    protected final BibEntryTypesManager entryTypesManager;

    @Mixin
    private SharedOptions sharedOptions = new SharedOptions();

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "display version info")
    private boolean versionInfoRequested;

    public ArgumentProcessor(CliPreferences cliPreferences, BibEntryTypesManager entryTypesManager) {
        this.cliPreferences = cliPreferences;
        this.entryTypesManager = entryTypesManager;
    }

    @Override
    public void run() {
       String bannerString = BuildInfo.JABREF_BANNER.toString();

       String formattedBanner = String.format(bannerString, new BuildInfo().version);

       System.out.println(formattedBanner);
    }

    /**
     * Reads URIs as input
     */
    protected static Optional<ParserResult> importFile(String importArguments,
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

    protected static Optional<ParserResult> importFile(Path file,
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

                ImportFormatReader.UnknownFormatImport importResult =
                        importFormatReader.importUnknownFormat(file, new DummyFileUpdateMonitor());

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

    protected static void saveDatabase(CliPreferences cliPreferences,
                                       BibEntryTypesManager entryTypesManager,
                                       BibDatabase newBase,
                                       Path outputFile) {
        saveDatabaseContext(cliPreferences, entryTypesManager, new BibDatabaseContext(newBase), outputFile);
    }

    protected static void saveDatabaseContext(CliPreferences cliPreferences,
                                              BibEntryTypesManager entryTypesManager,
                                              BibDatabaseContext bibDatabaseContext,
                                              Path outputFile) {
        try {
            if (!FileUtil.isBibFile(outputFile)) {
                System.err.println(Localization.lang("Invalid output file type provided."));
            }
            try (AtomicFileWriter fileWriter = new AtomicFileWriter(outputFile, StandardCharsets.UTF_8)) {
                BibWriter bibWriter = new BibWriter(fileWriter, OS.NEWLINE);
                SelfContainedSaveConfiguration saveConfiguration = (SelfContainedSaveConfiguration) new SelfContainedSaveConfiguration()
                        .withReformatOnSave(cliPreferences.getLibraryPreferences().shouldAlwaysReformatOnSave());
                BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                        bibWriter,
                        saveConfiguration,
                        cliPreferences.getFieldPreferences(),
                        cliPreferences.getCitationKeyPatternPreferences(),
                        entryTypesManager);
                databaseWriter.saveDatabase(bibDatabaseContext);

                // Show just a warning message if encoding did not work for all characters:
                if (fileWriter.hasEncodingProblems()) {
                    System.err.println(Localization.lang("Warning") + ": "
                            + Localization.lang("UTF-8 could not be used to encode the following characters: %0", fileWriter.getEncodingProblems()));
                }
                System.out.println(Localization.lang("Saved %0.", outputFile));
            }
        } catch (IOException ex) {
            System.err.println(Localization.lang("Could not save file.") + "\n" + ex.getLocalizedMessage());
        }
    }

    public static List<Pair<String, String>> getAvailableImportFormats(CliPreferences preferences) {
        ImportFormatReader importFormatReader = new ImportFormatReader(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getCitationKeyPatternPreferences(),
                new DummyFileUpdateMonitor()
        );
        return importFormatReader
                .getImportFormats().stream()
                .map(format -> new Pair<>(format.getName(), format.getId()))
                .toList();
    }

    public static List<Pair<String, String>> getAvailableExportFormats(CliPreferences preferences) {
        ExporterFactory exporterFactory = ExporterFactory.create(preferences);
        return exporterFactory.getExporters().stream()
                              .map(format -> new Pair<>(format.getName(), format.getId()))
                              .toList();
    }

    public static class SharedOptions {
        @Option(names = {"-d", "--debug"}, description = "Enable debug output")
        boolean debug;

        @Option(names = {"-p", "--porcelain"}, description = "Enable script-friendly output")
        boolean porcelain;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
        private boolean usageHelpRequested = true;
    }
}
