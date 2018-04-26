package org.jabref.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.gui.externalfiles.AutoSetLinks;
import org.jabref.gui.importer.fetcher.EntryFetcher;
import org.jabref.gui.importer.fetcher.EntryFetchers;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.FileSaveSession;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.SaveSession;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.logging.JabRefLogger;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.search.DatabaseSearcher;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.Defaults;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.SearchPreferences;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArgumentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentProcessor.class);
    private final JabRefCLI cli;
    private final List<ParserResult> parserResults;
    private final Mode startupMode;
    private boolean noGUINeeded;

    public ArgumentProcessor(String[] args, Mode startupMode) {
        cli = new JabRefCLI(args);
        this.startupMode = startupMode;
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

    private static Optional<ParserResult> importBibtexToOpenBase(String argument) {
        BibtexParser parser = new BibtexParser(Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor());
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
                file = Paths.get(address);
            } else {
                file = Paths.get(address.replace("~", System.getProperty("user.home")));
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

                ImportFormatReader.UnknownFormatImport importResult = Globals.IMPORT_FORMAT_READER.importUnknownFormat(file, Globals.getFileUpdateMonitor());

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
            JabRefLogger.setDebug();
        }

        if ((startupMode == Mode.INITIAL_START) && cli.isShowVersion()) {
            cli.displayVersion();
        }

        if ((startupMode == Mode.INITIAL_START) && cli.isHelp()) {
            cli.printUsage();
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

        if (cli.isGenerateBibtexKeys()) {
            regenerateBibtexKeys(loaded);
        }

        if (cli.isAutomaticallySetFileLinks()) {
            automaticallySetFileLinks(loaded);
        }

        if (cli.isFileExport()) {
            if (!loaded.isEmpty()) {
                exportFile(loaded, cli.getFileExport().split(","));
            } else {
                System.err.println(Localization.lang("The output option depends on a valid import option."));
            }
        }

        LOGGER.debug("Finished export");

        if (cli.isPreferencesExport()) {
            try {
                Globals.prefs.exportPreferences(cli.getPreferencesExport());
            } catch (JabRefException ex) {
                LOGGER.error("Cannot export preferences", ex);
            }
        }

        if (!cli.isBlank() && cli.isAuxImport()) {
            doAuxImport(loaded);
        }

        return loaded;
    }

    private boolean exportMatches(List<ParserResult> loaded) {
        String[] data = cli.getExportMatches().split(",");
        String searchTerm = data[0].replace("\\$", " "); //enables blanks within the search term:
        //$ stands for a blank
        ParserResult pr = loaded.get(loaded.size() - 1);
        BibDatabaseContext databaseContext = pr.getDatabaseContext();
        BibDatabase dataBase = pr.getDatabase();

        SearchPreferences searchPreferences = new SearchPreferences(Globals.prefs);
        SearchQuery query = new SearchQuery(searchTerm, searchPreferences.isCaseSensitive(),
                searchPreferences.isRegularExpression());
        List<BibEntry> matches = new DatabaseSearcher(query, dataBase).getMatches();

        //export matches
        if (!matches.isEmpty()) {
            String formatName;

            //read in the export format, take default format if no format entered
            switch (data.length) {
                case 3:
                    formatName = data[2];
                    break;
                case 2:
                    //default exporter: HTML table (with Abstract & BibTeX)
                    formatName = "tablerefsabsbib";
                    break;
                default:
                    System.err.println(Localization.lang("Output file missing").concat(". \n \t ")
                            .concat(Localization.lang("Usage")).concat(": ") + JabRefCLI.getExportMatchesSyntax());
                    noGUINeeded = true;
                    return false;
            }

            //export new database
            Optional<Exporter> exporter = Globals.exportFactory.getExporterByName(formatName);
            if (!exporter.isPresent()) {
                System.err.println(Localization.lang("Unknown export format") + ": " + formatName);
            } else {
                // We have an TemplateExporter instance:
                try {
                    System.out.println(Localization.lang("Exporting") + ": " + data[1]);
                    exporter.get().export(databaseContext, Paths.get(data[1]),
                            databaseContext.getMetaData().getEncoding().orElse(Globals.prefs.getDefaultEncoding()),
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
                    pr = OpenDatabase.loadDatabase(aLeftOver, Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor());
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
            importBibtexToOpenBase(cli.getBibtexImport()).ifPresent(loaded::add);
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

                try {
                    System.out.println(Localization.lang("Saving") + ": " + subName);
                    SavePreferences prefs = Globals.prefs.loadForSaveFromPreferences();
                    BibDatabaseWriter<SaveSession> databaseWriter = new BibtexDatabaseWriter<>(FileSaveSession::new);
                    Defaults defaults = new Defaults(Globals.prefs.getDefaultBibDatabaseMode());
                    SaveSession session = databaseWriter.saveDatabase(new BibDatabaseContext(newBase, defaults), prefs);

                    // Show just a warning message if encoding did not work for all characters:
                    if (!session.getWriter().couldEncodeAll()) {
                        System.err.println(Localization.lang("Warning") + ": "
                                + Localization.lang(
                                "The chosen encoding '%0' could not encode the following characters:",
                                session.getEncoding().displayName())
                                + " " + session.getWriter().getProblemCharacters());
                    }
                    session.commit(subName);
                } catch (SaveException ex) {
                    System.err.println(Localization.lang("Could not save file.") + "\n" + ex.getLocalizedMessage());
                }

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

    private void exportFile(List<ParserResult> loaded, String[] data) {
        if (data.length == 1) {
            // This signals that the latest import should be stored in BibTeX
            // format to the given file.
            if (!loaded.isEmpty()) {
                ParserResult pr = loaded.get(loaded.size() - 1);
                if (!pr.isInvalid()) {
                    try {
                        System.out.println(Localization.lang("Saving") + ": " + data[0]);
                        SavePreferences prefs = Globals.prefs.loadForSaveFromPreferences();
                        Defaults defaults = new Defaults(Globals.prefs.getDefaultBibDatabaseMode());
                        BibDatabaseWriter<SaveSession> databaseWriter = new BibtexDatabaseWriter<>(
                                FileSaveSession::new);
                        SaveSession session = databaseWriter.saveDatabase(
                                new BibDatabaseContext(pr.getDatabase(), pr.getMetaData(), defaults), prefs);

                        // Show just a warning message if encoding did not work for all characters:
                        if (!session.getWriter().couldEncodeAll()) {
                            System.err.println(Localization.lang("Warning") + ": "
                                    + Localization.lang(
                                    "The chosen encoding '%0' could not encode the following characters:",
                                    session.getEncoding().displayName())
                                    + " " + session.getWriter().getProblemCharacters());
                        }
                        session.commit(data[0]);
                    } catch (SaveException ex) {
                        System.err.println(Localization.lang("Could not save file.") + "\n" + ex.getLocalizedMessage());
                    }
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
            File theFile = pr.getFile().get();
            if (!theFile.isAbsolute()) {
                theFile = theFile.getAbsoluteFile();
            }
            BibDatabaseContext databaseContext = pr.getDatabaseContext();
            databaseContext.setDatabaseFile(theFile);
            Globals.prefs.fileDirForDatabase = databaseContext
                    .getFileDirectories(Globals.prefs.getFileDirectoryPreferences());
            System.out.println(Localization.lang("Exporting") + ": " + data[0]);
            Optional<Exporter> exporter = Globals.exportFactory.getExporterByName(data[1]);
            if (!exporter.isPresent()) {
                System.err.println(Localization.lang("Unknown export format") + ": " + data[1]);
            } else {
                // We have an exporter:
                try {
                    exporter.get().export(pr.getDatabaseContext(), Paths.get(data[0]),
                            pr.getDatabaseContext().getMetaData().getEncoding()
                                    .orElse(Globals.prefs.getDefaultEncoding()),
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
            Globals.prefs.importPreferences(cli.getPreferencesImport());
            EntryTypes.loadCustomEntryTypes(Globals.prefs.loadCustomEntryTypes(BibDatabaseMode.BIBTEX),
                    Globals.prefs.loadCustomEntryTypes(BibDatabaseMode.BIBLATEX));
            Map<String, TemplateExporter> customExporters = Globals.prefs.customExports.getCustomExportFormats(Globals.prefs,
                    Globals.journalAbbreviationLoader);
            LayoutFormatterPreferences layoutPreferences = Globals.prefs
                    .getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
            SavePreferences savePreferences = Globals.prefs.loadForExportFromPreferences();
            XmpPreferences xmpPreferences = Globals.prefs.getXMPPreferences();
            Globals.exportFactory = ExporterFactory.create(customExporters, layoutPreferences, savePreferences, xmpPreferences);
        } catch (JabRefException ex) {
            LOGGER.error("Cannot import preferences", ex);
        }
    }

    private void resetPreferences(String value) {
        if ("all".equals(value.trim())) {
            try {
                System.out.println(Localization.lang("Setting all preferences to default values."));
                Globals.prefs.clear();
                new SharedDatabasePreferences().clear();
            } catch (BackingStoreException e) {
                System.err.println(Localization.lang("Unable to clear preferences."));
                LOGGER.error("Unable to clear preferences", e);
            }
        } else {
            String[] keys = value.split(",");
            for (String key : keys) {
                if (Globals.prefs.hasKey(key.trim())) {
                    System.out.println(Localization.lang("Resetting preference key '%0'", key.trim()));
                    Globals.prefs.clear(key.trim());
                } else {
                    System.out.println(Localization.lang("Unknown preference key '%0'", key.trim()));
                }
            }
        }
    }

    private void automaticallySetFileLinks(List<ParserResult> loaded) {
        for (ParserResult parserResult : loaded) {
            BibDatabase database = parserResult.getDatabase();
            LOGGER.info(Localization.lang("Automatically setting file links"));
            AutoSetLinks.autoSetLinks(database.getEntries(), parserResult.getDatabaseContext());
        }
    }

    private void regenerateBibtexKeys(List<ParserResult> loaded) {
        for (ParserResult parserResult : loaded) {
            BibDatabase database = parserResult.getDatabase();

            LOGGER.info(Localization.lang("Regenerating BibTeX keys according to metadata"));

            BibtexKeyGenerator keyGenerator = new BibtexKeyGenerator(parserResult.getDatabaseContext(), Globals.prefs.getBibtexKeyPatternPreferences());
            for (BibEntry entry : database.getEntries()) {
                keyGenerator.generateAndSetKey(entry);
            }
        }
    }

    /**
     * Run an entry fetcher from the command line.
     * <p>
     * Note that this only works headlessly if the EntryFetcher does not show any GUI.
     *
     * @param fetchCommand A string containing both the fetcher to use (id of EntryFetcherExtension minus Fetcher) and
     *                     the search query, separated by a :
     * @return A parser result containing the entries fetched or null if an error occurred.
     */
    private Optional<ParserResult> fetch(String fetchCommand) {

        if ((fetchCommand == null) || !fetchCommand.contains(":") || (fetchCommand.split(":").length != 2)) {
            System.out.println(Localization.lang("Expected syntax for --fetch='<name of fetcher>:<query>'"));
            System.out.println(Localization.lang("The following fetchers are available:"));
            return Optional.empty();
        }

        String[] split = fetchCommand.split(":");
        String engine = split[0];

        EntryFetchers fetchers = new EntryFetchers(Globals.journalAbbreviationLoader);
        EntryFetcher fetcher = null;
        for (EntryFetcher e : fetchers.getEntryFetchers()) {
            if (engine.equalsIgnoreCase(e.getClass().getSimpleName().replace("Fetcher", ""))) {
                fetcher = e;
            }
        }

        if (fetcher == null) {
            System.out.println(Localization.lang("Could not find fetcher '%0'", engine));
            System.out.println(Localization.lang("The following fetchers are available:"));

            for (EntryFetcher e : fetchers.getEntryFetchers()) {
                System.out.println(
                        "  " + e.getClass().getSimpleName().replace("Fetcher", "").toLowerCase(Locale.ENGLISH));
            }
            return Optional.empty();
        }

        String query = split[1];
        System.out.println(Localization.lang("Running query '%0' with fetcher '%1'.", query, engine) + " "
                + Localization.lang("Please wait..."));
        Collection<BibEntry> result = new ImportInspectionCommandLine().query(query, fetcher);

        if (result.isEmpty()) {
            System.out.println(
                    Localization.lang("Query '%0' with fetcher '%1' did not return any results.", query, engine));
            return Optional.empty();
        }

        return Optional.of(new ParserResult(result));
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
