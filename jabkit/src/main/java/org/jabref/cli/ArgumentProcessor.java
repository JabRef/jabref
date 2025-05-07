package org.jabref.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.util.Pair;

import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;

/**
 * Cli plan -- FixMe: Make nice, make docs
 *
 * jabkit generate-citation-keys Chocolate.bib
 * jabkit --help
 * jabkit --version
 * jabkit --debug
 *
 * jabkit check-consistency Chocolate.bib
 * jabkit check-consistency --input Chocolate.bib --output-format csv
 * jabkit check-consistency --input Chocolate.bib --output-format txt
 *
 * jabkit check-integrity Chocolate.bib
 *
 * # Overwrite
 * jabkit fetch --provider Medline/PubMed --query cancer --output Chocolate.bib
 * # Append - similar to "--import to upen"
 * jabkit fetch --provider Medline/PubMed --query cancer --output Chocolate.bib --append
 * # query is read from stdin if not provided
 *
 * // Search within the library
 * // OLD: jabkit export-matches --from db.bib -m author=Newton,search.htm,html
 * // Place target at the end
 * // search.g4 / https://docs.jabref.org/finding-sorting-and-cleaning-entries/search
 * jabkit search [searchstring] Chocolate.bib
 * jabkit search --query [searchstring] --input Chocolate.bib --output newfile.bib
 * // no --input: stdin
 * // no --output: stdout
 * // --output-format medline # otherwise: auto detected
 * // --input-format ris      # otherwise: auto detected
 *
 * // standard format: Output matched citation keys a list
 * // sno
 * jabkit search --query [searchstring] --input Chocolate.bib
 * jabkit search [searchstring] Chocolate.bib --output-format [format]
 * jabkit search --query [searchstring] --input Chocolate.bib --output-format [format] --output newfile.txt
 *
 * // similarily: convert
 * jabkit convert a.ris b.bib
 * jabkit convert --input a.ris --output b.bib
 *
 * // Localization.lang("Sublibrary from AUX to BibTeX")
 * jabkit generate-bib-from-aux --aux thesis.aux --input thesis.bib --output small.bib
 *
 * # Reset preferences
 * jabkit preferences reset
 * # Import preferences from a file
 * jabkit preferences import [filename]
 * # Export preferences to a file
 * jabkit preferences export [filename]
 *
 *
 * # Write BibTeX data into PDF file
 * jabkit pdf write-xmp --citation-key key1 --citation-key key2 --input Chocolate.bib --output paper.pdf
 * also: jabkit pdf write-xmp -k key1 -k key2
 *
 * # takes Chocolate.bib, searches the citatoin-keys, looks up linked files and writes xmp
 * # Description?
 * jabkit pdf update --format=xmp --citation-key key1 --citation-key key2 --update-linked-files --input Chocolate.bib
 * jabkit pdf update --format=xmp --format=bibtex-attachment --citation-key key1 --citation-key key2 --update-linked-files --input Chocolate.bib
 * # default: all formats: xmp and bibtex-attachment
 * # implementation: open Chocolate.bib, search for key1 and key2 {@code (List<BibEntry>)}, search for linked files - map linked files to BibEntry, for each map entry: execute update action (xmp and/or embed-bibtex)
 *
 * # NOT jabkit pdf update-embedded-bibtex (reminder: as above)
 *
 * # jabkit pdf embed-metadata
 * # NOT CHOSEN:jabkit pdf embed --format=xmp --format=bibtex --citation-key key1 --citation-key key2 --update-linked-files --input Chocolate.bib
 *
 * # updates all linked files (only ommitting -k leads to error)
 * # NOT jabkit pdf write-xmp --all --update-linked-files --input Chocolate.bib
 *
 * // .desc(Localization.lang("Script-friendly output"))
 * jabkit whateveraction --porcelain
 */
@Command(name = "jabkit",
        mixinStandardHelpOptions = true,
        subcommands = {
                GenerateCitationKeys.class,
                CheckConsistency.class,
//                CheckIntegrity.class,
                Fetch.class,
                Search.class,
                Convert.class,
                GenerateBibFromAux.class,
                Preferences.class,
                Pdf.class
        })
public class ArgumentProcessor implements Runnable {
    public static final String JABREF_BANNER = """
    \u001B[35m
       &&&    &&&&&    &&&&&&&&   &&&&&&&&   &&&&&&&&& &&&&&&&&&
       &&&    &&&&&    &&&   &&&  &&&   &&&  &&&       &&&
       &&&   &&& &&&   &&&   &&&  &&&   &&&  &&&       &&&
       &&&   &&   &&   &&&&&&&    &&&&&&&&   &&&&&&&&  &&& %s
       &&&  &&&&&&&&&  &&&   &&&  &&&   &&&  &&&       &&&
       &&&  &&&   &&&  &&&   &&&  &&&   &&&  &&&       &&&
    &&&&&   &&&   &&&  &&&&&&&&   &&&   &&&  &&&&&&&&& &&&
    \u001B[0m
    Staying on top of your literature since 2003 - https://www.jabref.org/
    Please report issues at https://github.com/JabRef/jabref/issues
    """;

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
        System.out.printf(ArgumentProcessor.JABREF_BANNER + "%n", new BuildInfo().version);
    }

    /**
     * Reads URIs as input // ToDo: Bring back
     * importArguments Format: <code>fileName[,format]</code>
     */
    /* protected Optional<ParserResult> importFile(String importArguments, String importFormat) {
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
    } */

    protected static Optional<ParserResult> importFile(CliPreferences cliPreferences, Path file, String importFormat) {
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

    protected static void saveDatabase(CliPreferences cliPreferences, BibEntryTypesManager entryTypesManager, BibDatabase newBase, Path outputFile) {
        try {
            System.out.println(Localization.lang("Saving") + ": " + outputFile);
            try (AtomicFileWriter fileWriter = new AtomicFileWriter(outputFile, StandardCharsets.UTF_8)) {
                BibWriter bibWriter = new BibWriter(fileWriter, OS.NEWLINE);
                SelfContainedSaveConfiguration saveConfiguration = (SelfContainedSaveConfiguration) new SelfContainedSaveConfiguration()
                        .withReformatOnSave(cliPreferences.getLibraryPreferences().shouldAlwaysReformatOnSave());
                BibDatabaseWriter databaseWriter = new BibtexDatabaseWriter(
                        bibWriter,
                        saveConfiguration,
                        cliPreferences.getFieldPreferences(),
                        cliPreferences.getCitationKeyPatternPreferences(),
                        entryTypesManager);
                databaseWriter.saveDatabase(new BibDatabaseContext(newBase));

                // Show just a warning message if encoding did not work for all characters:
                if (fileWriter.hasEncodingProblems()) {
                    System.err.println(Localization.lang("Warning") + ": "
                            + Localization.lang("UTF-8 could not be used to encode the following characters: %0", fileWriter.getEncodingProblems()));
                }
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
