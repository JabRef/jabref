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

//import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.plastic.Plastic3DLookAndFeel;
import com.jgoodies.plaf.windows.ExtWindowsLookAndFeel;

import net.sf.jabref.export.*;
import net.sf.jabref.imports.*;
import net.sf.jabref.wizard.auximport.*;

import gnu.dtools.ritopt.*;

import java.awt.Color;
import java.awt.Font;

import java.io.*;
import java.io.File;

import java.util.*;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;


//import javax.swing.UIManager;
//import javax.swing.UIDefaults;
//import javax.swing.UnsupportedLookAndFeelException;
public class JabRef {
    static JabRefFrame jrf;

    /*
     * class StringArrayOption extends ArrayOption { public public void
     * modify(String value) { } public void modify(String[] value) { } public
     * Object[] getObjectArray() { return null; } public String getTypeName() {
     * return "Strings"; } public String getStringValue() { return ""; } public
     * Object getObject() { return null; } }
     */
    public static void main(String[] args) {
        boolean graphicFailure = false;

        System.setProperty("sun.awt.noerasebackground", "true");

        //System.out.println(java.awt.Toolkit.getDefaultToolkit().getDesktopProperty("awt.dynamicLayoutSupported"));
        // Make sure of a proper cleanup when quitting (e.g. deleting temporary
        // files).
        System.runFinalizersOnExit(true);

        // ----------------------------------------------------------------
        // First instantiate preferences and set language.
        // ----------------------------------------------------------------
        JabRefPreferences prefs = new JabRefPreferences();
        Globals.prefs = prefs;
        BibtexEntryType.loadCustomEntryTypes(prefs);
        Globals.setLanguage(prefs.get("language"), "");

        // ----------------------------------------------------------------
        // Option processing using RitOpt
        // ----------------------------------------------------------------
        Options options = new Options("JabRef "); // Create an options repository.
        options.setVersion(GUIGlobals.version);

        StringOption importFile = new StringOption("");
        importFile.setDescription("imopoepuoeu"); //Globals.lang);

        StringOption exportFile = new StringOption("");
        BooleanOption helpO = new BooleanOption();
        BooleanOption disableGui = new BooleanOption();

        //exportFile.setHelpDescriptionSize(80);
        //exportFile.setFileCompleteOptionSize(12);
        BooleanOption loadSess = new BooleanOption();
        StringOption exportPrefs = new StringOption("jabref_prefs.xml");
        StringOption importPrefs = new StringOption("jabref_prefs.xml");
        StringOption auxImExport = new StringOption("");

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
        options.setUseMenu(false);

        String[] leftOver = options.process(args);

        if (helpO.isInvoked()) {
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

            for (int i = 0; i < prefs.customExports.size(); i++) {
                String[] format = prefs.customExports.getElementAt(i);

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

        /*
         * boolean openGui = true; if (args.length > 0) for (int i=0; i
         * <args.length; i++) { if (args[i].startsWith("-o")) openGui = false; if
         * (args[i].equals("-h")) { System.out.println("Help info goes here.");
         * System.exit(0); } }
         */
        SplashScreen ss = null;

        if (!disableGui.isInvoked()) {
            try {
                ss = new SplashScreen();
                ss.show();
            } catch (Throwable ex) {
                graphicFailure = true;
                System.err.println(Globals.lang("Unable to create graphical interface")
                    + ".");
            }
        }

        //Util.pr("JabRef "+GUIGlobals.version);
        // Vector to put imported/loaded database(s) in.
        Vector loaded = new Vector();

        if (leftOver.length > 0) {
            for (int i = 0; i < leftOver.length; i++) {
                // Leftover arguments are interpreted as bib files to open.
                ParserResult pr = openBibFile(leftOver[i]);

                if (pr != null)
                    loaded.add(pr);
            }
        }

        if (importFile.isInvoked()) {
            String[] data = importFile.getStringValue().split(",");

            if (data.length == 1) {
                // Load a bibtex file:
                ParserResult pr = openBibFile(data[0]);

                if (pr != null)
                    loaded.add(pr);
            } else if (data.length == 2) {
                // Import a database in a certain format.
                try {
                    List entries;

                    if (!"*".equals(data[1])) {
                        System.out.println(Globals.lang("Importing") + ": " + data[0]);
                        entries =
                            Globals.importFormatReader.importFromFile(data[1],
                                data[0].replaceAll("~", System.getProperty("user.home")));
                    } else {
                        // * means "guess the format":
                        System.out.println(Globals.lang("Importing in unknown format")
                            + ": " + data[0]);

                        Object[] o =
                            Globals.importFormatReader.importUnknownFormat(data[0]
                                .replaceAll("~", System.getProperty("user.home")));
                        String formatName = (String) o[0];
                        entries = (java.util.List) o[1];

                        if (entries != null)
                            System.out.println(Globals.lang("Format used") + ": "
                                + formatName);
                        else
                            System.out.println(Globals.lang(
                                    "Could not find a suitable import format."));
                    }

                    if (entries != null) {
                        BibtexDatabase base = ImportFormatReader.createDatabase(entries);
                        ParserResult pr = new ParserResult(base, new HashMap());
                        pr.setFile(new File(data[0]));
                        loaded.add(pr);
                    }
                } catch (IOException ex) {
                    System.err.println(Globals.lang("Error opening file") + " '"
                        + data[0] + "': " + ex.getMessage());
                }
            }
        }

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
                            FileActions.saveDatabase(pr.getDatabase(),
                                new MetaData(pr.getMetaData()), new File(data[0]), prefs,
                                false, false, prefs.get("defaultEncoding"));
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

                    for (int i = 0; i < prefs.customExports.size(); i++) {
                        String[] format = prefs.customExports.getElementAt(i);

                        if (format[0].equals(data[1])) {
                            // Found the correct export format here.
                            //System.out.println(format[0]+" "+format[1]+" "+format[2]);
                            try {
                                File lfFile = new File(format[1]);

                                //System.out.println(lfFile.getName());
                                String fname = (lfFile.getName().split("\\."))[0];
                                FileActions.exportDatabase(pr.getDatabase(),
                                    lfFile.getParent() + File.separator, fname,
                                    new File(data[0]), prefs);
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
                                new File(data[0]), prefs);
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

        if (exportPrefs.isInvoked()) {
            try {
                prefs.exportPreferences(exportPrefs.getStringValue());
            } catch (IOException ex) {
                Util.pr(ex.getMessage());
            }
        }


	if (importPrefs.isInvoked()) {
	    try {
		prefs.importPreferences(importPrefs.getStringValue());
		BibtexEntryType.loadCustomEntryTypes(prefs);
	    }
	    catch (IOException ex) {
		Util.pr(ex.getMessage());
	    }
	}

        if (auxImExport.isInvoked()) {
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
                                FileActions.saveDatabase(newBase, new MetaData(), // no Metadata
                                    new File(subName), prefs, false, false,
                                    prefs.get("defaultEncoding"));
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

        //openGui = false;
        if (!graphicFailure && !disableGui.isInvoked()) {
            // Call the method performCompatibilityUpdate(), which does any
            // necessary changes for users with a preference set from an older
            // Jabref version.
            Util.performCompatibilityUpdate();

            //Font fnt = new Font("plain", Font.PLAIN, 12);
            /*
             * Font fnt = new Font (prefs.get("menuFontFamily"),
             * prefs.getInt("menuFontStyle"), prefs.getInt("menuFontSize"));
             *
             * Object fnt = new UIDefaults.ProxyLazyValue
             * ("javax.swing.plaf.FontUIResource", null, new Object[] {
             * prefs.get("menuFontFamily"), new
             * Integer(prefs.getInt("menuFontStyle")), new
             * Integer(prefs.getInt("menuFontSize")) });
             *
             * UIManager.put("MenuBar.font", fnt); UIManager.put("MenuItem.font",
             * fnt); UIManager.put("RadioButtonMenuItem.font", fnt);
             * UIManager.put("CheckBoxMenuItem.font", fnt); UIManager.put("Menu.font",
             * fnt); UIManager.put("PopupMenu.font", fnt);
             */
            //"Plain", new Integer(Font.PLAIN), new Integer(10)});
            /*
             * UIManager.put("Button.font", fnt); UIManager.put("ToggleButton.font",
             * fnt); UIManager.put("RadioButton.font", fnt);
             * UIManager.put("CheckBox.font", fnt); UIManager.put("ColorChooser.font",
             * fnt); UIManager.put("ComboBox.font", fnt); UIManager.put("Label.font",
             * fnt); UIManager.put("List.font", fnt); UIManager.put("MenuBar.font",
             * fnt); UIManager.put("MenuItem.font", fnt);
             * UIManager.put("RadioButtonMenuItem.font", fnt);
             * UIManager.put("CheckBoxMenuItem.font", fnt); UIManager.put("Menu.font",
             * fnt); UIManager.put("PopupMenu.font", fnt);
             * UIManager.put("OptionPane.font", fnt); UIManager.put("Panel.font",
             * fnt); UIManager.put("ProgressBar.font", fnt);
             * UIManager.put("ScrollPane.font", fnt); UIManager.put("Viewport.font",
             * fnt); UIManager.put("TabbedPane.font", fnt);
             * UIManager.put("Table.font", fnt); UIManager.put("TableHeader.font",
             * fnt); UIManager.put("TextField.font", fnt);
             * UIManager.put("PasswordField.font", fnt);
             * UIManager.put("TextArea.font", fnt); UIManager.put("TextPane.font",
             * fnt); UIManager.put("EditorPane.font", fnt);
             * //UIManager.put("TitledBorder.font", fnt);
             * UIManager.put("ToolBar.font", fnt); UIManager.put("ToolTip.font", fnt);
             * UIManager.put("Tree.font", fnt);
             */
            // This property is set to make the Mac OSX Java VM move the menu bar to
            // the top
            // of the screen, where Mac users expect it to be.
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            // Set antialiasing on everywhere. This only works in JRE >= 1.5.
            // Or... it doesn't work, period.
            //System.setProperty("swing.aatext", "true");
            // If we are not on Mac, deal with font sizes and LookAndFeels:
            if (!Globals.ON_MAC) {
                int fontSizes = prefs.getInt("menuFontSize");

                String defaultLookAndFeel;

                if (Globals.ON_WIN)
                    defaultLookAndFeel = GUIGlobals.windowsDefaultLookAndFeel;
                else
                    defaultLookAndFeel = GUIGlobals.linuxDefaultLookAndFeel;

                String lookAndFeel = null;

                if (!prefs.getBoolean("useDefaultLookAndFeel"))
                    lookAndFeel = prefs.get("lookAndFeel");
                else
                    lookAndFeel = defaultLookAndFeel;

                LookAndFeel lnf = null;

                //Class plastic =
                // Class.forName("com.jgoodies.plaf.plastic.PlasticLookAndFeel");
                //PlasticLookAndFeel lnf = new
                // com.jgoodies.plaf.plastic.Plastic3DLookAndFeel();
                Object objLnf = null;

                //Util.pr(lookAndFeel);
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
            if (prefs.getBoolean("openLastEdited") && (prefs.get("lastEdited") != null)) {
                // How to handle errors in the databases to open?
                String[] names = prefs.getStringArray("lastEdited");
lastEdLoop: 
                for (int i = 0; i < names.length; i++) {
                    File fileToOpen = new File(names[i]);

                    for (int j = 0; j < loaded.size(); j++) {
                        ParserResult pr = (ParserResult) loaded.elementAt(j);

                        if (pr.getFile().equals(fileToOpen))
                            continue lastEdLoop;
                    }

                    if (fileToOpen.exists()) {
                        ParserResult pr = openBibFile(names[i]);

                        if (pr != null)
                            loaded.add(pr);
                    }
                }
            }

            GUIGlobals.init();
            GUIGlobals.CURRENTFONT =
                new Font(prefs.get("fontFamily"), prefs.getInt("fontStyle"),
                    prefs.getInt("fontSize"));

            jrf = new JabRefFrame();

            // Add all loaded databases to the frame:
            if (loaded.size() > 0) {
                for (int i = 0; i < loaded.size(); i++) {
                    ParserResult pr = (ParserResult) loaded.elementAt(i);
                    jrf.addTab(pr.getDatabase(), pr.getFile(), pr.getMetaData(), (i == 0));
                }
            }

            if (loadSess.isInvoked())
                jrf.loadSessionAction.actionPerformed(new java.awt.event.ActionEvent(
                        jrf, 0, ""));

            ss.dispose();

            jrf.setVisible(true);

            if (loaded.size() > 0) {
                jrf.tabbedPane.setSelectedIndex(0);
                new FocusRequester(((BasePanel) jrf.tabbedPane.getComponentAt(0)).entryTable);
            }
        } else
            System.exit(0);
    }

    public static ParserResult openBibFile(String name) {
        System.out.println(Globals.lang("Opening") + ": " + name);

        try {
            File file = new File(name);
            String encoding = Globals.prefs.get("defaultEncoding");
            ParserResult pr = ImportFormatReader.loadDatabase(file, encoding);
            pr.setFile(file);

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
