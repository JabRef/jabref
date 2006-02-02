/*
 * Copyright (C) 2003 Morten O. Alver, Nizar N. Batada
 * 
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */
package net.sf.jabref;

import com.jgoodies.plaf.FontSizeHints;
import net.sf.jabref.export.*;
import net.sf.jabref.imports.*;
import net.sf.jabref.wizard.auximport.*;
import net.sf.jabref.remote.RemoteListener;

import gnu.dtools.ritopt.*;
import java.awt.Font;
import java.awt.Frame;

import java.io.*;
import java.io.File;

import java.util.*;

import javax.swing.*;

import com.jgoodies.plaf.plastic.Plastic3DLookAndFeel;
import com.jgoodies.plaf.windows.ExtWindowsLookAndFeel;




//import javax.swing.UIManager;
//import javax.swing.UIDefaults;
//import javax.swing.UnsupportedLookAndFeelException;
public class JabRef {
    public static JabRef ths;
    public static RemoteListener remoteListener = null;
    public JabRefFrame jrf;
    public Options options;
    public Frame splashScreen = null;

    boolean graphicFailure = false;

    StringOption importFile, exportFile, exportPrefs, importPrefs, auxImExport, importToOpenBase;
    BooleanOption helpO, disableGui, blank, loadSess;
    /*
    * class StringArrayOption extends ArrayOption { public public void
    * modify(String value) { } public void modify(String[] value) { } public
    * Object[] getObjectArray() { return null; } public String getTypeName() {
    * return "Strings"; } public String getStringValue() { return ""; } public
    * Object getObject() { return null; } }
    */
    public static void main(String[] args) {
        new JabRef(args);
    }

    public JabRef(String[] args) {

        /*
        String aut = "{Bill and Bob Alver}, Jr., Morten Omholt and Alfredsen, Jo A. and Øie, G. and Yngvar von Olsen";
        System.out.println("lastnameFirst: "+ImportFormatReader.fixAuthor_lastNameFirst(aut));
        System.out.println("lastnameFirstCommas: "+ImportFormatReader.fixAuthor_lastNameFirstCommas(aut,false));
        System.out.println("lastnameFirstCommas (abbr): "+ImportFormatReader.fixAuthor_lastNameFirstCommas(aut,true));
        System.out.println("firstNameFirst: "+ImportFormatReader.fixAuthor_firstNameFirst(aut));
        System.out.println("firstNameFirstCommas: "+ImportFormatReader.fixAuthor_firstNameFirstCommas(aut, false));
        System.out.println("forAlphabetization: "+ImportFormatReader.fixAuthorForAlphabetization(aut));
        */
        ths = this;
        JabRefPreferences prefs = JabRefPreferences.getInstance();
        Globals.prefs = prefs;
        Globals.setLanguage(prefs.get("language"), "");
                
        Globals.importFormatReader.resetImportFormats();
        BibtexEntryType.loadCustomEntryTypes(prefs);
        // Read list(s) of journal names and abbreviations:
        //Globals.turnOnFileLogging();

        Globals.initializeJournalNames();

        if (Globals.prefs.getBoolean("useRemoteServer")) {
            remoteListener = RemoteListener.openRemoteListener(this);
            if (remoteListener != null) {
                remoteListener.start();
            }

        // Unless we are alone, try to contact already running JabRef:
	    if (remoteListener == null) {
		    if (RemoteListener.sendToActiveJabRefInstance(args)) {
                // We have successfully sent our command line options through the socket to
                // another JabRef instance. So we assume it's all taken care of, and quit.
                System.out.println(Globals.lang("Arguments passed on to running JabRef instance. Shutting down."));
                System.exit(0);
            }
        }
	}

      /**
       * See if the user has a personal journal list set up. If so, add these
       * journal names and abbreviations to the list:
       */
      String personalJournalList = prefs.get("personalJournalList");
      if (personalJournalList != null) {
          try {
              Globals.journalAbbrev.readJournalList(new File(personalJournalList));
          } catch (FileNotFoundException e) {
              e.printStackTrace();
          }
      }

        
        //System.setProperty("sun.awt.noerasebackground", "true");
        
        //System.out.println(java.awt.Toolkit.getDefaultToolkit().getDesktopProperty("awt.dynamicLayoutSupported"));
        // Make sure of a proper cleanup when quitting (e.g. deleting temporary
        // files).
        System.runFinalizersOnExit(true);

        Vector loaded = processArguments(args, true);
        openWindow(loaded);
        //System.out.println("1");
    }

    private void setupOptions() {

        importFile = new StringOption("");
        exportFile = new StringOption("");
        helpO = new BooleanOption();
        disableGui = new BooleanOption();
        blank = new BooleanOption();
        loadSess = new BooleanOption();
        exportPrefs = new StringOption("jabref_prefs.xml");
        importPrefs = new StringOption("jabref_prefs.xml");
        auxImExport = new StringOption("");
        importToOpenBase = new StringOption("");

        options = new Options("JabRef "); // Create an options repository.
        options.setVersion(GUIGlobals.version);

        importFile.setDescription("imopoepuoeu"); //Globals.lang);
        options.register("nogui", 'n',
            Globals.lang("No GUI. Only process command line options."), disableGui);
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
        options.register("aux", 'a',
            Globals.lang("Subdatabase from aux") + ": " + Globals.lang("file")+"[.aux]" + ","+Globals.lang("new")+"[.bib]",
            auxImExport);
        options.register("blank", 'b', Globals.lang("Do not open any files at startup"), blank);

        options.register("importToOpen", '\0', Globals.lang("Import to open tab"), importToOpenBase);

        options.setUseMenu(false);
    }

    public Vector processArguments(String[] args, boolean initialStartup) {

        setupOptions();
        String[] leftOver = options.process(args);

        //Util.pr(": Options processed");

        if (initialStartup && helpO.isInvoked()) {
            System.out.println("jabref [options] [bibtex-file]\n");
            System.out.println(options.getHelp());

            String importFormats = Globals.importFormatReader.getImportFormatList();
            System.out.println(Globals.lang("Available import formats") + ":\n"
                + importFormats);

            // + ": biblioscape, bibtexml, endnote, inspec,\n\tisi, medline, ovid,
            // ris, scifinder, sixpack, jstor, silverplatter.");
            // To specify export formats, we need to take the custom export formats
            // into account.
            // So we iterate through the custom formats and add them.
            String outFormats = ": bibtexml, docbook, html, simplehtml";
            int length = outFormats.length();

            for (int i = 0; i < Globals.prefs.customExports.size(); i++) {
                String[] format = Globals.prefs.customExports.getElementAt(i);

                if ((length + format[0].length()) > 50) {
                    outFormats = outFormats + ",\n\t" + format[0];
                    length = format[0].length();
                } else {
                    outFormats = outFormats + ", " + format[0];
                    length += (1 + format[0].length());
                }
            }

            System.out.println(Globals.lang("Available export formats") + outFormats
                + ".");
            System.exit(0);
        }

        // First we quickly scan the command line parameters for any that signal
        // that the GUI
        // should not be opened. This is used to decide whether we should show the
        // splash screen or not.
        if (initialStartup && !disableGui.isInvoked()) {
            try {

                splashScreen = SplashScreen.splash();

            } catch (Throwable ex) {
                graphicFailure = true;
                System.err.println(Globals.lang("Unable to create graphical interface")
                    + ".");
            }
        }

        //Util.pr("JabRef "+GUIGlobals.version);
        // Vector to put imported/loaded database(s) in.
        Vector loaded = new Vector();
        Vector toImport = new Vector();
        if (!blank.isInvoked() && (leftOver.length > 0))  {
            for (int i = 0; i < leftOver.length; i++) {
                // Leftover arguments are interpreted as bib files to open.

                ParserResult pr = openBibFile(leftOver[i]);

                if (pr != null) {
                    if (pr == ParserResult.INVALID_FORMAT)
                        // We will try to import this file instead:
                        toImport.add(leftOver[i]);
                    else
                        loaded.add(pr);
                } else {

                }
            }
        }

        //Util.pr(": Checked blank");

        if (!blank.isInvoked() && importFile.isInvoked()) {
            toImport.add(importFile.getStringValue());
        }

        if (toImport.size() > 0) for (int i=0; i<toImport.size(); i++) {
            String[] data = ((String)toImport.elementAt(i)).split(",");

            /*if (data.length == 1) {
                // Load a bibtex file:
                ParserResult pr = openBibFile(data[0]);

                if (pr != null)
                    loaded.add(pr);
            } else if (data.length == 2) {*/
                // Import a database in a certain format.
                try {

                    if ((data.length > 1) && !"*".equals(data[1])) {
                        System.out.println(Globals.lang("Importing") + ": " + data[0]);
                        List entries =
                            Globals.importFormatReader.importFromFile(data[1],
                                data[0].replaceAll("~", System.getProperty("user.home")));
                        BibtexDatabase base = ImportFormatReader.createDatabase(entries);
                        ParserResult pr = new ParserResult(base, null, new HashMap());
                        loaded.add(pr);
                        
                    } else {
                        // * means "guess the format":
                        System.out.println(Globals.lang("Importing in unknown format")
                            + ": " + data[0]);

                        Object[] o =
                            Globals.importFormatReader.importUnknownFormat(data[0]
                                .replaceAll("~", System.getProperty("user.home")));
                        String formatName = (String) o[0];

                        if (formatName == null) {
                            System.err.println(Globals.lang("Error opening file")+" '"+data[0]+"'");    
                        }
                        else if (formatName.equals(ImportFormatReader.BIBTEX_FORMAT)) {
                            ParserResult pr = (ParserResult)o[1];
                            loaded.add(pr);  

                        }
                        else {
                            List entries = (java.util.List) o[1];
                            if (entries != null)
                                System.out.println(Globals.lang("Format used") + ": "
                                    + formatName);
                            else
                                System.out.println(Globals.lang(
                                        "Could not find a suitable import format."));

                            if (entries != null) {
                                BibtexDatabase base = ImportFormatReader.createDatabase(entries);
                                ParserResult pr = new ParserResult(base, null, new HashMap());
                        
                                //pr.setFile(new File(data[0]));
                                loaded.add(pr);
                            }
                        }
                    }
                } catch (IOException ex) {
                    System.err.println(Globals.lang("Error opening file") + " '"
                        + data[0] + "': " + ex.getMessage());
                }


        }


        if (/*!initialStartup && */!blank.isInvoked() && importToOpenBase.isInvoked()) {
            String[] data = importToOpenBase.getStringValue().split(",");

            /*if (data.length == 1) {
                // Load a bibtex file:
                ParserResult pr = openBibFile(data[0]);

                if (pr != null) {
                    pr.setToOpenTab(true);
                    loaded.add(pr);
                }
            } else if (data.length == 2) {*/
                // Import a database in a certain format.
                try {

                    if ((data.length > 1) && !"*".equals(data[1])) {
                        System.out.println(Globals.lang("Importing") + ": " + data[0]);
                        List entries =
                            Globals.importFormatReader.importFromFile(data[1],
                                data[0].replaceAll("~", System.getProperty("user.home")));
                        BibtexDatabase base = ImportFormatReader.createDatabase(entries);
                        ParserResult pr = new ParserResult(base, null, new HashMap());
                        pr.setToOpenTab(true);
                        loaded.add(pr);

                    } else {
                        // * means "guess the format":
                        System.out.println(Globals.lang("Importing in unknown format")
                            + ": " + data[0]);

                        Object[] o =
                            Globals.importFormatReader.importUnknownFormat(data[0]
                                .replaceAll("~", System.getProperty("user.home")));
                        String formatName = (String) o[0];

                        if (formatName.equals(ImportFormatReader.BIBTEX_FORMAT)) {
                            ParserResult pr = (ParserResult)o[1];
                            pr.setToOpenTab(true);
                            loaded.add(pr);

                        }
                        else {
                            List entries = (java.util.List) o[1];
                            if (entries != null)
                                System.out.println(Globals.lang("Format used") + ": "
                                    + formatName);
                            else
                                System.out.println(Globals.lang(
                                        "Could not find a suitable import format."));

                            if (entries != null) {
                                BibtexDatabase base = ImportFormatReader.createDatabase(entries);
                                ParserResult pr = new ParserResult(base, null, new HashMap());

                                //pr.setFile(new File(data[0]));
                                pr.setToOpenTab(true);
                                loaded.add(pr);
                            }
                        }
                    }
                } catch (IOException ex) {
                    System.err.println(Globals.lang("Error opening file") + " '"
                        + data[0] + "': " + ex.getMessage());
                }


        }
        //Util.pr(": Finished import");

        if (exportFile.isInvoked()) {
            if (loaded.size() > 0) {
                String[] data = exportFile.getStringValue().split(",");

                if (data.length == 1) {
                    // This signals that the latest import should be stored in BibTeX
                    // format to the given file.
                    if (loaded.size() > 0) {
                        ParserResult pr =
                            (ParserResult) loaded.elementAt(loaded.size() - 1);

                        try {
                            System.out.println(Globals.lang("Saving") + ": " + data[0]);
                            SaveSession session = FileActions.saveDatabase(pr.getDatabase(),
                                new MetaData(pr.getMetaData(),pr.getDatabase()), new File(data[0]), Globals.prefs,
                                false, false, Globals.prefs.get("defaultEncoding"));
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
                    } else
                        System.err.println(Globals.lang(
                                "The output option depends on a valid import option."));
                } else if (data.length == 2) {
                    // This signals that the latest import should be stored in the given
                    // format to the given file.
                    ParserResult pr = (ParserResult) loaded.elementAt(loaded.size() - 1);

                    // We first try to find a matching custom export format.
                    boolean foundCustom = false;

                    for (int i = 0; i < Globals.prefs.customExports.size(); i++) {
                        String[] format = Globals.prefs.customExports.getElementAt(i);

                        if (format[0].equals(data[1])) {
                            // Found the correct export format here.
                            //System.out.println(format[0]+" "+format[1]+" "+format[2]);
                            try {
                                File lfFile = new File(format[1]);

                                //System.out.println(lfFile.getName());
                                String fname = (lfFile.getName().split("\\."))[0];
                                FileActions.exportDatabase(pr.getDatabase(),
                                    lfFile.getParent() + File.separator, fname,
                                    new File(data[0]), pr.getEncoding());
                                System.out.println(Globals.lang("Exporting") + ": "
                                    + data[0]);
                            } catch (Exception ex) {
                                //ex.printStackTrace();
                                System.err.println(Globals.lang("Could not export file")
                                    + " '" + data[0] + "': " + ex.getMessage());
                            }

                            foundCustom = true;

                            break;
                        }
                    }

                    if (!foundCustom) {
                        try {
                            System.out.println(Globals.lang("Exporting") + ": " + data[0]);
                            FileActions.exportDatabase(pr.getDatabase(), data[1],
                                new File(data[0]), pr.getEncoding());
                        } catch (NullPointerException ex2) {
                            System.err.println(Globals.lang("Unknown export format")
                                + ": " + data[1]);
                        } catch (Exception ex) {
                            System.err.println(Globals.lang("Could not export file")
                                + " '" + data[0] + "': " + ex.getMessage());
                        }
                    }
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


	if (importPrefs.isInvoked()) {
	    try {
		Globals.prefs.importPreferences(importPrefs.getStringValue());
		BibtexEntryType.loadCustomEntryTypes(Globals.prefs);
	    }
	    catch (IOException ex) {
		Util.pr(ex.getMessage());
	    }
	}

        if (!blank.isInvoked() && auxImExport.isInvoked()) {
            boolean usageMsg = false;

            if (loaded.size() > 0) // bibtex file loaded
             {
                String[] data = auxImExport.getStringValue().split(",");

                if (data.length == 2) {
                    ParserResult pr = (ParserResult) loaded.firstElement();
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
                                    Globals.prefs.get("defaultEncoding"));
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

    public void openWindow(Vector loaded) {
        if (!graphicFailure && !disableGui.isInvoked()) {
            // Call the method performCompatibilityUpdate(), which does any
            // necessary changes for users with a preference set from an older
            // Jabref version.
            Util.performCompatibilityUpdate();

           // This property is set to make the Mac OSX Java VM move the menu bar to
            // the top
            // of the screen, where Mac users expect it to be.
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            // Set antialiasing on everywhere. This only works in JRE >= 1.5.
            // Or... it doesn't work, period.
            //System.setProperty("swing.aatext", "true");
            // If we are not on Mac, deal with font sizes and LookAndFeels:
            if (!Globals.ON_MAC) {
                int fontSizes = Globals.prefs.getInt("menuFontSize");

                String defaultLookAndFeel;

                if (Globals.ON_WIN)
                    defaultLookAndFeel = GUIGlobals.windowsDefaultLookAndFeel;
                else
                    defaultLookAndFeel = GUIGlobals.linuxDefaultLookAndFeel;

                String lookAndFeel = null;

                if (!Globals.prefs.getBoolean("useDefaultLookAndFeel"))
                    lookAndFeel = Globals.prefs.get("lookAndFeel");
                else
                    lookAndFeel = defaultLookAndFeel;

                LookAndFeel lnf = null;

                //Class plastic =
                // Class.forName("com.jgoodies.plaf.plastic.PlasticLookAndFeel");
                //PlasticLookAndFeel lnf = new
                // com.jgoodies.plaf.plastic.Plastic3DLookAndFeel();
                Object objLnf = null;


                try {
                    //lnf2 =
                    // Class.forName("com.jgoodies.plaf.plastic.Plastic3DLookAndFeel").newInstance();
                    if (lookAndFeel != null)
                        objLnf = Class.forName(lookAndFeel).newInstance();
                    else
                        objLnf = Class.forName(defaultLookAndFeel).newInstance();
                } catch (Exception ex) {
                    ex.printStackTrace();

                    try {
                        objLnf = Class.forName(defaultLookAndFeel).newInstance();
                    } catch (Exception ex2) {
                    }
                }

                if (objLnf != null)
                    lnf = (LookAndFeel) objLnf;

                // Set font sizes if we are using a JGoodies look and feel.
                if ((lnf != null) && (lnf instanceof Plastic3DLookAndFeel)) {
                    //MetalLookAndFeel.setCurrentTheme(new
                    // com.jgoodies.plaf.plastic.theme.SkyBluer());
                    Plastic3DLookAndFeel plLnf = (Plastic3DLookAndFeel) lnf;
                    plLnf.setFontSizeHints(new FontSizeHints(fontSizes, fontSizes,
                            fontSizes, fontSizes));
                } else if ((lnf != null) && (lnf instanceof ExtWindowsLookAndFeel)) {
                    //System.out.println("ttt");
                    ExtWindowsLookAndFeel plLnf = (ExtWindowsLookAndFeel) lnf;
                    plLnf.setFontSizeHints(new FontSizeHints(fontSizes, fontSizes,
                            fontSizes, fontSizes));
                }

                if (lnf != null) {
                    try {
                        UIManager.setLookAndFeel(lnf);
                        
                        if (!Globals.ON_WIN) {
                            UIManager.put("SimpleInternalFrame.activeTitleBackground", GUIGlobals.gradientBlue);
                            //UIManager.put("TabbedPane.selected", Color.red);
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        System.err.println("Trying to set system default Look&Feel...");

                        // if desired lnf could not be set, try system default
                        try {
                            UIManager.setLookAndFeel(UIManager
                                .getSystemLookAndFeelClassName());
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }

                    //LookAndFeel lnf = new com.sun.java.swing.plaf.gtk.GTKLookAndFeel();
                    //Look1AndFeel lnf = new
                    // com.incors.plaf.kunststoff.KunststoffLookAndFeel();
                    //com.incors.plaf.kunststoff.KunststoffLookAndFeel.setCurrentTheme(new
                    // com.incors.plaf.kunststoff.themes.KunststoffDesktopTheme());
                }
            }


            // If the option is enabled, open the last edited databases, if any.
            if (!blank.isInvoked() && Globals.prefs.getBoolean("openLastEdited") && (Globals.prefs.get("lastEdited") != null)) {
                // How to handle errors in the databases to open?
                String[] names = Globals.prefs.getStringArray("lastEdited");
lastEdLoop: 
                for (int i = 0; i < names.length; i++) {
                    File fileToOpen = new File(names[i]);

                    for (int j = 0; j < loaded.size(); j++) {
                        ParserResult pr = (ParserResult) loaded.elementAt(j);

                        if ((pr.getFile() != null) &&pr.getFile().equals(fileToOpen))
                            continue lastEdLoop;
                    }

                    if (fileToOpen.exists()) {
                        ParserResult pr = openBibFile(names[i]);

                        if (pr != null) {

			    if (pr == ParserResult.INVALID_FORMAT) {
				System.out.println(Globals.lang("Error opening file")+" '"+fileToOpen.getPath()+"'");
			    }
			    else
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
            if (loaded.size() > 0) {
                for (Iterator i=loaded.iterator(); i.hasNext();) {
                    ParserResult pr = (ParserResult)i.next();
		    jrf.addTab(pr.getDatabase(), pr.getFile(), pr.getMetaData(), pr.getEncoding(), first);
		    first = false;
                }
            }

            if (loadSess.isInvoked())
                jrf.loadSessionAction.actionPerformed(new java.awt.event.ActionEvent(
                        jrf, 0, ""));

            if (splashScreen != null) {// do this only if splashscreen was actually created
                splashScreen.dispose();
                splashScreen = null;
            }

            //Util.pr(": Showing frame");
            jrf.setVisible(true);

            for (int i = 0; i < loaded.size(); i++) {
                ParserResult pr = (ParserResult) loaded.elementAt(i);
                if (Globals.prefs.getBoolean("displayKeyWarningDialogAtStartup") && pr.hasWarnings()) {
                    String[] wrns = pr.warnings();
                    StringBuffer wrn = new StringBuffer();
                    for (int j = 0; j<wrns.length; j++)
                        wrn.append(j + 1).append(". ").append(wrns[j]).append("\n");
                    if (wrn.length() > 0)
                        wrn.deleteCharAt(wrn.length() - 1);
                    jrf.showBaseAt(i);
                    JOptionPane.showMessageDialog(jrf, wrn.toString(),
                        Globals.lang("Warnings"),
                        JOptionPane.WARNING_MESSAGE);
                }
            }

            //Util.pr(": Finished adding panels");

            if (loaded.size() > 0) {
                jrf.tabbedPane.setSelectedIndex(0);
                new FocusRequester(((BasePanel) jrf.tabbedPane.getComponentAt(0)).mainTable);
            }
        } else
            System.exit(0);
    }

    public static ParserResult openBibFile(String name) {
        System.out.println(Globals.lang("Opening") + ": " + name);

        try {
            File file = new File(name);
            String encoding = Globals.prefs.get("defaultEncoding");
            ParserResult pr = OpenDatabaseAction.loadDatabase(file, encoding);
            if (pr == null)
                return ParserResult.INVALID_FORMAT;
            pr.setFile(file);
            if (pr.hasWarnings()) {
                String[] warn = pr.warnings();
                for (int i=0; i<warn.length; i++)
                    System.out.println(Globals.lang("Warning")+": "+warn[i]);

            }
            return pr;
        } catch (Throwable ex) {
            //System.err.println(Globals.lang("Error opening file")+" '"+ name+"':
            // "+ex.getMessage());
            System.err.println(Globals.lang("Error opening file") + ": "
                + ex.getMessage());
        }

        return null;
    }
}
