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

import net.sf.jabref.label.*;
import net.sf.jabref.export.FileActions;
import net.sf.jabref.imports.*;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;
import java.net.URL;
import java.util.regex.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.undo.UndoableInsertString;
import net.sf.jabref.export.ExportCustomizationDialog;
import net.sf.jabref.export.CustomExportList;
import javax.swing.text.DefaultEditorKit;
import java.lang.reflect.*;
import javax.swing.event.*;


/**
 * The main window of the application.
 */
public class JabRefFrame
    extends JFrame {

  JabRefFrame ths = this;
  JabRefPreferences prefs = Globals.prefs; //new JabRefPreferences();

  JTabbedPane tabbedPane = new JTabbedPane();
  final Insets marg = new Insets(0,0,0,0);
  class ToolBar extends JToolBar {
    void addAction(Action a) {
      JButton b = new JButton(a);
      b.setText(null);
      b.setMargin(marg);
      add(b);
    }
  }
  ToolBar tlb = new ToolBar();

  JMenuBar mb = new JMenuBar();/* {
    public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D)g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                          RenderingHints.VALUE_RENDER_QUALITY);
      super.paintComponent(g2);

    }

  };*/
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints con = new GridBagConstraints();

  JLabel statusLine = new JLabel("", SwingConstants.LEFT),
      statusLabel = new JLabel(Globals.lang("Status") + ":",
                               SwingConstants.LEFT);
  //SearchManager searchManager  = new SearchManager(ths, prefs);

  FileHistory fileHistory = new FileHistory(prefs, this);

  LabelMaker labelMaker;
  File fileToOpen = null;


  // The help window.
  public HelpDialog helpDiag = new HelpDialog(this);

  // Here we instantiate menu/toolbar actions. Actions regarding
  // the currently open database are defined as a GeneralAction
  // with a unique command string. This causes the appropriate
  // BasePanel's runCommand() method to be called with that command.
  // Note: GeneralAction's constructor automatically gets translations
  // for the name and message strings.

  // References to the toggle buttons in the toolbar:
  JToggleButton groupToggle, searchToggle;

  AbstractAction
      open = new OpenDatabaseAction(),
      close = new CloseDatabaseAction(),
      quit = new CloseAction(),
      selectKeys = new SelectKeysAction(),
      newDatabaseAction = new NewDatabaseAction(),
      help = new HelpAction("JabRef help", helpDiag,
                            GUIGlobals.baseFrameHelp, "JabRef help",
                            prefs.getKey("Help")),
      contents = new HelpAction("Help contents", helpDiag,
                                GUIGlobals.helpContents, "Help contents",
                                GUIGlobals.helpContentsIconFile),
      about = new HelpAction("About JabRef", helpDiag,
                             GUIGlobals.aboutPage, "About JabRef",
                             GUIGlobals.aboutIcon),
      editEntry = new GeneralAction("edit", "Edit entry",
                               "Edit entry", GUIGlobals.editIconFile,
                               prefs.getKey("Edit entry")),
      save = new GeneralAction("save", "Save database",
                               "Save database", GUIGlobals.saveIconFile,
                               prefs.getKey("Save database")),
      saveAs = new GeneralAction("saveAs", "Save database as ...",
                                 "Save database as ...",
                                 GUIGlobals.saveAsIconFile,
                                 prefs.getKey("Save database as ...")),
      saveSelectedAs = new GeneralAction("saveSelectedAs",
                                         "Save selected as ...",
                                         "Save selected as ...",
                                         GUIGlobals.saveAsIconFile),
      nextTab = new ChangeTabAction(true),
      prevTab = new ChangeTabAction(false),
      undo = new GeneralAction("undo", "Undo", "Undo",
                               GUIGlobals.undoIconFile,
                               prefs.getKey("Undo")),
      redo = new GeneralAction("redo", "Redo", "Redo",
                               GUIGlobals.redoIconFile,
                               prefs.getKey("Redo")),
      /*cut = new GeneralAction("cut", "Cut", "Cut",
         GUIGlobals.cutIconFile,
         prefs.getKey("Cut")),*/
      delete = new GeneralAction("delete", "Delete", "Delete",
                                 GUIGlobals.removeIconFile,
                                 prefs.getKey("Delete")),
      /*copy = new GeneralAction("copy", "Copy", "Copy",
                               GUIGlobals.copyIconFile,
                               prefs.getKey("Copy")),*/
      copy = new EditAction("copy", GUIGlobals.copyIconFile),
      paste = new EditAction("paste", GUIGlobals.pasteIconFile),
      cut = new EditAction("cut", GUIGlobals.cutIconFile),
      mark = new GeneralAction("markEntries", "Mark entries",
                               prefs.getKey("Mark entries")),
       unmark = new GeneralAction("unmarkEntries", "Unmark entries",
                                  prefs.getKey("Unmark entries")),
       unmarkAll = new GeneralAction("unmarkAll", "Unmark all"),
      saveSessionAction = new SaveSessionAction(),
      loadSessionAction = new LoadSessionAction(),
      incrementalSearch = new GeneralAction("incSearch", "Incremental search",
                                            "Start incremental search",
                                            GUIGlobals.searchIconFile,
                                            prefs.getKey("Incremental search")),
      normalSearch = new GeneralAction("search", "Search", "Toggle search panel",
                                       GUIGlobals.searchIconFile,
                                       prefs.getKey("Search")),
      fetchCiteSeer = new FetchCiteSeerAction(),
      importCiteSeer = new ImportCiteSeerAction(),
      fetchMedline = new FetchMedlineAction(),
      fetchAuthorMedline = new FetchAuthorMedlineAction(),
      copyKey = new GeneralAction("copyKey", "Copy BibTeX key"),
      //"Put a BibTeX reference to the selected entries on the clipboard",
      copyCiteKey = new GeneralAction("copyCiteKey", "Copy \\cite{BibTeX key}",
                                      //"Put a BibTeX reference to the selected entries on the clipboard",
                                      prefs.getKey("Copy \\cite{BibTeX key}")),
      mergeDatabaseAction = new GeneralAction("mergeDatabase",
                                              "Append database",
                                              "Append contents from a BibTeX database into the currently viewed database",
                                              GUIGlobals.openIconFile),
      //prefs.getKey("Open")),
      /*remove = new GeneralAction("remove", "Remove", "Remove selected entries",
        GUIGlobals.removeIconFile),*/
      selectAll = new GeneralAction("selectAll", "Select all",
                                    prefs.getKey("Select all")),
      replaceAll = new GeneralAction("replaceAll", "Replace string",
                                     prefs.getKey("Replace string")),

      editPreamble = new GeneralAction("editPreamble", "Edit preamble",
                                       "Edit preamble",
                                       GUIGlobals.preambleIconFile,
                                       prefs.getKey("Edit preamble")),
      editStrings = new GeneralAction("editStrings", "Edit strings",
                                      "Edit strings",
                                      GUIGlobals.stringsIconFile,
                                      prefs.getKey("Edit strings")),
      toggleGroups = new GeneralAction("toggleGroups",
                                       "Toggle groups interface",
                                       "Toggle groups interface",
                                       GUIGlobals.groupsIconFile,
                                       prefs.getKey("Toggle groups interface")),
      togglePreview = new GeneralAction("togglePreview",

                                        "Toggle entry preview",
                                        prefs.getKey("Toggle entry preview")),
       makeKeyAction = new GeneralAction("makeKey", "Autogenerate BibTeX keys",
                                        "Autogenerate BibTeX keys",
                                        GUIGlobals.genKeyIconFile,
                                        prefs.getKey("Autogenerate BibTeX keys")),
      lyxPushAction = new GeneralAction("pushToLyX",
                                        "Insert selected citations into LyX",
                                        "push selection to lyx",
                                        GUIGlobals.lyxIconFile,
                                        prefs.getKey("Push to LyX")),
      winEdtPushAction = new GeneralAction("pushToWinEdt",
                                        "Insert selected citations into WinEdt",
                                        "Push selection to WinEdt",
                                        GUIGlobals.winEdtIcon,
                                        prefs.getKey("Push to WinEdt")),
      openFile = new GeneralAction("openFile", "Open PDF or PS",
                                   "Open PDF or PS",
                                   GUIGlobals.pdfIcon,
                                   prefs.getKey("Open PDF or PS")),
      openUrl = new GeneralAction("openUrl", "Open URL or DOI",
                                  "Open URL or DOI",
                                  GUIGlobals.wwwIcon,
                                  prefs.getKey("Open URL or DOI")),
      dupliCheck = new GeneralAction("dupliCheck", "Find duplicates"),


      customExpAction = new CustomizeExportsAction();

  /*setupSelector = new GeneralAction("setupSelector", "", "",
          GUIGlobals.pasteIconFile,
          prefs.getKey(")),*/


  // The menus for importing/appending other formats
  JMenu importMenu = new JMenu(Globals.lang("Import and append")),
      importNewMenu = new JMenu(Globals.lang("Import")),
      exportMenu = new JMenu(Globals.lang("Export")),
      customExportMenu = new JMenu(Globals.lang("Custom export"));

  // The action for adding a new entry of unspecified type.
  NewEntryAction newEntryAction = new NewEntryAction(prefs.getKey("New entry"));
  NewEntryAction[] newSpecificEntryAction = new NewEntryAction[] {
      new NewEntryAction("article", prefs.getKey("New article")),
      new NewEntryAction("book", prefs.getKey("New book")),
      new NewEntryAction("phdthesis", prefs.getKey("New phdthesis")),
      new NewEntryAction("inbook", prefs.getKey("New inbook")),
      new NewEntryAction("mastersthesis", prefs.getKey("New mastersthesis")),
      new NewEntryAction("proceedings", prefs.getKey("New proceedings")),
      new NewEntryAction("inproceedings"),
      new NewEntryAction("incollection"),
      new NewEntryAction("booklet"),
      new NewEntryAction("manual"),
      new NewEntryAction("techreport"),
      new NewEntryAction("unpublished",
                         prefs.getKey("New unpublished")),
      new NewEntryAction("misc")
  };

  public JabRefFrame() {
    init();

    // If the option is enabled, open the last edited databases, if any.
    if (prefs.getBoolean("openLastEdited")
        && (prefs.get("lastEdited") != null)) {

      // How to handle errors in the databases to open?
      String[] names = prefs.getStringArray("lastEdited");
      for (int i = 0; i < names.length; i++) {
        fileToOpen = new File(names[i]);
        if (fileToOpen.exists()) {
          //Util.pr("Opening last edited file:"
          //+fileToOpen.getName());
          openDatabaseAction.openIt(i == 0);
        }
      }
      output(Globals.lang("Files opened") + ": " + tabbedPane.getTabCount());
    }
    //setVisible(true);
    if (tabbedPane.getTabCount() > 0) {
      tabbedPane.setSelectedIndex(0);
      new FocusRequester( ( (BasePanel) tabbedPane.getComponentAt(0))
                         .entryTable);
    }
    else {
      setEmptyState();
    }
  }

  private void init() {
    /*try {
        //UIManager.setLookAndFeel("com.jgoodies.plaf.windows.ExtWindowsLookAndFeel");
        UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        } catch (Exception e) { e.printStackTrace();}*/

    //Globals.setLanguage("no", "");

    macOSXRegistration();

    setTitle(GUIGlobals.frameTitle);
    setIconImage(new ImageIcon(GUIGlobals.jabreflogo).getImage());
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        (new CloseAction()).actionPerformed(null);
      }
    });

    initLabelMaker();

    setupLayout();
    setSize(new Dimension(prefs.getInt("sizeX"),
                          prefs.getInt("sizeY")));
    setLocation(new Point(prefs.getInt("posX"),
                          prefs.getInt("posY")));

    // The following state listener makes sure focus is registered with the correct database
    // when the user switches tabs. Without this, cut/paste/copy operations would some times
    // occur in the wrong tab.
    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        BasePanel bp = basePanel();
        if (bp != null) {
          groupToggle.setSelected(bp.sidePaneManager.isPanelVisible("groups"));
          searchToggle.setSelected(bp.sidePaneManager.isPanelVisible("search"));

          Globals.focusListener.setFocused(bp.entryTable);
        }
      }

    });
  }

  // General info dialog.  The OSXAdapter calls this method when "About OSXAdapter"
  // is selected from the application menu.
  public void about() {
    JDialog about = new JDialog(ths, Globals.lang("About JabRef"),
                                true);
    JEditorPane jp = new JEditorPane();
    JScrollPane sp = new JScrollPane
        (jp, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    jp.setEditable(false);
    try {
      jp.setPage(GUIGlobals.class.getResource("/help/About.html"));//GUIGlobals.aboutPage);
      // We need a hyperlink listener to be able to switch to the license
      // terms and back.
      jp.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
        public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent e) {
          if (e.getEventType()
              == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
            try {
              ( (JEditorPane) e.getSource()).setPage(e.getURL());
            }
            catch (IOException ex) {}
          }
        }
      });
      about.getContentPane().add(sp);
      about.setSize(GUIGlobals.aboutSize);
      Util.placeDialog(about, ths);
      about.setVisible(true);
    }
    catch (IOException ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(ths, "Could not load file 'About.html'",
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }

  }

  // General preferences dialog.  The OSXAdapter calls this method when "Preferences..."
  // is selected from the application menu.
  public void preferences() {
    //PrefsDialog.showPrefsDialog(ths, prefs);
    PrefsDialog2 pd = new PrefsDialog2(ths, prefs);
    Util.placeDialog(pd, ths);
    pd.show();
  }

public JabRefPreferences prefs() {
  return prefs;
}

  // General info dialog.  The OSXAdapter calls this method when "Quit OSXAdapter"
  // is selected from the application menu, Cmd-Q is pressed, or "Quit" is selected from the Dock.
  public void quit() {
    // Ask here if the user really wants to close, if the base
    // has not been saved since last save.
    boolean close = true;
    Vector filenames = new Vector();
    if (tabbedPane.getTabCount() > 0) {
      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        if (baseAt(i).baseChanged) {
          tabbedPane.setSelectedIndex(i);
          int answer = JOptionPane.showConfirmDialog
              (ths, Globals.lang
               ("Database has changed. Do you "
                + "want to save before closing?"),
               Globals.lang("Save before closing"),
               JOptionPane.YES_NO_CANCEL_OPTION);

          if ( (answer == JOptionPane.CANCEL_OPTION) ||
              (answer == JOptionPane.CLOSED_OPTION)) {
            close = false; // The user has cancelled.
          }
          if (answer == JOptionPane.YES_OPTION) {
            // The user wants to save.
            try {
              basePanel().runCommand("save");
            }
            catch (Throwable ex) {
              // Something prevented the file
              // from being saved. Break!!!
              close = false;
              break;
            }
          }
        }
        if (baseAt(i).file != null) {
          filenames.add(baseAt(i).file.getPath());
        }
      }
    }
    if (close) {
      dispose();

      prefs.putInt("posX", ths.getLocation().x);
      prefs.putInt("posY", ths.getLocation().y);
      prefs.putInt("sizeX", ths.getSize().width);
      prefs.putInt("sizeY", ths.getSize().height);

      if (prefs.getBoolean("openLastEdited")) {
        // Here we store the names of allcurrent filea. If
        // there is no current file, we remove any
        // previously stored file name.
        if (filenames.size() == 0) {
          prefs.remove("lastEdited");
        }
        else {
          String[] names = new String[filenames.size()];
          for (int i = 0; i < filenames.size(); i++) {
            names[i] = (String) filenames.elementAt(i);

          }
          prefs.putStringArray("lastEdited", names);
        }

      }

      fileHistory.storeHistory();
      prefs.customExports.store();
      BibtexEntryType.saveCustomEntryTypes(prefs);

      // Let the search interface store changes to prefs.
      // But which one? Let's use the one that is visible.
      if (basePanel() != null) {
        basePanel().searchManager.updatePrefs();

      }
      System.exit(0); // End program.
    }
  }

  private void macOSXRegistration() {
    if (Globals.osName.equals(Globals.MAC)) {
      try {
        Class osxAdapter = Class.forName("osxadapter.OSXAdapter");

        Class[] defArgs = {
            JabRefFrame.class};
        Method registerMethod = osxAdapter.getDeclaredMethod(
            "registerMacOSXApplication", defArgs);
        if (registerMethod != null) {
          Object[] args = {
              this};
          registerMethod.invoke(osxAdapter, args);
        }
        // This is slightly gross.  to reflectively access methods with boolean args,
        // use "boolean.class", then pass a Boolean object in as the arg, which apparently

        defArgs[0] = boolean.class;
        Method prefsEnableMethod = osxAdapter.getDeclaredMethod("enablePrefs",
            defArgs);
        if (prefsEnableMethod != null) {
          Object args[] = {
              Boolean.TRUE};
          prefsEnableMethod.invoke(osxAdapter, args);
        }
      }
      catch (NoClassDefFoundError e) {
        // This will be thrown first if the OSXAdapter is loaded on a system without the EAWT
        // because OSXAdapter extends ApplicationAdapter in its def
        System.err.println("This version of Mac OS X does not support the Apple EAWT.  Application Menu handling has been disabled (" +
                           e + ")");
      }
      catch (ClassNotFoundException e) {
        // This shouldn't be reached; if there's a problem with the OSXAdapter we should get the
        // above NoClassDefFoundError first.
        System.err.println("This version of Mac OS X does not support the Apple EAWT.  Application Menu handling has been disabled (" +
                           e + ")");
      }
      catch (Exception e) {
        System.err.println("Exception while loading the OSXAdapter:");
        e.printStackTrace();
      }
    }
  }

  private void setupLayout() {
    fillMenu();
    createToolBar();
    getContentPane().setLayout(gbl);
    //getContentPane().setBackground(GUIGlobals.lightGray);
    con.fill = GridBagConstraints.HORIZONTAL;
    con.anchor = GridBagConstraints.WEST;
    con.weightx = 1;
    con.weighty = 0;
    con.gridwidth = GridBagConstraints.REMAINDER;
    mb.setMinimumSize(mb.getPreferredSize());
    //gbl.setConstraints(mb, con);
    //getContentPane().add(mb);
    setJMenuBar(mb);
    con.anchor = GridBagConstraints.NORTH;
    //con.gridwidth = 1;//GridBagConstraints.REMAINDER;;
    gbl.setConstraints(tlb, con);
    getContentPane().add(tlb);

    Component lim = Box.createGlue();
    gbl.setConstraints(lim, con);
    //getContentPane().add(lim);
    /*
      JPanel empt = new JPanel();
      empt.setBackground(GUIGlobals.lightGray);
      gbl.setConstraints(empt, con);
           getContentPane().add(empt);

      con.insets = new Insets(1,0,1,1);
      con.anchor = GridBagConstraints.EAST;
      con.weightx = 0;
      gbl.setConstraints(searchManager, con);
      getContentPane().add(searchManager);*/
    con.gridwidth = GridBagConstraints.REMAINDER;
    con.weightx = 1;
    con.weighty = 0;
    con.fill = GridBagConstraints.BOTH;
    con.anchor = GridBagConstraints.WEST;
    con.insets = new Insets(0, 0, 0, 0);
    lim = Box.createGlue();
    gbl.setConstraints(lim, con);
    getContentPane().add(lim);
    //tabbedPane.setVisible(false);
    //tabbedPane.setForeground(GUIGlobals.lightGray);
    con.weighty = 1;
    gbl.setConstraints(tabbedPane, con);
    getContentPane().add(tabbedPane);

    JPanel status = new JPanel();
    status.setLayout(gbl);
    con.weighty = 0;
    con.weightx = 0;
    con.gridwidth = 1;
    con.insets = new Insets(0, 2, 0, 0);
    gbl.setConstraints(statusLabel, con);
    status.add(statusLabel);
    con.weightx = 1;
    con.insets = new Insets(0, 4, 0, 0);
    con.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(statusLine, con);
    status.add(statusLine);
    con.gridwidth = GridBagConstraints.REMAINDER;
    statusLabel.setForeground(GUIGlobals.validFieldColor.darker());
    con.insets = new Insets(0, 0, 0, 0);
    gbl.setConstraints(status, con);
    getContentPane().add(status);

  }

  private void initLabelMaker() {
    // initialize the labelMaker
    labelMaker = new LabelMaker();
    labelMaker.addRule(new ArticleLabelRule(),
                       BibtexEntryType.ARTICLE);
    labelMaker.addRule(new BookLabelRule(),
                       BibtexEntryType.BOOK);
    labelMaker.addRule(new IncollectionLabelRule(),
                       BibtexEntryType.INCOLLECTION);
    labelMaker.addRule(new InproceedingsLabelRule(),
                       BibtexEntryType.INPROCEEDINGS);
  }

  /**
   * Returns the indexed BasePanel.
   * @param i Index of base
   */
  BasePanel baseAt(int i) {
    return (BasePanel) tabbedPane.getComponentAt(i);
  }

  /**
   * Returns the currently viewed BasePanel.
   */
  BasePanel basePanel() {
    return (BasePanel) tabbedPane.getSelectedComponent();
  }

  private int getTabIndex(JComponent comp) {
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      if (tabbedPane.getComponentAt(i) == comp) {
        return i;
      }
    }
    return -1;
  }

  public String getTabTitle(JComponent comp) {
    return tabbedPane.getTitleAt(getTabIndex(comp));
  }

  public void setTabTitle(JComponent comp, String s) {
    tabbedPane.setTitleAt(getTabIndex(comp), s);
  }

  class GeneralAction
      extends AbstractAction {
    private String command;
    public GeneralAction(String command, String text,
                         String description, URL icon) {
      super(Globals.lang(text), new ImageIcon(icon));
      this.command = command;
      putValue(SHORT_DESCRIPTION, Globals.lang(description));
    }

    public GeneralAction(String command, String text,
                         String description, URL icon,
                         KeyStroke key) {
      super(Globals.lang(text), new ImageIcon(icon));
      this.command = command;
      putValue(ACCELERATOR_KEY, key);
      putValue(SHORT_DESCRIPTION, Globals.lang(description));
    }

    public GeneralAction(String command, String text) {
      super(Globals.lang(text));
      this.command = command;
    }

    public GeneralAction(String command, String text, KeyStroke key) {
      super(Globals.lang(text));
      this.command = command;
      putValue(ACCELERATOR_KEY, key);
    }

    public void actionPerformed(ActionEvent e) {
      if (tabbedPane.getTabCount() > 0) {
        try {
          ( (BasePanel) (tabbedPane.getSelectedComponent()))
              .runCommand(command);
        }
        catch (Throwable ex) {
          ex.printStackTrace();
        }
      }
      else {
        Util.pr("Action '" + command + "' must be disabled when no "
                + "database is open.");
      }
    }
  }

  /** This got removed when we introduced SearchManager2.
       class IncrementalSearchAction extends AbstractAction {
    public IncrementalSearchAction() {
   super("Incremental search", new ImageIcon(GUIGlobals.searchIconFile));
   putValue(SHORT_DESCRIPTION, Globals.lang("Start incremental search"));
   putValue(ACCELERATOR_KEY, prefs.getKey("Incremental search"));
    }
    public void actionPerformed(ActionEvent e) {
   if (tabbedPane.getTabCount() > 0)
     searchManager.startIncrementalSearch();
    }
       }

       class SearchAction extends AbstractAction {
    public SearchAction() {
   super("Search", new ImageIcon(GUIGlobals.searchIconFile));
   putValue(SHORT_DESCRIPTION, Globals.lang("Start search"));
   putValue(ACCELERATOR_KEY, prefs.getKey("Search"));
    }
    public void actionPerformed(ActionEvent e) {
   if (tabbedPane.getTabCount() > 0)
     searchManager.startSearch();
    }
       }
   */

  class NewEntryAction
      extends AbstractAction {

    String type = null; // The type of item to create.
    KeyStroke keyStroke = null; // Used for the specific instances.

    public NewEntryAction(KeyStroke key) {
      // This action leads to a dialog asking for entry type.
      super(Globals.lang("New entry"),
            new ImageIcon(GUIGlobals.addIconFile));
      putValue(ACCELERATOR_KEY, key);
      putValue(SHORT_DESCRIPTION, Globals.lang("New BibTeX entry"));
    }

    public NewEntryAction(String type_) {
      // This action leads to the creation of a specific entry.
      super(Util.nCase(type_));
      type = type_;
    }

    public NewEntryAction(String type_, KeyStroke key) {
      // This action leads to the creation of a specific entry.
      super(Util.nCase(type_));
      putValue(ACCELERATOR_KEY, key);
      type = type_;
    }

    public void actionPerformed(ActionEvent e) {
      String thisType = type;
      if (thisType == null) {
        EntryTypeDialog etd = new EntryTypeDialog(ths);
        Util.placeDialog(etd, ths);
        etd.show();
        BibtexEntryType tp = etd.getChoice();
        if (tp == null) {
          return;
        }
        thisType = tp.getName();
      }

      if (tabbedPane.getTabCount() > 0) {
        ( (BasePanel) (tabbedPane.getSelectedComponent()))
            .newEntry(BibtexEntryType.getType(thisType));
      }
      else {
        Util.pr("Action 'New entry' must be disabled when no "
                + "database is open.");
      }
    }
  }

  /*
       private void setupDatabaseLayout() {
    // This method is called whenever this frame has been provided
    // with a database, and completes the layout.


    if (file != null)
   setTitle(GUIGlobals.baseTitle+file.getName());
    else
    setTitle(GUIGlobals.untitledTitle);

    //DragNDropManager dndm = new DragNDropManager(this);

    //setNonEmptyState();
    Util.pr("JabRefFrame: Must set non-empty state.");
    }*/

  private void fillMenu() {
    JMenu file = new JMenu(Globals.lang("File")),
        edit = new JMenu(Globals.lang("Edit")),
        bibtex = new JMenu(Globals.lang("BibTeX")),
        view = new JMenu(Globals.lang("View")),
        tools = new JMenu(Globals.lang("Tools")),
        options = new JMenu(Globals.lang("Options")),
        newSpec = new JMenu(Globals.lang("New entry...")),
        helpMenu = new JMenu(Globals.lang("Help"));

    setUpImportMenu(importMenu, false);
    setUpImportMenu(importNewMenu, true);
    setUpExportMenu(exportMenu);
    setUpCustomExportMenu();

    file.add(newDatabaseAction);
    file.add(open); //opendatabaseaction
    file.add(mergeDatabaseAction);
    file.add(importMenu);
    file.add(importMenu);
    file.add(importNewMenu);
    file.add(save);
    file.add(saveAs);
    file.add(saveSelectedAs);
    file.add(exportMenu);
    file.add(customExportMenu);
    file.addSeparator();
    file.add(fileHistory);
    //file.addSeparator();
    file.add(loadSessionAction);
    file.add(saveSessionAction);
    file.addSeparator();
    file.add(close);
    //==============================
    // NB: I added this because my frame borders are so tiny that I cannot click
    // on the "x" close button. Anyways, I think it is good to have and "exit" button
    // I was too lazy to make a new ExitAction
    //JMenuItem exit_mItem = new JMenuItem(Globals.lang("Exit"));
    //exit_mItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK)); //Ctrl-Q to exit
    // above keybinding should be from user define
    //exit_mItem.addActionListener(new CloseAction() );
    //file.add( exit_mItem);
    //=====================================
    file.add(quit);
    mb.add(file);

    edit.add(undo);
    edit.add(redo);
    edit.addSeparator();

    edit.add(cut);
    edit.add(copy);
    edit.add(paste);
    //edit.add(remove);
    edit.add(delete);
    edit.add(copyKey);
    edit.add(copyCiteKey);
    edit.addSeparator();
    edit.add(mark);
    edit.add(unmark);
    edit.add(unmarkAll);
    edit.addSeparator();
    edit.add(selectAll);
    mb.add(edit);
    view.add(nextTab);
    view.add(prevTab);
    view.addSeparator();
    view.add(togglePreview);
    view.add(toggleGroups);
    mb.add(view);

    bibtex.add(newEntryAction);
    for (int i = 0; i < newSpecificEntryAction.length; i++) {
      newSpec.add(newSpecificEntryAction[i]);
    }
    bibtex.add(newSpec);
    bibtex.addSeparator();
    bibtex.add(editEntry);
    bibtex.add(importCiteSeer);
    bibtex.add(editPreamble);
    bibtex.add(editStrings);
    mb.add(bibtex);
    tools.add(normalSearch);
    tools.add(incrementalSearch);
    tools.add(replaceAll);
    tools.add(dupliCheck);
    tools.addSeparator();
    tools.add(makeKeyAction);
    tools.add(lyxPushAction);
    tools.add(winEdtPushAction);
    tools.add(fetchMedline);
    tools.add(fetchCiteSeer);
    //tools.add(fetchAuthorMedline);
    tools.add(openFile);
    tools.add(openUrl);


    mb.add(tools);

    options.add(showPrefs);
    options.add(new AbstractAction(Globals.lang("Customize entry types")) {
      public void actionPerformed(ActionEvent e) {
        JDialog dl = new EntryCustomizationDialog(ths);
        Util.placeDialog(dl, ths);
        dl.show();
      }
    });
    options.add(customExpAction);

    /*options.add(new AbstractAction("Font") {
     public void actionPerformed(ActionEvent e) {
         // JDialog dl = new EntryCustomizationDialog(ths);
         Font f=new FontSelectorDialog
       (ths, GUIGlobals.CURRENTFONT).getSelectedFont();
      if(f==null)
       return;
      else
       GUIGlobals.CURRENTFONT=f;
      // updatefont
      prefs.put("fontFamily", GUIGlobals.CURRENTFONT.getFamily());
      prefs.putInt("fontStyle", GUIGlobals.CURRENTFONT.getStyle());
      prefs.putInt("fontSize", GUIGlobals.CURRENTFONT.getSize());
      if (tabbedPane.getTabCount() > 0) {
       for (int i=0; i<tabbedPane.getTabCount(); i++) {
        baseAt(i).entryTable.updateFont();
        baseAt(i).refreshTable();
       }
      }
     }
     });*/

    //options.add(selectKeys);
    mb.add(options);

    helpMenu.add(help);
    helpMenu.add(contents);
    helpMenu.addSeparator();
    helpMenu.add(about);
    mb.add(helpMenu);
  }

  private void createToolBar() {
    tlb.setRollover(true);

    //tlb.setBorderPainted(true);
    //tlb.setBackground(GUIGlobals.lightGray);
    //tlb.setForeground(GUIGlobals.lightGray);
    tlb.setFloatable(false);
    tlb.addAction(newDatabaseAction);
    tlb.addAction(open);
    tlb.addAction(save);

    tlb.addSeparator();
    tlb.addAction(cut);
    tlb.addAction(copy);
    tlb.addAction(paste);
    tlb.addAction(undo);
    tlb.addAction(redo);

    tlb.addSeparator();
    tlb.addAction(newEntryAction);
    tlb.addAction(editEntry);
    tlb.addAction(editPreamble);
    tlb.addAction(editStrings);
    tlb.addAction(makeKeyAction);

    tlb.addSeparator();
    searchToggle = new JToggleButton(normalSearch);
    searchToggle.setText(null);
    searchToggle.setMargin(marg);
    tlb.add(searchToggle);

    groupToggle = new JToggleButton(toggleGroups);
    groupToggle.setText(null);
    groupToggle.setMargin(marg);
    tlb.add(groupToggle);

    tlb.addSeparator();
    tlb.addAction(lyxPushAction);
    tlb.addAction(winEdtPushAction);
    tlb.addAction(openFile);
    tlb.addAction(openUrl);

    tlb.addSeparator();
    tlb.addAction(showPrefs);
    tlb.add(Box.createHorizontalGlue());
    //tlb.add(new JabRefLabel(GUIGlobals.frameTitle+" "+GUIGlobals.version));

    tlb.addAction(closeDatabaseAction);
    //Insets margin = new Insets(0, 0, 0, 0);
    //for (int i=0; i<tlb.getComponentCount(); i++)
    //  ((JButton)tlb.getComponentAtIndex(i)).setMargin(margin);

  }

  private class JabRefLabel
      extends JPanel {
    private String label;
    public JabRefLabel(String name) {
      label = name;
    }

    public void paint(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(GUIGlobals.nullFieldColor);
      g2.setFont(GUIGlobals.jabRefFont);
      FontMetrics fm = g2.getFontMetrics();
      int width = fm.stringWidth(label);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);
      g2.drawString(label, getWidth() - width - 7, getHeight() - 10);

    }
  }

  private JMenuItem mItem(AbstractAction a, KeyStroke ks) {
    // Set up a menu item with action and accelerator key.
    JMenuItem mi = new JMenuItem();
    mi.setAction(a);
    if (ks != null) {
      mi.setAccelerator(ks);
    }
    return mi;
  }

  //private void setupMainPanel() {


  /*public Completer getAutoCompleter(String field) {
    return (Completer)autoCompleters.get(field);
    }


       public void assignAutoCompleters() {
    // Set up which fields should have autocompletion. This should
    // probably be made customizable. Existing Completer objects are
    // forgotten. The completers must be updated towards the database.
    byte[] fields = prefs.getByteArray("autoCompFields");
    autoCompleters = new Hashtable();
    for (int i=0; i<fields.length; i++) {
   autoCompleters.put(GUIGlobals.ALL_FIELDS[fields[i]], new Completer());
    }

       }

       public void updateAutoCompleters() {
    if (database != null)
   database.setCompleters(autoCompleters);
   }*/

  public void output(String s) {
    statusLine.setText(s);
    statusLine.repaint();
  }

  public void stopShowingSearchResults() {
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      baseAt(i).stopShowingSearchResults();
    }
  }

  private void setEmptyState() {
    // Disable actions that demand an open database.
    mergeDatabaseAction.setEnabled(false);
    close.setEnabled(false);
    save.setEnabled(false);
    saveAs.setEnabled(false);
    saveSelectedAs.setEnabled(false);
    nextTab.setEnabled(false);
    prevTab.setEnabled(false);
    undo.setEnabled(false);
    redo.setEnabled(false);
    cut.setEnabled(false);
    delete.setEnabled(false);
    copy.setEnabled(false);
    paste.setEnabled(false);
    mark.setEnabled(false);
    unmark.setEnabled(false);
    unmarkAll.setEnabled(false);
    editEntry.setEnabled(false);
    importCiteSeer.setEnabled(false);
    selectAll.setEnabled(false);
    copyKey.setEnabled(false);
    copyCiteKey.setEnabled(false);
    editPreamble.setEnabled(false);
    editStrings.setEnabled(false);
    toggleGroups.setEnabled(false);
    makeKeyAction.setEnabled(false);
    lyxPushAction.setEnabled(false);
    winEdtPushAction.setEnabled(false);
    normalSearch.setEnabled(false);
    incrementalSearch.setEnabled(false);
    replaceAll.setEnabled(false);
    importMenu.setEnabled(false);
    exportMenu.setEnabled(false);
    fetchMedline.setEnabled(false);
    fetchCiteSeer.setEnabled(false);
    openFile.setEnabled(false);
    openUrl.setEnabled(false);
    togglePreview.setEnabled(false);
    dupliCheck.setEnabled(false);
    for (int i = 0; i < newSpecificEntryAction.length; i++) {
      newSpecificEntryAction[i].setEnabled(false);
    }
    newEntryAction.setEnabled(false);
    closeDatabaseAction.setEnabled(false);
  }

  private void setNonEmptyState() {
    // Enable actions that demand an open database.
    mergeDatabaseAction.setEnabled(true);
    close.setEnabled(true);
    save.setEnabled(true);
    saveAs.setEnabled(true);
    saveSelectedAs.setEnabled(true);
    nextTab.setEnabled(true);
    prevTab.setEnabled(true);
    undo.setEnabled(true);
    redo.setEnabled(true);
    cut.setEnabled(true);
    delete.setEnabled(true);
    copy.setEnabled(true);
    paste.setEnabled(true);
    mark.setEnabled(true);
    unmark.setEnabled(true);
    unmarkAll.setEnabled(true);
    editEntry.setEnabled(true);
	importCiteSeer.setEnabled(true);
    selectAll.setEnabled(true);
    copyKey.setEnabled(true);
    copyCiteKey.setEnabled(true);
    editPreamble.setEnabled(true);
    editStrings.setEnabled(true);
    toggleGroups.setEnabled(true);
    makeKeyAction.setEnabled(true);
    lyxPushAction.setEnabled(true);
    winEdtPushAction.setEnabled(true);
    normalSearch.setEnabled(true);
    incrementalSearch.setEnabled(true);
    replaceAll.setEnabled(true);
    importMenu.setEnabled(true);
    exportMenu.setEnabled(true);
    fetchMedline.setEnabled(true);
    fetchCiteSeer.setEnabled(true);
    openFile.setEnabled(true);
    openUrl.setEnabled(true);
    togglePreview.setEnabled(true);
    dupliCheck.setEnabled(true);
    for (int i = 0; i < newSpecificEntryAction.length; i++) {
      newSpecificEntryAction[i].setEnabled(true);
    }
    newEntryAction.setEnabled(true);
    closeDatabaseAction.setEnabled(true);
  }

  /**
   * This method causes all open BasePanels to set up their tables
   * anew. When called from PrefsDialog2, this updates to the new
   * settings.
   */
  public void setupAllTables() {
    // This action can be invoked without an open database, so
    // we have to check if we have one before trying to invoke
    // methods to execute changes in the preferences.

    // We want to notify all tabs about the changes to
    // avoid problems when changing the column set.
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      BasePanel bf = baseAt(i);
      if (bf.database != null) {
        bf.entryTable.updateFont();
        bf.setupTable();
      }
    }
  }

  public void addTab(BibtexDatabase db, File file, HashMap meta, boolean raisePanel) {
    BasePanel bp = new BasePanel(ths, db, file, meta, prefs);
    tabbedPane.add(file.getName(), bp);
    if (raisePanel) {
      tabbedPane.setSelectedComponent(bp);
    }
    if (tabbedPane.getTabCount() == 1) {
      setNonEmptyState();
    }
  }

  class SelectKeysAction
      extends AbstractAction {
    public SelectKeysAction() {
      super(Globals.lang("Customize key bindings"));
    }

    public void actionPerformed(ActionEvent e) {
      KeyBindingsDialog d = new KeyBindingsDialog
          ( (HashMap) prefs.getKeyBindings().clone(),
           prefs.getDefaultKeys());
      d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      d.pack(); //setSize(300,500);
      Util.placeDialog(d, ths);
      d.setVisible(true);
      if (d.getAction()) {
        prefs.setNewKeyBindings(d.getNewKeyBindings());
        JOptionPane.showMessageDialog
            (ths,
             Globals.lang("Your new key bindings have been stored.") + "\n"
             + Globals.lang("You must restart JabRef for the new key "
                            + "bindings to work properly."),
             Globals.lang("Key bindings changed"),
             JOptionPane.INFORMATION_MESSAGE);
      }
    }
  }

  /**
   * The action concerned with closing the window.
   */
  class CloseAction
      extends AbstractAction {
    public CloseAction() {
      super(Globals.lang("Quit"));
      putValue(SHORT_DESCRIPTION, Globals.lang("Quit JabRef"));
      putValue(ACCELERATOR_KEY, prefs.getKey("Quit JabRef"));
      //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q,
      //    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    }

    public void actionPerformed(ActionEvent e) {
      quit();
    }
  }

  // The action for closing the current database and leaving the window open.
  CloseDatabaseAction closeDatabaseAction = new CloseDatabaseAction();
  class CloseDatabaseAction
      extends AbstractAction {
    public CloseDatabaseAction() {
      super(Globals.lang("Close database"),
            new ImageIcon(GUIGlobals.closeIconFile));

      putValue(SHORT_DESCRIPTION,
               Globals.lang("Close the current database"));
      putValue(ACCELERATOR_KEY, prefs.getKey("Close database"));
    }

    public void actionPerformed(ActionEvent e) {
      // Ask here if the user really wants to close, if the base
      // has not been saved since last save.
      boolean close = true;
      if (basePanel() == null) { // when it is initially empty
        return; //nbatada nov 7
      }
      if (basePanel().baseChanged) {
        int answer = JOptionPane.showConfirmDialog
            (ths, Globals.lang("Database has changed. Do you want to save " +
                               "before closing?"),
             Globals.lang("Save before closing"),
             JOptionPane.YES_NO_CANCEL_OPTION);
        if ( (answer == JOptionPane.CANCEL_OPTION) ||
            (answer == JOptionPane.CLOSED_OPTION)) {
          close = false; // The user has cancelled.
        }
        if (answer == JOptionPane.YES_OPTION) {
          // The user wants to save.
          try {
            basePanel().runCommand("save");
          }
          catch (Throwable ex) {
            // Something prevented the file
            // from being saved. Break!!!
            close = false;
          }

        }
      }

      if (close) {
        tabbedPane.remove(basePanel());
        if (tabbedPane.getTabCount() == 0) {
          setEmptyState();
        }
        output(Globals.lang("Closed database") + ".");
      }
    }
  }

  // The action concerned with opening an existing database.
  OpenDatabaseAction openDatabaseAction = new OpenDatabaseAction();
  class OpenDatabaseAction
      extends AbstractAction {
    public OpenDatabaseAction() {
      super(Globals.lang("Open database"),
            new ImageIcon(GUIGlobals.openIconFile));
      putValue(ACCELERATOR_KEY, prefs.getKey("Open database"));
      putValue(SHORT_DESCRIPTION, Globals.lang("Open BibTeX database"));
    }

    public void actionPerformed(ActionEvent e) {
      // Open a new database.
      if ( (e.getActionCommand() == null) ||
          (e.getActionCommand().equals(Globals.lang("Open database")))) {
        /*JFileChooser chooser = (prefs.get("workingDirectory") == null) ?
            new JabRefFileChooser( (File)null) :
            new JabRefFileChooser(new File(prefs.get("workingDirectory")));
        JFileChooser chooser = (prefs.get("workingDirectory") == null) ?
            new JFileChooser((File)null) :
            new JFileChooser(new File(prefs.get("workingDirectory")));*/

        //chooser.addChoosableFileFilter(new OpenFileFilter()); //nb nov2
        //int returnVal = chooser.showOpenDialog(ths);

        String chosenFile = Globals.getNewFile(ths, prefs, new File(prefs.get("workingDirectory")), ".bib",
                                               JFileChooser.OPEN_DIALOG, true);

        /*FileDialog chooser = new FileDialog(ths, "Open", FileDialog.LOAD);
                             chooser.show();
                             int returnVal = 0;*/
        //if (returnVal == JFileChooser.APPROVE_OPTION) {
        if (chosenFile != null) {
          fileToOpen = new File(chosenFile);
          //fileToOpen = new File(chooser.getFile());
        }
      }
      else {
        Util.pr(NAME);
        Util.pr(e.getActionCommand());
        fileToOpen = new File(Util.checkName(e.getActionCommand()));
      }

      // Run the actual open in a thread to prevent the program
      // locking until the file is loaded.
      if (fileToOpen != null) {
        (new Thread() {
          public void run() {
            openIt(true);
          }
        }).start();
        fileHistory.newFile(fileToOpen.getPath());
      }
    }

    public void openIt(boolean raisePanel) {
      if ( (fileToOpen != null) && (fileToOpen.exists())) {
        try {
          prefs.put("workingDirectory", fileToOpen.getPath());
          // Should this be done _after_ we know it was successfully opened?
          String encoding = Globals.prefs.get("defaultEncoding");
          ParserResult pr = ImportFormatReader.loadDatabase(fileToOpen, encoding);
          BibtexDatabase db = pr.getDatabase();
          HashMap meta = pr.getMetaData();

          if (pr.hasWarnings()) {
            final String[] wrns = pr.warnings();
            (new Thread() {
              public void run() {
                StringBuffer wrn = new StringBuffer();
                for (int i = 0; i < wrns.length; i++)
                  wrn.append( (i + 1) + ". " + wrns[i] + "\n");
                if (wrn.length() > 0)
                  wrn.deleteCharAt(wrn.length() - 1);
                JOptionPane.showMessageDialog(ths, wrn.toString(),
                                              Globals.lang("Warnings"),
                                              JOptionPane.WARNING_MESSAGE);
              }
            }).start();
          }

          BasePanel bp = new BasePanel(ths, db, fileToOpen,
                                       meta, prefs);
          bp.encoding = pr.getEncoding(); // Keep track of which encoding was used for loading.
          /*
            if (prefs.getBoolean("autoComplete")) {
            db.setCompleters(autoCompleters);
            }
           */
          tabbedPane.add(fileToOpen.getName(), bp);
          if (raisePanel) {
            tabbedPane.setSelectedComponent(bp);
          }
          if (tabbedPane.getTabCount() == 1) {
            setNonEmptyState();
          }
          output(Globals.lang("Opened database") + " '" + fileToOpen.getPath() +
                 "' " + Globals.lang("with") + " " +
                 db.getEntryCount() + " " + Globals.lang("entries") + ".");

          fileToOpen = null;

        }
        catch (Throwable ex) {
            ex.printStackTrace();
          JOptionPane.showMessageDialog
              (ths, ex.getMessage(),
               Globals.lang("Open database"), JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  // The action concerned with opening a new database.
  class NewDatabaseAction
      extends AbstractAction {
    public NewDatabaseAction() {
      super(Globals.lang("New database"),

            new ImageIcon(GUIGlobals.newIconFile));
      putValue(SHORT_DESCRIPTION, Globals.lang("New BibTeX database"));
      //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);
    }

    public void actionPerformed(ActionEvent e) {

      // Create a new, empty, database.
      BasePanel bp = new BasePanel(ths, prefs);
      tabbedPane.add(Globals.lang(GUIGlobals.untitledTitle), bp);
      tabbedPane.setSelectedComponent(bp);
      if (tabbedPane.getTabCount() == 1) {
        setNonEmptyState();
      }
      output(Globals.lang("New database created."));

      /*
        if (prefs.getBoolean("autoComplete"))
        db.setCompleters(autoCompleters);*/
    }
  }

class ImportCiteSeerAction
	extends AbstractAction {

    public ImportCiteSeerAction() {
        super(Globals.lang("Import Data from CiteSeer"),
                new ImageIcon(GUIGlobals.wwwCiteSeerIcon));
        putValue(SHORT_DESCRIPTION, Globals.lang("Import Data from CiteSeer Database"));

	}

	public void actionPerformed(ActionEvent e) {

		if(basePanel().citeSeerFetcher.activateImportFetcher()) {


			(new Thread() {

				BasePanel currentBp;
				String id;

				Runnable updateComponent = new Runnable() {
					public void run() {
					    currentBp.citeSeerFetcher.endImportCiteSeerProgress();
						currentBp.refreshTable();
						int row = currentBp.tableModel.getNumberFromName(id);
						currentBp.entryTable.setRowSelectionInterval(row, row);
						currentBp.entryTable.scrollTo(row);
						currentBp.markBaseChanged(); // The database just changed.
						currentBp.updateViewToSelected();
						output(Globals.lang("Completed citation import from CiteSeer."));
						 }
				};

			    public void run() {
			        currentBp = (BasePanel) tabbedPane.getSelectedComponent();
					int clickedOn = -1;
					// We demand that one and only one row is selected.
					int rowCount = currentBp.entryTable.getSelectedRowCount();
					if (rowCount == 1) {
						clickedOn = currentBp.entryTable.getSelectedRow();
					} else if (rowCount > 1) {
						JOptionPane.showMessageDialog(currentBp.frame(),
				        Globals.lang("This operation cannot work on multiple rows."),
				        Globals.lang("CiteSeer Import Error"),
						JOptionPane.WARNING_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(currentBp.frame(),
						Globals.lang("You must select a row to perform this operation."),
						Globals.lang("CiteSeer Import Error"),
						JOptionPane.WARNING_MESSAGE);
					}
					if (clickedOn >= 0) {
					    currentBp.citeSeerFetcher.beginImportCiteSeerProgress();
						id =  currentBp.tableModel.getNameFromNumber(clickedOn);
						NamedCompound citeseerNamedCompound =
							new NamedCompound("CiteSeer Import Fields");
						BibtexEntry be =
							(BibtexEntry) currentBp.database.getEntryById(id);
						boolean newValue =
						    currentBp.citeSeerFetcher.importCiteSeerEntry(be, citeseerNamedCompound);
						if (newValue) {
							citeseerNamedCompound.end();
							currentBp.undoManager.addEdit(citeseerNamedCompound);
							SwingUtilities.invokeLater(updateComponent);
						} else {
						    currentBp.citeSeerFetcher.endImportCiteSeerProgress();
							output(Globals.lang("Citation import from CiteSeer failed."));
						}
					}
					basePanel().citeSeerFetcher.deactivateImportFetcher();
			    }
			}).start();
		} else {
			JOptionPane.showMessageDialog((BasePanel) tabbedPane.getSelectedComponent(),
					Globals.lang("A CiteSeer import operation is currently in progress.") + "  " +
					Globals.lang("Please wait until it has finished."),
					Globals.lang("CiteSeer Import Error"),
					JOptionPane.WARNING_MESSAGE);
		}
	}
}

class FetchCiteSeerAction
	extends AbstractAction {

		public FetchCiteSeerAction() {
			super(Globals.lang("Fetch Citations from CiteSeer"),
			        new ImageIcon(GUIGlobals.wwwCiteSeerIcon));
			putValue(SHORT_DESCRIPTION, Globals.lang("Fetch Articles Citing your Database"));
		}

		public void actionPerformed(ActionEvent e) {

			if(basePanel().citeSeerFetcher.activateCitationFetcher()) {
				basePanel().sidePaneManager.ensureVisible("CiteSeerProgress");
				(new Thread() {
					BasePanel newBp;
					BasePanel targetBp;
					BibtexDatabase newDatabase;
					BibtexDatabase targetDatabase;

					Runnable updateComponent = new Runnable() {

						/* TODO: This should probably be selectable on/off
						 * in the preferences window, but for now all
						 * Citation fetcher operations will sort by citation count.
						 */
						private void setSortingByCitationCount() {
							newBp.frame.prefs.put("priSort", "citeseerCitationCount");
							newBp.frame.prefs.put("secSort", "year");
							newBp.frame.prefs.put("terSort", "author");
							newBp.frame.prefs.putBoolean("priDescending", true);
							newBp.frame.prefs.putBoolean("secDescending", true);
							newBp.frame.prefs.putBoolean("terDescending", true);
						}

						public void run() {
							setSortingByCitationCount();
							tabbedPane.add(Globals.lang(GUIGlobals.untitledTitle), newBp);
							tabbedPane.setSelectedComponent(newBp);
							newBp.refreshTable();
							output(Globals.lang("Fetched all citations from target database."));
							targetBp.citeSeerFetcher.deactivateCitationFetcher();
							 }
					};

				  public void run() {
					try {
					newBp = new BasePanel(ths, prefs);
						targetBp = (BasePanel) tabbedPane.getSelectedComponent();
						newDatabase = newBp.getDatabase();
						targetDatabase = targetBp.getDatabase();
						basePanel().citeSeerFetcher.populate(newDatabase, targetDatabase);
						SwingUtilities.invokeLater(updateComponent);
					}
					catch (Exception ex) {
					  ex.printStackTrace();
					}
				  }
				}).start();
			} else {
			    JOptionPane.showMessageDialog((BasePanel) tabbedPane.getSelectedComponent(),
						Globals.lang("A CiteSeer fetch operation is currently in progress.") + "  " +
						Globals.lang("Please wait until it has finished."),
						Globals.lang("CiteSeer Fetch Error"),
						JOptionPane.WARNING_MESSAGE);
			}
		}
	}

  class FetchMedlineAction
      extends AbstractAction {
    public FetchMedlineAction() {
      super(Globals.lang("Fetch Medline"),
            new ImageIcon(GUIGlobals.fetchMedlineIcon));
      putValue(ACCELERATOR_KEY, prefs.getKey("Fetch Medline"));
      putValue(SHORT_DESCRIPTION, Globals.lang("Fetch Medline by ID"));
    }

    public void actionPerformed(ActionEvent e) {
      if (tabbedPane.getTabCount() > 0) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
          ( (BasePanel) tabbedPane.getComponentAt(i)).sidePaneManager.
              ensureVisible("fetchMedline");
          new FocusRequester(basePanel().medlineFetcher);
        }
      }
    }

  }

  class FetchAuthorMedlineAction
      extends AbstractAction {
    public FetchAuthorMedlineAction() {
      super(Globals.lang("Fetch Medline by author"),
            new ImageIcon(GUIGlobals.fetchMedlineIcon));
      putValue(SHORT_DESCRIPTION, Globals.lang("Fetch Medline by author"));
    }

    public void actionPerformed(ActionEvent e) {
      if (tabbedPane.getTabCount() > 0) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
          ( (BasePanel) tabbedPane.getComponentAt(i)).sidePaneManager.
              ensureVisible("fetchAuthorMedline");
          new FocusRequester(basePanel().medlineFetcher);
        }
      }
    }

  }

  // The action for opening the preferences dialog.

  AbstractAction showPrefs = new ShowPrefsAction();

  class ShowPrefsAction
      extends AbstractAction {
    public ShowPrefsAction() {
      super(Globals.lang("Preferences"),
            new ImageIcon(GUIGlobals.prefsIconFile));
      putValue(SHORT_DESCRIPTION, Globals.lang("Preferences"));
    }

    public void actionPerformed(ActionEvent e) {
      preferences();
    }
  }

  AboutAction aboutAction = new AboutAction();
  class AboutAction
      extends AbstractAction {
    public AboutAction() {
      super(Globals.lang("About JabRef"));

    }

    public void actionPerformed(ActionEvent e) {
      about();
    }
  }

  private void addBibEntries(ArrayList bibentries, String filename,
                             boolean intoNew) {
    if (bibentries.size() == 0) {
      // No entries found. We need a message for this.
      JOptionPane.showMessageDialog(ths, Globals.lang("No entries found. Please make sure you are "
                                                      +"using the correct import filter."), Globals.lang("Import failed"),
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    // Set owner field:
    if (prefs.getBoolean("useOwner"))
      Util.setDefaultOwner( bibentries, prefs.get("defaultOwner"));

    // check if bibentries is null
    if (bibentries == null) {
      output(Globals.lang("Ne entries imported."));
      return;
    }
    if (intoNew || (tabbedPane.getTabCount() == 0)) {
      // Import into new database.
      BibtexDatabase database = new BibtexDatabase();
      Iterator it = bibentries.iterator();
      while (it.hasNext()) {
        BibtexEntry entry = (BibtexEntry) it.next();
        try {
          entry.setId(Util.createId(entry.getType(), database));
          database.insertEntry(entry);
        }
        catch (KeyCollisionException ex) {
          //ignore
          System.err.println("KeyCollisionException [ addBibEntries(...) ]");
        }
      }
      HashMap meta = new HashMap();
      // Metadata are only put in bibtex files, so we will not find it
      // in imported files. Instead we pass an empty HashMap.
      BasePanel bp = new BasePanel(ths, database, null,
                                   meta, prefs);
      /*
            if (prefs.getBoolean("autoComplete")) {
            db.setCompleters(autoCompleters);
            }
       */

      tabbedPane.add(Globals.lang("untitled"), bp);
      bp.markBaseChanged();
      tabbedPane.setSelectedComponent(bp);
      if (tabbedPane.getTabCount() == 1) {
        setNonEmptyState();
      }
      output(Globals.lang("Imported database") + " '" + filename + "' " +
             Globals.lang("with") + " " +
             database.getEntryCount() + " " +
             Globals.lang("entries into new database") + ".");
    }
    else {
      // Import into current database.
      BasePanel basePanel = basePanel();
      BibtexDatabase database = basePanel.database;
      int oldCount = database.getEntryCount();
      NamedCompound ce = new NamedCompound("Import database");
      Iterator it = bibentries.iterator();
      while (it.hasNext()) {
        BibtexEntry entry = (BibtexEntry) it.next();
        try {
          entry.setId(Util.createId(entry.getType(), database));
          database.insertEntry(entry);
          ce.addEdit(new UndoableInsertEntry
                     (database, entry, basePanel));
        }
        catch (KeyCollisionException ex) {
          //ignore
          System.err.println("KeyCollisionException [ addBibEntries(...) ]");
        }
      }
      ce.end();
      basePanel.undoManager.addEdit(ce);
      basePanel.markBaseChanged();
      basePanel.refreshTable();
      output(Globals.lang("Imported database") + " '" + filename + "' " +
             Globals.lang("with") + " " +
             (database.getEntryCount() - oldCount) + " " +
             Globals.lang("entries into new database") + ".");

    }
  }

  private void setUpImportMenu(JMenu importMenu, boolean intoNew_) {
    final boolean intoNew = intoNew_;
    //
    // put in menu
    //
    //########################################
    JMenuItem newEndnoteFile_mItem = new JMenuItem(Globals.lang("Refer/Endnote")); //,						       new ImageIcon(getClass().getResource("images16/Open16.gif")));
    newEndnoteFile_mItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tempFilename = getNewFile();
        if (tempFilename != null) {
          ArrayList bibs = ImportFormatReader.readEndnote(tempFilename); //MedlineParser.readMedline(tempFilename);

          addBibEntries(bibs, tempFilename, intoNew);
        }
      }
    });
    importMenu.add(newEndnoteFile_mItem);
    //########################################
    JMenuItem newINSPECFile_mItem = new JMenuItem(Globals.lang("INSPEC")); //, new ImageIcon(getClass().getResource("images16/Open16.gif")));
    newINSPECFile_mItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tempFilename = getNewFile();
        if (tempFilename != null) {
          ArrayList bibs = ImportFormatReader.readINSPEC(tempFilename);

          addBibEntries(bibs, tempFilename, intoNew);
        }

      }
    });
    importMenu.add(newINSPECFile_mItem);
    //########################################
    JMenuItem newISIFile_mItem = new JMenuItem(Globals.lang("ISI")); //, new ImageIcon(getClass().getResource("images16/Open16.gif")));
    newISIFile_mItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tempFilename = getNewFile();
        if (tempFilename != null) {
          ArrayList bibs = ImportFormatReader.readISI(tempFilename);

          addBibEntries(bibs, tempFilename, intoNew);
        }

      }
    });
    importMenu.add(newISIFile_mItem);

    //########################################
    JMenuItem newMedlineFile_mItem = new JMenuItem(Globals.lang(
        "Medline XML File"));
    newMedlineFile_mItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tempFilename = getNewFile();
        if (tempFilename != null) {
          ArrayList bibs = ImportFormatReader.readMedline(tempFilename);

          addBibEntries(bibs, tempFilename, intoNew);
        }
      }
    });
    importMenu.add(newMedlineFile_mItem);
    //########################################
    JMenuItem newBibTeXMLFile_mItem = new JMenuItem(Globals.lang(
        "BibTeXML File"));
    newBibTeXMLFile_mItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tempFilename = getNewFile();
        if (tempFilename != null) {
          ArrayList bibs = ImportFormatReader.readBibTeXML(tempFilename);
          addBibEntries(bibs, tempFilename, intoNew);
        }
      }
    });
    importMenu.add(newBibTeXMLFile_mItem);
    //##############################
    /*JMenuItem newMedlineId_mItem = new JMenuItem(Globals.lang("Medline Fetch By ID"));
         newMedlineId_mItem.addActionListener(new ActionListener()
        {
     public void actionPerformed(ActionEvent e)
     {
            String idList = JOptionPane.showInputDialog(ths,"Enter a comma separated list of Medline ids","Fetch Medline Citation",JOptionPane.PLAIN_MESSAGE);
        if(idList==null || idList.trim().equals(""))//if user pressed cancel
     return;
        Pattern p = Pattern.compile("\\d+[,\\d+]*");
             Matcher m = p.matcher( idList );
         if ( m.matches() ) {
             ArrayList bibs = ImportFormatReader.fetchMedline(idList);
             addBibEntries( bibs, idList, intoNew);
         } else {
     JOptionPane.showMessageDialog(ths,"Sorry, I was expecting a comma separated list of Medline IDs (numbers)!","Input Error",JOptionPane.ERROR_MESSAGE);
        }
     }
        });
         importMenu.add(newMedlineId_mItem);*/

    //########################################
    JMenuItem newOvidFile_mItem = new JMenuItem(Globals.lang("Ovid")); //,new ImageIcon(getClass().getResource("images16/Open16.gif")));
    newOvidFile_mItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tempFilename = getNewFile();
        if (tempFilename != null) {
          ArrayList bibs = ImportFormatReader.readOvid(tempFilename);

          addBibEntries(bibs, tempFilename, intoNew);
        }

      }
    });
    importMenu.add(newOvidFile_mItem);
    //########################################
    JMenuItem newRefMan_mItem = new JMenuItem(Globals.lang("RIS")); //, new ImageIcon(getClass().getResource("images16/Open16.gif")));
    newRefMan_mItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tempFilename = getNewFile();
        if (tempFilename != null) {
          ArrayList bibs = ImportFormatReader.readReferenceManager10(
              tempFilename);

          addBibEntries(bibs, tempFilename, intoNew);
        }

      }
    });
    importMenu.add(newRefMan_mItem);

    //########################################

    JMenuItem newSciFinderFile_mItem = new JMenuItem(Globals.lang("SciFinder")); //,new ImageIcon(getClass().getResource("images16/Open16.gif")));
    //newSciFinderFile_mItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)); //Ctrl-F for new file
    newSciFinderFile_mItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tempFilename = getNewFile();
        if (tempFilename != null) { //filenm != null)
          //ArrayList bibs = Scifinder2bibtex.readSciFinderFile( tempFilename);//filename);//filenm );
          ArrayList bibs = ImportFormatReader.readScifinder(tempFilename);

          addBibEntries(bibs, tempFilename, intoNew);
        }
      }
    });
    importMenu.add(newSciFinderFile_mItem);

    JMenuItem newSixpack_mItem = new JMenuItem(Globals.lang("Sixpack"));
    newSixpack_mItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tempFilename = getNewFile();
        if (tempFilename != null) { //filenm != null)
          //ArrayList bibs = Scifinder2bibtex.readSciFinderFile( tempFilename);//filename);//filenm );
          ArrayList bibs = ImportFormatReader.readSixpack(tempFilename);

          addBibEntries(bibs, tempFilename, intoNew);
        }
      }
    });
    importMenu.add(newSixpack_mItem);

    //########################################
    JMenuItem newBiblioscapeTagFile_mItem = new JMenuItem(Globals.lang("Biblioscape Tag file"));//,new ImageIcon(getClass().getResource("images16/Open16.gif")));
    newBiblioscapeTagFile_mItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        String tempFilename = getNewFile();
        if( tempFilename != null )//filenm != null)
        {
          ArrayList bibs=ImportFormatReader.readBiblioscapeTagFile(tempFilename);

          addBibEntries( bibs, tempFilename, intoNew);
        }
      }
    });
    importMenu.add(newBiblioscapeTagFile_mItem);

    //########################################
    JMenuItem newJStorFile_mItem = new JMenuItem(Globals.lang("JStor file"));
    newJStorFile_mItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        String tempFilename = getNewFile();
        if( tempFilename != null )
        {
          ArrayList bibs=ImportFormatReader.readJStorFile(tempFilename);

          addBibEntries( bibs, tempFilename, intoNew);
        }
      }
    });
    importMenu.add(newJStorFile_mItem);

  }



  //
  // simply opens up a jfilechooser dialog and gets a filename
  // returns null if user selects cancel
  // it should also do a check perhaps to see if
  // file exists and is readable?
  //

  public String getNewFile() {

    return Globals.getNewFile(ths, prefs, new File(prefs.get("workingDirectory")),
                              null, JFileChooser.OPEN_DIALOG, false);

    /*JFileChooser fc;
    if (prefs.get("workingDirectory") == null) {
      fc = new JabRefFileChooser(new File(System.getProperty("user.home"))); //cwd));
    }
    else {
      fc = new JabRefFileChooser(new File(prefs.get("workingDirectory"))); //cwd));
    }

    fc.addChoosableFileFilter(new OpenFileFilter());
    fc.setDialogType(JFileChooser.OPEN_DIALOG);
    fc.showOpenDialog(null);
    File selectedFile = fc.getSelectedFile();
    if (selectedFile == null) { // cancel
      return null;
    }
    prefs.put("workingDirectory", selectedFile.getPath());
    return selectedFile.getAbsolutePath();*/
  }

  JMenuItem
      htmlItem = new JMenuItem(Globals.lang("HTML")),
      simpleHtmlItem = new JMenuItem(Globals.lang("Simple HTML")),
      //plainTextItem = new JMenuItem(Globals.lang("Plain text")),
      docbookItem = new JMenuItem(Globals.lang("Docbook")),
      bibtexmlItem = new JMenuItem(Globals.lang("BibTeXML"));

  private void setUpExportMenu(JMenu menu) {
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem) e.getSource();
        String lfFileName = null, extension = null;
        if (source == htmlItem) {
          lfFileName = "html";
          extension = ".html";
        }
        if (source == simpleHtmlItem) {
          lfFileName = "simplehtml";
          extension = ".html";
        }
        //else if (source == plainTextItem)
        //lfFileName = "text";
        else if (source == docbookItem) {
          lfFileName = "docbook";
          extension = ".xml";
        }
        else if (source == bibtexmlItem) {
          lfFileName = "bibtexml";
          extension = ".xml";
        }


        // We need to find out:
        // 1. The layout definition string to use. Or, rather, we
        //    must provide a Reader for the layout definition.
        // 2. The preferred extension for the layout format.
        // 3. The name of the file to use.
        File outFile = null;
        String chosenFile = Globals.getNewFile(ths, prefs, new File(prefs.get("workingDirectory")),
                                               extension, JFileChooser.SAVE_DIALOG, false);

         if (chosenFile != null)
           outFile = new File(chosenFile);

         else {
           return;
         }

        final String lfName = lfFileName;
        final File oFile = outFile;

        (new Thread() {
          public void run() {
            try {
              FileActions.exportDatabase
                  (basePanel().database,
                   lfName, oFile, prefs);
              output(Globals.lang("Exported database to file") + " '" +
                     oFile.getPath() + "'.");
            }
            catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        }).start();

      }
    };
    JMenuItem item;

    htmlItem.addActionListener(listener);
    menu.add(htmlItem);
    simpleHtmlItem.addActionListener(listener);
    menu.add(simpleHtmlItem);
    //plainTextItem.addActionListener(listener);
    //menu.add(plainTextItem);
    bibtexmlItem.addActionListener(listener);
    menu.add(bibtexmlItem);
    docbookItem.addActionListener(listener);
    menu.add(docbookItem);

  }

  /**
   * Interrogates the list of custom export formats defined, and adds them to the custom
   * export menu.
   */
  public void setUpCustomExportMenu() {
    customExportMenu.removeAll();
    for (int i=0; i<prefs.customExports.size(); i++) {
      String[] s = prefs.customExports.getElementAt(i);
      customExportMenu.add(new CustomExportAction(s[0], s[2], s[1]));
    }

  }

  class SaveSessionAction
      extends AbstractAction {
    public SaveSessionAction() {
      super(Globals.lang("Save session"), new ImageIcon(GUIGlobals.saveIconFile));
      putValue(ACCELERATOR_KEY, prefs.getKey("Save session"));
    }

    public void actionPerformed(ActionEvent e) {
      // Here we store the names of allcurrent filea. If
      // there is no current file, we remove any
      // previously stored file name.
      Vector filenames = new Vector();
      if (tabbedPane.getTabCount() > 0) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
          if (tabbedPane.getTitleAt(i).equals(GUIGlobals.untitledTitle)) {
            tabbedPane.setSelectedIndex(i);
            int answer = JOptionPane.showConfirmDialog
                (ths, Globals.lang
                 ("This untitled database must be saved first to be "
                  + "included in the saved session. Save now?"),
                 Globals.lang("Save database"),
                 JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
              // The user wants to save.
              try {
                basePanel().runCommand("save");
              }
              catch (Throwable ex) {}
            }
          }
          if (baseAt(i).file != null) {
            filenames.add(baseAt(i).file.getPath());
          }
        }
      }

      if (filenames.size() == 0) {
        output(Globals.lang("Not saved (empty session)") + ".");
        return;
      }
      else {
        String[] names = new String[filenames.size()];
        for (int i = 0; i < filenames.size(); i++) {
          names[i] = (String) filenames.elementAt(i);
        }
        prefs.putStringArray("savedSession", names);
        output(Globals.lang("Saved session") + ".");
      }

    }
  }

  class CustomExportAction extends AbstractAction {

    String extension, lfFileName, directory;

    public CustomExportAction(String name, String ext, String lf) {
      super(name);
      File lfFile = new File(lf);
      extension = ext;
      String filename = lfFile.getName();

      lfFileName = filename.substring(0, filename.length()-7);
      directory = lfFile.getParent()+File.separator;
    }

    public void actionPerformed(ActionEvent e) {
      // We need to find out:
      // 1. The layout definition string to use. Or, rather, we
      //    must provide a Reader for the layout definition.
      // 2. The preferred extension for the layout format.
      // 3. The name of the file to use.
      File outFile = null;
      String chosenFile = Globals.getNewFile(ths, prefs,
                                             new File(prefs.get("workingDirectory")),
                                             extension,
                                             JFileChooser.SAVE_DIALOG, false);

      if (chosenFile != null)
        outFile = new File(chosenFile);

      else {
        return;
      }

      final String lfName = lfFileName;
      final File oFile = outFile;

      (new Thread() {
        public void run() {
          try {
            FileActions.exportDatabase
                (basePanel().database, directory,
                 lfName, oFile, prefs);
            output(Globals.lang("Exported database to file") + " '" +
                   oFile.getPath() + "'.");
          }
          catch (Exception ex) {
            //ex.printStackTrace();
            JOptionPane.showMessageDialog(ths, ex.getMessage(), Globals.lang("Error"),
                                          JOptionPane.ERROR_MESSAGE);
          }
        }
      }).start();
    }
  }

  class LoadSessionAction
      extends AbstractAction {
    public LoadSessionAction() {
      super(Globals.lang("Load session"), new ImageIcon(GUIGlobals.openIconFile));
      putValue(ACCELERATOR_KEY, prefs.getKey("Load session"));
    }

    public void actionPerformed(ActionEvent e) {
      if (prefs.get("savedSession") == null) {
        output(Globals.lang("No saved session found."));
        return;
      }
      output(Globals.lang("Loading session..."));
      (new Thread() {
        public void run() {
          HashSet currentFiles = new HashSet();
          if (tabbedPane.getTabCount() > 0) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
              currentFiles.add(baseAt(i).file.getPath());
            }
          }
          int i0 = tabbedPane.getTabCount();
          String[] names = prefs.getStringArray("savedSession");
          for (int i = 0; i < names.length; i++) {
            if (!currentFiles.contains(names[i])) {
              fileToOpen = new File(names[i]);
              if (fileToOpen.exists()) {
                //Util.pr("Opening last edited file:"
                //+fileToOpen.getName());
                openDatabaseAction.openIt(i == 0);
              }
            }
          }
          output(Globals.lang("Files opened") + ": " +
                 (tabbedPane.getTabCount() - i0));
        }
      }).start();

    }
  }

  class ChangeTabAction
      extends AbstractAction {
    private boolean next;
    public ChangeTabAction(boolean next) {
      super(Globals.lang(next ? "Next tab" : "Previous tab"));
      this.next = next;
      //Util.pr(""+prefs.getKey("Next tab"));
      putValue(ACCELERATOR_KEY,
               (next ? prefs.getKey("Next tab") : prefs.getKey("Previous tab")));
    }

    public void actionPerformed(ActionEvent e) {
      int i = tabbedPane.getSelectedIndex();
      int newI = (next ? i + 1 : i - 1);
      if (newI < 0) {
        newI = tabbedPane.getTabCount() - 1;
      }
      if (newI == tabbedPane.getTabCount()) {
        newI = 0;
      }
      tabbedPane.setSelectedIndex(newI);
    }
  }

  /**
   * Class for handling general actions; cut, copy and paste. The focused component is
   * kept track of by Globals.focusListener, and we call the action stored under the
   * relevant name in its action map.
   */
  class EditAction
      extends AbstractAction {
    private String command;
    public EditAction(String command, URL icon) {
      super(Globals.lang(Util.nCase(command)), new ImageIcon(icon));
      this.command = command;
      String nName = Util.nCase(command);
      putValue(ACCELERATOR_KEY, prefs.getKey(nName));
      putValue(SHORT_DESCRIPTION, Globals.lang(nName));
      //putValue(ACCELERATOR_KEY,
      //         (next?prefs.getKey("Next tab"):prefs.getKey("Previous tab")));
    }

    public void actionPerformed(ActionEvent e) {

      //Util.pr(Globals.focusListener.getFocused().toString());
      JComponent source = Globals.focusListener.getFocused();
      try {
        source.getActionMap().get(command).actionPerformed
            (new ActionEvent(source, 0, command));
      } catch (NullPointerException ex) {
        // No component is focused, so we do nothing.
      }
    }
  }

  class CustomizeExportsAction extends AbstractAction {
    public CustomizeExportsAction() {
      super(Globals.lang("Manage custom exports"));
    }

    public void actionPerformed(ActionEvent e) {
      ExportCustomizationDialog ecd = new ExportCustomizationDialog(ths);
      ecd.show();
    }
  }

}
