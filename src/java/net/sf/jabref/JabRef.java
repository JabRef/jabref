/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;
import gnu.dtools.ritopt.*;
import java.io.File;
import java.awt.Font;
import java.awt.Color;
import javax.swing.*;
import net.sf.jabref.imports.*;
import java.io.*;
import java.util.*;
import net.sf.jabref.export.*;
//import javax.swing.UIManager;
//import javax.swing.UIDefaults;
//import javax.swing.UnsupportedLookAndFeelException;

public class JabRef {

/*  class StringArrayOption extends ArrayOption {
    public
    public void modify(String value) {
    }
    public void modify(String[] value) {
    }
    public Object[] getObjectArray() {
      return null;
    }
    public String getTypeName() {
      return "Strings";
    }
    public String getStringValue() {
      return "";
    }
    public Object getObject() {
      return null;
    }
  }*/

    public static void main(String[] args) {

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
      importFile.setDescription("imopoepuoeu");//Globals.lang);
      StringOption exportFile = new StringOption("");
      NotifyOption helpO = new NotifyOption("");
      NotifyOption disableGui = new NotifyOption("");
      exportFile.setHelpDescriptionSize(80);
      NotifyOption loadSess = new NotifyOption("");

      options.register("nogui", 'n', Globals.lang("No GUI. Only process command line options."), disableGui);
      options.register("import", 'i', Globals.lang("Import file")+": "+Globals.lang("filename")+"[,import format]", importFile);
      options.register("output", 'o', Globals.lang("Output or export file")+": "+Globals.lang("filename")+"[,export format]", exportFile);
      options.register("help", 'h', Globals.lang("Display help on command line options"),helpO);
      options.register("loadsession", 'l', Globals.lang("Load session"), loadSess);
      options.setUseMenu(false);

      options.process(args);

      if (helpO.isInvoked()) {
        System.out.println(options.getHelp());
        System.out.println(Globals.lang("Available import formats")+": biblioscape, bibtexml, endnote, inspec, isi, medline, ovid, ris, scifinder, sixpack.");
        System.out.println(Globals.lang("Available export formats")+": bibtexml, docbook, html, simplehtml.\n");
        System.exit(0);
      }


      // First we quickly scan the command line parameters for any that signal that the GUI
      // should not be opened. This is used to decide whether we should show the splash screen or not.
      /*boolean openGui = true;
      if (args.length > 0) for (int i=0; i<args.length; i++) {
        if (args[i].startsWith("-o"))
          openGui = false;
        if (args[i].equals("-h")) {
          System.out.println("Help info goes here.");
          System.exit(0);
        }
      }*/

  SplashScreen ss = null;
  if (!disableGui.isInvoked()) {
    ss = new SplashScreen();
    ss.show();
  }
        //Util.pr("JabRef "+GUIGlobals.version);

        //Font fnt = new Font("plain", Font.PLAIN, 12);
        Object fnt = new UIDefaults.ProxyLazyValue
            ("javax.swing.plaf.FontUIResource", null,
             new Object[] {"Arial", new Integer(Font.PLAIN), new Integer(12)});

	UIManager.put("Button.font", fnt);
	UIManager.put("ToggleButton.font", fnt);
	UIManager.put("RadioButton.font", fnt);
	UIManager.put("CheckBox.font", fnt);
	UIManager.put("ColorChooser.font", fnt);
	UIManager.put("ComboBox.font", fnt);
	UIManager.put("Label.font", fnt);
	UIManager.put("List.font", fnt);
	UIManager.put("MenuBar.font", fnt);
	UIManager.put("MenuItem.font", fnt);
	UIManager.put("RadioButtonMenuItem.font", fnt);
	UIManager.put("CheckBoxMenuItem.font", fnt);
	UIManager.put("Menu.font", fnt);
	UIManager.put("PopupMenu.font", fnt);
	UIManager.put("OptionPane.font", fnt);
	UIManager.put("Panel.font", fnt);
	UIManager.put("ProgressBar.font", fnt);
	UIManager.put("ScrollPane.font", fnt);
	UIManager.put("Viewport.font", fnt);
	UIManager.put("TabbedPane.font", fnt);
	UIManager.put("Table.font", fnt);
	UIManager.put("TableHeader.font", fnt);
	UIManager.put("TextField.font", fnt);
	UIManager.put("PasswordField.font", fnt);
	UIManager.put("TextArea.font", fnt);
	UIManager.put("TextPane.font", fnt);
	UIManager.put("EditorPane.font", fnt);
	UIManager.put("TitledBorder.font", fnt);
	UIManager.put("ToolBar.font", fnt);
	UIManager.put("ToolTip.font", fnt);
	UIManager.put("Tree.font", fnt);

        // This property is set to make the Mac OSX Java VM move the menu bar to the top
        // of the screen, where Mac users expect it to be.
        System.setProperty("apple.laf.useScreenMenuBar", "true");

	//String osName = System.getProperty("os.name", "def");
        if (Globals.ON_WIN) {
          try {
            UIManager.setLookAndFeel(new com.sun.java.swing.plaf.windows.WindowsLookAndFeel());
          } catch (UnsupportedLookAndFeelException ex) {}
        }
	else if (!Globals.ON_MAC) {
          try {
            //Class plastic = Class.forName("com.jgoodies.plaf.plastic.PlasticLookAndFeel");
            //LookAndFeel lnf = new com.jgoodies.plaf.plastic.PlasticLookAndFeel();
            //LookAndFeel lnf = new com.sun.java.swing.plaf.gtk.GTKLookAndFeel();
            LookAndFeel lnf = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
            //com.incors.plaf.kunststoff.KunststoffLookAndFeel.setCurrentTheme(new com.incors.plaf.kunststoff.themes.KunststoffDesktopTheme());
            UIManager.setLookAndFeel(lnf);
          } catch (UnsupportedLookAndFeelException ex) {}
        }

	GUIGlobals.CURRENTFONT = new Font
	    (prefs.get("fontFamily"), prefs.getInt("fontStyle"),
	     prefs.getInt("fontSize"));

        // Vector to put imported/loaded database(s) in.
        Vector loaded = new Vector();

        if (importFile.isInvoked()) {
          String[] data = importFile.getStringValue().split(",");
          if (data.length == 1) {
            // Load a bibtex file:
            System.out.println(Globals.lang("Opening")+": " + data[0]);
            try {
              File file = new File(data[0]);
              ParserResult pr = ImportFormatReader.loadDatabase(file);
              pr.setFile(file);
              loaded.add(pr);
            } catch (IOException ex) {
              System.err.println(Globals.lang("Error opening file")+" '"+ data[0]+"': "+ex.getMessage());
            }

          } else if (data.length == 2) {
            // Import a database in a certain format.
            try {
              System.out.println(Globals.lang("Importing")+": " + data[0]);
              BibtexDatabase base = ImportFormatReader.importFile(data[1],
                  data[0].replaceAll("~", System.getProperty("user.home")));
              ParserResult pr = new ParserResult(base, new HashMap());
              pr.setFile(new File(data[0]));
              loaded.add(pr);
            } catch (IOException ex) {
              System.err.println(Globals.lang("Error opening file")+" '"+ data[0]+"': "+ex.getMessage());
            }
          }
        }

        if (exportFile.isInvoked()) {
          if (loaded.size() == 1) {
            String[] data = exportFile.getStringValue().split(",");
            if (data.length == 1) {
              // This signals that the latest import should be stored in BibTeX format to the given file.
              if (loaded.size() > 0) {
                ParserResult pr = (ParserResult)loaded.elementAt(loaded.size()-1);
                try {
                  System.out.println(Globals.lang("Saving")+": "+data[0]);
                  FileActions.saveDatabase(pr.getDatabase(),
                                           new MetaData(pr.getMetaData()),
                                           new File(data[0]),
                                           prefs, false, false);
                } catch (SaveException ex) {
                  System.err.println(Globals.lang("Could not save file")+" '"+ data[0]+"': "+ex.getMessage());
                }
              } else System.err.println(Globals.lang("The output option depends on a valid import option."));

            } else if (data.length == 2) {
              // This signals that the latest import should be stored in the given format to the given file.
              ParserResult pr = (ParserResult) loaded.elementAt(loaded.size() - 1);
              try {
                System.out.println(Globals.lang("Exporting")+": "+data[0]);
                FileActions.exportDatabase(pr.getDatabase(), data[1],
                                           new File(data[0]), prefs);
              }
              catch (IOException ex) {
                System.err.println(Globals.lang("Could not export file")+" '"+ data[0]+"': "+ex.getMessage());
              }
              catch (NullPointerException ex2) {
                System.err.println(Globals.lang("Unknown export format")+": "+data[1]);
              }
            }
          } else System.err.println(Globals.lang("The output option depends on a valid import option."));
        }


        /*
	if(args.length > 0) for (int i=0; i<args.length; i++) {
          if (!args[i].startsWith("-")) {
            // Load a bibtex file:
            System.out.println(Globals.lang("Opening")+": " + args[i]);
            try {
              File file = new File(args[i]);
              ParserResult pr = ImportFormatReader.loadDatabase(file);
              pr.setFile(file);
              loaded.add(pr);
            } catch (IOException ex) {
              System.err.println(Globals.lang("Error opening file")+" '"+ args[i]+"': "+ex.getMessage());
            }

            /*jrf.output("Opening: " + args[i]);
            //verify the file
            File f = new File (args[i]);
            if( f.exists() && f.canRead() && f.isFile()) {
              jrf.fileToOpen=f;
              jrf.openDatabaseAction.openIt(true);
              base = jrf.basePanel().database();
            }else{
              System.err.println("Error" + args[i] + " is not a valid file or is not readable");
              //JOptionPane...
            }/
          } else {
            // A command line switch.
            if (args[i].startsWith("-i") && (args[i].length() > 3) && (args.length > i+1)) {
              // Import a database in a certain format.
              try {
                System.out.println(Globals.lang("Importing")+": " + args[i+1]);
                BibtexDatabase base = ImportFormatReader.importFile(args[i].substring(3), args[i+1]);
                ParserResult pr = new ParserResult(base, new HashMap());
                pr.setFile(new File(args[i+1]));
                loaded.add(pr);
              } catch (IOException ex) {
                System.err.println(Globals.lang("Error opening file")+" '"+ args[i+1]+"': "+ex.getMessage());
              }
              i++;
            }
            else if (args[i].equals("-o") && (args.length > i+1)) {
              // This signals that the latest import should be stored in BibTeX format to the given file.

              if (loaded.size() > 0) {
                ParserResult pr = (ParserResult)loaded.elementAt(loaded.size()-1);
                try {
                  System.out.println(Globals.lang("Saving")+": "+args[i+1]);
                  FileActions.saveDatabase(pr.getDatabase(),
                                           new MetaData(pr.getMetaData()),
                                           new File(args[i+1]),
                                           prefs, false, false);
                } catch (SaveException ex) {
                  System.err.println(Globals.lang("Could   not save file")+" '"+ args[i+1]+"': "+ex.getMessage());
                }
              } else System.err.println(Globals.lang("The -o option must be preceded by an import option."));
              i++;
            }
            else if (args[i].startsWith("-o") && (args.length > i+1)) {
              // The database should be exported to the named database in the format following "-o_"
              if (loaded.size() > 0) {
                ParserResult pr = (ParserResult) loaded.elementAt(loaded.size() - 1);
                try {
                  System.out.println(Globals.lang("Exporting")+": "+args[i+1]);
                  FileActions.exportDatabase(pr.getDatabase(),
                                             args[i].substring(3),
                                             new File(args[i + 1]), prefs);
                }
                catch (IOException ex) {
                  System.err.println(Globals.lang("Could not export file")+" '"+ args[i+1]+"': "+ex.getMessage());
                }
                catch (NullPointerException ex2) {
                  System.err.println(Globals.lang("Unknown export format")+": "+args[i].substring(3));
                }

              } else System.err.println(Globals.lang("The -o option must be preceded by an import option."));
              i++;
            }
          }

	}else{//no arguments (this will be for later and other command line switches)
	    // ignore..
	}*/

      //openGui = false;
     if (!disableGui.isInvoked()) {
       JabRefFrame jrf = new JabRefFrame();

       if (loaded.size() > 0) for (int i=0; i<loaded.size(); i++) {
         ParserResult pr = (ParserResult)loaded.elementAt(i);
         jrf.addTab(pr.getDatabase(), pr.getFile(), pr.getMetaData(), (i==0));
       }

       if (loadSess.isInvoked()) {
         jrf.loadSessionAction.actionPerformed(new java.awt.event.ActionEvent(jrf, 0, ""));
       }

       ss.dispose();
       jrf.setVisible(true);
     } else System.exit(0);

   }

}
