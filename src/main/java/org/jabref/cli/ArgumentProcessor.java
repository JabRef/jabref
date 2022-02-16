package org.jabref.cli;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.prefs.BackingStoreException;

import org.jabref.gui.Globals;
import org.jabref.gui.externalfiles.AutoSetFileLinksUtil;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.JabRefException;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.exporter.XmpPdfExporter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.search.DatabaseSearcher;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArgumentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentProcessor.class);
    private final JabRefCLI cli;
    private final List<ParserResult> parserResults;
    private final Mode startupMode;
    private final PreferencesService preferencesService;
    private boolean noGUINeeded;

    public ArgumentProcessor(String[] args, Mode startupMode, PreferencesService preferencesService) throws org.apache.commons.cli.ParseException {
        cli = new JabRefCLI(args);
        this.startupMode = startupMode;
        this.preferencesService = preferencesService;
        parserResults = processArguments();
    }

    /**
     * Will open a file (like importFile), but will also request JabRef to focus on this database
     *
     * @param argument See importFile.
     * @return ParserResult with setToOpenTab(true)
     */
    private static Optional<ParserResult> importToOpenBase(String argument) {
        Optional<ParserResult> result = importFile(argument);

        result.ifPresent(ParserResult::setToOpenTab);

        return result;
    }

    private static Optional<ParserResult> importBibtexToOpenBase(String argument, ImportFormatPreferences importFormatPreferences) {
        BibtexParser parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
        try {
            List<BibEntry> entries = parser.parseEntries(argument);
            ParserResult result = new ParserResult(entries);
            result.setToOpenTab();
            return Optional.of(result);
        } catch (ParseException e) {
            System.err.println(Localization.lang("Error occurred when parsing entry") + ": " + e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    private static Optional<ParserResult> importFile(String argument) {
        String[] data = argument.split(",");

        String address = data[0];
        Path file;
        if (address.startsWith("http://") || address.startsWith("https://") || address.startsWith("ftp://")) {
            // Download web resource to temporary file
            try {
                file = new URLDownload(address).toTemporaryFile();
            } catch (IOException e) {
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
            OutputPrinter printer = new SystemOutputPrinter();
            if (result.hasWarnings()) {
                printer.showMessage(result.getErrorMessage());
            }
        });
        return importResult;
    }

    private static Optional<ParserResult> importFile(Path file, String importFormat) {
        try {
            if (!"*".equals(importFormat)) {
                System.out.println(Localization.lang("Importing") + ": " + file);
                ParserResult result = Globals.IMPORT_FORMAT_READER.importFromFile(importFormat, file);
                return Optional.of(result);
            } else {
                // * means "guess the format":
                System.out.println(Localization.lang("Importing in unknown format") + ": " + file);

                ImportFormatReader.UnknownFormatImport importResult = Globals.IMPORT_FORMAT_READER.importUnknownFormat(file, new DummyFileUpdateMonitor());

                System.out.println(Localization.lang("Format used") + ": " + importResult.format);
                return Optional.of(importResult.parserResult);
            }
        } catch (ImportException ex) {
            System.err
                    .println(Localization.lang("Error opening file") + " '" + file + "': " + ex.getLocalizedMessage());
            return Optional.empty();
        }
    }

    public List<ParserResult> getParserResults() {
        return parserResults;
    }

    public boolean hasParserResults() {
        return !parserResults.isEmpty();
    }

    private List<ParserResult> processArguments() {

        if (!cli.isBlank() && cli.isDebugLogging()) {
            System.err.println("use java property -Dtinylog.level=debug");
        }

        if ((startupMode == Mode.INITIAL_START) && cli.isShowVersion()) {
            cli.displayVersion();
        }

        if ((startupMode == Mode.INITIAL_START) && cli.isHelp()) {
            JabRefCLI.printUsage();
            noGUINeeded = true;
            return Collections.emptyList();
        }

        // Check if we should reset all preferences to default values:
        if (cli.isPreferencesReset()) {
            resetPreferences(cli.getPreferencesReset());
        }

        // Check if we should import preferences from a file:
        if (cli.isPreferencesImport()) {
            importPreferences();
        }

        // List to put imported/loaded database(s) in.
        List<ParserResult> loaded = importAndOpenFiles();

        if (!cli.isBlank() && cli.isFetcherEngine()) {
            fetch(cli.getFetcherEngine()).ifPresent(loaded::add);
        }

        if (cli.isExportMatches()) {
            if (!loaded.isEmpty()) {
                if (!exportMatches(loaded)) {
                    return Collections.emptyList();
                }
            } else {
                System.err.println(Localization.lang("The output option depends on a valid input option."));
            }
        }

        if (cli.isGenerateCitationKeys()) {
            regenerateCitationKeys(loaded);
        }

        if (cli.isAutomaticallySetFileLinks()) {
            automaticallySetFileLinks(loaded);
        }

        if ((cli.isWriteXMPtoPdf() && cli.isEmbeddBibfileInPdf()) || (cli.isWriteMetadatatoPdf() && (cli.isWriteXMPtoPdf() || cli.isEmbeddBibfileInPdf()))) {
            System.err.println("Give only one of [writeXMPtoPdf, embeddBibfileInPdf, writeMetadatatoPdf]");
        }

        if (cli.isWriteMetadatatoPdf() || cli.isWriteXMPtoPdf() || cli.isEmbeddBibfileInPdf()) {
            if (!loaded.isEmpty()) {
                writeMetadatatoPdf(loaded,
                        cli.getWriteMetadatatoPdf(),
                        preferencesService.getGeneralPreferences().getDefaultEncoding(),
                        preferencesService.getXmpPreferences(),
                        preferencesService.getFilePreferences(),
                        preferencesService.getGeneralPreferences().getDefaultBibDatabaseMode(),
                        Globals.entryTypesManager,
                        preferencesService.getFieldWriterPreferences(),
                        cli.isWriteXMPtoPdf() || cli.isWriteMetadatatoPdf(),
                        cli.isEmbeddBibfileInPdf() || cli.isWriteMetadatatoPdf());
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
                preferencesService.exportPreferences(Path.of(cli.getPreferencesExport()));
            } catch (JabRefException ex) {
                LOGGER.error("Cannot export preferences", ex);
            }
        }

        if (!cli.isBlank() && cli.isAuxImport()) {
            doAuxImport(loaded);
        }

        return loaded;
    }

    private void writeMetadatatoPdf(List<ParserResult> loaded, String filesAndCitekeys, Charset encoding, XmpPreferences xmpPreferences, FilePreferences filePreferences, BibDatabaseMode databaseMode, BibEntryTypesManager entryTypesManager, FieldWriterPreferences fieldWriterPreferences, boolean writeXMP, boolean embeddBibfile) {
        if (loaded.isEmpty()) {
            LOGGER.error("The write xmp option depends on a valid import option.");
            return;
        }
        ParserResult pr = loaded.get(loaded.size() - 1);
        BibDatabaseContext databaseContext = pr.getDatabaseContext();
        BibDatabase dataBase = pr.getDatabase();

        XmpPdfExporter xmpPdfExporter = new XmpPdfExporter(xmpPreferences);
        EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter = new EmbeddedBibFilePdfExporter(databaseMode, entryTypesManager, fieldWriterPreferences);

        if ("all".equals(filesAndCitekeys)) {
            for (BibEntry entry : dataBase.getEntries()) {
                writeMetadatatoPDFsOfEntry(databaseContext, entry.getCitationKey().orElse("<no cite key defined>"), entry, encoding, filePreferences, xmpPdfExporter, embeddedBibFilePdfExporter, writeXMP, embeddBibfile);
            }
            return;
        }

        Vector<String> citeKeys = new Vector<>();
        Vector<String> pdfs = new Vector<>();
        for (String fileOrCiteKey : filesAndCitekeys.split(",")) {
            if (fileOrCiteKey.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                pdfs.add(fileOrCiteKey);
            } else {
                citeKeys.add(fileOrCiteKey);
            }
        }

        writeMetadatatoPdfByCitekey(databaseContext, dataBase, citeKeys, encoding, filePreferences, xmpPdfExporter, embeddedBibFilePdfExporter, writeXMP, embeddBibfile);
        writeMetadatatoPdfByFileNames(databaseContext, dataBase, pdfs, encoding, filePreferences, xmpPdfExporter, embeddedBibFilePdfExporter, writeXMP, embeddBibfile);

    }

    private void writeMetadatatoPDFsOfEntry(BibDatabaseContext databaseContext, String citeKey, BibEntry entry, Charset encoding, FilePreferences filePreferences, XmpPdfExporter xmpPdfExporter, EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter, boolean writeXMP, boolean embeddBibfile) {
        try {
            if (writeXMP) {
                if (xmpPdfExporter.exportToAllFilesOfEntry(databaseContext, encoding, filePreferences, entry, List.of(entry))) {
                    System.out.printf("Successfully written XMP metadata on at least one linked file of %s%n", citeKey);
                } else {
                    System.err.printf("Cannot write XMP metadata on any linked files of %s. Make sure there is at least one linked file and the path is correct.%n", citeKey);
                }
            }
            if (embeddBibfile) {
                if (embeddedBibFilePdfExporter.exportToAllFilesOfEntry(databaseContext, encoding, filePreferences, entry, List.of(entry))) {
                    System.out.printf("Successfully embedded metadata on at least one linked file of %s%n", citeKey);
                } else {
                    System.out.printf("Cannot embedd metadata on any linked files of %s. Make sure there is at least one linked file and the path is correct.%n", citeKey);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed writing metadata on a linked file of {}.", citeKey);
        }
    }

    private void writeMetadatatoPdfByCitekey(BibDatabaseContext databaseContext, BibDatabase dataBase, Vector<String> citeKeys, Charset encoding, FilePreferences filePreferences, XmpPdfExporter xmpPdfExporter, EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter, boolean writeXMP, boolean embeddBibfile) {
        for (String citeKey : citeKeys) {
            List<BibEntry> bibEntryList = dataBase.getEntriesByCitationKey(citeKey);
            if (bibEntryList.isEmpty()) {
                System.err.printf("Skipped - Cannot find %s in library.%n", citeKey);
                continue;
            }
            for (BibEntry entry : bibEntryList) {
                writeMetadatatoPDFsOfEntry(databaseContext, citeKey, entry, encoding, filePreferences, xmpPdfExporter, embeddedBibFilePdfExporter, writeXMP, embeddBibfile);
            }
        }
    }

    private void writeMetadatatoPdfByFileNames(BibDatabaseContext databaseContext, BibDatabase dataBase, Vector<String> fileNames, Charset encoding, FilePreferences filePreferences, XmpPdfExporter xmpPdfExporter, EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter, boolean writeXMP, boolean embeddBibfile) {
        for (String fileName : fileNames) {
            Path filePath = Path.of(fileName);
            if (!filePath.isAbsolute()) {
                filePath = FileHelper.find(fileName, databaseContext.getFileDirectories(filePreferences)).orElse(FileHelper.find(fileName, List.of(Path.of("").toAbsolutePath())).orElse(filePath));
            }
            if (Files.exists(filePath)) {
                try {
                    if (writeXMP) {
                        if (xmpPdfExporter.exportToFileByPath(databaseContext, dataBase, encoding, filePreferences, filePath)) {
                            System.out.printf("Successfully written XMP metadata of at least one entry to %s%n", fileName);
                        } else {
                            System.out.printf("File %s is not linked to any entry in database.%n", fileName);
                        }
                    }
                    if (embeddBibfile) {
                        if (embeddedBibFilePdfExporter.exportToFileByPath(databaseContext, dataBase, encoding, filePreferences, filePath)) {
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
        ParserResult pr = loaded.get(loaded.size() - 1);
        BibDatabaseContext databaseContext = pr.getDatabaseContext();
        BibDatabase dataBase = pr.getDatabase();

        SearchPreferences searchPreferences = preferencesService.getSearchPreferences();
        SearchQuery query = new SearchQuery(searchTerm, searchPreferences.getSearchFlags());
        List<BibEntry> matches = new DatabaseSearcher(query, dataBase).getMatches();

        // export matches
        if (!matches.isEmpty()) {
            String formatName;

            // read in the export format, take default format if no format entered
            switch (data.length) {
                case 3 -> formatName = data[2];
                case 2 ->
                        // default exporter: HTML table (with Abstract & BibTeX)
                        formatName = "tablerefsabsbib";
                default -> {
                    System.err.println(Localization.lang("Output file missing").concat(". \n \t ")
                                                   .concat(Localization.lang("Usage")).concat(": ") + JabRefCLI.getExportMatchesSyntax());
                    noGUINeeded = true;
                    return false;
                }
            }

            // export new database
            Optional<Exporter> exporter = Globals.exportFactory.getExporterByName(formatName);
            if (exporter.isEmpty()) {
                System.err.println(Localization.lang("Unknown export format") + ": " + formatName);
            } else {
                // We have an TemplateExporter instance:
                try {
                    System.out.println(Localization.lang("Exporting") + ": " + data[1]);
                    exporter.get().export(databaseContext, Path.of(data[1]),
                            databaseContext.getMetaData().getEncoding().orElse(preferencesService.getGeneralPreferences().getDefaultEncoding()),
                            matches);
                } catch (Exception ex) {
                    System.err.println(Localization.lang("Could not export file") + " '" + data[1] + "': "
                            + Throwables.getStackTraceAsString(ex));
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
            System.out.println(Localization.lang("no base-BibTeX-file specified") + "!");
            System.out.println(Localization.lang("usage") + " :");
            System.out.println("jabref --aux infile[.aux],outfile[.bib] base-BibTeX-file");
        }
    }

    private List<ParserResult> importAndOpenFiles() {
        List<ParserResult> loaded = new ArrayList<>();
        List<String> toImport = new ArrayList<>();
        if (!cli.isBlank() && (!cli.getLeftOver().isEmpty())) {
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
                                preferencesService.getGeneralPreferences(),
                                preferencesService.getImportFormatPreferences(),
                                Globals.getFileUpdateMonitor());
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

        if (!cli.isBlank() && cli.isFileImport()) {
            toImport.add(cli.getFileImport());
        }

        for (String filenameString : toImport) {
            importFile(filenameString).ifPresent(loaded::add);
        }

        if (!cli.isBlank() && cli.isImportToOpenBase()) {
            importToOpenBase(cli.getImportToOpenBase()).ifPresent(loaded::add);
        }

        if (!cli.isBlank() && cli.isBibtexImport()) {
            importBibtexToOpenBase(cli.getBibtexImport(), preferencesService.getImportFormatPreferences()).ifPresent(loaded::add);
        }

        return loaded;
    }

    private boolean generateAux(List<ParserResult> loaded, String[] data) {
        if (data.length == 2) {
            ParserResult pr = loaded.get(0);
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
            GeneralPreferences generalPreferences = preferencesService.getGeneralPreferences();
            SavePreferences savePreferences = preferencesService.getSavePreferences();
            AtomicFileWriter fileWriter = new AtomicFileWriter(Path.of(subName), generalPreferences.getDefaultEncoding());
            BibWriter bibWriter = new BibWriter(fileWriter, OS.NEWLINE);
            BibDatabaseWriter databaseWriter = new BibtexDatabaseWriter(bibWriter, generalPreferences, savePreferences, Globals.entryTypesManager);
            databaseWriter.saveDatabase(new BibDatabaseContext(newBase));

            // Show just a warning message if encoding did not work for all characters:
            if (fileWriter.hasEncodingProblems()) {
                System.err.println(Localization.lang("Warning") + ": "
                        + Localization.lang(
                        "The chosen encoding '%0' could not encode the following characters:",
                        generalPreferences.getDefaultEncoding().displayName())
                        + " " + fileWriter.getEncodingProblems());
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
                ParserResult pr = loaded.get(loaded.size() - 1);
                if (!pr.isInvalid()) {
                    saveDatabase(pr.getDatabase(), data[0]);
                }
            } else {
                System.err.println(Localization.lang("The output option depends on a valid import option."));
            }
        } else if (data.length == 2) {
            // This signals that the latest import should be stored in the given
            // format to the given file.
            ParserResult pr = loaded.get(loaded.size() - 1);

            // Set the global variable for this database's file directory before exporting,
            // so formatters can resolve linked files correctly.
            // (This is an ugly hack!)
            Path path = pr.getPath().get().toAbsolutePath();
            BibDatabaseContext databaseContext = pr.getDatabaseContext();
            databaseContext.setDatabasePath(path);
            Globals.prefs.fileDirForDatabase = databaseContext
                    .getFileDirectories(preferencesService.getFilePreferences());
            System.out.println(Localization.lang("Exporting") + ": " + data[0]);
            Optional<Exporter> exporter = Globals.exportFactory.getExporterByName(data[1]);
            if (exporter.isEmpty()) {
                System.err.println(Localization.lang("Unknown export format") + ": " + data[1]);
            } else {
                // We have an exporter:
                try {
                    exporter.get().export(pr.getDatabaseContext(), Path.of(data[0]),
                            pr.getDatabaseContext().getMetaData().getEncoding()
                              .orElse(preferencesService.getGeneralPreferences().getDefaultEncoding()),
                            pr.getDatabaseContext().getDatabase().getEntries());
                } catch (Exception ex) {
                    System.err.println(Localization.lang("Could not export file") + " '" + data[0] + "': "
                            + Throwables.getStackTraceAsString(ex));
                }
            }
        }
    }

    private void importPreferences() {
        try {
            preferencesService.importPreferences(Path.of(cli.getPreferencesImport()));
            Globals.entryTypesManager.addCustomOrModifiedTypes(preferencesService.getBibEntryTypes(BibDatabaseMode.BIBTEX),
                    preferencesService.getBibEntryTypes(BibDatabaseMode.BIBLATEX));
            List<TemplateExporter> customExporters = preferencesService.getCustomExportFormats(Globals.journalAbbreviationRepository);
            LayoutFormatterPreferences layoutPreferences =
                    preferencesService.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository);
            SavePreferences savePreferences = preferencesService.getSavePreferencesForExport();
            XmpPreferences xmpPreferences = preferencesService.getXmpPreferences();
            BibDatabaseMode bibDatabaseMode = preferencesService.getGeneralPreferences().getDefaultBibDatabaseMode();
            Globals.exportFactory = ExporterFactory.create(customExporters, layoutPreferences, savePreferences, xmpPreferences, bibDatabaseMode, Globals.entryTypesManager);
        } catch (JabRefException ex) {
            LOGGER.error("Cannot import preferences", ex);
        }
    }

    private void resetPreferences(String value) {
        if ("all".equals(value.trim())) {
            try {
                System.out.println(Localization.lang("Setting all preferences to default values."));
                preferencesService.clear();
                new SharedDatabasePreferences().clear();
            } catch (BackingStoreException e) {
                System.err.println(Localization.lang("Unable to clear preferences."));
                LOGGER.error("Unable to clear preferences", e);
            }
        } else {
            String[] keys = value.split(",");
            for (String key : keys) {
                try {
                    preferencesService.deleteKey(key.trim());
                    System.out.println(Localization.lang("Resetting preference key '%0'", key.trim()));
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private void automaticallySetFileLinks(List<ParserResult> loaded) {
        for (ParserResult parserResult : loaded) {
            BibDatabase database = parserResult.getDatabase();
            LOGGER.info(Localization.lang("Automatically setting file links"));
            AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(
                    parserResult.getDatabaseContext(),
                    preferencesService.getFilePreferences(),
                    preferencesService.getAutoLinkPreferences(),
                    ExternalFileTypes.getInstance());
            util.linkAssociatedFiles(database.getEntries(), new NamedCompound(""));
        }
    }

    private void regenerateCitationKeys(List<ParserResult> loaded) {
        for (ParserResult parserResult : loaded) {
            BibDatabase database = parserResult.getDatabase();

            LOGGER.info(Localization.lang("Regenerating citation keys according to metadata"));

            CitationKeyGenerator keyGenerator = new CitationKeyGenerator(
                    parserResult.getDatabaseContext(),
                    preferencesService.getCitationKeyPatternPreferences());
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

        Set<SearchBasedFetcher> fetchers = WebFetchers.getSearchBasedFetchers(preferencesService.getImportFormatPreferences());
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

    public boolean isBlank() {
        return cli.isBlank();
    }

    public boolean shouldShutDown() {
        return cli.isDisableGui() || cli.isShowVersion() || noGUINeeded;
    }

    public enum Mode {
        INITIAL_START, REMOTE_START
    }
}
