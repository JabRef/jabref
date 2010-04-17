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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.export.*;
import net.sf.jabref.external.ExternalFileTypeEditor;
import net.sf.jabref.external.PushToApplicationButton;
import net.sf.jabref.groups.EntryTableTransferHandler;
import net.sf.jabref.groups.GroupSelector;
import net.sf.jabref.gui.DatabasePropertiesDialog;
import net.sf.jabref.gui.EntryCustomizationDialog2;
import net.sf.jabref.gui.GenFieldsCustomizer;
import net.sf.jabref.gui.ImportInspectionDialog;
import net.sf.jabref.gui.SortTabsAction;
import net.sf.jabref.imports.CiteSeerFetcher;
import net.sf.jabref.imports.EntryFetcher;
import net.sf.jabref.imports.GeneralFetcher;
import net.sf.jabref.imports.ImportCustomizationDialog;
import net.sf.jabref.imports.ImportFormat;
import net.sf.jabref.imports.ImportFormats;
import net.sf.jabref.imports.ImportMenuItem;
import net.sf.jabref.imports.OpenDatabaseAction;
import net.sf.jabref.journals.ManageJournalsAction;
import net.sf.jabref.label.ArticleLabelRule;
import net.sf.jabref.label.BookLabelRule;
import net.sf.jabref.label.IncollectionLabelRule;
import net.sf.jabref.label.InproceedingsLabelRule;
import net.sf.jabref.label.LabelMaker;
import net.sf.jabref.plugin.PluginCore;
import net.sf.jabref.plugin.PluginInstallerAction;
import net.sf.jabref.plugin.core.JabRefPlugin;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.EntryFetcherExtension;
import net.sf.jabref.sql.DbImportAction;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.undo.UndoableRemoveEntry;
import net.sf.jabref.util.MassSetFieldAction;
import net.sf.jabref.wizard.auximport.gui.FromAuxDialog;
import net.sf.jabref.wizard.integrity.gui.IntegrityWizard;

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;
import com.jgoodies.uif_lite.component.UIFSplitPane;

/**
 * The main window of the application.
 */
public class JabRefFrame extends JFrame implements OutputPrinter {

    UIFSplitPane contentPane = new UIFSplitPane();

    JabRefPreferences prefs = Globals.prefs; 
    PrefsDialog3 prefsDialog = null;
    
    private int lastTabbedPanelSelectionIndex = -1 ;

    // The sidepane manager takes care of populating the sidepane.
    public SidePaneManager sidePaneManager;

    JTabbedPane tabbedPane = new JTabbedPane();
    
    final Insets marg = new Insets(1,0,2,0);

    class ToolBar extends JToolBar {
      void addAction(Action a) {
        JButton b = new JButton(a);
        b.setText(null);
        if (!Globals.ON_MAC)
            b.setMargin(marg);
        add(b);
      }
    }
    ToolBar tlb = new ToolBar();

    JMenuBar mb = new JMenuBar();
    JMenu pluginMenu = subMenu("Plugins");
    boolean addedToPluginMenu = false;

    GridBagLayout gbl = new GridBagLayout();

    GridBagConstraints con = new GridBagConstraints();

    JLabel statusLine = new JLabel("", SwingConstants.LEFT), statusLabel = new JLabel(Globals
        .lang("Status")
        + ":", SwingConstants.LEFT);
    JProgressBar progressBar = new JProgressBar();

    private FileHistory fileHistory = new FileHistory(prefs, this);

    LabelMaker labelMaker;

    // The help window.
    public HelpDialog helpDiag = new HelpDialog(this);

    // Here we instantiate menu/toolbar actions. Actions regarding
    // the currently open database are defined as a GeneralAction
    // with a unique command string. This causes the appropriate
    // BasePanel's runCommand() method to be called with that command.
    // Note: GeneralAction's constructor automatically gets translations
    // for the name and message strings.

  // References to the toggle buttons in the toolbar:
  public JToggleButton groupToggle, searchToggle, previewToggle, highlightAny,
      highlightAll;

  OpenDatabaseAction
      open = new OpenDatabaseAction(this, true);
  AbstractAction
      close = new CloseDatabaseAction(),
      quit = new CloseAction(),
      selectKeys = new SelectKeysAction(),
      newDatabaseAction = new NewDatabaseAction(),
      newSubDatabaseAction = new NewSubDatabaseAction(),
      integrityCheckAction = new IntegrityCheckAction(),
      help = new HelpAction("JabRef help", helpDiag,
                            GUIGlobals.baseFrameHelp, Globals.lang("JabRef help"),
                            prefs.getKey("Help")),
      contents = new HelpAction("Help contents", helpDiag,
                                GUIGlobals.helpContents, Globals.lang("Help contents"),
                                GUIGlobals.getIconUrl("helpContents")),
      about = new HelpAction("About JabRef", helpDiag,
                             GUIGlobals.aboutPage, Globals.lang("About JabRef"),
                             GUIGlobals.getIconUrl("about")),
      editEntry = new GeneralAction("edit", "Edit entry",
                               Globals.lang("Edit entry"),
                               prefs.getKey("Edit entry")),
      focusTable = new GeneralAction("focusTable", "Focus entry table",
                Globals.lang("Move the keyboard focus to the entry table"),
                prefs.getKey("Focus entry table")),
      save = new GeneralAction("save", "Save database",
                               Globals.lang("Save database"),
                               prefs.getKey("Save database")),
      saveAs = new GeneralAction("saveAs", "Save database as ...",
                                 Globals.lang("Save database as ..."),
                                 prefs.getKey("Save database as ...")),
      saveAll = new SaveAllAction(JabRefFrame.this),
      saveSelectedAs = new GeneralAction("saveSelectedAs",
                                         "Save selected as ...",
                                         Globals.lang("Save selected as ..."),
                                         GUIGlobals.getIconUrl("saveAs")),
      exportAll = ExportFormats.getExportAction(this, false),
      exportSelected = ExportFormats.getExportAction(this, true),
      importCurrent = ImportFormats.getImportAction(this, false),
      importNew = ImportFormats.getImportAction(this, true),
      nextTab = new ChangeTabAction(true),
      prevTab = new ChangeTabAction(false),
      sortTabs = new SortTabsAction(this),
      undo = new GeneralAction("undo", "Undo", Globals.lang("Undo"),
                               prefs.getKey("Undo")),
      redo = new GeneralAction("redo", "Redo", Globals.lang("Redo"),
                               prefs.getKey("Redo")),
      forward = new GeneralAction("forward", "Forward", Globals.lang("Forward"),
              "right", prefs.getKey("Forward")),
      back = new GeneralAction("back", "Back", Globals.lang("Back"),
              "left", prefs.getKey("Back")),
      /*cut = new GeneralAction("cut", "Cut", Globals.lang("Cut"),
         GUIGlobals.cutIconFile,
         prefs.getKey("Cut")),*/
      delete = new GeneralAction("delete", "Delete", Globals.lang("Delete"),
                                 prefs.getKey("Delete")),
      /*copy = new GeneralAction("copy", "Copy", Globals.lang("Copy"),
                               GUIGlobals.copyIconFile,
                               prefs.getKey("Copy")),*/
      copy = new EditAction("copy", GUIGlobals.getIconUrl("copy")),
      paste = new EditAction("paste", GUIGlobals.getIconUrl("paste")),
      cut = new EditAction("cut", GUIGlobals.getIconUrl("cut")),
      mark = new GeneralAction("markEntries", "Mark entries",
                               Globals.lang("Mark entries"),
                               prefs.getKey("Mark entries")),
       unmark = new GeneralAction("unmarkEntries", "Unmark entries",
                                  Globals.lang("Unmark entries"),
                                  prefs.getKey("Unmark entries")),
       unmarkAll = new GeneralAction("unmarkAll", "Unmark all"),
      manageSelectors = new GeneralAction("manageSelectors", "Manage content selectors"),
      saveSessionAction = new SaveSessionAction(),
      loadSessionAction = new LoadSessionAction(),
      incrementalSearch = new GeneralAction("incSearch", "Incremental search",
                                            Globals.lang("Start incremental search"),
                                            prefs.getKey("Incremental search")),
      normalSearch = new GeneralAction("search", "Search", Globals.lang("Search"),
                                       prefs.getKey("Search")),
      toggleSearch = new GeneralAction("toggleSearch", "Search", Globals.lang("Toggle search panel")),

      fetchCiteSeer = new FetchCiteSeerAction(),
      importCiteSeer = new ImportCiteSeerAction(),
      copyKey = new GeneralAction("copyKey", "Copy BibTeX key", 
            prefs.getKey("Copy BibTeX key")),
      //"Put a BibTeX reference to the selected entries on the clipboard",
      copyCiteKey = new GeneralAction("copyCiteKey", "Copy \\cite{BibTeX key}",
                                      //"Put a BibTeX reference to the selected entries on the clipboard",
                                      prefs.getKey("Copy \\cite{BibTeX key}")),
      mergeDatabaseAction = new GeneralAction("mergeDatabase",
                                              "Append database",
                                              Globals.lang("Append contents from a BibTeX database into the currently viewed database"),
                                              GUIGlobals.getIconUrl("open")),
      //prefs.getKey("Open")),
      /*remove = new GeneralAction("remove", "Remove", "Remove selected entries",
        GUIGlobals.removeIconFile),*/
      selectAll = new GeneralAction("selectAll", "Select all",
                                    prefs.getKey("Select all")),
      replaceAll = new GeneralAction("replaceAll", "Replace string",
                                     prefs.getKey("Replace string")),

      editPreamble = new GeneralAction("editPreamble", "Edit preamble",
                                       Globals.lang("Edit preamble"),
                                       prefs.getKey("Edit preamble")),
      editStrings = new GeneralAction("editStrings", "Edit strings",
                                      Globals.lang("Edit strings"),
                                      prefs.getKey("Edit strings")),
      toggleGroups = new GeneralAction("toggleGroups",
                                       "Toggle groups interface",
                                       Globals.lang("Toggle groups interface"),
                                       prefs.getKey("Toggle groups interface")),
      togglePreview = new GeneralAction("togglePreview",
                                        "Toggle entry preview",
                                        Globals.lang("Toggle entry preview"),
                                        prefs.getKey("Toggle entry preview")),
      toggleHighlightAny = new GeneralAction("toggleHighlightGroupsMatchingAny",
                                        "Highlight groups matching any selected entry",
                                        Globals.lang("Highlight groups matching any selected entry"),
                                        GUIGlobals.getIconUrl("groupsHighlightAny")),
      toggleHighlightAll = new GeneralAction("toggleHighlightGroupsMatchingAll",
                                        "Highlight groups matching all selected entries",
                                        Globals.lang("Highlight groups matching all selected entries"),
                                        GUIGlobals.getIconUrl("groupsHighlightAll")),
      switchPreview = new GeneralAction("switchPreview",
                                        "Switch preview layout",
                                        prefs.getKey("Switch preview layout")),
       makeKeyAction = new GeneralAction("makeKey", "Autogenerate BibTeX keys",
                                        Globals.lang("Autogenerate BibTeX keys"),
                                        prefs.getKey("Autogenerate BibTeX keys")),


      writeXmpAction = new GeneralAction("writeXMP", "Write XMP-metadata to PDFs",
                                        Globals.lang("Will write XMP-metadata to the PDFs linked from selected entries."),
                                        prefs.getKey("Write XMP")),
      openFile = new GeneralAction("openExternalFile", "Open file",
                                   Globals.lang("Open file"),
                                   prefs.getKey("Open file")),
      openPdf = new GeneralAction("openFile", "Open PDF or PS",
                                   Globals.lang("Open PDF or PS"),
                                   prefs.getKey("Open PDF or PS")),
      openUrl = new GeneralAction("openUrl", "Open URL or DOI",
                                  Globals.lang("Open URL or DOI"),
                                  prefs.getKey("Open URL or DOI")),
      openSpires = new GeneralAction("openSpires", "Open SPIRES entry",
                                          Globals.lang("Open SPIRES entry"),
                                          prefs.getKey("Open SPIRES entry")),
      dupliCheck = new GeneralAction("dupliCheck", "Find duplicates"),
      //strictDupliCheck = new GeneralAction("strictDupliCheck", "Find and remove exact duplicates"),
      plainTextImport = new GeneralAction("plainTextImport",
                                          "New entry from plain text",
                                          prefs.getKey("New from plain text")),


      customExpAction = new CustomizeExportsAction(),
      customImpAction = new CustomizeImportsAction(),
      customFileTypesAction = ExternalFileTypeEditor.getAction(this),
      exportToClipboard = new GeneralAction("exportToClipboard", "Export selected entries to clipboard"),
      expandEndnoteZip = new ExpandEndnoteFilters(this),
        autoSetPdf = new GeneralAction("autoSetPdf", Globals.lang("Synchronize %0 links", "PDF"), Globals.prefs.getKey("Synchronize PDF")),
        autoSetPs = new GeneralAction("autoSetPs", Globals.lang("Synchronize %0 links", "PS"), Globals.prefs.getKey("Synchronize PS")),
        autoSetFile = new GeneralAction("autoSetFile", Globals.lang("Synchronize file links"), Globals.prefs.getKey("Synchronize files")),

    abbreviateMedline = new GeneralAction("abbreviateMedline", "Abbreviate journal names (MEDLINE)",
                Globals.lang("Abbreviate journal names of the selected entries (MEDLINE abbreviation)")),
  abbreviateIso = new GeneralAction("abbreviateIso", "Abbreviate journal names (ISO)",
                          Globals.lang("Abbreviate journal names of the selected entries (ISO abbreviation)"),
                          Globals.prefs.getKey("Abbreviate")),


    unabbreviate = new GeneralAction("unabbreviate", "Unabbreviate journal names",
                    Globals.lang("Unabbreviate journal names of the selected entries"),
            Globals.prefs.getKey("Unabbreviate")),
    manageJournals = new ManageJournalsAction(this),
    databaseProperties = new DatabasePropertiesAction(),
    upgradeExternalLinks = new GeneralAction("upgradeLinks", "Upgrade external links",
            Globals.lang("Upgrade external PDF/PS links to use the '%0' field.", GUIGlobals.FILE_FIELD)),
      errorConsole = Globals.errorConsole.getAction(this),
    test = new GeneralAction("test", "Test"),

    dbConnect = new GeneralAction("dbConnect", "Connect to external SQL database",
         Globals.lang("Connect to external SQL database"), 
          GUIGlobals.getIconUrl("dbConnect") ),

    dbExport = new GeneralAction("dbExport", "Export to external SQL database",
         Globals.lang("Export to external SQL database"), 
          GUIGlobals.getIconUrl("dbExport") ),
    dbImport = new DbImportAction(this).getAction(),
    //downloadFullText = new GeneralAction("downloadFullText", "Look up full text document",
    //        Globals.lang("Follow DOI or URL link and try to locate PDF full text document")),
    increaseFontSize = new IncreaseTableFontSizeAction(),
    decreseFontSize = new DecreaseTableFontSizeAction(),
    installPlugin = new PluginInstallerAction(this);
            
    PushToApplicationButton pushExternalButton;

    CiteSeerFetcher citeSeerFetcher;
    
    List<EntryFetcher> fetchers = new LinkedList<EntryFetcher>();
    List<Action> fetcherActions = new LinkedList<Action>();

    SearchManager2 searchManager;
    public GroupSelector groupSelector;

  // The menus for importing/appending other formats
  JMenu importMenu = subMenu("Import into current database"),
      importNewMenu = subMenu("Import into new database"),
      exportMenu = subMenu("Export"),
      customExportMenu = subMenu("Custom export"),
      newDatabaseMenu = subMenu("New database" );

  // Other submenus
  JMenu checkAndFix = subMenu("Scan database...");


  // The action for adding a new entry of unspecified type.
  NewEntryAction newEntryAction = new NewEntryAction(prefs.getKey("New entry"));
  NewEntryAction[] newSpecificEntryAction = new NewEntryAction[]
  {
      new NewEntryAction("article", prefs.getKey("New article")),
      new NewEntryAction("book", prefs.getKey("New book")),
      new NewEntryAction("phdthesis", prefs.getKey("New phdthesis")),
      new NewEntryAction("inbook", prefs.getKey("New inbook")),
      new NewEntryAction("mastersthesis", prefs.getKey("New mastersthesis")),
      new NewEntryAction("proceedings", prefs.getKey("New proceedings")),
      new NewEntryAction("inproceedings"),
      new NewEntryAction("conference"),
      new NewEntryAction("incollection"),
      new NewEntryAction("booklet"),
      new NewEntryAction("manual"),
      new NewEntryAction("techreport"),
      new NewEntryAction("unpublished",
                         prefs.getKey("New unpublished")),
      new NewEntryAction("misc"),
      new NewEntryAction("other")
  };

  public JabRefFrame() {
    init();
    updateEnabledState();
  }

  private void init() {

        macOSXRegistration();

        UIManager.put("FileChooser.readOnly", Globals.prefs.getBoolean("filechooserDisableRename"));
      
        MyGlassPane glassPane = new MyGlassPane();
        setGlassPane(glassPane);
        // glassPane.setVisible(true);

        setTitle(GUIGlobals.frameTitle);
        setIconImage(GUIGlobals.getImage("jabrefIcon").getImage());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                (new CloseAction()).actionPerformed(null);
            }
        });

        initLabelMaker();

        initSidePane();
        
        initLayout();
        
        initActions();

        // Fixes occasional Window malbehaviour in Linux
        setBounds(0, 0, (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth(),
            (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() );
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        if (Globals.prefs.getBoolean("rememberWindowLocation")) {
            
            int sizeX = prefs.getInt("sizeX");
            int sizeY = prefs.getInt("sizeY");
            int posX = prefs.getInt("posX");
            int posY = prefs.getInt("posY");
            
            //
            // Fix for [ 1738920 ] Windows Position in Multi-Monitor environment
            //
            // Do not put a window outside the screen if the preference values are wrong.
            //
            // Useful reference: http://www.exampledepot.com/egs/java.awt/screen_ScreenSize.html?l=rel
            // googled on forums.java.sun.com graphicsenvironment second screen java
            //
            if (GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length == 1){

                Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

                int height = (int) dim.getHeight();
                int width = (int) dim.getWidth();

                if (posX + sizeX > width) {
                    if (sizeX <= width) {
                        posX = width - sizeX;
                    } else {
                        posX = prefs.getIntDefault("posX");
                        sizeX = prefs.getIntDefault("sizeX");
                    }
                }

                if (posY + sizeY > height) {
                    if (sizeY <= height) {
                        posY = height - sizeY;
                    } else {
                        posY = prefs.getIntDefault("posY");
                        sizeY = prefs.getIntDefault("sizeY");
                    }
                }
            }
            setBounds(posX, posY, sizeX, sizeY);
        }
        tabbedPane.setBorder(null);
        tabbedPane.setForeground(GUIGlobals.inActiveTabbed);

        /*
         * The following state listener makes sure focus is registered with the
         * correct database when the user switches tabs. Without this,
         * cut/paste/copy operations would some times occur in the wrong tab.
         */
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                markActiveBasePanel();

                BasePanel bp = basePanel();
                if (bp != null) {
                    groupToggle.setSelected(sidePaneManager.isComponentVisible("groups"));
                    searchToggle.setSelected(sidePaneManager.isComponentVisible("search"));
                    previewToggle.setSelected(Globals.prefs.getBoolean("previewEnabled"));
                    highlightAny
                        .setSelected(Globals.prefs.getBoolean("highlightGroupsMatchingAny"));
                    highlightAll
                        .setSelected(Globals.prefs.getBoolean("highlightGroupsMatchingAll"));
                    Globals.focusListener.setFocused(bp.mainTable);
                    // Set correct enabled state for Back and Forward actions:
                    bp.setBackAndForwardEnabledState();
                    new FocusRequester(bp.mainTable);
                }
            }
        });
    }



    private void initSidePane() {
        sidePaneManager = new SidePaneManager(this);

        Globals.sidePaneManager = this.sidePaneManager;
        Globals.helpDiag = this.helpDiag;

        /*
         * Load fetchers that are plug-in extensions
         */
        JabRefPlugin jabrefPlugin = JabRefPlugin.getInstance(PluginCore.getManager());
    	if (jabrefPlugin != null){
                for (EntryFetcherExtension ext : jabrefPlugin.getEntryFetcherExtensions()){
                    try {
                            EntryFetcher fetcher = ext.getEntryFetcher();
                            if (fetcher != null){
                                    fetchers.add(fetcher);
                            }
                    } catch (ClassCastException ex) {
                        PluginCore.getManager().disablePlugin(ext.getDeclaringPlugin().getDescriptor());
                        ex.printStackTrace();
                    }
                } 
    	}
        
        citeSeerFetcher = new CiteSeerFetcher(sidePaneManager);
        groupSelector = new GroupSelector(this, sidePaneManager);
        searchManager = new SearchManager2(this, sidePaneManager);

        sidePaneManager.register("CiteSeerProgress", citeSeerFetcher);
        sidePaneManager.register("groups", groupSelector);
        sidePaneManager.register("search", searchManager);

        // Show the search panel if it was visible at last shutdown:
        if (Globals.prefs.getBoolean("searchPanelVisible"))
            sidePaneManager.show("search");
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


  // General info dialog.  The OSXAdapter calls this method when "About OSXAdapter"
  // is selected from the application menu.
  public void about() {
    JDialog about = new JDialog(JabRefFrame.this, Globals.lang("About JabRef"),
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
      Util.placeDialog(about, JabRefFrame.this);
      about.setVisible(true);
    }
    catch (IOException ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(JabRefFrame.this, "Could not load file 'About.html'",
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }

  }

  // General preferences dialog.  The OSXAdapter calls this method when "Preferences..."
  // is selected from the application menu.
  public void preferences() {
    //PrefsDialog.showPrefsDialog(JabRefFrame.this, prefs);
      AbstractWorker worker = new AbstractWorker() {
              public void run() {
                  output(Globals.lang("Opening preferences..."));
                  if (prefsDialog == null) {
                      prefsDialog = new PrefsDialog3(JabRefFrame.this);
                      Util.placeDialog(prefsDialog, JabRefFrame.this);
                  }
                  else
                      prefsDialog.setValues();

              }
              public void update() {
                  prefsDialog.setVisible(true);
                  output("");
              }
          };
      worker.getWorker().run();
      worker.getCallBack().update();
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
    Vector<String> filenames = new Vector<String>();
    if (tabbedPane.getTabCount() > 0) {
      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        if (baseAt(i).baseChanged) {
          tabbedPane.setSelectedIndex(i);
          int answer = JOptionPane.showConfirmDialog
              (JabRefFrame.this, Globals.lang
               ("Database has changed. Do you "
                + "want to save before closing?"),
               Globals.lang("Save before closing"),
               JOptionPane.YES_NO_CANCEL_OPTION);

          if ( (answer == JOptionPane.CANCEL_OPTION) ||
              (answer == JOptionPane.CLOSED_OPTION)) {
            close = false; // The user has cancelled.
              return;
          }
          if (answer == JOptionPane.YES_OPTION) {
            // The user wants to save.
            try {
              //basePanel().runCommand("save");
                SaveDatabaseAction saveAction = new SaveDatabaseAction(basePanel());
                saveAction.runCommand();
                if (saveAction.isCancelled() || !saveAction.isSuccess()) {
                    // The action was either cancelled or unsuccessful.
                    // Break!
                    output(Globals.lang("Unable to save database"));
                    close = false;
                }
            }
            catch (Throwable ex) {
              // Something prevented the file
              // from being saved. Break!!!
              close = false;
              break;
            }
          }
        }
        if (baseAt(i).getFile() != null) {
          filenames.add(baseAt(i).getFile().getAbsolutePath());
        }
      }
    }
    if (close) {
      dispose();

      prefs.putInt("posX", JabRefFrame.this.getLocation().x);
      prefs.putInt("posY", JabRefFrame.this.getLocation().y);
      prefs.putInt("sizeX", JabRefFrame.this.getSize().width);
      prefs.putInt("sizeY", JabRefFrame.this.getSize().height);
//      prefs.putBoolean("windowMaximised", (getExtendedState()&MAXIMIZED_BOTH)>0);
      prefs.putBoolean("windowMaximised", (getExtendedState() == Frame.MAXIMIZED_BOTH));
      prefs.putBoolean("rememberWindowLocation", !Globals.prefs.getBoolean("windowMaximised"));

      prefs.putBoolean("searchPanelVisible", sidePaneManager.isComponentVisible("search"));
      // Store divider location for side pane:
      int width = contentPane.getDividerLocation();
      if (width > 0) 
          prefs.putInt("sidePaneWidth", width);
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
            names[i] = filenames.elementAt(i);

          }
          prefs.putStringArray("lastEdited", names);
        }

      }

      fileHistory.storeHistory();
      prefs.customExports.store();
      prefs.customImports.store();
      BibtexEntryType.saveCustomEntryTypes(prefs);

      // Clear autosave files:
      if (Globals.autoSaveManager != null)
        Globals.autoSaveManager.clearAutoSaves();

      // Let the search interface store changes to prefs.
      // But which one? Let's use the one that is visible.
      if (basePanel() != null) {
        (searchManager).updatePrefs();
      }
      
      prefs.flush();
      
      System.exit(0); // End program.
    }
  }

    

  private void macOSXRegistration() {
    if (Globals.osName.equals(Globals.MAC)) {
      try {
        Class<?> osxAdapter = Class.forName("osxadapter.OSXAdapter");

        Class<?>[] defArgs = {
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


  private void initLayout() {
    tabbedPane.putClientProperty(Options.NO_CONTENT_BORDER_KEY, Boolean.TRUE);

    setProgressBarVisible(false);

      pushExternalButton = new PushToApplicationButton(this,
              PushToApplicationButton.applications);
    fillMenu();
    createToolBar();
    getContentPane().setLayout(gbl);
      contentPane.setDividerSize(2);
      contentPane.setBorder(null);
    //getContentPane().setBackground(GUIGlobals.lightGray);
    con.fill = GridBagConstraints.HORIZONTAL;
    con.anchor = GridBagConstraints.WEST;
    con.weightx = 1;
    con.weighty = 0;
    con.gridwidth = GridBagConstraints.REMAINDER;

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
    gbl.setConstraints(contentPane, con);
    getContentPane().add(contentPane);
    contentPane.setRightComponent(tabbedPane);
    contentPane.setLeftComponent(sidePaneManager.getPanel());
    sidePaneManager.updateView();

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
    con.gridwidth = 1;
    gbl.setConstraints(statusLine, con);
    status.add(statusLine);
    con.weightx = 0;
    con.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(progressBar, con);
    status.add(progressBar);
    con.weightx = 1;
    con.gridwidth = GridBagConstraints.REMAINDER;
    statusLabel.setForeground(GUIGlobals.validFieldColor.darker());
    con.insets = new Insets(0, 0, 0, 0);
    gbl.setConstraints(status, con);
    getContentPane().add(status);


      // Drag and drop for tabbedPane:
      TransferHandler xfer = new EntryTableTransferHandler(null, this, null);
      tabbedPane.setTransferHandler(xfer);
      tlb.setTransferHandler(xfer);
      mb.setTransferHandler(xfer);
      sidePaneManager.getPanel().setTransferHandler(xfer);
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
  public BasePanel baseAt(int i) {
    return (BasePanel) tabbedPane.getComponentAt(i);
  }

  public void showBaseAt(int i) {
      tabbedPane.setSelectedIndex(i);
  }

    public void showBasePanel(BasePanel bp) {
        tabbedPane.setSelectedComponent(bp);
    }

  /**
   * Returns the currently viewed BasePanel.
   */
  public BasePanel basePanel() {
    return (BasePanel) tabbedPane.getSelectedComponent();
  }

    /**
     * @return the BasePanel count.
     */
    public int baseCount() {
        return tabbedPane.getComponentCount();
    }
    
  /**
   * handle the color of active and inactive JTabbedPane tabs
   */
  private void markActiveBasePanel()
  {
    int now = tabbedPane.getSelectedIndex() ;
    int len = tabbedPane.getTabCount() ;
    if ((lastTabbedPanelSelectionIndex > -1) && (lastTabbedPanelSelectionIndex < len))
      tabbedPane.setForegroundAt(lastTabbedPanelSelectionIndex, GUIGlobals.inActiveTabbed);
    if ( (now > -1) &&  (now < len))
      tabbedPane.setForegroundAt(now, GUIGlobals.activeTabbed);
    lastTabbedPanelSelectionIndex = now ;
  }

  private int getTabIndex(JComponent comp) {
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      if (tabbedPane.getComponentAt(i) == comp) {
        return i;
      }
    }
    return -1;
  }

  public JTabbedPane getTabbedPane() { return tabbedPane; }

  public String getTabTitle(JComponent comp) {
    return tabbedPane.getTitleAt(getTabIndex(comp));
  }

    public String getTabTooltip(JComponent comp) {
        return tabbedPane.getToolTipTextAt(getTabIndex(comp));
    }

  public void setTabTitle(JComponent comp, String title, String toolTip) {
      int index = getTabIndex(comp);
      tabbedPane.setTitleAt(index, title);
      tabbedPane.setToolTipTextAt(index, toolTip);
  }

  class GeneralAction
      extends MnemonicAwareAction {
    private String command;
    public GeneralAction(String command, String text,
                         String description, URL icon) {
      super(new ImageIcon(icon));
      this.command = command;
      putValue(NAME, text);
      putValue(SHORT_DESCRIPTION, Globals.lang(description));
    }

    public GeneralAction(String command, String text,
                         String description, String imageName,
                         KeyStroke key) {
      super(GUIGlobals.getImage(imageName));
      this.command = command;
      putValue(NAME, text);
      putValue(ACCELERATOR_KEY, key);
      putValue(SHORT_DESCRIPTION, Globals.lang(description));
    }

      public GeneralAction(String command, String text) {
          putValue(NAME, text);
          this.command = command;
      }

      public GeneralAction(String command, String text, KeyStroke key) {
          this.command = command;
          putValue(NAME, text);
          putValue(ACCELERATOR_KEY, key);
      }

      public GeneralAction(String command, String text, String description) {
          this.command = command;
          ImageIcon icon = GUIGlobals.getImage(command);
          if (icon != null)
              putValue(SMALL_ICON, icon);
          putValue(NAME, text);
          putValue(SHORT_DESCRIPTION, Globals.lang(description));
      }

      public GeneralAction(String command, String text, String description, KeyStroke key) {
          this.command = command;
          ImageIcon icon = GUIGlobals.getImage(command);
          if (icon != null)
              putValue(SMALL_ICON, icon);
          putValue(NAME, text);
          putValue(SHORT_DESCRIPTION, Globals.lang(description));
          putValue(ACCELERATOR_KEY, key);
      }

  /*    public GeneralAction(String command, String text, String description,
                           URL imageUrl, KeyStroke key) {
      this.command = command;
        ImageIcon icon = GUIGlobals.getImage(command);
        if (icon != null)
            putValue(SMALL_ICON, icon);
      putValue(NAME, text);
      putValue(SHORT_DESCRIPTION, Globals.lang(description));
        putValue(ACCELERATOR_KEY, key);
    }*/

    public void actionPerformed(ActionEvent e) {
      if (tabbedPane.getTabCount() > 0) {
        try {
          ( (BasePanel) (tabbedPane.getSelectedComponent ()))
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
      extends MnemonicAwareAction {

    String type = null; // The type of item to create.
    KeyStroke keyStroke = null; // Used for the specific instances.

    public NewEntryAction(KeyStroke key) {
      // This action leads to a dialog asking for entry type.
      super(GUIGlobals.getImage("add"));
      putValue(NAME, "New entry");
      putValue(ACCELERATOR_KEY, key);
      putValue(SHORT_DESCRIPTION, Globals.lang("New BibTeX entry"));
    }

    public NewEntryAction(String type_) {
      // This action leads to the creation of a specific entry.
      putValue(NAME, Util.nCase(type_));
      type = type_;
    }

    public NewEntryAction(String type_, KeyStroke key) {
        // This action leads to the creation of a specific entry.
        putValue(NAME, Util.nCase(type_));
        putValue(ACCELERATOR_KEY, key);
        type = type_;
    }

    public void actionPerformed(ActionEvent e) {
      String thisType = type;
      if (thisType == null) {
        EntryTypeDialog etd = new EntryTypeDialog(JabRefFrame.this);
        Util.placeDialog(etd, JabRefFrame.this);
        etd.setVisible(true);
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

  /**
   * Refresh import menus.
   */
  public void setUpImportMenus() {
    setUpImportMenu(importMenu, false);
    setUpImportMenu(importNewMenu, true);
  }

  private void fillMenu() {
      //mb.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
      mb.setBorder(null);
      JMenu file = subMenu("File"),
              sessions = subMenu("Sessions"),
              edit = subMenu("Edit"),
              bibtex = subMenu("BibTeX"),
              view = subMenu("View"),
              tools = subMenu("Tools"),
              web = subMenu("Web search"),
              options = subMenu("Options"),
              newSpec = subMenu("New entry..."),
              helpMenu = subMenu("Help");

      setUpImportMenus();

      newDatabaseMenu.add(newDatabaseAction);
      newDatabaseMenu.add(newSubDatabaseAction);

      file.add(newDatabaseAction);
      file.add(open); //opendatabaseaction
      file.add(mergeDatabaseAction);
      file.add(save);
      file.add(saveAs);
      file.add(saveAll);
      file.add(saveSelectedAs);
      file.addSeparator();
      //file.add(importMenu);
      //file.add(importNewMenu);
      file.add(importNew);
      file.add(importCurrent);
      file.add(exportAll);
      file.add(exportSelected);
      file.add(dbConnect);
      file.add(dbImport);
      file.add(dbExport);

      file.addSeparator();
      file.add(databaseProperties);
      file.addSeparator();

      sessions.add(loadSessionAction);
      sessions.add(saveSessionAction);
      file.add(sessions);
      file.add(fileHistory);
      //file.addSeparator();

      file.addSeparator();
      file.add(close);
      file.add(quit);
      mb.add(file);
      //edit.add(test);
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
      //edit.add(exportToClipboard);
      edit.addSeparator();
      edit.add(mark);
      edit.add(unmark);
      edit.add(unmarkAll);
      edit.addSeparator();
      edit.add(selectAll);
      mb.add(edit);
      view.add(back);
      view.add(forward);
      view.add(focusTable);
      view.add(nextTab);
      view.add(prevTab);
      view.add(sortTabs);
      view.addSeparator();
      view.add(increaseFontSize);
      view.add(decreseFontSize);
      view.addSeparator();
      view.add(toggleGroups);
      view.add(togglePreview);
      view.add(switchPreview);
      view.addSeparator();
      view.add(toggleHighlightAny);
      view.add(toggleHighlightAll);
      mb.add(view);

      bibtex.add(newEntryAction);
      for (int i = 0; i < newSpecificEntryAction.length; i++) {
          newSpec.add(newSpecificEntryAction[i]);
      }
      bibtex.add(newSpec);
      bibtex.add(plainTextImport);
      bibtex.addSeparator();
      bibtex.add(editEntry);
      bibtex.add(importCiteSeer);
      bibtex.add(editPreamble);
      bibtex.add(editStrings);
      mb.add(bibtex);

      tools.add(normalSearch);
      tools.add(incrementalSearch);
      tools.add(replaceAll);
      tools.add(new MassSetFieldAction(this));
      tools.add(makeKeyAction);
      //tools.add(downloadFullText);
      // [kiar] I think we should group these festures
      tools.add(checkAndFix);
      checkAndFix.add(dupliCheck);
      //checkAndFix.add(strictDupliCheck);
      checkAndFix.add(autoSetFile);
      checkAndFix.add(autoSetPdf);
      checkAndFix.add(autoSetPs);
      checkAndFix.add(integrityCheckAction);
      checkAndFix.addSeparator();
      checkAndFix.add(upgradeExternalLinks);

      tools.addSeparator();
      tools.add(manageSelectors);

      tools.add(pushExternalButton.getMenuAction());
      tools.add(writeXmpAction);

      tools.addSeparator();
      tools.add(openFile);
      tools.add(openPdf);
      tools.add(openUrl);
      //tools.add(openSpires);
      tools.addSeparator();
      tools.add(newSubDatabaseAction);

      tools.addSeparator();
      tools.add(abbreviateIso);
      tools.add(abbreviateMedline);
      tools.add(unabbreviate);

      // TODO: Temporary for 2.2 release: we should perhaps find a better solution:
      tools.addSeparator();
      tools.add(new ExpandEndnoteFilters(JabRefFrame.this));
      
      mb.add(tools);

      web.add(fetchCiteSeer);
      
      /*
       * Add all entryFetchers
       */
      for (EntryFetcher fetcher : fetchers){
    	  GeneralFetcher generalFetcher = new GeneralFetcher(sidePaneManager, this, fetcher);
          generalFetcher.setHelpResourceOwner(fetcher.getClass());
    	  web.add(generalFetcher.getAction());
    	  fetcherActions.add(generalFetcher.getAction());
      }

      mb.add(web);

      options.add(showPrefs);
      AbstractAction customizeAction = new CustomizeEntryTypeAction();
      AbstractAction genFieldsCustomization = new GenFieldsCustomizationAction();
      options.add(customizeAction);
      options.add(genFieldsCustomization);
      options.add(customExpAction);
      options.add(customImpAction);
      options.add(customFileTypesAction);
      options.add(manageJournals);

      /*options.add(new AbstractAction("Font") {
      public void actionPerformed(ActionEvent e) {
          // JDialog dl = new EntryCustomizationDialog(JabRefFrame.this);
          Font f=new FontSelectorDialog
        (JabRefFrame.this, GUIGlobals.CURRENTFONT).getSelectedFont();
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

      pluginMenu.add(installPlugin);

      //pluginMenu.setEnabled(false);
      mb.add(pluginMenu);


      //options.add(selectKeys);
      mb.add(options);

      helpMenu.add(help);
      helpMenu.add(contents);
      helpMenu.addSeparator();
//old about    helpMenu.add(about);
      helpMenu.add(about);
      mb.add(helpMenu);
      helpMenu.addSeparator();
      helpMenu.add(errorConsole);
  }

    private JMenu subMenu(String name) {
        name = Globals.menuTitle(name);
        int i = name.indexOf('&');
        JMenu res;
        if (i >= 0) {
            res = new JMenu(name.substring(0, i)+name.substring(i+1));
            char mnemonic = Character.toUpperCase(name.charAt(i+1));
            res.setMnemonic((int)mnemonic);
        }
        else res = new JMenu(name);

        return res;
    }


    public void addPluginMenuItem(JMenuItem item) {
        if (!addedToPluginMenu) {
            pluginMenu.addSeparator();
            addedToPluginMenu = true;
        }
        pluginMenu.add(item);
    }

  private void createToolBar() {
    tlb.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
    tlb.setBorder(null);
    tlb.setRollover(true);

    //tlb.setBorderPainted(true);
    //tlb.setBackground(GUIGlobals.lightGray);
    //tlb.setForeground(GUIGlobals.lightGray);
    tlb.setFloatable(false);
    tlb.addAction(newDatabaseAction);
    tlb.addAction(open);
    tlb.addAction(save);
    tlb.addAction(saveAll);
    //tlb.addAction(dbConnect);
    //tlb.addAction(dbExport);
    
    tlb.addSeparator();
    tlb.addAction(cut);
    tlb.addAction(copy);
    tlb.addAction(paste);
    tlb.addAction(undo);
    tlb.addAction(redo);

    tlb.addSeparator();
    tlb.addAction(back);
    tlb.addAction(forward);
    tlb.addSeparator();
    tlb.addAction(newEntryAction);
    tlb.addAction(editEntry);
    tlb.addAction(editPreamble);
    tlb.addAction(editStrings);
    tlb.addAction(makeKeyAction);


    tlb.addSeparator();
    tlb.addAction(mark);
    tlb.addAction(unmark);

    tlb.addSeparator();
    searchToggle = new JToggleButton(toggleSearch);
    searchToggle.setText(null);
    if (!Globals.ON_MAC)
        searchToggle.setMargin(marg);
    tlb.add(searchToggle);

    previewToggle = new JToggleButton(togglePreview);
    previewToggle.setText(null);
    if (!Globals.ON_MAC)
        previewToggle.setMargin(marg);
    tlb.add(previewToggle);
    tlb.addSeparator();

    groupToggle = new JToggleButton(toggleGroups);
    groupToggle.setText(null);
    if (!Globals.ON_MAC)
        groupToggle.setMargin(marg);
    tlb.add(groupToggle);


    highlightAny = new JToggleButton(toggleHighlightAny);
    highlightAny.setText(null);
    if (!Globals.ON_MAC)
        highlightAny.setMargin(marg);
    tlb.add(highlightAny);
    highlightAll = new JToggleButton(toggleHighlightAll);
    highlightAll.setText(null);
    if (!Globals.ON_MAC)
        highlightAll.setMargin(marg);
    tlb.add(highlightAll);

    tlb.addSeparator();

      // Removing the separate push-to buttons, replacing them by the
      // multipurpose button:
      //tlb.addAction(emacsPushAction);
      //tlb.addAction(lyxPushAction);
      //tlb.addAction(winEdtPushAction);
      tlb.add(pushExternalButton.getComponent());

      tlb.addAction(openFile);
    //tlb.addAction(openPdf);
    //tlb.addAction(openUrl);


    //tlb.addSeparator();
    //tlb.addAction(showPrefs);
    tlb.add(Box.createHorizontalGlue());
    //tlb.add(new JabRefLabel(GUIGlobals.frameTitle+" "+GUIGlobals.version));

    tlb.addAction(closeDatabaseAction);
    //Insets margin = new Insets(0, 0, 0, 0);
    //for (int i=0; i<tlb.getComponentCount(); i++)
    //  ((JButton)tlb.getComponentAtIndex(i)).setMargin(margin);

  }

  

 public void output(final String s) {

      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              statusLine.setText(s);
              statusLine.repaint();
          }
      });
  }

  public void stopShowingSearchResults() {
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      baseAt(i).stopShowingSearchResults();
    }
  }

  protected List<Object> openDatabaseOnlyActions = new LinkedList<Object>();
  protected List<Object> severalDatabasesOnlyActions = new LinkedList<Object>();
  
    protected void initActions() {
        openDatabaseOnlyActions = new LinkedList<Object>();
        openDatabaseOnlyActions.addAll(Arrays.asList(new Object[] { manageSelectors,
            mergeDatabaseAction, newSubDatabaseAction, close, save, saveAs, saveSelectedAs, undo,
            redo, cut, delete, copy, paste, mark, unmark, unmarkAll, editEntry, importCiteSeer,
            selectAll, copyKey, copyCiteKey, editPreamble, editStrings, toggleGroups, toggleSearch,
            makeKeyAction, normalSearch,
            incrementalSearch, replaceAll, importMenu, exportMenu, fetchCiteSeer,
                openPdf, openUrl, openFile, openSpires, togglePreview, dupliCheck, /*strictDupliCheck,*/ highlightAll,
            highlightAny, newEntryAction, plainTextImport,
            closeDatabaseAction, switchPreview, integrityCheckAction, autoSetPdf, autoSetPs,
            toggleHighlightAny, toggleHighlightAll, databaseProperties, abbreviateIso,
            abbreviateMedline, unabbreviate, exportAll, exportSelected,
            importCurrent, saveAll, dbConnect, dbExport, focusTable}));
        
        openDatabaseOnlyActions.addAll(fetcherActions);

        openDatabaseOnlyActions.addAll(Arrays.asList(newSpecificEntryAction));

        severalDatabasesOnlyActions = new LinkedList<Object>();
        severalDatabasesOnlyActions.addAll(Arrays
            .asList(new Action[] { nextTab, prevTab, sortTabs }));

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                updateEnabledState();
            }
        });
        
        

    }

    /**
     * Takes a list of Object and calls the method setEnabled on them, depending on whether it is an Action or a Component.
     * @param list List that should contain Actions and Components.
     * @param enabled 
     */
    public static void setEnabled(List<Object> list, boolean enabled) {
        for (Object o : list){
            if (o instanceof Action)
                ((Action)o).setEnabled(enabled);
            if (o instanceof Component)
                ((Component)o).setEnabled(enabled);
        }
    }

    protected int previousTabCount = -1;
    
    /**
     * Enable or Disable all actions based on the number of open tabs.
     * 
     * The action that are affected are set in initActions.
     */
    protected void updateEnabledState() {
        int tabCount = tabbedPane.getTabCount();
        if (tabCount != previousTabCount){
            previousTabCount = tabCount;
            setEnabled(openDatabaseOnlyActions, tabCount > 0);
            setEnabled(severalDatabasesOnlyActions, tabCount > 1);
        }
        if (tabCount == 0) {
            back.setEnabled(false);
            forward.setEnabled(false);
        }
    }

  /**
   * This method causes all open BasePanels to set up their tables
   * anew. When called from PrefsDialog3, this updates to the new
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

      // Update tables:
      if (bf.database != null) {
        bf.setupMainPanel();

      }

    }
  }

  public BasePanel addTab(BibtexDatabase db, File file, HashMap<String, String> meta, String encoding, boolean raisePanel) {
      BasePanel bp = new BasePanel(JabRefFrame.this, db, file, meta, encoding);
      addTab(bp, file, raisePanel);
      return bp;
  }

    public BasePanel addTab(BibtexDatabase db, File file, MetaData meta, String encoding, boolean raisePanel) {
        BasePanel bp = new BasePanel(JabRefFrame.this, db, file, meta, encoding);
        addTab(bp, file, raisePanel);
        return bp;
    }


    public void addTab(BasePanel bp, File file, boolean raisePanel) {
        tabbedPane.add((file != null ? file.getName(): Globals.lang(GUIGlobals.untitledTitle)),
                       bp);
        tabbedPane.setToolTipTextAt(tabbedPane.getTabCount()-1,
                file != null ? file.getAbsolutePath() : null);
        if (raisePanel) {
            tabbedPane.setSelectedComponent(bp);
        }
    }

    /**
     * Signal closing of the current tab. Standard warnings will be given if the
     * database has been changed.
     */
    public void closeCurrentTab() {
        closeDatabaseAction.actionPerformed(null);
    }

    /**
     * Close the current tab without giving any warning if the database has been changed.
     */
    public void closeCurrentTabNoWarning() {
        closeDatabaseAction.close();
    }

  class SelectKeysAction
      extends AbstractAction {
    public SelectKeysAction() {
      super(Globals.lang("Customize key bindings"));
    }

    public void actionPerformed(ActionEvent e) {
      KeyBindingsDialog d = new KeyBindingsDialog
          ( new HashMap<String, String>(prefs.getKeyBindings()),
           prefs.getDefaultKeys());
      d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      d.pack(); //setSize(300,500);
      Util.placeDialog(d, JabRefFrame.this);
      d.setVisible(true);
      if (d.getAction()) {
        prefs.setNewKeyBindings(d.getNewKeyBindings());
        JOptionPane.showMessageDialog
            (JabRefFrame.this,
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
      extends MnemonicAwareAction {
    public CloseAction() {
      putValue(NAME, "Quit");
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

    class CloseDatabaseAction extends MnemonicAwareAction {
        public CloseDatabaseAction() {
            super(GUIGlobals.getImage("close"));
            putValue(NAME, "Close database");
            putValue(SHORT_DESCRIPTION, Globals.lang("Close the current database"));
            putValue(ACCELERATOR_KEY, prefs.getKey("Close database"));
        }

        public void actionPerformed(ActionEvent e) {
            // Ask here if the user really wants to close, if the base
            // has not been saved since last save.
            boolean close = true;
            if (basePanel() == null) { // when it is initially empty
                return; // nbatada nov 7
            }

            if (basePanel().baseChanged) {
                int answer = JOptionPane.showConfirmDialog(JabRefFrame.this, Globals
                    .lang("Database has changed. Do you want to save " + "before closing?"),
                    Globals.lang("Save before closing"), JOptionPane.YES_NO_CANCEL_OPTION);
                if ((answer == JOptionPane.CANCEL_OPTION) || (answer == JOptionPane.CLOSED_OPTION)) {
                    close = false; // The user has cancelled.
                }
                if (answer == JOptionPane.YES_OPTION) {
                    // The user wants to save.
                    try {
                        SaveDatabaseAction saveAction = new SaveDatabaseAction(basePanel());
                        saveAction.runCommand();
                        if (saveAction.isCancelled() || !saveAction.isSuccess())
                            // The action either not cancelled or unsuccessful.
                            // Break! 
                            close = false;
                        
                    } catch (Throwable ex) {
                        // Something prevented the file
                        // from being saved. Break!!!
                        close = false;
                    }

                }
            }

            if (close) {
                close();
            }
        }

        public void close() {
            BasePanel pan = basePanel();
            pan.cleanUp();
            AutoSaveManager.deleteAutoSaveFile(pan); // Delete autosave
            tabbedPane.remove(pan);
            if (tabbedPane.getTabCount() > 0) {
                markActiveBasePanel();
            }
            updateEnabledState(); // Man, this is what I call a bug that this is not called.
            output(Globals.lang("Closed database") + ".");
            System.gc(); // Test
        }
    }


  // The action concerned with opening a new database.
  class NewDatabaseAction
      extends MnemonicAwareAction {
    public NewDatabaseAction() {
        super(GUIGlobals.getImage("new"));
        putValue(NAME, "New database");
        putValue(SHORT_DESCRIPTION, Globals.lang("New BibTeX database"));
        //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);
    }

    public void actionPerformed(ActionEvent e) {
        // Create a new, empty, database.
        BibtexDatabase database = new BibtexDatabase();
        addTab(database, null, (HashMap<String,String>)null, Globals.prefs.get("defaultEncoding"), true);
        output(Globals.lang("New database created."));
    }
  }

class ImportCiteSeerAction
        extends MnemonicAwareAction {

    public ImportCiteSeerAction() {
        super(GUIGlobals.getImage("citeseer"));
        putValue(NAME, "Import Fields from CiteSeer");
        putValue(SHORT_DESCRIPTION, Globals.lang("Import Fields from CiteSeer Database"));
        putValue(ACCELERATOR_KEY, prefs.getKey("Import Fields from CiteSeer")); // Key defined in MenuTitles!
        }

        public void actionPerformed(ActionEvent e) {

                if(citeSeerFetcher.activateImportFetcher()) {


                        (new Thread() {

                                BasePanel currentBp;
                                int[] clickedOn = null;

                                class UpdateComponent implements Runnable {
                                        boolean changesMade;

                                        UpdateComponent(boolean changesMade) {
                                                this.changesMade = changesMade;
                                        }

                                        public void run() {
                                            citeSeerFetcher.endImportCiteSeerProgress();
                                            if (changesMade)
                                                    currentBp.markBaseChanged();
                                                //for(int i=0; i < clickedOn.length; i++)
                                                //        currentBp.entryTable.addRowSelectionInterval(i,i);
                                                //currentBp.showEntry(toShow);
                                                output(Globals.lang("Completed Import Fields from CiteSeer."));
                                        }
                                }

                            public void run() {
                                currentBp = (BasePanel) tabbedPane.getSelectedComponent();
                                        // We demand that at least one row is selected.

                                        int rowCount = currentBp.mainTable.getSelectedRowCount();
                                        if (rowCount >= 1) {
                                                clickedOn = currentBp.mainTable.getSelectedRows();
                                        } else {
                                                JOptionPane.showMessageDialog(currentBp.frame(),
                                                Globals.lang("You must select at least one row to perform this operation."),
                                                Globals.lang("CiteSeer Import Error"),
                                                JOptionPane.WARNING_MESSAGE);
                                        }
                                        if (clickedOn != null) {
                                                citeSeerFetcher.beginImportCiteSeerProgress();
                                                NamedCompound citeseerNamedCompound =
                                                        new NamedCompound(Globals.lang("CiteSeer Import Fields"));
                                                boolean newValues = citeSeerFetcher.importCiteSeerEntries(clickedOn, citeseerNamedCompound);
                                                if (newValues) {
                                                        citeseerNamedCompound.end();
                                                        currentBp.undoManager.addEdit(citeseerNamedCompound);
                                                }
                                                UpdateComponent updateComponent = new UpdateComponent(newValues);
                                                SwingUtilities.invokeLater(updateComponent);
                                        }
                                        citeSeerFetcher.deactivateImportFetcher();
                            }
                        }).start();
                } else {
                        JOptionPane.showMessageDialog(tabbedPane.getSelectedComponent(),
                                        Globals.lang("A CiteSeer import operation is currently in progress.") + "  " +
                                        Globals.lang("Please wait until it has finished."),
                                        Globals.lang("CiteSeer Import Error"),
                                        JOptionPane.WARNING_MESSAGE);
                }
        }
}

class FetchCiteSeerAction
        extends MnemonicAwareAction {

                public FetchCiteSeerAction() {
                    super(GUIGlobals.getImage("citeseer"));
                    putValue(NAME, "Fetch citations from CiteSeer");

                    putValue(SHORT_DESCRIPTION, Globals.lang("Fetch Articles Citing your Database"));
                    putValue(ACCELERATOR_KEY, prefs.getKey("Fetch citations from CiteSeer"));
                }

                public void actionPerformed(ActionEvent e) {

                        if(citeSeerFetcher.activateCitationFetcher()) {
                                sidePaneManager.show("CiteSeerProgress");
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
                                                        newBp.sortingByCiteSeerResults = true;
                                                }

                                                public void run() {
                                                        setSortingByCitationCount();
                                                        tabbedPane.add(Globals.lang(GUIGlobals.untitledTitle), newBp);
                                                        tabbedPane.setSelectedComponent(newBp);
                                                        output(Globals.lang("Fetched all citations from target database."));
                                                        citeSeerFetcher.deactivateCitationFetcher();
                                                }
                                        };

                                  public void run() {
                                        try {
                                                newBp = new BasePanel(JabRefFrame.this);
                                                int errorCode;
                                                targetBp = (BasePanel) tabbedPane.getSelectedComponent();
                                                newDatabase = newBp.getDatabase();
                                                targetDatabase = targetBp.getDatabase();
                                                errorCode = citeSeerFetcher.populate(newDatabase, targetDatabase);
                                                if (newDatabase.getEntryCount() > 0) {
                                                        SwingUtilities.invokeLater(updateComponent);
                                                } else if(errorCode == 0) {
                                                        SwingUtilities.invokeLater(citeSeerFetcher.getEmptyFetchSetDialog());
                                            } else {
                                                    citeSeerFetcher.deactivateCitationFetcher();
                                            }
                                        }
                                        catch (Exception ex) {
                                          ex.printStackTrace();
                                        }
                                  }
                                }).start();
                        } else {
                            JOptionPane.showMessageDialog(tabbedPane.getSelectedComponent(),
                                                Globals.lang("A CiteSeer fetch operation is currently in progress.") + "  " +
                                                Globals.lang("Please wait until it has finished."),
                                                Globals.lang("CiteSeer Fetch Error"),
                                                JOptionPane.WARNING_MESSAGE);
                        }
                }
        }



    // The action concerned with generate a new (sub-)database from latex aux file.
    class NewSubDatabaseAction extends MnemonicAwareAction
    {
      public NewSubDatabaseAction()
      {
        super(GUIGlobals.getImage("new"));
        putValue(NAME, "New subdatabase based on AUX file" );
        putValue( SHORT_DESCRIPTION, Globals.lang( "New BibTeX subdatabase" ) ) ;
            //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);
      }

      public void actionPerformed( ActionEvent e )
      {
        // Create a new, empty, database.

        FromAuxDialog dialog = new FromAuxDialog(JabRefFrame.this, "", true, JabRefFrame.this.tabbedPane) ;

        Util.placeDialog(dialog, JabRefFrame.this);
        dialog.setVisible(true) ;

        if (dialog.okPressed())
        {
          BasePanel bp = new BasePanel( JabRefFrame.this,
                                        dialog.getGenerateDB(),   // database
                                        null,                     // file
                                        (HashMap<String,String>)null, Globals.prefs.get("defaultEncoding"));                     // meta data
          tabbedPane.add( Globals.lang( GUIGlobals.untitledTitle ), bp ) ;
          tabbedPane.setSelectedComponent( bp ) ;
          output( Globals.lang( "New database created." ) ) ;
        }
      }
    }


    // The action should test the database and report errors/warnings
    class IntegrityCheckAction extends AbstractAction
    {
      public IntegrityCheckAction()
      {
        super(Globals.menuTitle("Integrity check"),
               GUIGlobals.getImage("integrityCheck")) ;
               //putValue( SHORT_DESCRIPTION, "integrity" ) ;  //Globals.lang( "integrity" ) ) ;
            //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);
      }

      public void actionPerformed( ActionEvent e )
      {
       Object selComp = tabbedPane.getSelectedComponent() ;
       if (selComp != null)
       {
         BasePanel bp = ( BasePanel ) selComp ;
         BibtexDatabase refBase = bp.getDatabase() ;
         if (refBase != null)
         {
             IntegrityWizard wizard = new IntegrityWizard(JabRefFrame.this, basePanel()) ;
             Util.placeDialog(wizard, JabRefFrame.this);
             wizard.setVisible(true) ;

         }
       }
      }
    }

  // The action for opening the preferences dialog.
  AbstractAction showPrefs = new ShowPrefsAction();

  class ShowPrefsAction
      extends MnemonicAwareAction {
    public ShowPrefsAction() {
      super(GUIGlobals.getImage("preferences"));
      putValue(NAME, "Preferences");
      putValue(SHORT_DESCRIPTION, Globals.lang("Preferences"));
    }

    public void actionPerformed(ActionEvent e) {
      preferences();
    }
  }

  /**
     * This method does the job of adding imported entries into the active
     * database, or into a new one. It shows the ImportInspectionDialog if
     * preferences indicate it should be used. Otherwise it imports directly.
     * 
     * @param panel
     *            The BasePanel to add to.
     * @param entries
     *            The entries to add.
     * @param filename
     *            Name of the file where the import came from.
     * @param openInNew
     *            Should the entries be imported into a new database?
     */
    public void addImportedEntries(final BasePanel panel, final List<BibtexEntry> entries,
        String filename, final boolean openInNew) {
        /*
         * Use the import inspection dialog if it is enabled in preferences, and
         * (there are more than one entry or the inspection dialog is also
         * enabled for single entries):
         */
        if (Globals.prefs.getBoolean("useImportInspectionDialog") &&
            (Globals.prefs.getBoolean("useImportInspectionDialogForSingle") || (entries.size() > 1))) {

            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    ImportInspectionDialog diag = new ImportInspectionDialog(JabRefFrame.this,
                        panel, BibtexFields.DEFAULT_INSPECTION_FIELDS, Globals.lang("Import"),
                        openInNew);
                    diag.addEntries(entries);
                    diag.entryListComplete();
                    Util.placeDialog(diag, JabRefFrame.this);
                    diag.setVisible(true);
                    diag.toFront();
                }
            });

        } else {
            JabRefFrame.this.addBibEntries(entries, filename, openInNew);
            if ((panel != null) && (entries.size() == 1)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        panel.highlightEntry(entries.get(0));
                    }
                });
            }
        }
    }

    /**
     * Adds the entries to the database, possibly checking for duplicates first.
     * @param filename If non-null, a message is printed to the status line describing
     * how many entries were imported, and from which file. If null, the message will not
     * be printed.
     * @param intoNew Determines if the entries will be put in a new database or in the current
     * one.
     */
  public int addBibEntries(List<BibtexEntry> bibentries, String filename,
                           boolean intoNew) {
          if (bibentries == null || bibentries.size() == 0) {

      // No entries found. We need a message for this.
      JOptionPane.showMessageDialog(JabRefFrame.this, Globals.lang("No entries found. Please make sure you are "
                                                      +"using the correct import filter."), Globals.lang("Import failed"),
                                    JOptionPane.ERROR_MESSAGE);
      return 0;
    }

      int addedEntries = 0;

    // Set owner and timestamp fields:
    Util.setAutomaticFields(bibentries, Globals.prefs.getBoolean("overwriteOwner"),
            Globals.prefs.getBoolean("overwriteTimeStamp"), Globals.prefs.getBoolean("markImportedEntries"));

    if (intoNew || (tabbedPane.getTabCount() == 0)) {
      // Import into new database.
      BibtexDatabase database = new BibtexDatabase();
      for (BibtexEntry entry : bibentries){
        try {
          entry.setId(Util.createNeutralId());
          database.insertEntry(entry);
        }
        catch (KeyCollisionException ex) {
          //ignore
          System.err.println("KeyCollisionException [ addBibEntries(...) ]");
        }
      }
      HashMap<String, String> meta = new HashMap<String, String>();
      // Metadata are only put in bibtex files, so we will not find it
      // in imported files. Instead we pass an empty HashMap.
      BasePanel bp = new BasePanel(JabRefFrame.this, database, null, meta, Globals.prefs.get("defaultEncoding"));
      /*
            if (prefs.getBoolean("autoComplete")) {
            db.setCompleters(autoCompleters);
            }
       */
      addedEntries = database.getEntryCount();
      tabbedPane.add(GUIGlobals.untitledTitle, bp);
      bp.markBaseChanged();
      tabbedPane.setSelectedComponent(bp);
      if (filename != null)
          output(Globals.lang("Imported database") + " '" + filename + "' " +
                 Globals.lang("with") + " " +
                 database.getEntryCount() + " " +
                 Globals.lang("entries into new database") + ".");
    }
    else {
      // Import into current database.
      boolean checkForDuplicates = true;
      BasePanel basePanel = basePanel();
      BibtexDatabase database = basePanel.database;
      int oldCount = database.getEntryCount();
      NamedCompound ce = new NamedCompound(Globals.lang("Import entries"));

      mainLoop: 
      for (BibtexEntry entry : bibentries){
        boolean dupli = false;
        // Check for duplicates among the current entries:
        if (checkForDuplicates) {
            loop: for (Iterator<String> i2=database.getKeySet().iterator();
                       i2.hasNext();) {
                BibtexEntry existingEntry = database.getEntryById(i2.next());
                if (DuplicateCheck.isDuplicate(entry, existingEntry
                )) {
                    DuplicateResolverDialog drd = new DuplicateResolverDialog
                        (JabRefFrame.this, existingEntry, entry, DuplicateResolverDialog.IMPORT_CHECK);
                    drd.setVisible(true);
                    int res = drd.getSelected();
                    if (res == DuplicateResolverDialog.KEEP_LOWER)   {
                        dupli = true;
                    }
                    else if (res == DuplicateResolverDialog.KEEP_UPPER) {
                        database.removeEntry(existingEntry.getId());
                        ce.addEdit(new UndoableRemoveEntry
                                   (database, existingEntry, basePanel));
                    } else if (res == DuplicateResolverDialog.BREAK) {
                        break mainLoop;
                    }
                    break loop;
                }
            }
        }

        if (!dupli) {
            try {
                entry.setId(Util.createNeutralId());
                database.insertEntry(entry);
                ce.addEdit(new UndoableInsertEntry
                           (database, entry, basePanel));
                addedEntries++;
            }
            catch (KeyCollisionException ex) {
                //ignore
                System.err.println("KeyCollisionException [ addBibEntries(...) ]");
            }
        }
      }
        if (addedEntries > 0) {
            ce.end();
            basePanel.undoManager.addEdit(ce);
            basePanel.markBaseChanged();
            if (filename != null)
                output(Globals.lang("Imported database") + " '" + filename + "' " +
                     Globals.lang("with") + " " +
                     (database.getEntryCount() - oldCount) + " " +
                     Globals.lang("entries into new database") + ".");
        }

    }

    return addedEntries;
  }

  private void setUpImportMenu(JMenu importMenu, boolean intoNew_) {
      final boolean intoNew = intoNew_;
      importMenu.removeAll();

      // Add a menu item for autodetecting import format:
      importMenu.add(new ImportMenuItem(JabRefFrame.this, intoNew));

      // Add custom importers
      importMenu.addSeparator();

      SortedSet<ImportFormat> customImporters = Globals.importFormatReader.getCustomImportFormats();
      JMenu submenu = new JMenu(Globals.lang("Custom importers"));
      submenu.setMnemonic(KeyEvent.VK_S);
      
      // Put in all formatters registered in ImportFormatReader:
        for (ImportFormat imFo : customImporters){
            submenu.add(new ImportMenuItem(JabRefFrame.this, intoNew, imFo));
        }
      
      if (customImporters.size() > 0)
          submenu.addSeparator();
      
      submenu.add(customImpAction);

      importMenu.add(submenu);
      importMenu.addSeparator();

      // Put in all formatters registered in ImportFormatReader:
      for (ImportFormat imFo : Globals.importFormatReader.getBuiltInInputFormats()){
          importMenu.add(new ImportMenuItem(JabRefFrame.this, intoNew, imFo));
      }
  }


    public FileHistory getFileHistory() {
        return fileHistory;
    }


    /**
     * Set the preview active state for all BasePanel instances.
     * @param enabled
     */
    public void setPreviewActive(boolean enabled) {
        for (int i=0; i<tabbedPane.getTabCount(); i++) {
            baseAt(i).setPreviewActive(enabled);
        }
    }


   public void removeCachedEntryEditors() {
       for (int j=0; j<tabbedPane.getTabCount(); j++) {
            BasePanel bp = (BasePanel)tabbedPane.getComponentAt(j);
            bp.entryEditors.clear();
       }
   }

    /**
     * This method shows a wait cursor and blocks all input to the JFrame's contents.
     */
    public void block() {
        getGlassPane().setVisible(true);
    }

    /**
     * This method reverts the cursor to normal, and stops blocking input to the JFrame's contents.
     * There are no adverse effects of calling this method redundantly.
     */
    public void unblock() {
        getGlassPane().setVisible(false);
    }

    /** Set the visibility of the progress bar in the right end of the
      * status line at the bottom of the frame.
      *
      * If not called on the event dispatch thread, this method uses
      * SwingUtilities.invokeLater() to do the actual operation on the EDT.
      */
    public void setProgressBarVisible(final boolean visible) {
    if (SwingUtilities.isEventDispatchThread())
        progressBar.setVisible(visible);
    else SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            progressBar.setVisible(visible);
        }
        });
    }


    /**
     * Sets the current value of the progress bar.
      *
      * If not called on the event dispatch thread, this method uses
      * SwingUtilities.invokeLater() to do the actual operation on the EDT.
     */
    public void setProgressBarValue(final int value) {
    if (SwingUtilities.isEventDispatchThread())
        progressBar.setValue(value);
    else SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            progressBar.setValue(value);
        }
        });

    }


    /**
     * Sets the indeterminate status of the progress bar.
     *
     * If not called on the event dispatch thread, this method uses
     * SwingUtilities.invokeLater() to do the actual operation on the EDT.
     */
    public void setProgressBarIndeterminate(final boolean value) {
        if (SwingUtilities.isEventDispatchThread())
            progressBar.setIndeterminate(value);
        else SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(value);
            }
        });

    }

    /**
     * Sets the maximum value of the progress bar. Always call this method
     * before using the progress bar, to set a maximum value appropriate to
     * the task at hand.
      *
      * If not called on the event dispatch thread, this method uses
      * SwingUtilities.invokeLater() to do the actual operation on the EDT.
     */
    public void setProgressBarMaximum(final int value) {
    if (SwingUtilities.isEventDispatchThread())
        progressBar.setMaximum(value);
    else SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            progressBar.setMaximum(value);
        }
        });


    }

class SaveSessionAction
      extends MnemonicAwareAction {
    public SaveSessionAction() {
      super(GUIGlobals.getImage("save"));
      putValue(NAME, "Save session");
      putValue(ACCELERATOR_KEY, prefs.getKey("Save session"));
    }

    public void actionPerformed(ActionEvent e) {
      // Here we store the names of allcurrent filea. If
      // there is no current file, we remove any
      // previously stored file name.
      Vector<String> filenames = new Vector<String>();
      if (tabbedPane.getTabCount() > 0) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
          if (tabbedPane.getTitleAt(i).equals(GUIGlobals.untitledTitle)) {
            tabbedPane.setSelectedIndex(i);
            int answer = JOptionPane.showConfirmDialog
                (JabRefFrame.this, Globals.lang
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
          if (baseAt(i).getFile() != null) {
            filenames.add(baseAt(i).getFile().getPath());
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
          names[i] = filenames.elementAt(i);
        }
        prefs.putStringArray("savedSession", names);
        output(Globals.lang("Saved session") + ".");
      }

    }
  }

  class LoadSessionAction
      extends MnemonicAwareAction {
      boolean running = false;
    public LoadSessionAction() {
      super(GUIGlobals.getImage("loadSession"));
      putValue(NAME, "Load session");
      putValue(ACCELERATOR_KEY, prefs.getKey("Load session"));
    }

    public void actionPerformed(ActionEvent e) {
      if (prefs.get("savedSession") == null) {
        output(Globals.lang("No saved session found."));
        return;
      }
      if (running)
          return;
      else running = true;
      output(Globals.lang("Loading session..."));
      (new Thread() {
        public void run() {
          HashSet<String> currentFiles = new HashSet<String>();
          if (tabbedPane.getTabCount() > 0) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (baseAt(i).getFile() != null)
                    currentFiles.add(baseAt(i).getFile().getPath());
            }
          }
          int i0 = tabbedPane.getTabCount();
          String[] names = prefs.getStringArray("savedSession");
          for (int i = 0; i < names.length; i++) {
            if (!currentFiles.contains(names[i])) {
              File file = new File(names[i]);
              if (file.exists()) {
                //Util.pr("Opening last edited file:"
                //+fileToOpen.getName());
                open.openIt(file, i == 0);
              }
            }
          }
          output(Globals.lang("Files opened") + ": " +
                 (tabbedPane.getTabCount() - i0));
          running = false;
        }
      }).start();

    }
  }

  class ChangeTabAction
      extends MnemonicAwareAction {
    private boolean next;
    public ChangeTabAction(boolean next) {
      putValue(NAME, next ? "Next tab" : "Previous tab");
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
      extends MnemonicAwareAction {
    private String command;
    public EditAction(String command, URL icon) {
      super(new ImageIcon(icon));
      this.command = command;
      String nName = Util.nCase(command);
      putValue(NAME, nName);
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

  class CustomizeExportsAction extends MnemonicAwareAction {
    public CustomizeExportsAction() {
      putValue(NAME, "Manage custom exports");
    }

    public void actionPerformed(ActionEvent e) {
      ExportCustomizationDialog ecd = new ExportCustomizationDialog(JabRefFrame.this);
      ecd.setVisible(true);
    }
  }

  class CustomizeImportsAction extends MnemonicAwareAction {
    public CustomizeImportsAction() {
      putValue(NAME, "Manage custom imports");
    }

    public void actionPerformed(ActionEvent e) {
      ImportCustomizationDialog ecd = new ImportCustomizationDialog(JabRefFrame.this);
      ecd.setVisible(true);
    }
  }

 
    class CustomizeEntryTypeAction extends MnemonicAwareAction {
        public CustomizeEntryTypeAction() {
            putValue(NAME, "Customize entry types");
        }
        public void actionPerformed(ActionEvent e) {
            JDialog dl = new EntryCustomizationDialog2(JabRefFrame.this);
            Util.placeDialog(dl, JabRefFrame.this);
            dl.setVisible(true);
        }
    }

    class GenFieldsCustomizationAction extends MnemonicAwareAction {
        public GenFieldsCustomizationAction() {
            putValue(NAME, "Set up general fields");
        }
        public void actionPerformed(ActionEvent e) {
            GenFieldsCustomizer gf = new GenFieldsCustomizer(JabRefFrame.this);
            Util.placeDialog(gf, JabRefFrame.this);
            gf.setVisible(true);

        }
    }

    class DatabasePropertiesAction extends MnemonicAwareAction {
        DatabasePropertiesDialog propertiesDialog = null;
        public DatabasePropertiesAction() {
            putValue(NAME, "Database properties");
        }

        public void actionPerformed(ActionEvent e) {
            if (propertiesDialog == null)
                propertiesDialog = new DatabasePropertiesDialog(JabRefFrame.this);
            propertiesDialog.setPanel(basePanel());
            Util.placeDialog(propertiesDialog, JabRefFrame.this);
            propertiesDialog.setVisible(true);
        }
    }

    class IncreaseTableFontSizeAction extends MnemonicAwareAction {
        public IncreaseTableFontSizeAction() {
            putValue(NAME, "Increase table font size");
            putValue(ACCELERATOR_KEY, Globals.prefs.getKey("Increase table font size"));
        }
        public void actionPerformed(ActionEvent event) {
            int currentSize = GUIGlobals.CURRENTFONT.getSize();
            Font newFont = new Font(GUIGlobals.CURRENTFONT.getFamily(), GUIGlobals.CURRENTFONT.getStyle(),
                    currentSize+1);
            GUIGlobals.CURRENTFONT = newFont;
            Globals.prefs.putInt("fontSize", currentSize+1);
            for (int i=0; i<baseCount(); i++) {
                baseAt(i).updateTableFont();
            }
        }
    }

    class DecreaseTableFontSizeAction extends MnemonicAwareAction {
        public DecreaseTableFontSizeAction() {
            putValue(NAME, "Decrease table font size");
            putValue(ACCELERATOR_KEY, Globals.prefs.getKey("Decrease table font size"));
        }
        public void actionPerformed(ActionEvent event) {
            int currentSize = GUIGlobals.CURRENTFONT.getSize();
            if (currentSize < 2 )
                return;
            Font newFont = new Font(GUIGlobals.CURRENTFONT.getFamily(), GUIGlobals.CURRENTFONT.getStyle(),
                    currentSize-1);
            GUIGlobals.CURRENTFONT = newFont;
            Globals.prefs.putInt("fontSize", currentSize-1);
            for (int i=0; i<baseCount(); i++) {
                baseAt(i).updateTableFont();
            }
        }
    }

    /*private class ForegroundLabel extends JLabel {
         public ForegroundLabel(String s) {
             super(s);
             setFont(new Font("plain", Font.BOLD, 70));
             setHorizontalAlignment(JLabel.CENTER);
         }

        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            super.paint(g2);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }       */

  private class MyGlassPane extends JPanel {
    //ForegroundLabel infoLabel = new ForegroundLabel("Showing search");
    public MyGlassPane() {
      addKeyListener(new KeyAdapter() { });
      addMouseListener(new MouseAdapter() { });
      /*  infoLabel.setForeground(new Color(255, 100, 100, 124));

        setLayout(new BorderLayout());
        add(infoLabel, BorderLayout.CENTER);*/
      super.setCursor(
        Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
      // Override isOpaque() to prevent the glasspane from hiding the window contents:
      public boolean isOpaque() { return false; }
  }

  public void showMessage(Object message, String title, int msgType){
      JOptionPane.showMessageDialog(this, message, title, msgType);
  }

  public void setStatus(String s){
	  output(s);
  }

  public void showMessage(String message){
	  JOptionPane.showMessageDialog(this, message);
  }
}
