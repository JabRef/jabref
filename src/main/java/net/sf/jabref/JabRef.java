/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.charset.Charset;
import java.util.*;
import java.util.prefs.BackingStoreException;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyBluer;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.cli.AuxCommandLine;
import net.sf.jabref.exporter.*;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.remote.JabRefMessageHandler;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fetcher.EntryFetcher;
import net.sf.jabref.importer.fetcher.EntryFetchers;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;
import net.sf.jabref.logic.logging.JabRefLogger;
import net.sf.jabref.logic.net.ProxyAuthenticator;
import net.sf.jabref.logic.net.ProxyPreferences;
import net.sf.jabref.logic.net.ProxyRegisterer;
import net.sf.jabref.logic.preferences.LastFocusedTabPreferences;
import net.sf.jabref.logic.remote.RemotePreferences;
import net.sf.jabref.logic.remote.client.RemoteListenerClient;
import net.sf.jabref.logic.search.DatabaseSearcher;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.migrations.PreferencesMigrations;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JabRef Main Class - The application gets started here.
 */
public class JabRef {
    private static final Log LOGGER = LogFactory.getLog(JabRef.class);

    public static JabRefFrame mainFrame;

    private JabRefCLI cli;

    public void start(String[] args) {
        JabRefPreferences preferences = JabRefPreferences.getInstance();

        ProxyPreferences proxyPreferences = ProxyPreferences.loadFromPreferences(preferences);
        ProxyRegisterer.register(proxyPreferences);
        if (proxyPreferences.isUseProxy() && proxyPreferences.isUseAuthentication()) {
            Authenticator.setDefault(new ProxyAuthenticator());
        }

        Globals.startBackgroundTasks();
        Globals.prefs = preferences;
        Localization.setLanguage(preferences.get(JabRefPreferences.LANGUAGE));
        Globals.prefs.setLanguageDependentDefaultValues();

        // Update which fields should be treated as numeric, based on preferences:
        InternalBibtexFields.setNumericFieldsFromPrefs();

        /* Build list of Import and Export formats */
        Globals.IMPORT_FORMAT_READER.resetImportFormats();
        CustomEntryTypesManager.loadCustomEntryTypes(preferences);
        ExportFormats.initAllExports();

        // Read list(s) of journal names and abbreviations
        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader(Globals.prefs);

        // Check for running JabRef
        RemotePreferences remotePreferences = new RemotePreferences(Globals.prefs);
        if (remotePreferences.useRemoteServer()) {
            Globals.REMOTE_LISTENER.open(new JabRefMessageHandler(this), remotePreferences.getPort());

            if (!Globals.REMOTE_LISTENER.isOpen()) {
                // we are not alone, there is already a server out there, try to contact already running JabRef:
                if (RemoteListenerClient.sendToActiveJabRefInstance(args, remotePreferences.getPort())) {
                    // We have successfully sent our command line options through the socket to another JabRef instance.
                    // So we assume it's all taken care of, and quit.
                    System.out.println(Localization.lang("Arguments passed on to running JabRef instance. Shutting down."));
                    JabRefExecutorService.INSTANCE.shutdownEverything();
                    return;
                }
            }
            // we are alone, we start the server
            Globals.REMOTE_LISTENER.start();
        }

        // override used newline character with the one stored in the preferences
        // The preferences return the system newline character sequence as default
        Globals.NEWLINE = Globals.prefs.get(JabRefPreferences.NEWLINE);

        Optional<Vector<ParserResult>> loaded = processArguments(args, true);

        if ((!(loaded.isPresent())) || cli.isDisableGui() || cli.isShowVersion()) {
            JabRefExecutorService.INSTANCE.shutdownEverything();
            return;
        }

        SwingUtilities.invokeLater(() -> openWindow(loaded.get()));
    }

    public Optional<Vector<ParserResult>> processArguments(String[] args, boolean initialStartup) {
        cli = new JabRefCLI(args);

        if (!cli.isBlank() && cli.isDebugLogging()) {
            JabRefLogger.setDebug();
        }

        if (initialStartup && cli.isShowVersion()) {
            cli.displayVersion();
        }

        if (initialStartup && cli.isHelp()) {
            cli.printUsage();
            return Optional.empty();
        }

        // Check if we should reset all preferences to default values:
        if (cli.isPreferencesReset()) {
            String value = cli.getPreferencesReset();
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

        // Check if we should import preferences from a file:
        if (cli.isPreferencesImport()) {
            try {
                Globals.prefs.importPreferences(cli.getPreferencesImport());
                CustomEntryTypesManager.loadCustomEntryTypes(Globals.prefs);
                ExportFormats.initAllExports();
            } catch (JabRefException ex) {
                LOGGER.error("Cannot import preferences", ex);
            }
        }

        // Vector to put imported/loaded database(s) in.
        Vector<ParserResult> loaded = new Vector<>();
        Vector<String> toImport = new Vector<>();
        if (!cli.isBlank() && (cli.getLeftOver().length > 0)) {
            for (String aLeftOver : cli.getLeftOver()) {
                // Leftover arguments that have a "bib" extension are interpreted as
                // bib files to open. Other files, and files that could not be opened
                // as bib, we try to import instead.
                boolean bibExtension = aLeftOver.toLowerCase().endsWith("bib");
                ParserResult pr = null;
                if (bibExtension) {
                    pr = JabRef.openBibFile(aLeftOver, false);
                }

                if ((pr == null) || (pr == ParserResult.INVALID_FORMAT)) {
                    // We will try to import this file. Normally we
                    // will import it into a new tab, but if this import has
                    // been initiated by another instance through the remote
                    // listener, we will instead import it into the current database.
                    // This will enable easy integration with web browsers that can
                    // open a reference file in JabRef.
                    if (initialStartup) {
                        toImport.add(aLeftOver);
                    } else {
                        loaded.add(JabRef.importToOpenBase(aLeftOver).orElse(ParserResult.INVALID_FORMAT));
                    }
                } else if (pr != ParserResult.FILE_LOCKED) {
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

        if (!cli.isBlank() && cli.isFetcherEngine()) {
            fetch(cli.getFetcherEngine()).ifPresent(loaded::add);
        }

        if (cli.isExportMatches()) {
            if (!loaded.isEmpty()) {
                String[] data = cli.getExportMatches().split(",");
                String searchTerm = data[0].replace("\\$", " "); //enables blanks within the search term:
                //? stands for a blank
                ParserResult pr = loaded.elementAt(loaded.size() - 1);
                BibDatabase dataBase = pr.getDatabase();

                SearchQuery query = new SearchQuery(searchTerm, Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE),
                        Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP));
                BibDatabase newBase = new DatabaseSearcher(query, dataBase).getDatabaseFromMatches(); //newBase contains only match entries

                //export database
                if ((newBase != null) && newBase.hasEntries()) {
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
                        return Optional.empty();
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
                String[] data = cli.getFileExport().split(",");

                if (data.length == 1) {
                    // This signals that the latest import should be stored in BibTeX
                    // format to the given file.
                    if (!loaded.isEmpty()) {
                        ParserResult pr = loaded.elementAt(loaded.size() - 1);
                        if (!pr.isInvalid()) {
                            try {
                                System.out.println(Localization.lang("Saving") + ": " + data[0]);
                                SavePreferences prefs = SavePreferences.loadForSaveFromPreferences(Globals.prefs);
                                Defaults defaults = new Defaults(BibDatabaseMode
                                        .fromPreference(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)));
                                BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
                                SaveSession session = databaseWriter.saveDatabase(
                                        new BibDatabaseContext(pr.getDatabase(), pr.getMetaData(), defaults), prefs);

                                // Show just a warning message if encoding didn't work for all characters:
                                if (!session.getWriter().couldEncodeAll()) {
                                    System.err.println(Localization.lang("Warning") + ": "
                                            + Localization.lang(
                                            "The chosen encoding '%0' could not encode the following characters:",
                                            session.getEncoding().displayName())
                                            + " "
                                            + session.getWriter().getProblemCharacters());
                                }
                                session.commit(new File(data[0]));
                            } catch (SaveException ex) {
                                System.err.println(Localization.lang("Could not save file.") + "\n"
                                        + ex.getLocalizedMessage());
                            }
                        }
                    } else {
                        System.err.println(Localization.lang("The output option depends on a valid import option."));
                    }
                } else if (data.length == 2) {
                    // This signals that the latest import should be stored in the given
                    // format to the given file.
                    ParserResult pr = loaded.elementAt(loaded.size() - 1);

                    // Set the global variable for this database's file directory before exporting,
                    // so formatters can resolve linked files correctly.
                    // (This is an ugly hack!)
                    File theFile = pr.getFile();
                    if (!theFile.isAbsolute()) {
                        theFile = theFile.getAbsoluteFile();
                    }
                    BibDatabaseContext databaseContext = pr.getDatabaseContext();
                    databaseContext.setDatabaseFile(theFile);
                    Globals.prefs.fileDirForDatabase = databaseContext.getFileDirectory().toArray(new String[0]);
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
            boolean usageMsg = false;

            if (!loaded.isEmpty()) {
                String[] data = cli.getAuxImport().split(",");

                if (data.length == 2) {
                    ParserResult pr = loaded.firstElement();
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
                } else {
                    usageMsg = true;
                }
            } else {
                usageMsg = true;
            }

            if (usageMsg) {
                System.out.println(Localization.lang("no base-BibTeX-file specified") + "!");
                System.out.println(Localization.lang("usage") + " :");
                System.out.println("jabref --aux infile[.aux],outfile[.bib] base-BibTeX-file");
            }
        }

        return Optional.of(loaded);
    }

    private void automaticallySetFileLinks(List<ParserResult> loaded) {
        for (ParserResult parserResult : loaded) {
            BibDatabase database = parserResult.getDatabase();
            LOGGER.info(Localization.lang("Automatically setting file links"));
            Util.autoSetLinks(database.getEntries(), parserResult.getDatabaseContext());
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

        if ((result == null) || result.isEmpty()) {
            System.out.println(
                    Localization.lang("Query '%0' with fetcher '%1' did not return any results.", query, engine));
            return Optional.empty();
        }

        return Optional.of(new ParserResult(result));
    }

    private void setLookAndFeel() {
        try {
            String lookFeel;
            String systemLookFeel = UIManager.getSystemLookAndFeelClassName();

            if (Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_LOOK_AND_FEEL)) {
                // FIXME: Problems with OpenJDK and GTK L&F
                // See https://github.com/JabRef/jabref/issues/393, https://github.com/JabRef/jabref/issues/638
                if (System.getProperty("java.runtime.name").contains("OpenJDK")) {
                    // Metal L&F
                    lookFeel = UIManager.getCrossPlatformLookAndFeelClassName();
                    LOGGER.warn("There seem to be problems with OpenJDK and the default GTK Look&Feel. Using Metal L&F instead. Change to another L&F with caution.");
                } else {
                    lookFeel = systemLookFeel;
                }
            } else {
                lookFeel = Globals.prefs.get(JabRefPreferences.WIN_LOOK_AND_FEEL);
            }
            
            // FIXME: Open JDK problem
            if (UIManager.getCrossPlatformLookAndFeelClassName().equals(lookFeel) && !System.getProperty("java.runtime.name").contains("OpenJDK")) {
                // try to avoid ending up with the ugly Metal L&F
                Plastic3DLookAndFeel lnf = new Plastic3DLookAndFeel();
                Plastic3DLookAndFeel.setCurrentTheme(new SkyBluer());
                com.jgoodies.looks.Options.setPopupDropShadowEnabled(true);
                UIManager.setLookAndFeel(lnf);
            } else {
                try {
                    UIManager.setLookAndFeel(lookFeel);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                        UnsupportedLookAndFeelException e) {
                    // specified look and feel does not exist on the classpath, so use system l&f
                    UIManager.setLookAndFeel(systemLookFeel);
                    // also set system l&f as default
                    Globals.prefs.put(JabRefPreferences.WIN_LOOK_AND_FEEL, systemLookFeel);
                    // notify the user
                    JOptionPane.showMessageDialog(JabRef.mainFrame,
                            Localization
                                    .lang("Unable to find the requested Look & Feel and thus the default one is used."),
                            Localization.lang("Warning"), JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Look and feel could not be set", e);
        }

        // In JabRef v2.8, we did it only on NON-Mac. Now, we try on all platforms
        boolean overrideDefaultFonts = Globals.prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS);
        if (overrideDefaultFonts) {
            int fontSize = Globals.prefs.getInt(JabRefPreferences.MENU_FONT_SIZE);
            UIDefaults defaults = UIManager.getDefaults();
            Enumeration<Object> keys = defaults.keys();
            for (Object key : Collections.list(keys)) {
                if ((key instanceof String) && ((String) key).endsWith(".font")) {
                    FontUIResource font = (FontUIResource) UIManager.get(key);
                    font = new FontUIResource(font.getName(), font.getStyle(), fontSize);
                    defaults.put(key, font);
                }
            }
        }
    }

    private void openWindow(List<ParserResult> loaded) {
        // Perform checks and changes for users with a preference set from an older JabRef version.
        PreferencesMigrations.replaceAbstractField();
        PreferencesMigrations.upgradeSortOrder();
        PreferencesMigrations.upgradeFaultyEncodingStrings();

        // This property is set to make the Mac OSX Java VM move the menu bar to
        // the top of the screen, where Mac users expect it to be.
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // Set antialiasing on everywhere. This only works in JRE >= 1.5.
        // Or... it doesn't work, period.
        // TODO test and maybe remove this! I found this commented out with no additional info ( payload@lavabit.com )
        // Enabled since JabRef 2.11 beta 4
        System.setProperty("swing.aatext", "true");
        // Default is "on".
        // "lcd" instead of "on" because of http://wiki.netbeans.org/FaqFontRendering and http://docs.oracle.com/javase/6/docs/technotes/guides/2d/flags.html#aaFonts
        System.setProperty("awt.useSystemAAFontSettings", "lcd");

        // Look & Feel. This MUST be the first thing to do before loading any Swing-specific code!
        setLookAndFeel();

        // If the option is enabled, open the last edited databases, if any.
        if (!cli.isBlank() && Globals.prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)
                && (Globals.prefs.get(JabRefPreferences.LAST_EDITED) != null)) {
            // How to handle errors in the databases to open?
            List<String> names = Globals.prefs.getStringList(JabRefPreferences.LAST_EDITED);
            lastEdLoop:
            for (String name : names) {
                File fileToOpen = new File(name);

                for (int j = 0; j < loaded.size(); j++) {
                    ParserResult pr = loaded.get(j);

                    if ((pr.getFile() != null) && pr.getFile().equals(fileToOpen)) {
                        continue lastEdLoop;
                    }
                }

                if (fileToOpen.exists()) {
                    ParserResult pr = JabRef.openBibFile(name, false);

                    if (pr != null) {

                        if (pr == ParserResult.INVALID_FORMAT) {
                            System.out.println(
                                    Localization.lang("Error opening file") + " '" + fileToOpen.getPath() + "'");
                        } else if (pr != ParserResult.FILE_LOCKED) {
                            loaded.add(pr);
                        }

                    }
                }
            }
        }

        GUIGlobals.init();
        GUIGlobals.currentFont = new Font(Globals.prefs.get(JabRefPreferences.FONT_FAMILY),
                Globals.prefs.getInt(JabRefPreferences.FONT_STYLE), Globals.prefs.getInt(JabRefPreferences.FONT_SIZE));

        LOGGER.debug("Initializing frame");
        JabRef.mainFrame = new JabRefFrame(this);

        // Add all loaded databases to the frame:
        boolean first = true;
        List<File> postponed = new ArrayList<>();
        List<ParserResult> failed = new ArrayList<>();
        List<ParserResult> toOpenTab = new ArrayList<>();
        if (!loaded.isEmpty()) {
            for (Iterator<ParserResult> i = loaded.iterator(); i.hasNext(); ) {
                ParserResult pr = i.next();

                if (new LastFocusedTabPreferences(Globals.prefs).hadLastFocus(pr.getFile())) {
                    first = true;
                }

                if (pr.isInvalid()) {
                    failed.add(pr);
                    i.remove();
                } else if (!pr.isPostponedAutosaveFound()) {
                    if (pr.toOpenTab()) {
                        // things to be appended to an opened tab should be done after opening all tabs
                        // add them to the list
                        toOpenTab.add(pr);
                    } else {
                        JabRef.mainFrame.addParserResult(pr, first);
                        first = false;
                    }
                } else {
                    i.remove();
                    postponed.add(pr.getFile());
                }
            }
        }

        // finally add things to the currently opened tab
        for (ParserResult pr : toOpenTab) {
            JabRef.mainFrame.addParserResult(pr, first);
            first = false;
        }

        // Start auto save timer:
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_SAVE)) {
            Globals.startAutoSaveManager(JabRef.mainFrame);
        }

        // If we are set to remember the window location, we also remember the maximised
        // state. This needs to be set after the window has been made visible, so we
        // do it here:
        if (Globals.prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            JabRef.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        JabRef.mainFrame.setVisible(true);

        if (Globals.prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            JabRef.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        for (ParserResult pr : failed) {
            String message = "<html>" + Localization.lang("Error opening file '%0'.", pr.getFile().getName()) + "<p>"
                    + pr.getErrorMessage() + "</html>";

            JOptionPane.showMessageDialog(JabRef.mainFrame, message, Localization.lang("Error opening file"),
                    JOptionPane.ERROR_MESSAGE);
        }

        for (int i = 0; i < loaded.size(); i++) {
            if (Globals.prefs.getBoolean(JabRefPreferences.DISPLAY_KEY_WARNING_DIALOG_AT_STARTUP)) {
                ParserResultWarningDialog.showParserResultWarningDialog(loaded.get(i), mainFrame, i);
            }
        }

        // After adding the databases, go through each and see if
        // any post open actions need to be done. For instance, checking
        // if we found new entry types that can be imported, or checking
        // if the database contents should be modified due to new features
        // in this version of JabRef.
        // Note that we have to check whether i does not go over getBasePanelCount().
        // This is because importToOpen might have been used, which adds to
        // loaded, but not to getBasePanelCount()
        for (int i = 0; (i < loaded.size()) && (i < JabRef.mainFrame.getBasePanelCount()); i++) {
            ParserResult pr = loaded.get(i);
            BasePanel panel = JabRef.mainFrame.getBasePanelAt(i);
            OpenDatabaseAction.performPostOpenActions(panel, pr, true);
        }

        LOGGER.debug("Finished adding panels");

        // If any database loading was postponed due to an autosave, schedule them
        // for handing now:
        if (!postponed.isEmpty()) {
            AutosaveStartupPrompter asp = new AutosaveStartupPrompter(JabRef.mainFrame, postponed);
            SwingUtilities.invokeLater(asp);
        }

        if (!loaded.isEmpty()) {
            new FocusRequester(JabRef.mainFrame.getCurrentBasePanel().mainTable);
        }
    }

    public static ParserResult openBibFile(String name, boolean ignoreAutosave) {
        // String in OpenDatabaseAction.java
        LOGGER.info("Opening: " + name);
        File file = new File(name);
        if (!file.exists()) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            System.err.println(Localization.lang("Error") + ": " + Localization.lang("File not found"));
            return pr;

        }
        try {

            if (!ignoreAutosave) {
                boolean autoSaveFound = AutoSaveManager.newerAutoSaveExists(file);
                if (autoSaveFound) {
                    // We have found a newer autosave. Make a note of this, so it can be
                    // handled after startup:
                    ParserResult postp = new ParserResult(null, null, null);
                    postp.setPostponedAutosaveFound(true);
                    postp.setFile(file);
                    return postp;
                }
            }

            if (!FileBasedLock.waitForFileLock(file, 10)) {
                System.out.println(Localization.lang("Error opening file") + " '" + name + "'. "
                        + "File is locked by another JabRef instance.");
                return ParserResult.FILE_LOCKED;
            }

            Charset encoding = Globals.prefs.getDefaultEncoding();
            ParserResult pr = OpenDatabaseAction.loadDatabase(file, encoding);
            if (pr == null) {
                pr = new ParserResult(null, null, null);
                pr.setFile(file);
                pr.setInvalid(true);
                return pr;

            }
            pr.setFile(file);
            if (pr.hasWarnings()) {
                for (String aWarn : pr.warnings()) {
                    LOGGER.warn(aWarn);
                }

            }
            return pr;
        } catch (Throwable ex) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            pr.setErrorMessage(ex.getMessage());
            LOGGER.info("Problem opening .bib-file", ex);
            return pr;
        }

    }

    private static Optional<ParserResult> importFile(String argument) {
        String[] data = argument.split(",");
        try {
            if ((data.length > 1) && !"*".equals(data[1])) {
                System.out.println(Localization.lang("Importing") + ": " + data[0]);
                try {
                    List<BibEntry> entries;
                    if (OS.WINDOWS) {
                        entries = Globals.IMPORT_FORMAT_READER.importFromFile(data[1], data[0], JabRef.mainFrame);
                    } else {
                        entries = Globals.IMPORT_FORMAT_READER.importFromFile(data[1],
                                data[0].replace("~", System.getProperty("user.home")), JabRef.mainFrame);
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

    /**
     * Will open a file (like importFile), but will also request JabRef to focus on this database
     *
     * @param argument See importFile.
     * @return ParserResult with setToOpenTab(true)
     */
    private static Optional<ParserResult> importToOpenBase(String argument) {
        Optional<ParserResult> result = JabRef.importFile(argument);

        result.ifPresent(x -> x.setToOpenTab(true));

        return result;
    }
}
