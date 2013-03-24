/*  Copyright (C) 2003-2011 JabRef contributors.
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

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyBluer;
import gnu.dtools.ritopt.BooleanOption;
import gnu.dtools.ritopt.Options;
import gnu.dtools.ritopt.StringOption;

import java.awt.Font;
import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.prefs.BackingStoreException;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

import net.sf.jabref.export.AutoSaveManager;
import net.sf.jabref.export.ExportFormats;
import net.sf.jabref.export.FileActions;
import net.sf.jabref.export.IExportFormat;
import net.sf.jabref.export.SaveException;
import net.sf.jabref.export.SaveSession;
import net.sf.jabref.imports.AutosaveStartupPrompter;
import net.sf.jabref.imports.EntryFetcher;
import net.sf.jabref.imports.ImportInspectionCommandLine;
import net.sf.jabref.imports.OpenDatabaseAction;
import net.sf.jabref.imports.ParserResult;
import net.sf.jabref.plugin.PluginCore;
import net.sf.jabref.plugin.PluginInstaller;
import net.sf.jabref.plugin.SidePanePlugin;
import net.sf.jabref.plugin.core.JabRefPlugin;
import net.sf.jabref.plugin.core.generated._JabRefPlugin;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.EntryFetcherExtension;
import net.sf.jabref.remote.RemoteListener;
import net.sf.jabref.util.Pair;
import net.sf.jabref.wizard.auximport.AuxCommandLine;

/**
 * JabRef Main Class - The application gets started here.
 *
 */
public class JabRef {

	public static JabRef singleton;
    public static RemoteListener remoteListener = null;
    public static JabRefFrame jrf;
    public Options options;
    public static Frame splashScreen = null;

    boolean graphicFailure = false;

    StringOption importFile, exportFile, exportPrefs, importPrefs, auxImExport, importToOpenBase,
            fetcherEngine, exportMatches, defPrefs;
    BooleanOption helpO, disableGui, blank, loadSess, showVersion, disableSplash;

    private final static String exportMatchesSyntax = "[".concat(Globals.lang("field")).concat("]").concat("searchTerm").concat(",").concat("outputFile").concat(": ").concat(Globals.lang("file")).concat("[,").concat(Globals.lang("exportFormat")).concat("]");

    public static final int MAX_DIALOG_WARNINGS = 10;

    public static void main(String[] args) {
        new JabRef(args);
    }

    protected JabRef(String[] args) {

		singleton = this;

		// The following two lines signal that the system proxy settings should
		// be used:
		System.setProperty("java.net.useSystemProxies", "true");
		System.getProperties().put("proxySet", "true");

		JabRefPreferences prefs = JabRefPreferences.getInstance();

        // See if there are plugins scheduled for deletion:
        if (prefs.hasKey("deletePlugins") && (prefs.get("deletePlugins").length() > 0)) {
            String[] toDelete = prefs.getStringArray("deletePlugins");
            PluginInstaller.deletePluginsOnStartup(toDelete);
            prefs.put("deletePlugins", "");
        }

        Globals.startBackgroundTasks();
        Globals.setupLogging();
		Globals.prefs = prefs;
        String langStr = prefs.get("language");
        String[] parts = langStr.split("_");
        String language, country;
        if (parts.length == 1) {
            language = langStr;
            country = "";
        }
        else {
            language = parts[0];
            country = parts[1];
        }

		Globals.setLanguage(language, country);
        Globals.prefs.setLanguageDependentDefaultValues();
		/*
		 * The Plug-in System is started automatically on the first call to
		 * PluginCore.getManager().
		 * 
		 * Plug-ins are activated on the first call to their getInstance method.
		 */

        // Update which fields should be treated as numeric, based on preferences:
        BibtexFields.setNumericFieldsFromPrefs();
		
		/* Build list of Import and Export formats */
		Globals.importFormatReader.resetImportFormats();
		BibtexEntryType.loadCustomEntryTypes(prefs);
		ExportFormats.initAllExports();
		
		// Read list(s) of journal names and abbreviations:
        Globals.initializeJournalNames();

		// Check for running JabRef
		if (Globals.prefs.getBoolean("useRemoteServer")) {
			remoteListener = RemoteListener.openRemoteListener(this);

			if (remoteListener == null) {
				// Unless we are alone, try to contact already running JabRef:
				if (RemoteListener.sendToActiveJabRefInstance(args)) {

					/*
					 * We have successfully sent our command line options
					 * through the socket to another JabRef instance. So we
					 * assume it's all taken care of, and quit.
					 */
					System.out.println(
                            Globals.lang("Arguments passed on to running JabRef instance. Shutting down."));
					System.exit(0);
				}
			} else {
				// No listener found, thus we are the first instance to be
				// started.
				remoteListener.start();
			}
		}

		/*
		 * See if the user has a personal journal list set up. If so, add these
		 * journal names and abbreviations to the list:
		 */
		String personalJournalList = prefs.get("personalJournalList");
		if (personalJournalList != null) {
			try {
				Globals.journalAbbrev.readJournalList(new File(
						personalJournalList));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		/*
		 * Make sure of a proper cleanup when quitting (e.g. deleting temporary
		 * files).
		 * 
		 * CO 2007-07-12: Since this is deprecated, commented out:
		 * 
		 * System.runFinalizersOnExit(true);
		 * 
		 */
		
		openWindow(processArguments(args, true));
	}
    
    private void setupOptions() {

        importFile = new StringOption("");
        exportFile = new StringOption("");
        helpO = new BooleanOption();
        disableGui = new BooleanOption();
        disableSplash = new BooleanOption();
        blank = new BooleanOption();
        loadSess = new BooleanOption();
        showVersion = new BooleanOption();
        exportPrefs = new StringOption("jabref_prefs.xml");
        importPrefs = new StringOption("jabref_prefs.xml");
        defPrefs = new StringOption("");
        auxImExport = new StringOption("");
        importToOpenBase = new StringOption("");
        fetcherEngine = new StringOption("");
    	exportMatches = new StringOption(""); 

        options = new Options("JabRef "); // Create an options repository.
        options.setVersion(GUIGlobals.version);

        importFile.setDescription("imopoepuoeu"); //Globals.lang);
        options.register("version", 'v',
                Globals.lang("Display version"), showVersion);
        options.register("nogui", 'n',
            Globals.lang("No GUI. Only process command line options."), disableGui);
        options.register("nosplash", 's',
                Globals.lang("Do not show splash window at startup"), disableSplash);
        options.register("import", 'i',
            Globals.lang("Import file") + ": " + Globals.lang("filename")
            + "[,import format]", importFile);
        options.register("output", 'o',
            Globals.lang("Output or export file") + ": " + Globals.lang("filename")
            + "[,export format]", exportFile);
        options.register("help", 'h',
            Globals.lang("Display help on command line options"), helpO);
        options.register("loads", 'l', Globals.lang("Load session"), loadSess);
        options.register("prexp", 'x', Globals.lang("Export preferences to file"),
            exportPrefs);
        options.register("primp", 'p', Globals.lang("Import preferences from file"),
            importPrefs);
        options.register("prdef", 'd', Globals.lang("Reset preferences (key1,key2,... or 'all')"),
            defPrefs);
        options.register("aux", 'a',
            Globals.lang("Subdatabase from aux") + ": " + Globals.lang("file")+"[.aux]" + ","+Globals.lang("new")+"[.bib]",
            auxImExport);
        options.register("blank", 'b', Globals.lang("Do not open any files at startup"), blank);

        options.register("importToOpen", '\0', Globals.lang("Import to open tab"), importToOpenBase);

        options.register("fetch", 'f', Globals.lang("Run Fetcher, e.g. \"--fetch=Medline:cancer\""), fetcherEngine);

        options.register("exportMatches", 'm', exportMatchesSyntax, exportMatches); 

        options.setUseMenu(false);
    }

    public Vector<ParserResult> processArguments(String[] args, boolean initialStartup) {

        setupOptions();
        String[] leftOver = options.process(args);

        if (initialStartup && showVersion.isInvoked()) {
            options.displayVersion();
            disableGui.setInvoked(true);
        }

        if (initialStartup && helpO.isInvoked()) {
            System.out.println("jabref [options] [bibtex-file]\n");
            System.out.println(options.getHelp());

            String importFormats = Globals.importFormatReader.getImportFormatList();
            System.out.println(Globals.lang("Available import formats") + ":\n"
                + importFormats);

            String outFormats = ExportFormats.getConsoleExportList(70, 20, "\t");
            System.out.println(Globals.lang("Available export formats") + ": " + outFormats
                + ".");
            System.exit(0);
        }
        
        boolean commandmode = disableGui.isInvoked() || fetcherEngine.isInvoked();
        
        // First we quickly scan the command line parameters for any that signal
        // that the GUI
        // should not be opened. This is used to decide whether we should show the
        // splash screen or not.
        if (initialStartup && !commandmode && !disableSplash.isInvoked()) {
            try {
                splashScreen = SplashScreen.splash();
            } catch (Throwable ex) {
                graphicFailure = true;
                System.err.println(Globals.lang("Unable to create graphical interface")
                    + ".");
            }
        }

        // Check if we should reset all preferences to default values:
        if (defPrefs.isInvoked()) {
            String value = defPrefs.getStringValue();
            if (value.trim().equals("all")) {
                try {
                    System.out.println(Globals.lang("Setting all preferences to default values."));
                    Globals.prefs.clear();
                } catch (BackingStoreException e) {
                    System.err.println(Globals.lang("Unable to clear preferences."));
                    e.printStackTrace();
                }
            } else {
                String[] keys = value.split(",");
                for (int i=0; i<keys.length; i++) {
                    try {
                        if (Globals.prefs.hasKey(keys[i].trim())) {
                            System.out.println(Globals.lang("Resetting preference key '%0'", keys[i].trim()));
                            Globals.prefs.clear(keys[i].trim());
                        } else {
                            System.out.println(Globals.lang("Unknown preference key '%0'", keys[i].trim()));
                        }
                    } catch (BackingStoreException e) {
                        System.err.println(Globals.lang("Unable to clear preferences."));
                        e.printStackTrace();
                    }
                }
            }

        }

        // Check if we should import preferences from a file:
        if (importPrefs.isInvoked()) {
            try {
                Globals.prefs.importPreferences(importPrefs.getStringValue());
                BibtexEntryType.loadCustomEntryTypes(Globals.prefs);
                ExportFormats.initAllExports();
            }
            catch (IOException ex) {
            Util.pr(ex.getMessage());
            }
        }

        // Vector to put imported/loaded database(s) in.
        Vector<ParserResult> loaded = new Vector<ParserResult>();
        Vector<String> toImport = new Vector<String>();
        if (!blank.isInvoked() && (leftOver.length > 0))  {
            for (int i = 0; i < leftOver.length; i++) {
                // Leftover arguments that have a "bib" extension are interpreted as
                // bib files to open. Other files, and files that could not be opened
                // as bib, we try to import instead.
                boolean bibExtension = leftOver[i].toLowerCase().endsWith("bib");
                ParserResult pr = null;
                if (bibExtension)
                    pr = openBibFile(leftOver[i], false);

                if ((pr == null) || (pr == ParserResult.INVALID_FORMAT)) {
                    // We will try to import this file. Normally we
                    // will import it into a new tab, but if this import has
                    // been initiated by another instance through the remote
                    // listener, we will instead import it into the current database.
                    // This will enable easy integration with web browers that can
                    // open a reference file in JabRef.
                    if (initialStartup) {
                        toImport.add(leftOver[i]);
                    } else {
                        ParserResult res = importToOpenBase(leftOver[i]);
                        if (res != null)
                            loaded.add(res);
                        else
                            loaded.add(ParserResult.INVALID_FORMAT);
                    }
                }
                else if (pr != ParserResult.FILE_LOCKED)
                    loaded.add(pr);

            }
        }

        if (!blank.isInvoked() && importFile.isInvoked()) {
            toImport.add(importFile.getStringValue());
        }

        for (String filenameString : toImport) {
			ParserResult pr = importFile(filenameString);
			if (pr != null)
				loaded.add(pr);
		}

        if (!blank.isInvoked() && importToOpenBase.isInvoked()) {
            ParserResult res = importToOpenBase(importToOpenBase.getStringValue());
            if (res != null)
                loaded.add(res);
        }

        if (!blank.isInvoked() && fetcherEngine.isInvoked()) {
            ParserResult res = fetch(fetcherEngine.getStringValue());
            if (res != null)
                loaded.add(res);
        }


        if(exportMatches.isInvoked()) {
            if (loaded.size() > 0) {
                String[] data = exportMatches.getStringValue().split(",");
                String searchTerm = data[0].replace("\\$"," "); //enables blanks within the search term:
                                                                //? stands for a blank
                ParserResult pr =
                    (ParserResult) loaded.elementAt(loaded.size() - 1);
                BibtexDatabase dataBase = pr.getDatabase();
                SearchManagerNoGUI smng = new SearchManagerNoGUI(searchTerm, dataBase);
                BibtexDatabase newBase = smng.getDBfromMatches(); //newBase contains only match entries
                
                
                //export database
                if (newBase != null && newBase.getEntryCount() > 0) {
                	String formatName = null;
	                IExportFormat format = null;

	                //read in the export format, take default format if no format entered
	                switch (data.length){
		                case(3):{
		                	formatName = data[2];
		                	break;
		                }
		                case (2):{
		                	//default ExportFormat: HTML table (with Abstract & BibTeX)
		                	formatName = "tablerefsabsbib";
		                	break;
		                }
		                default:{
		                	System.err.println(Globals.lang("Output file missing").concat(". \n \t ").concat("Usage").concat(": ") + exportMatchesSyntax);
		                	System.exit(0);
		                }
	                } //end switch
	                
	                //export new database
	                format = ExportFormats.getExportFormat(formatName);
	                if (format != null) {
	                    // We have an ExportFormat instance:
	                    try {
		                System.out.println(Globals.lang("Exporting") + ": " + data[1]);
	                        format.performExport(newBase, pr.getMetaData(), data[1], pr.getEncoding(), null);
	                    } catch (Exception ex) {
	                        System.err.println(Globals.lang("Could not export file")
	                            + " '" + data[1] + "': " + ex.getMessage());
	                    }
	                } else
	                    System.err.println(Globals.lang("Unknown export format")
	                            + ": " + formatName);
                } /*end if newBase != null*/ else {
                	System.err.println(Globals.lang("No search matches."));
                }
            } else {
            	System.err.println(Globals.lang("The output option depends on a valid input option."));
            }  //end if(loaded.size > 0)
        } //end exportMatches invoked 


        if (exportFile.isInvoked()) {
            if (loaded.size() > 0) {
                String[] data = exportFile.getStringValue().split(",");

                if (data.length == 1) {
                    // This signals that the latest import should be stored in BibTeX
                    // format to the given file.
                    if (loaded.size() > 0) {
                        ParserResult pr =
                            loaded.elementAt(loaded.size() - 1);
                        if (!pr.isInvalid()) {
                            try {
                                System.out.println(Globals.lang("Saving") + ": " + data[0]);
                                SaveSession session = FileActions.saveDatabase(pr.getDatabase(),
                                    pr.getMetaData(), new File(data[0]), Globals.prefs,
                                    false, false, Globals.prefs.get("defaultEncoding"), false);
                                // Show just a warning message if encoding didn't work for all characters:
                                if (!session.getWriter().couldEncodeAll())
                                    System.err.println(Globals.lang("Warning")+": "+
                                        Globals.lang("The chosen encoding '%0' could not encode the following characters: ",
                                        session.getEncoding())+session.getWriter().getProblemCharacters());
                                session.commit();
                            } catch (SaveException ex) {
                                System.err.println(Globals.lang("Could not save file") + " '"
                                    + data[0] + "': " + ex.getMessage());
                            }
                        }
                    } else
                        System.err.println(Globals.lang(
                                "The output option depends on a valid import option."));
                } else if (data.length == 2) {
                    // This signals that the latest import should be stored in the given
                    // format to the given file.
                    ParserResult pr = loaded.elementAt(loaded.size() - 1);

                    // Set the global variable for this database's file directory before exporting,
                    // so formatters can resolve linked files correctly.
                    // (This is an ugly hack!)
                    File theFile = pr.getFile();
                    if (!theFile.isAbsolute())
                        theFile = theFile.getAbsoluteFile();
                    MetaData metaData = pr.getMetaData();
                    metaData.setFile(theFile);
                    Globals.prefs.fileDirForDatabase = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
                    Globals.prefs.databaseFile = metaData.getFile();
                    System.out.println(Globals.lang("Exporting") + ": " + data[0]);
                    IExportFormat format = ExportFormats.getExportFormat(data[1]);
                    if (format != null) {
                        // We have an ExportFormat instance:
                        try {
                            format.performExport(pr.getDatabase(), 
                                    pr.getMetaData(), data[0], pr.getEncoding(), null);
                        } catch (Exception ex) {
                            System.err.println(Globals.lang("Could not export file")
                                + " '" + data[0] + "': " + ex.getMessage());
                        }
                    }
                    else
                        System.err.println(Globals.lang("Unknown export format")
                                + ": " + data[1]);

                }
            } else
                System.err.println(Globals.lang(
                        "The output option depends on a valid import option."));
        }

        //Util.pr(": Finished export");

        if (exportPrefs.isInvoked()) {
            try {
                Globals.prefs.exportPreferences(exportPrefs.getStringValue());
            } catch (IOException ex) {
                Util.pr(ex.getMessage());
            }
        }


        if (!blank.isInvoked() && auxImExport.isInvoked()) {
            boolean usageMsg = false;

            if (loaded.size() > 0) // bibtex file loaded
             {
                String[] data = auxImExport.getStringValue().split(",");

                if (data.length == 2) {
                    ParserResult pr = loaded.firstElement();
                    AuxCommandLine acl = new AuxCommandLine(data[0], pr.getDatabase());
                    BibtexDatabase newBase = acl.perform();

                    boolean notSavedMsg = false;

                    // write an output, if something could be resolved
                    if (newBase != null) {
                        if (newBase.getEntryCount() > 0) {
                            String subName = Util.getCorrectFileName(data[1], "bib");

                            try {
                                System.out.println(Globals.lang("Saving") + ": "
                                    + subName);
                                SaveSession session = FileActions.saveDatabase(newBase, new MetaData(), // no Metadata
                                    new File(subName), Globals.prefs, false, false,
                                    Globals.prefs.get("defaultEncoding"), false);
                                // Show just a warning message if encoding didn't work for all characters:
                                if (!session.getWriter().couldEncodeAll())
                                    System.err.println(Globals.lang("Warning")+": "+
                                        Globals.lang("The chosen encoding '%0' could not encode the following characters: ",
                                        session.getEncoding())+session.getWriter().getProblemCharacters());
                                session.commit();
                            } catch (SaveException ex) {
                                System.err.println(Globals.lang("Could not save file")
                                    + " '" + subName + "': " + ex.getMessage());
                            }

                            notSavedMsg = true;
                        }
                    }

                    if (!notSavedMsg)
                        System.out.println(Globals.lang("no database generated"));
                } else
                    usageMsg = true;
            } else
                usageMsg = true;

            if (usageMsg) {
                System.out.println(Globals.lang("no base-bibtex-file specified"));
                System.out.println(Globals.lang("usage") + " :");
                System.out.println(
                    "jabref --aux infile[.aux],outfile[.bib] base-bibtex-file");
            }
        }

        return loaded;
    }

    /**
     * Run an entry fetcher from the command line.
     * 
     * Note that this only works headlessly if the EntryFetcher does not show
     * any GUI.
     * 
     * @param fetchCommand
     *            A string containing both the fetcher to use (id of
     *            EntryFetcherExtension minus Fetcher) and the search query,
     *            separated by a :
     * @return A parser result containing the entries fetched or null if an
     *         error occurred.
     */
    protected ParserResult fetch(String fetchCommand) {

        if (fetchCommand == null || !fetchCommand.contains(":") ||
            fetchCommand.split(":").length != 2) {
            System.out.println(Globals.lang("Expected syntax for --fetch='<name of fetcher>:<query>'"));
            System.out.println(Globals.lang("The following fetchers are available:"));
            return null;
        }

        String engine = fetchCommand.split(":")[0];
        String query = fetchCommand.split(":")[1];

        EntryFetcher fetcher = null;
        for (EntryFetcherExtension e : JabRefPlugin.getInstance(PluginCore.getManager())
            .getEntryFetcherExtensions()) {
            if (engine.toLowerCase().equals(e.getId().replaceAll("Fetcher", "").toLowerCase()))
                fetcher = e.getEntryFetcher();
        }

        if (fetcher == null) {
            System.out.println(Globals.lang("Could not find fetcher '%0'", engine));
            System.out.println(Globals.lang("The following fetchers are available:"));
            for (EntryFetcherExtension e : JabRefPlugin.getInstance(PluginCore.getManager())
                .getEntryFetcherExtensions()) {
                System.out.println("  " + e.getId().replaceAll("Fetcher", "").toLowerCase());
            }
            return null;
        }

        System.out.println(Globals.lang("Running Query '%0' with fetcher '%1'.", query, engine) +
            " " + Globals.lang("Please wait!"));
        Collection<BibtexEntry> result = new ImportInspectionCommandLine().query(query, fetcher);

        if (result == null || result.size() == 0) {
            System.out.println(Globals.lang(
                "Query '%0' with fetcher '%1' did not return any results.", query, engine));
            return null;
        }

        return new ParserResult(result);
    }
    
    private void setLookAndFeel() {
        try {
            String systemLnF;
            // * Look first into the Preferences
            // * Fallback to the System Look & Fell
            if (Globals.prefs.getBoolean("useDefaultLookAndFeel")) {
                systemLnF = UIManager.getSystemLookAndFeelClassName();
            } else {
                systemLnF = Globals.prefs.get("lookAndFeel");
            }

            // At all cost, avoid ending up with the Metal look and feel:
            if (systemLnF.equals("javax.swing.plaf.metal.MetalLookAndFeel")) {
                Plastic3DLookAndFeel lnf = new Plastic3DLookAndFeel();
                Plastic3DLookAndFeel.setCurrentTheme(new SkyBluer());
                com.jgoodies.looks.Options.setPopupDropShadowEnabled(true);
                UIManager.setLookAndFeel(lnf);
            }
            else {
                UIManager.setLookAndFeel(systemLnF);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // In JabRef v2.8, we did it only on NON-Mac. Now, we try on all platforms
        boolean overrideDefaultFonts = Globals.prefs.getBoolean("overrideDefaultFonts");
        if (overrideDefaultFonts) {
            int fontSize = Globals.prefs.getInt("menuFontSize");
            UIDefaults defaults = UIManager.getDefaults();
            Enumeration<Object> keys = defaults.keys();
            Double zoomLevel = null;
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                if ((key instanceof String) && (((String) key).endsWith(".font"))) {
                    FontUIResource font = (FontUIResource) UIManager.get(key);
                    if (zoomLevel == null) {
                        // zoomLevel not yet set, calculate it based on the first found font
                        zoomLevel = new Double(fontSize) / new Double(font.getSize());
                    }
                    font = new FontUIResource(font.getName(), font.getStyle(), fontSize);
                    defaults.put(key, font);
                }
            }
            if (zoomLevel != null) {
                GUIGlobals.zoomLevel = zoomLevel;
            }
        }
    }

	public void openWindow(Vector<ParserResult> loaded) {
        if (!graphicFailure && !disableGui.isInvoked()) {
            // Call the method performCompatibilityUpdate(), which does any
            // necessary changes for users with a preference set from an older
            // Jabref version.
            Util.performCompatibilityUpdate();


            // Set up custom or default icon theme:
            GUIGlobals.setUpIconTheme();

            // TODO: remove temporary registering of external file types?
            Globals.prefs.updateExternalFileTypes();

           // This property is set to make the Mac OSX Java VM move the menu bar to
            // the top
            // of the screen, where Mac users expect it to be.
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            // Set antialiasing on everywhere. This only works in JRE >= 1.5.
            // Or... it doesn't work, period.
            //System.setProperty("swing.aatext", "true");
            // TODO test and maybe remove this! I found this commented out with no additional info ( payload@lavabit.com )

            // Set the Look & Feel for Swing.
            try {
                setLookAndFeel();
            } catch (Throwable e) {
                e.printStackTrace();
            }


            // If the option is enabled, open the last edited databases, if any.
            if (!blank.isInvoked() && Globals.prefs.getBoolean("openLastEdited") && (Globals.prefs.get("lastEdited") != null)) {
                // How to handle errors in the databases to open?
                String[] names = Globals.prefs.getStringArray("lastEdited");
                lastEdLoop: 
                for (int i = 0; i < names.length; i++) {
                    File fileToOpen = new File(names[i]);

                    for (int j = 0; j < loaded.size(); j++) {
                        ParserResult pr = loaded.elementAt(j);

                        if ((pr.getFile() != null) &&pr.getFile().equals(fileToOpen))
                            continue lastEdLoop;
                    }

                    if (fileToOpen.exists()) {
                        ParserResult pr = openBibFile(names[i], false);

                        if (pr != null) {

                            if (pr == ParserResult.INVALID_FORMAT) {
                                System.out.println(Globals.lang("Error opening file")+" '"+fileToOpen.getPath()+"'");
                            }
                            else if (pr != ParserResult.FILE_LOCKED)
                                loaded.add(pr);

                        }
                    }
                }
            }

            GUIGlobals.init();
            GUIGlobals.CURRENTFONT =
                new Font(Globals.prefs.get("fontFamily"), Globals.prefs.getInt("fontStyle"),
                    Globals.prefs.getInt("fontSize"));

            //Util.pr(": Initializing frame");
            jrf = new JabRefFrame();

            // Add all loaded databases to the frame:
            
	        boolean first = true;
            List<File> postponed = new ArrayList<File>();
            List<ParserResult> failed = new ArrayList<ParserResult>();
            List<ParserResult> toOpenTab = new ArrayList<ParserResult>();
            if (loaded.size() > 0) {
                for (Iterator<ParserResult> i = loaded.iterator(); i.hasNext();){
                    ParserResult pr = i.next();
                    if (pr.isInvalid()) {
                        failed.add(pr);
                        i.remove();
                    }
                    else if (!pr.isPostponedAutosaveFound()) {
                        if (pr.toOpenTab()) {
                            // things to be appended to an opened tab should be done after opening all tabs
                            // add them to the list
                            toOpenTab.add(pr);
                        } else {
                            jrf.addParserResult(pr, first);
                            first = false;
                        }
                    }
                    else {
                        i.remove();
                        postponed.add(pr.getFile());
                    }
                }
            }

            // finally add things to the currently opened tab
            for (ParserResult pr: toOpenTab) {
                jrf.addParserResult(pr, first);
                first = false;
            }

            if (loadSess.isInvoked())
                jrf.loadSessionAction.actionPerformed(new java.awt.event.ActionEvent(
                        jrf, 0, ""));

            if (splashScreen != null) {// do this only if splashscreen was actually created
                splashScreen.dispose();
                splashScreen = null;
            }

            /*JOptionPane.showMessageDialog(null, Globals.lang("Please note that this "
                +"is an early beta version. Do not use it without backing up your files!"),
                    Globals.lang("Beta version"), JOptionPane.WARNING_MESSAGE);*/


            // Start auto save timer:
            if (Globals.prefs.getBoolean("autoSave"))
                Globals.startAutoSaveManager(jrf);

            // If we are set to remember the window location, we also remember the maximised
            // state. This needs to be set after the window has been made visible, so we
            // do it here:
            if (Globals.prefs.getBoolean("windowMaximised")) {
                jrf.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }

            jrf.setVisible(true);

            if (Globals.prefs.getBoolean("windowMaximised")) {
                jrf.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }

            // TEST TEST TEST TEST TEST TEST
            startSidePanePlugins(jrf);

            for (ParserResult pr : failed) {
                String message = "<html>"+Globals.lang("Error opening file '%0'.", pr.getFile().getName())
                    +"<p>"+pr.getErrorMessage()+"</html>";

                JOptionPane.showMessageDialog(jrf, message, Globals.lang("Error opening file"),
                    JOptionPane.ERROR_MESSAGE);
            }

            for (int i = 0; i < loaded.size(); i++) {
                ParserResult pr = loaded.elementAt(i);
                if (Globals.prefs.getBoolean("displayKeyWarningDialogAtStartup") && pr.hasWarnings()) {
                    String[] wrns = pr.warnings();
                    StringBuilder wrn = new StringBuilder();
                    for (int j = 0; j<Math.min(MAX_DIALOG_WARNINGS, wrns.length); j++)
                        wrn.append(j + 1).append(". ").append(wrns[j]).append("\n");
                    if (wrns.length > MAX_DIALOG_WARNINGS) {
                        wrn.append("... ");
                        wrn.append(Globals.lang("%0 warnings", String.valueOf(wrns.length)));
                    }
                    else if (wrn.length() > 0)
                        wrn.deleteCharAt(wrn.length() - 1);
                    jrf.showBaseAt(i);
                    JOptionPane.showMessageDialog(jrf, wrn.toString(),
                        Globals.lang("Warnings")+" ("+pr.getFile().getName()+")",
                        JOptionPane.WARNING_MESSAGE);
                }
            }

            // After adding the databases, go through each and see if
            // any post open actions need to be done. For instance, checking
            // if we found new entry types that can be imported, or checking
            // if the database contents should be modified due to new features
            // in this version of JabRef.
            // Note that we have to check whether i does not go over baseCount().
            // This is because importToOpen might have been used, which adds to
            // loaded, but not to baseCount()
            for (int i = 0; (i < loaded.size()) && (i < jrf.baseCount()); i++) {
                ParserResult pr = loaded.elementAt(i);
                BasePanel panel = jrf.baseAt(i);
                OpenDatabaseAction.performPostOpenActions(panel, pr, true);
            }

            //Util.pr(": Finished adding panels");

            // If any database loading was postponed due to an autosave, schedule them
            // for handing now:
            if (postponed.size() > 0) {
                AutosaveStartupPrompter asp = new AutosaveStartupPrompter(jrf, postponed);
                SwingUtilities.invokeLater(asp);
            }

            if (loaded.size() > 0) {
                jrf.tabbedPane.setSelectedIndex(0);
                new FocusRequester(((BasePanel) jrf.tabbedPane.getComponentAt(0)).mainTable);
            }
        } else
            System.exit(0);
    }

    /**
     * Go through all registered instances of SidePanePlugin, and register them
     * in the SidePaneManager.
     *
     * @param jrf The JabRefFrame.
     */
    private void startSidePanePlugins(JabRefFrame jrf) {

        JabRefPlugin jabrefPlugin = JabRefPlugin.getInstance(PluginCore.getManager());
        List<_JabRefPlugin.SidePanePluginExtension> plugins = jabrefPlugin.getSidePanePluginExtensions();
        for (_JabRefPlugin.SidePanePluginExtension extension : plugins) {
            SidePanePlugin plugin = extension.getSidePanePlugin();
            plugin.init(jrf, jrf.sidePaneManager);
            SidePaneComponent comp = plugin.getSidePaneComponent();
            jrf.sidePaneManager.register(comp.getName(), comp);
            jrf.addPluginMenuItem(plugin.getMenuItem());
        }
    }

    public static ParserResult openBibFile(String name, boolean ignoreAutosave) {
    	Globals.logger(Globals.lang("Opening") + ": " + name);
        File file = new File(name);
        if (!file.exists()) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            System.err.println(Globals.lang("Error")+": "+Globals.lang("File not found"));
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

            if (!Util.waitForFileLock(file, 10)) {
                System.out.println(Globals.lang("Error opening file")+" '"+name+"'. "+
                    "File is locked by another JabRef instance.");
                return ParserResult.FILE_LOCKED;
            }

            String encoding = Globals.prefs.get("defaultEncoding");
            ParserResult pr = OpenDatabaseAction.loadDatabase(file, encoding);
            if (pr == null) {
                pr = new ParserResult(null, null, null);
                pr.setFile(file);
                pr.setInvalid(true);
                return pr;

            }
            pr.setFile(file);
            if (pr.hasWarnings()) {
                String[] warn = pr.warnings();
                for (int i=0; i<warn.length; i++)
                    System.out.println(Globals.lang("Warning")+": "+warn[i]);

            }
            return pr;
        } catch (Throwable ex) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            pr.setErrorMessage(ex.getMessage());
            return pr;
        }

    }

    public static ParserResult importFile(String argument){
    	String[] data = argument.split(",");
        try {
            if ((data.length > 1) && !"*".equals(data[1])) {
                System.out.println(Globals.lang("Importing") + ": " + data[0]);
                try {
                    List<BibtexEntry> entries;
                    if (Globals.ON_WIN) {
                      entries = Globals.importFormatReader.importFromFile(data[1], data[0], jrf);
                    }
                    else {
                      entries = Globals.importFormatReader.importFromFile( data[1],
                                data[0].replaceAll("~", System.getProperty("user.home")), jrf );
                    }
                    return new ParserResult(entries);
                } catch (IllegalArgumentException ex) {
                    System.err.println(Globals.lang("Unknown import format")+": "+data[1]);
                    return null;
                }
            } else {
                // * means "guess the format":
                System.out.println(Globals.lang("Importing in unknown format")
                        + ": " + data[0]);

                Pair<String, ParserResult>  importResult;
                if (Globals.ON_WIN) {
            	  importResult = Globals.importFormatReader.importUnknownFormat(data[0]);
                }
                else {
                  importResult = Globals.importFormatReader.importUnknownFormat(
                                 data[0].replaceAll("~", System.getProperty("user.home")));
                }
            	
            	if (importResult != null){
            		System.out.println(Globals.lang("Format used") + ": "
                        + importResult.p);
            		
            		return importResult.v;
            	} else {
            		System.out.println(Globals.lang(
                                "Could not find a suitable import format."));
                }
            }
        } catch (IOException ex) {
            System.err.println(Globals.lang("Error opening file") + " '"
                    + data[0] + "': " + ex.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Will open a file (like importFile), but will also request JabRef to focus on this database 
     * @param argument See importFile.
     * @return ParserResult with setToOpenTab(true)
     */
    public static ParserResult importToOpenBase(String argument) {
    	ParserResult result = importFile(argument);
    	
    	if (result != null)
    		result.setToOpenTab(true);
    	
    	return result;
    }
}
