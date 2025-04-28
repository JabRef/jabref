package org.jabref.cli;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.BackingStoreException;

import javafx.util.Pair;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.exporter.XmpPdfExporter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.DatabaseSearcher;
import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 jabkit generate-citation-keys Chocolate.bib
 jabkit --help
 jabkit --version
 jabkit --debug

 jabkit check-consistency Chocolate.bib
 jabkit check-consistency --input Chocolate.bib --output-format csv
 jabkit check-consistency --input Chocolate.bib --output-format txt

 jabkit check-integrity Chocolate.bib

 # Overwrite
 jabkit fetch --provider Medline/PubMed --query cancer --output Chocolate.bib
 # Append - similar to "--import to upen"
 jabkit fetch --provider Medline/PubMed --query cancer --output Chocolate.bib --append
 # query is read from stdin if not provided

 // Search within the library
 // OLD: jabkit export-matches --from db.bib -m author=Newton,search.htm,html
 // Place target at the end
 // search.g4 / https://docs.jabref.org/finding-sorting-and-cleaning-entries/search
 jabkit search <searchstring> Chocolate.bib
 jabkit search --query <searchstring> --input Chocolate.bib --output newfile.bib
 // no --input: stdin
 // no --output: stdout
 // --output-format medline # otherwise: auto detected
 // --input-format ris      # otherwise: auto detected

 // standard format: Output matched citation keys a list
 // sno
 jabkit search --query <searchstring> --input Chocolate.bib
 jabkit search <searchstring> Chocolate.bib --output-format <format>
 jabkit search --query <searchstring> --input Chocolate.bib --output-format <format> --output newfile.txt

 // similarily: convert
 jabkit convert a.ris b.bib
 jabkit convert --input a.ris --output b.bib

 // Localization.lang("Sublibrary from AUX to BibTeX")
 jabkit generate-bib-from-aux --aux thesis.aux --input thesis.bib --output small.bib

 # Reset preferences
 jabkit preferences reset
 # Import preferences from a file
 jabkit preferences import <filename>
 # Export preferences to a file
 jabkit preferences export <filename>


 # Write BibTeX data into PDF file
 jabkit pdf write-xmp --citation-key key1 --citation-key key2 --input Chocolate.bib --output paper.pdf
 also: jabkit pdf write-xmp -k key1 -k key2

 # takes Chocolate.bib, searches the citatoin-keys, looks up linked files and writes xmp
 # Description?
 jabkit pdf update --format=xmp --citation-key key1 --citation-key key2 --update-linked-files --input Chocolate.bib
 jabkit pdf update --format=xmp --format=bibtex-attachment --citation-key key1 --citation-key key2 --update-linked-files --input Chocolate.bib
 # default: all formats: xmp and bibtex-attachment
 # implementation: open Chocolate.bib, search for key1 and key2 (List<BibEntry>), search for linked files - map linked files to BibEntry, for each map entry: execute update action (xmp and/or embed-bibtex)

 # NOT jabkit pdf update-embedded-bibtex (reminder: as above)

 # jabkit pdf embed-metadata
 # NOT CHOSEN:jabkit pdf embed --format=xmp --format=bibtex --citation-key key1 --citation-key key2 --update-linked-files --input Chocolate.bib

 # updates all linked files (only ommitting -k leads to error)
 # NOT jabkit pdf write-xmp --all --update-linked-files --input Chocolate.bib

 // .desc(Localization.lang("Script-friendly output"))
 jabkit <whateveraction> --porcelain
 */
public class ArgumentProcessor {
    private static final String JABREF_BANNER = """

       &&&    &&&&&    &&&&&&&&   &&&&&&&&   &&&&&&&&& &&&&&&&&&
       &&&    &&&&&    &&&   &&&  &&&   &&&  &&&       &&&
       &&&   &&& &&&   &&&   &&&  &&&   &&&  &&&       &&&
       &&&   &&   &&   &&&&&&&    &&&&&&&&   &&&&&&&&  &&& %s
       &&&  &&&&&&&&&  &&&   &&&  &&&   &&&  &&&       &&&
       &&&  &&&   &&&  &&&   &&&  &&&   &&&  &&&       &&&
    &&&&&   &&&   &&&  &&&&&&&&   &&&   &&&  &&&&&&&&& &&&

    Staying on top of your literature since 2003 - https://www.jabref.org/
    Please report issues at https://github.com/JabRef/jabref/issues
    """;

    private static final String WRAPPED_LINE_PREFIX = ""; // If a line break is added, this prefix will be inserted at the beginning of the next line
    private static final String STRING_TABLE_DELIMITER = " : ";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentProcessor.class);

    public enum Mode { INITIAL_START, REMOTE_START }

    private final Mode startupMode;

    private final CliPreferences cliPreferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;

    private final KitCommandLine kitCli;
    private final CommandLine cli;

    /**
     * First call the constructor, then call {@link #processArguments()}.
     */
    public ArgumentProcessor(Mode startupMode,
                             CliPreferences cliPreferences,
                             FileUpdateMonitor fileUpdateMonitor,
                             BibEntryTypesManager entryTypesManager) {
        this.startupMode = startupMode;
        this.cliPreferences = cliPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.kitCli = new KitCommandLine(cliPreferences, fileUpdateMonitor, entryTypesManager);

        cli = new CommandLine(this.kitCli);
    }

    /**
     *
     * @param importArguments Format: <code>fileName[,format]</code>
     */
    private Optional<ParserResult> importFile(String importArguments) {
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

        String importFormat;
        if (data.length > 1) {
            importFormat = data[1];
        } else {
            importFormat = "*";
        }

        Optional<ParserResult> importResult = importFile(file, importFormat);
        importResult.ifPresent(result -> {
            if (result.hasWarnings()) {
                System.out.println(result.getErrorMessage());
            }
        });
        return importResult;
    }

    private Optional<ParserResult> importFile(Path file, String importFormat) {
        try {
            ImportFormatReader importFormatReader = new ImportFormatReader(
                    cliPreferences.getImporterPreferences(),
                    cliPreferences.getImportFormatPreferences(),
                    cliPreferences.getCitationKeyPatternPreferences(),
                    fileUpdateMonitor
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

    public void processArguments(String[] args) {
        cli.execute(args);

        if (cli.isVersionHelpRequested()) {
            System.out.printf(JABREF_BANNER + "%n", new BuildInfo().version);
            return;
        }

        if (cli.isUsageHelpRequested()) {
            System.out.printf(JABREF_BANNER + "%n", new BuildInfo().version);
            System.out.println(cli.getUsageMessage());

            System.out.println(Localization.lang("Available import formats"));
            System.out.println(alignStringTable(getAvailableImportFormats(cliPreferences)));

            return;
        }

        /*
        if ((startupMode == Mode.INITIAL_START) && cli.isShowVersion()) {
            cli.displayVersion();
        }

        if ((startupMode == Mode.INITIAL_START) && cli.isHelp()) {
            JabKitCliOptions.printUsage(cliPreferences);
            return;
        }

        // Check if we should reset all preferences to default values:
        if (cli.isPreferencesReset()) {
            resetPreferences(cli.getPreferencesReset());
        }

        // Check if we should import preferences from a file:
        if (cli.isPreferencesImport()) {
            importPreferences();
        }

        List<ParserResult> loaded = importAndOpenFiles();

        if (cli.isFetcherEngine()) {
            fetch(cli.getFetcherEngine()).ifPresent(loaded::add);
        }

        if (cli.isExportMatches()) {
            if (!loaded.isEmpty()) {
                if (!exportMatches(loaded)) {
                    return;
                }
            } else {
                System.err.println(Localization.lang("The output option depends on a valid input option."));
            }
        }

        if (cli.isGenerateCitationKeys()) {
            regenerateCitationKeys(loaded);
        }

        if ((cli.isWriteXmpToPdf() && cli.isEmbedBibFileInPdf()) || (cli.isWriteMetadataToPdf() && (cli.isWriteXmpToPdf() || cli.isEmbedBibFileInPdf()))) {
            System.err.println("Give only one of [writeXmpToPdf, embedBibFileInPdf, writeMetadataToPdf]");
        }

        if (cli.isWriteMetadataToPdf() || cli.isWriteXmpToPdf() || cli.isEmbedBibFileInPdf()) {
            if (!loaded.isEmpty()) {
                writeMetadataToPdf(loaded,
                        cli.getWriteMetadataToPdf(),
                        cliPreferences.getXmpPreferences(),
                        cliPreferences.getFilePreferences(),
                        cliPreferences.getLibraryPreferences().getDefaultBibDatabaseMode(),
                        cliPreferences.getCustomEntryTypesRepository(),
                        cliPreferences.getFieldPreferences(),
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                        cli.isWriteXmpToPdf() || cli.isWriteMetadataToPdf(),
                        cli.isEmbedBibFileInPdf() || cli.isWriteMetadataToPdf());
            }
        }

        if (cli.isFileExport()) {
            if (!loaded.isEmpty()) {
                exportFile(loaded, cli.getFileExport().split(","));
                LOGGER.debug("Finished export");
            } else {
                System.err.println(Localization.lang("The output option depends on a valid import option."));
            }
        }

        if (cli.isPreferencesExport()) {
            try {
                cliPreferences.exportPreferences(Path.of(cli.getPreferencesExport()));
            } catch (JabRefException ex) {
                LOGGER.error("Cannot export preferences", ex);
            }
        }

        if (cli.isAuxImport()) {
            doAuxImport(loaded);
        }

        if (cli.isCheckConsistency()) {
            checkConsistency(cliPreferences, entryTypesManager);
        }

         */
    }

    private static void writeMetadataToPdf(List<ParserResult> loaded,
                                    String filesAndCiteKeys,
                                    XmpPreferences xmpPreferences,
                                    FilePreferences filePreferences,
                                    BibDatabaseMode databaseMode,
                                    BibEntryTypesManager entryTypesManager,
                                    FieldPreferences fieldPreferences,
                                    JournalAbbreviationRepository abbreviationRepository,
                                    boolean writeXMP,
                                    boolean embeddBibfile) {
        if (loaded.isEmpty()) {
            LOGGER.error("The write xmp option depends on a valid import option.");
            return;
        }
        ParserResult pr = loaded.getLast();
        BibDatabaseContext databaseContext = pr.getDatabaseContext();

        XmpPdfExporter xmpPdfExporter = new XmpPdfExporter(xmpPreferences);
        EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter = new EmbeddedBibFilePdfExporter(databaseMode, entryTypesManager, fieldPreferences);

        if ("all".equals(filesAndCiteKeys)) {
            for (BibEntry entry : databaseContext.getEntries()) {
                writeMetadataToPDFsOfEntry(
                        databaseContext,
                        entry.getCitationKey().orElse("<no cite key defined>"),
                        entry,
                        filePreferences,
                        xmpPdfExporter,
                        embeddedBibFilePdfExporter,
                        abbreviationRepository,
                        writeXMP,
                        embeddBibfile);
            }
            return;
        }

        List<String> citeKeys = new ArrayList<>();
        List<String> pdfs = new ArrayList<>();
        for (String fileOrCiteKey : filesAndCiteKeys.split(",")) {
            if (fileOrCiteKey.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                pdfs.add(fileOrCiteKey);
            } else {
                citeKeys.add(fileOrCiteKey);
            }
        }

        writeMetadataToPdfByCitekey(
                databaseContext,
                citeKeys,
                filePreferences,
                xmpPdfExporter,
                embeddedBibFilePdfExporter,
                abbreviationRepository,
                writeXMP,
                embeddBibfile);
        writeMetadataToPdfByFileNames(
                databaseContext,
                pdfs,
                filePreferences,
                xmpPdfExporter,
                embeddedBibFilePdfExporter,
                abbreviationRepository,
                writeXMP,
                embeddBibfile);
    }

    private static void writeMetadataToPDFsOfEntry(BibDatabaseContext databaseContext,
                                            String citeKey,
                                            BibEntry entry,
                                            FilePreferences filePreferences,
                                            XmpPdfExporter xmpPdfExporter,
                                            EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter,
                                            JournalAbbreviationRepository abbreviationRepository,
                                            boolean writeXMP,
                                            boolean embedBibfile) {
        try {
            if (writeXMP) {
                if (xmpPdfExporter.exportToAllFilesOfEntry(databaseContext, filePreferences, entry, List.of(entry), abbreviationRepository)) {
                    System.out.printf("Successfully written XMP metadata on at least one linked file of %s%n", citeKey);
                } else {
                    System.err.printf("Cannot write XMP metadata on any linked files of %s. Make sure there is at least one linked file and the path is correct.%n", citeKey);
                }
            }
            if (embedBibfile) {
                if (embeddedBibFilePdfExporter.exportToAllFilesOfEntry(databaseContext, filePreferences, entry, List.of(entry), abbreviationRepository)) {
                    System.out.printf("Successfully embedded metadata on at least one linked file of %s%n", citeKey);
                } else {
                    System.out.printf("Cannot embed metadata on any linked files of %s. Make sure there is at least one linked file and the path is correct.%n", citeKey);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed writing metadata on a linked file of {}.", citeKey);
        }
    }

    private static void writeMetadataToPdfByCitekey(BibDatabaseContext databaseContext,
                                             List<String> citeKeys,
                                             FilePreferences filePreferences,
                                             XmpPdfExporter xmpPdfExporter,
                                             EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter,
                                             JournalAbbreviationRepository abbreviationRepository,
                                             boolean writeXMP,
                                             boolean embeddBibfile) {
        for (String citeKey : citeKeys) {
            List<BibEntry> bibEntryList = databaseContext.getDatabase().getEntriesByCitationKey(citeKey);
            if (bibEntryList.isEmpty()) {
                System.err.printf("Skipped - Cannot find %s in library.%n", citeKey);
                continue;
            }
            for (BibEntry entry : bibEntryList) {
                writeMetadataToPDFsOfEntry(databaseContext, citeKey, entry, filePreferences, xmpPdfExporter, embeddedBibFilePdfExporter, abbreviationRepository, writeXMP, embeddBibfile);
            }
        }
    }

    private static void writeMetadataToPdfByFileNames(BibDatabaseContext databaseContext,
                                               List<String> pdfs,
                                               FilePreferences filePreferences,
                                               XmpPdfExporter xmpPdfExporter,
                                               EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter,
                                               JournalAbbreviationRepository abbreviationRepository,
                                               boolean writeXMP,
                                               boolean embeddBibfile) {
        for (String fileName : pdfs) {
            Path filePath = Path.of(fileName);
            if (!filePath.isAbsolute()) {
                filePath = FileUtil.find(fileName, databaseContext.getFileDirectories(filePreferences)).orElse(FileUtil.find(fileName, List.of(Path.of("").toAbsolutePath())).orElse(filePath));
            }
            if (Files.exists(filePath)) {
                try {
                    if (writeXMP) {
                        if (xmpPdfExporter.exportToFileByPath(databaseContext, filePreferences, filePath, abbreviationRepository)) {
                            System.out.printf("Successfully written XMP metadata of at least one entry to %s%n", fileName);
                        } else {
                            System.out.printf("File %s is not linked to any entry in database.%n", fileName);
                        }
                    }
                    if (embeddBibfile) {
                        if (embeddedBibFilePdfExporter.exportToFileByPath(databaseContext, filePreferences, filePath, abbreviationRepository)) {
                            System.out.printf("Successfully embedded XMP metadata of at least one entry to %s%n", fileName);
                        } else {
                            System.out.printf("File %s is not linked to any entry in database.%n", fileName);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Error accessing file '{}'.", fileName);
                } catch (Exception e) {
                    LOGGER.error("Error writing entry to {}.", fileName);
                }
            } else {
                LOGGER.error("Skipped - PDF {} does not exist", fileName);
            }
        }
    }

    private boolean exportMatches(List<ParserResult> loaded) {
        String[] data = cli.getExportMatches().split(",");
        String searchTerm = data[0].replace("\\$", " "); // enables blanks within the search term:
        // $ stands for a blank
        ParserResult pr = loaded.getLast();
        BibDatabaseContext databaseContext = pr.getDatabaseContext();

        SearchPreferences searchPreferences = cliPreferences.getSearchPreferences();
        SearchQuery query = new SearchQuery(searchTerm, searchPreferences.getSearchFlags());

        List<BibEntry> matches;
        try {
            // extract current thread task executor from indexManager
            matches = new DatabaseSearcher(query, databaseContext, new CurrentThreadTaskExecutor(), cliPreferences, Injector.instantiateModelOrService(PostgreServer.class)).getMatches();
        } catch (IOException e) {
            LOGGER.error("Error occurred when searching", e);
            return false;
        }

        // export matches
        if (!matches.isEmpty()) {
            String formatName;

            // read in the export format, take default format if no format entered
            switch (data.length) {
                case 3 -> formatName = data[2];
                case 2 ->
                        // default exporter: bib file
                        formatName = "bib";
                default -> {
                    System.err.println(Localization.lang("Output file missing").concat(". \n \t ")
                                                   .concat(Localization.lang("Usage")).concat(": ") + JabKitCliOptions.getExportMatchesSyntax());
                    return false;
                }
            }

            if ("bib".equals(formatName)) {
                // output a bib file as default or if
                // provided exportFormat is "bib"
                saveDatabase(new BibDatabase(matches), data[1]);
                LOGGER.debug("Finished export");
            } else {
                // export new database
                ExporterFactory exporterFactory = ExporterFactory.create(cliPreferences);
                Optional<Exporter> exporter = exporterFactory.getExporterByName(formatName);
                if (exporter.isEmpty()) {
                    System.err.println(Localization.lang("Unknown export format %0", formatName));
                } else {
                    // We have an TemplateExporter instance:
                    try {
                        System.out.println(Localization.lang("Exporting %0", data[1]));
                        exporter.get().export(
                                databaseContext,
                                Path.of(data[1]),
                                matches,
                                List.of(),
                                Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
                    } catch (Exception ex) {
                        System.err.println(Localization.lang("Could not export file '%0' (reason: %1)", data[1], Throwables.getStackTraceAsString(ex)));
                    }
                }
            }
        } else {
            System.err.println(Localization.lang("No search matches."));
        }
        return true;
    }

    private void doAuxImport(List<ParserResult> loaded) {
        boolean usageMsg;

        if (!loaded.isEmpty()) {
            usageMsg = generateAux(loaded, cli.getAuxImport().split(","));
        } else {
            usageMsg = true;
        }

        if (usageMsg) {
            System.out.println(Localization.lang("no base-BibTeX-file specified!"));
            System.out.println(Localization.lang("usage") + " :");
            System.out.println("jabref --aux infile[.aux],outfile[.bib] base-BibTeX-file");
        }
    }

    /**
     * @return List of opened files (could be .bib, but also other formats). May also contain error results.
     */
    private List<ParserResult> importAndOpenFiles() {
        List<ParserResult> loaded = new ArrayList<>();
        List<String> toImport = new ArrayList<>();
        if (!cli.getLeftOver().isEmpty()) {
            for (String aLeftOver : cli.getLeftOver()) {
                // Leftover arguments that have a "bib" extension are interpreted as
                // BIB files to open. Other files, and files that could not be opened
                // as bib, we try to import instead.
                boolean bibExtension = aLeftOver.toLowerCase(Locale.ENGLISH).endsWith("bib");

                ParserResult pr = new ParserResult();
                if (bibExtension) {
                    try {
                        pr = OpenDatabase.loadDatabase(
                                Path.of(aLeftOver),
                                cliPreferences.getImportFormatPreferences(),
                                fileUpdateMonitor);
                        // In contrast to org.jabref.gui.LibraryTab.onDatabaseLoadingSucceed, we do not execute OpenDatabaseAction.performPostOpenActions(result, dialogService);
                    } catch (IOException ex) {
                        pr = ParserResult.fromError(ex);
                        LOGGER.error("Error opening file '{}'", aLeftOver, ex);
                    }
                }

                if (!bibExtension || (pr.isEmpty())) {
                    // We will try to import this file. Normally we
                    // will import it into a new tab, but if this import has
                    // been initiated by another instance through the remote
                    // listener, we will instead import it into the current library.
                    // This will enable easy integration with web browsers that can
                    // open a reference file in JabRef.
                    if (startupMode == Mode.INITIAL_START) {
                        toImport.add(aLeftOver);
                    } else {
                        loaded.add(importToOpenBase(aLeftOver).orElse(new ParserResult()));
                    }
                } else {
                    loaded.add(pr);
                }
            }
        }

        if (cli.isFileImport()) {
            toImport.add(cli.getFileImport());
        }

        for (String filenameString : toImport) {
            importFile(filenameString).ifPresent(loaded::add);
        }

        if (cli.isBibtexImport()) {
            importBibtexToOpenBase(cli.getBibtexImport(), cliPreferences.getImportFormatPreferences()).ifPresent(loaded::add);
        }

        return loaded;
    }

    /**
     * Generates a new library being a subset of the given library
     *
     * @param loaded The library used as base
     * @param data [0]: the .aux file; [1]: the target .bib file
     */
    private boolean generateAux(List<ParserResult> loaded, String[] data) {
        if (data.length == 2) {
            ParserResult pr = loaded.getFirst();
            AuxCommandLine acl = new AuxCommandLine(data[0], pr.getDatabase());
            BibDatabase newBase = acl.perform();

            boolean notSavedMsg = false;

            // write an output, if something could be resolved
            if ((newBase != null) && newBase.hasEntries()) {
                String subName = StringUtil.getCorrectFileName(data[1], "bib");
                saveDatabase(newBase, subName);
                notSavedMsg = true;
            }

            if (!notSavedMsg) {
                System.out.println(Localization.lang("no library generated"));
            }
            return false;
        } else {
            return true;
        }
    }

    private void saveDatabase(BibDatabase newBase, String subName) {
        try {
            System.out.println(Localization.lang("Saving") + ": " + subName);
            try (AtomicFileWriter fileWriter = new AtomicFileWriter(Path.of(subName), StandardCharsets.UTF_8)) {
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

    private void exportFile(List<ParserResult> loaded, String[] data) {
        if (data.length == 1) {
            // This signals that the latest import should be stored in BibTeX
            // format to the given file.
            if (!loaded.isEmpty()) {
                ParserResult pr = loaded.getLast();
                if (!pr.isInvalid()) {
                    saveDatabase(pr.getDatabase(), data[0]);
                }
            } else {
                System.err.println(Localization.lang("The output option depends on a valid import option."));
            }
        } else if (data.length == 2) {
            // This signals that the latest import should be stored in the given
            // format to the given file.
            ParserResult parserResult = loaded.getLast();

            Path path = parserResult.getPath().get().toAbsolutePath();
            BibDatabaseContext databaseContext = parserResult.getDatabaseContext();
            databaseContext.setDatabasePath(path);
            List<Path> fileDirForDatabase = databaseContext
                    .getFileDirectories(cliPreferences.getFilePreferences());
            System.out.println(Localization.lang("Exporting %0", data[0]));
            ExporterFactory exporterFactory = ExporterFactory.create(cliPreferences);
            Optional<Exporter> exporter = exporterFactory.getExporterByName(data[1]);
            if (exporter.isEmpty()) {
                System.err.println(Localization.lang("Unknown export format %0", data[1]));
            } else {
                // We have an exporter:
                try {
                    exporter.get().export(
                            parserResult.getDatabaseContext(),
                            Path.of(data[0]),
                            parserResult.getDatabaseContext().getDatabase().getEntries(),
                            fileDirForDatabase,
                            Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
                } catch (Exception ex) {
                    System.err.println(Localization.lang("Could not export file '%0' (reason: %1)", data[0], Throwables.getStackTraceAsString(ex)));
                }
            }
        }
    }

    private void importPreferences() {
        try {
            cliPreferences.importPreferences(Path.of(cli.getPreferencesImport()));
            Injector.setModelOrService(BibEntryTypesManager.class, cliPreferences.getCustomEntryTypesRepository());
        } catch (JabRefException ex) {
            LOGGER.error("Cannot import preferences", ex);
        }
    }

    private void resetPreferences(String value) {
        if ("all".equals(value.trim())) {
            try {
                System.out.println(Localization.lang("Setting all preferences to default values."));
                cliPreferences.clear();
                new SharedDatabasePreferences().clear();
            } catch (BackingStoreException e) {
                System.err.println(Localization.lang("Unable to clear preferences."));
                LOGGER.error("Unable to clear preferences", e);
            }
        } else {
            String[] keys = value.split(",");
            for (String key : keys) {
                try {
                    cliPreferences.deleteKey(key.trim());
                    System.out.println(Localization.lang("Resetting preference key '%0'", key.trim()));
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private void regenerateCitationKeys(List<ParserResult> loaded) {
        for (ParserResult parserResult : loaded) {
            BibDatabase database = parserResult.getDatabase();

            LOGGER.info(Localization.lang("Regenerating citation keys according to metadata"));

            CitationKeyGenerator keyGenerator = new CitationKeyGenerator(
                    parserResult.getDatabaseContext(),
                    cliPreferences.getCitationKeyPatternPreferences());
            for (BibEntry entry : database.getEntries()) {
                keyGenerator.generateAndSetKey(entry);
            }
        }
    }

    /**
     * Run an entry fetcher from the command line.
     *
     * @param fetchCommand A string containing both the name of the fetcher to use and the search query, separated by a :
     * @return A parser result containing the entries fetched or null if an error occurred.
     */
    private Optional<ParserResult> fetch(String fetchCommand) {
        if ((fetchCommand == null) || !fetchCommand.contains(":")) {
            System.out.println(Localization.lang("Expected syntax for --fetch='<name of fetcher>:<query>'"));
            System.out.println(Localization.lang("The following fetchers are available:"));
            return Optional.empty();
        }

        String[] split = fetchCommand.split(":");
        String engine = split[0];
        String query = split[1];

        Set<SearchBasedFetcher> fetchers = WebFetchers.getSearchBasedFetchers(
                cliPreferences.getImportFormatPreferences(),
                cliPreferences.getImporterPreferences());
        Optional<SearchBasedFetcher> selectedFetcher = fetchers.stream()
                                                               .filter(fetcher -> fetcher.getName().equalsIgnoreCase(engine))
                                                               .findFirst();
        if (selectedFetcher.isEmpty()) {
            System.out.println(Localization.lang("Could not find fetcher '%0'", engine));

            System.out.println(Localization.lang("The following fetchers are available:"));
            fetchers.forEach(fetcher -> System.out.println("  " + fetcher.getName()));

            return Optional.empty();
        } else {
            System.out.println(Localization.lang("Running query '%0' with fetcher '%1'.", query, engine));
            System.out.print(Localization.lang("Please wait..."));
            try {
                List<BibEntry> matches = selectedFetcher.get().performSearch(query);
                if (matches.isEmpty()) {
                    System.out.println("\r" + Localization.lang("No results found."));
                    return Optional.empty();
                } else {
                    System.out.println("\r" + Localization.lang("Found %0 results.", String.valueOf(matches.size())));
                    return Optional.of(new ParserResult(matches));
                }
            } catch (FetcherException e) {
                LOGGER.error("Error while fetching", e);
                return Optional.empty();
            }
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

    protected static String alignStringTable(List<Pair<String, String>> table) {
        StringBuilder sb = new StringBuilder();

        int maxLength = table.stream()
                             .mapToInt(pair -> Objects.requireNonNullElse(pair.getKey(), "").length())
                             .max().orElse(0);

        for (Pair<String, String> pair : table) {
            int padding = Math.max(0, maxLength - pair.getKey().length());
            sb.append(WRAPPED_LINE_PREFIX);
            sb.append(pair.getKey());

            sb.append(StringUtil.repeatSpaces(padding));

            sb.append(STRING_TABLE_DELIMITER);
            sb.append(pair.getValue());
            sb.append(OS.NEWLINE);
        }

        return sb.toString();
    }
}
