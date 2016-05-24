package net.sf.jabref.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefException;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.BibDatabaseWriter;
import net.sf.jabref.exporter.ExportFormats;
import net.sf.jabref.exporter.IExportFormat;
import net.sf.jabref.exporter.SaveException;
import net.sf.jabref.exporter.SavePreferences;
import net.sf.jabref.exporter.SaveSession;
import net.sf.jabref.external.AutoSetLinks;
import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.ImportInspectionCommandLine;
import net.sf.jabref.importer.OpenDatabaseAction;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fetcher.EntryFetcher;
import net.sf.jabref.importer.fetcher.EntryFetchers;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;
import net.sf.jabref.logic.logging.JabRefLogger;
import net.sf.jabref.logic.search.DatabaseSearcher;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ArgumentProcessor {

    public enum Mode {
        INITIAL_START,
        REMOTE_START
    }


    private static final Log LOGGER = LogFactory.getLog(ArgumentProcessor.class);

    private final JabRefCLI cli;

    private final List<ParserResult> parserResults;

    private final Mode startupMode;

    private boolean noGUINeeded;

    public ArgumentProcessor(String[] args, Mode startupMode) {
        cli = new JabRefCLI(args);
        this.startupMode = startupMode;
        parserResults = processArguments();
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
        BibDatabase dataBase = pr.getDatabase();

        SearchQuery query = new SearchQuery(searchTerm,
                Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE),
                Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP));
        BibDatabase newBase = new DatabaseSearcher(query, dataBase).getDatabaseFromMatches(); //newBase contains only match entries

        //export database
        if (newBase.hasEntries()) {
            String formatName;

            //read in the export format, take default format if no format entered
            switch (data.length) {
            case 3:
                formatName = data[2];
                break;
            case 2:
                //default ExportFormat: HTML table (with Abstract & BibTeX)
                formatName = "tablerefsabsbib";
                break;
            default:
                System.err.println(Localization.lang("Output file missing").concat(". \n \t ")
                        .concat(Localization.lang("Usage")).concat(": ") + JabRefCLI.getExportMatchesSyntax());
                noGUINeeded = true;
                return false;
            }

            //export new database
            IExportFormat format = ExportFormats.getExportFormat(formatName);
            if (format == null) {
                System.err.println(Localization.lang("Unknown export format") + ": " + formatName);
            } else {
                // We have an ExportFormat instance:
                try {
                    System.out.println(Localization.lang("Exporting") + ": " + data[1]);
                    BibDatabaseContext databaseContext = new BibDatabaseContext(newBase, pr.getMetaData());
                    format.performExport(databaseContext, data[1], pr.getEncoding(), newBase.getEntries());
                } catch (Exception ex) {
                    System.err.println(Localization.lang("Could not export file") + " '" + data[1] + "': "
                            + ex.getMessage());
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
        if (!cli.isBlank() && (cli.getLeftOver().length > 0)) {
            for (String aLeftOver : cli.getLeftOver()) {
                // Leftover arguments that have a "bib" extension are interpreted as
                // bib files to open. Other files, and files that could not be opened
                // as bib, we try to import instead.
                boolean bibExtension = aLeftOver.toLowerCase(Locale.ENGLISH).endsWith("bib");
                ParserResult pr = null;
                if (bibExtension) {
                    pr = OpenDatabaseAction.loadDatabaseOrAutoSave(aLeftOver, false);
                }

                if (!bibExtension || (pr.isNullResult())) {
                    // We will try to import this file. Normally we
                    // will import it into a new tab, but if this import has
                    // been initiated by another instance through the remote
                    // listener, we will instead import it into the current database.
                    // This will enable easy integration with web browsers that can
                    // open a reference file in JabRef.
                    if (startupMode == Mode.INITIAL_START) {
                        toImport.add(aLeftOver);
                    } else {
                        loaded.add(importToOpenBase(aLeftOver).orElse(ParserResult.getNullResult()));
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
                    SavePreferences prefs = SavePreferences.loadForSaveFromPreferences(Globals.prefs);
                    BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
                    Defaults defaults = new Defaults(BibDatabaseMode
                            .fromPreference(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)));
                    SaveSession session = databaseWriter.saveDatabase(new BibDatabaseContext(newBase, defaults),
                            prefs);

                    // Show just a warning message if encoding didn't work for all characters:
                    if (!session.getWriter().couldEncodeAll()) {
                        System.err.println(Localization.lang("Warning") + ": "
                                + Localization.lang(
                                        "The chosen encoding '%0' could not encode the following characters:",
                                        session.getEncoding().displayName())
                                + " " + session.getWriter().getProblemCharacters());
                    }
                    session.commit(new File(subName));
                } catch (SaveException ex) {
                    System.err.println(
                            Localization.lang("Could not save file.") + "\n" + ex.getLocalizedMessage());
                }

                notSavedMsg = true;
            }

            if (!notSavedMsg) {
                System.out.println(Localization.lang("no database generated"));
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
                        SavePreferences prefs = SavePreferences.loadForSaveFromPreferences(Globals.prefs);
                        Defaults defaults = new Defaults(BibDatabaseMode.fromPreference(
                                Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)));
                        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
                        SaveSession session = databaseWriter.saveDatabase(
                                new BibDatabaseContext(pr.getDatabase(), pr.getMetaData(), defaults), prefs);

                        // Show just a warning message if encoding didn't work for all characters:
                        if (!session.getWriter().couldEncodeAll()) {
                            System.err.println(Localization.lang("Warning") + ": "
                                    + Localization.lang(
                                            "The chosen encoding '%0' could not encode the following characters:",
                                            session.getEncoding().displayName())
                                    + " " + session.getWriter().getProblemCharacters());
                        }
                        session.commit(new File(data[0]));
                    } catch (SaveException ex) {
                        System.err.println(
                                Localization.lang("Could not save file.") + "\n" + ex.getLocalizedMessage());
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
            File theFile = pr.getFile();
            if (!theFile.isAbsolute()) {
                theFile = theFile.getAbsoluteFile();
            }
            BibDatabaseContext databaseContext = pr.getDatabaseContext();
            databaseContext.setDatabaseFile(theFile);
            Globals.prefs.fileDirForDatabase = databaseContext.getFileDirectory();
            System.out.println(Localization.lang("Exporting") + ": " + data[0]);
            IExportFormat format = ExportFormats.getExportFormat(data[1]);
            if (format == null) {
                System.err.println(Localization.lang("Unknown export format") + ": " + data[1]);
            } else {
                // We have an ExportFormat instance:
                try {
                    format.performExport(pr.getDatabaseContext(), data[0], pr.getEncoding(), null);
                } catch (Exception ex) {
                    System.err.println(Localization.lang("Could not export file") + " '" + data[0] + "': "
                            + ex.getMessage());
                }
            }

        }
    }

    private void importPreferences() {
        try {
            Globals.prefs.importPreferences(cli.getPreferencesImport());
            CustomEntryTypesManager.loadCustomEntryTypes(Globals.prefs);
            ExportFormats.initAllExports();
        } catch (JabRefException ex) {
            LOGGER.error("Cannot import preferences", ex);
        }
    }

    private void resetPreferences(String value) {
        if ("all".equals(value.trim())) {
            try {
                System.out.println(Localization.lang("Setting all preferences to default values."));
                Globals.prefs.clear();
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

            MetaData metaData = parserResult.getMetaData();
            if (metaData != null) {
                LOGGER.info(Localization.lang("Regenerating BibTeX keys according to metadata"));
                for (BibEntry entry : database.getEntries()) {
                    // try to make a new label
                    LabelPatternUtil.makeLabel(metaData, database, entry);
                }
            } else {
                LOGGER.info(Localization.lang("No meta data present in bibfile. Cannot regenerate BibTeX keys"));
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
        System.out.println(Localization.lang("Running Query '%0' with fetcher '%1'.", query, engine) + " "
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

    /**
     * Will open a file (like importFile), but will also request JabRef to focus on this database
     *
     * @param argument See importFile.
     * @return ParserResult with setToOpenTab(true)
     */
    private static Optional<ParserResult> importToOpenBase(String argument) {
        Optional<ParserResult> result = importFile(argument);

        result.ifPresent(x -> x.setToOpenTab(true));

        return result;
    }

    private static Optional<ParserResult> importFile(String argument) {
        String[] data = argument.split(",");
        try {
            if ((data.length > 1) && !"*".equals(data[1])) {
                System.out.println(Localization.lang("Importing") + ": " + data[0]);
                try {
                    List<BibEntry> entries;
                    if (OS.WINDOWS) {
                        entries = Globals.IMPORT_FORMAT_READER.importFromFile(data[1], data[0], JabRefGUI.getMainFrame());
                    } else {
                        entries = Globals.IMPORT_FORMAT_READER.importFromFile(data[1],
                                data[0].replace("~", System.getProperty("user.home")), JabRefGUI.getMainFrame());
                    }
                    return Optional.of(new ParserResult(entries));
                } catch (IllegalArgumentException ex) {
                    System.err.println(Localization.lang("Unknown import format") + ": " + data[1]);
                    return Optional.empty();
                }
            } else {
                // * means "guess the format":
                System.out.println(Localization.lang("Importing in unknown format") + ": " + data[0]);

                ImportFormatReader.UnknownFormatImport importResult;
                if (OS.WINDOWS) {
                    importResult = Globals.IMPORT_FORMAT_READER.importUnknownFormat(data[0]);
                } else {
                    importResult = Globals.IMPORT_FORMAT_READER
                            .importUnknownFormat(data[0].replace("~", System.getProperty("user.home")));
                }

                if (importResult == null) {
                    System.out.println(Localization.lang("Could not find a suitable import format."));
                } else {
                    System.out.println(Localization.lang("Format used") + ": " + importResult.format);

                    return Optional.of(importResult.parserResult);
                }
            }
        } catch (IOException ex) {
            System.err.println(
                    Localization.lang("Error opening file") + " '" + data[0] + "': " + ex.getLocalizedMessage());
        }
        return Optional.empty();
    }

    public boolean shouldShutDown() {
        return cli.isDisableGui() || cli.isShowVersion() || noGUINeeded;
    }

}
