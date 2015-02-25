/*  Copyright (C) 2003-2012 JabRef contributors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.java.ayatana.ApplicationMenu;

import net.sf.jabref.export.*;
import net.sf.jabref.external.ExternalFileTypeEditor;
import net.sf.jabref.external.PushToApplicationButton;
import net.sf.jabref.groups.EntryTableTransferHandler;
import net.sf.jabref.groups.GroupSelector;
import net.sf.jabref.gui.*;
import net.sf.jabref.help.HelpAction;
import net.sf.jabref.help.HelpDialog;
import net.sf.jabref.imports.EntryFetcher;
import net.sf.jabref.imports.GeneralFetcher;
import net.sf.jabref.imports.ImportCustomizationDialog;
import net.sf.jabref.imports.ImportFormat;
import net.sf.jabref.imports.ImportFormats;
import net.sf.jabref.imports.ImportMenuItem;
import net.sf.jabref.imports.OpenDatabaseAction;
import net.sf.jabref.imports.ParserResult;
import net.sf.jabref.journals.ManageJournalsAction;
import net.sf.jabref.label.ArticleLabelRule;
import net.sf.jabref.label.BookLabelRule;
import net.sf.jabref.label.IncollectionLabelRule;
import net.sf.jabref.label.InproceedingsLabelRule;
import net.sf.jabref.label.LabelMaker;
import net.sf.jabref.oo.OpenOfficePanel;
import net.sf.jabref.plugin.PluginCore;
import net.sf.jabref.plugin.PluginInstallerAction;
import net.sf.jabref.plugin.core.JabRefPlugin;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.EntryFetcherExtension;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;
import net.sf.jabref.specialfields.SpecialFieldsUtils;
import net.sf.jabref.sql.importer.DbImportAction;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.undo.UndoableRemoveEntry;
import net.sf.jabref.util.ManageKeywordsAction;
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

    JTabbedPane tabbedPane; // initialized at constructor
    
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

    JLabel statusLine = new JLabel("", SwingConstants.LEFT), statusLabel = new JLabel(
        Globals.lang("Status")
        + ":", SwingConstants.LEFT);
    JProgressBar progressBar = new JProgressBar();

    private FileHistory fileHistory = new FileHistory(prefs, this);

    private SysTray sysTray = null;

    LabelMaker labelMaker;

    // The help window.
    public HelpDialog helpDiag = new HelpDialog(this);

    // Here we instantiate menu/toolbar actions. Actions regarding
    // the currently open database are defined as a GeneralAction
    // with a unique command string. This causes the appropriate
    // BasePanel's runCommand() method to be called with that command.
    // Note: GeneralAction's constructor automatically gets translations
    // for the name and message strings.

  /* References to the toggle buttons in the toolbar */
  // the groups interface
  public JToggleButton groupToggle;
  public JToggleButton searchToggle, previewToggle, highlightAny, highlightAll;

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
      //cut = new GeneralAction("cut", "Cut", Globals.lang("Cut"),
      //   GUIGlobals.cutIconFile,
      //   prefs.getKey("Cut")),
      delete = new GeneralAction("delete", "Delete", Globals.lang("Delete"),
                                 prefs.getKey("Delete")),
      //copy = new GeneralAction("copy", "Copy", Globals.lang("Copy"),
      //                         GUIGlobals.copyIconFile,
      //                         prefs.getKey("Copy")),
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
       toggleRelevance = new GeneralAction(
    		   Relevance.getInstance().getValues().get(0).getActionName(), 
    		   Relevance.getInstance().getValues().get(0).getMenuString(),
    		   Relevance.getInstance().getValues().get(0).getToolTipText()),
       toggleQualityAssured = new GeneralAction(
				Quality.getInstance().getValues().get(0).getActionName(),
				Quality.getInstance().getValues().get(0).getMenuString(),
				Quality.getInstance().getValues().get(0).getToolTipText()),
		togglePrinted = new GeneralAction(
				Printed.getInstance().getValues().get(0).getActionName(),
				Printed.getInstance().getValues().get(0).getMenuString(),
				Printed.getInstance().getValues().get(0).getToolTipText()),
//    	priority = new GeneralAction("setPriority", "Set priority",
//    			                                            Globals.lang("Set priority")),
      manageSelectors = new GeneralAction("manageSelectors", "Manage content selectors"),
      saveSessionAction = new SaveSessionAction(),
      loadSessionAction = new LoadSessionAction(),
      incrementalSearch = new GeneralAction("incSearch", "Incremental search",
                                            Globals.lang("Start incremental search"),
                                            prefs.getKey("Incremental search")),
      normalSearch = new GeneralAction("search", "Search", Globals.lang("Search"),
                                       prefs.getKey("Search")),
      toggleSearch = new GeneralAction("toggleSearch", "Search", Globals.lang("Toggle search panel")),

      copyKey = new GeneralAction("copyKey", "Copy BibTeX key",
            prefs.getKey("Copy BibTeX key")),
      //"Put a BibTeX reference to the selected entries on the clipboard",
      copyCiteKey = new GeneralAction("copyCiteKey", "Copy \\cite{BibTeX key}",
                                      //"Put a BibTeX reference to the selected entries on the clipboard",
                                      prefs.getKey("Copy \\cite{BibTeX key}")),
      copyKeyAndTitle = new GeneralAction("copyKeyAndTitle", 
    		  							  "Copy BibTeX key and title",
    		  							  prefs.getKey("Copy BibTeX key and title")),
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
                                        
      openFolder = new GeneralAction("openFolder", "Open folder",
                                        Globals.lang("Open folder"),
                                        prefs.getKey("Open folder")),
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
      /*
	   * It looks like this wasn't being implemented for spires anyway so we
	   * comment it out for now.
	   *
	  openInspire = new GeneralAction("openInspire", "Open INSPIRE entry",
                                          Globals.lang("Open INSPIRE entry"),
                                          prefs.getKey("Open INSPIRE entry")),
		*/
      dupliCheck = new GeneralAction("dupliCheck", "Find duplicates"),
      //strictDupliCheck = new GeneralAction("strictDupliCheck", "Find and remove exact duplicates"),
      plainTextImport = new GeneralAction("plainTextImport",
                                          "New entry from plain text",
                                          prefs.getKey("New from plain text")),


      customExpAction = new CustomizeExportsAction(),
      customImpAction = new CustomizeImportsAction(),
      customFileTypesAction = ExternalFileTypeEditor.getAction(this),
      exportToClipboard = new GeneralAction("exportToClipboard", "Export selected entries to clipboard"),
      //expandEndnoteZip = new ExpandEndnoteFilters(this),
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
    bibtexKeyPattern = new BibtexKeyPatternAction(),
    errorConsole = Globals.errorConsole.getAction(this),
    test = new GeneralAction("test", "Test"),

    dbConnect = new GeneralAction("dbConnect", "Connect to external SQL database",
         Globals.lang("Connect to external SQL database"), 
          GUIGlobals.getIconUrl("dbConnect") ),

    dbExport = new GeneralAction("dbExport", "Export to external SQL database",
         Globals.lang("Export to external SQL database"), 
          GUIGlobals.getIconUrl("dbExport") ),

    Cleanup = new GeneralAction("Cleanup", "Cleanup entries", 
					Globals.lang("Cleanup entries"), 
					prefs.getKey("Cleanup"),
					("cleanupentries")),
          
    mergeEntries = new GeneralAction("mergeEntries", "Merge entries", 
					Globals.lang("Merge entries"),
                                        GUIGlobals.getIconUrl("mergeentries")),
					
    dbImport = new DbImportAction(this).getAction(),
    //downloadFullText = new GeneralAction("downloadFullText", "Look up full text document",
    //        Globals.lang("Follow DOI or URL link and try to locate PDF full text document")),
    increaseFontSize = new IncreaseTableFontSizeAction(),
    decreseFontSize = new DecreaseTableFontSizeAction(),
    installPlugin = new PluginInstallerAction(this),
    resolveDuplicateKeys = new GeneralAction("resolveDuplicateKeys", "Resolve duplicate BibTeX keys",
              Globals.lang("Find and remove duplicate BibTeX keys"),
              prefs.getKey("Resolve duplicate BibTeX keys"));

    MassSetFieldAction massSetField = new MassSetFieldAction(this);
    ManageKeywordsAction manageKeywords = new ManageKeywordsAction(this);

	GeneralAction findUnlinkedFiles = new GeneralAction(
  			FindUnlinkedFilesDialog.ACTION_COMMAND,
  			FindUnlinkedFilesDialog.ACTION_TITLE,
  			FindUnlinkedFilesDialog.ACTION_SHORT_DESCRIPTION,
  			FindUnlinkedFilesDialog.ACTION_ICON,
  			prefs.getKey(FindUnlinkedFilesDialog.ACTION_COMMAND)
  	);

	AutoLinkFilesAction autoLinkFile = new AutoLinkFilesAction();

    PushToApplicationButton pushExternalButton;

    List<EntryFetcher> fetchers = new LinkedList<EntryFetcher>();
    List<Action> fetcherActions = new LinkedList<Action>();

    private SearchManager2 searchManager;

	public GroupSelector groupSelector;

  // The menus for importing/appending other formats
  JMenu importMenu = subMenu("Import into current database"),
      importNewMenu = subMenu("Import into new database"),
      exportMenu = subMenu("Export"),
      customExportMenu = subMenu("Custom export"),
      newDatabaseMenu = subMenu("New database" );

  // Other submenus
  JMenu checkAndFix = subMenu("Legacy tools...");


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
	    tabbedPane = new DragDropPopupPane(manageSelectors, databaseProperties, bibtexKeyPattern);

        macOSXRegistration();

        UIManager.put("FileChooser.readOnly", Globals.prefs.getBoolean("filechooserDisableRename"));

        MyGlassPane glassPane = new MyGlassPane();
        setGlassPane(glassPane);
        // glassPane.setVisible(true);

        setTitle(GUIGlobals.frameTitle);
        //setIconImage(GUIGlobals.getImage("jabrefIcon").getImage());
        setIconImage(GUIGlobals.getImage("jabrefIcon48").getImage());
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

      
      setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
      if ( !prefs.getBoolean("windowMaximised") ) {
         
        int sizeX = prefs.getInt("sizeX");
        int sizeY = prefs.getInt("sizeY");
        int posX = prefs.getInt("posX");
        int posY = prefs.getInt("posY");

        /*
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();


        // Get size of each screen
        for (int i=0; i<gs.length; i++) {
            DisplayMode dm = gs[i].getDisplayMode();
            int screenWidth = dm.getWidth();
            int screenHeight = dm.getHeight();
            System.out.println(gs[i].getDefaultConfiguration().getBounds());
        }*/

        //
        // Fix for [ 1738920 ] Windows Position in Multi-Monitor environment
        //
        // Do not put a window outside the screen if the preference values are wrong.
        //
        // Useful reference: http://www.exampledepot.com/egs/java.awt/screen_ScreenSize.html?l=rel
        // googled on forums.java.sun.com graphicsenvironment second screen java
        //
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length == 1){

            Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0]
                    .getDefaultConfiguration().getBounds();
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

            // Make sure we are not above or to the left of the screen bounds:
            if (posX < bounds.x)
                posX = bounds.x;
            if (posY < bounds.y)
                posY = bounds.y;

            int height = (int) dim.getHeight();
            int width = (int) dim.getWidth();

            //if (posX < )

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
                    setWindowTitle();
                    // Update search autocompleter with information for the correct database:
                    bp.updateSearchManager();
                    // Set correct enabled state for Back and Forward actions:
                    bp.setBackAndForwardEnabledState();
                    new FocusRequester(bp.mainTable);
                }
            }
        });

        // The following sets up integration with Unity's global menu, but currently (Nov 18, 2012)
        // this doesn't work with OpenJDK 6 (leads to crash), only with 7.
        String javaVersion = System.getProperty("java.version", null);
        if (javaVersion.compareTo("1.7") >= 0)
            ApplicationMenu.tryInstall(this);

    }

    public void setWindowTitle() {
        // Set window title:
        BasePanel bp = basePanel();
        if (bp == null) {
            setTitle(GUIGlobals.frameTitle);
            return;
        }
        String star = bp.isBaseChanged() ? "*" : "";
        if (bp.getFile() != null) {
            setTitle(GUIGlobals.frameTitle+" - "+bp.getFile().getPath()+star);
        } else {
            setTitle(GUIGlobals.frameTitle+" - "+Globals.lang("untitled")+star);
        }
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
        
        groupSelector = new GroupSelector(this, sidePaneManager);
        searchManager = new SearchManager2(this, sidePaneManager);

        sidePaneManager.register("groups", groupSelector);
        sidePaneManager.register("search", searchManager);

        // Show the search panel if it was visible at last shutdown:
        if (Globals.prefs.getBoolean("searchPanelVisible"))
            sidePaneManager.show("search");
    }

    // The OSXAdapter calls this method when a ".bib" file has been double-clicked from the Finder.
    public void openAction(String filePath) {
    	File file = new File(filePath);
    	
        // Check if the file is already open.
    	for (int i=0; i<this.getTabbedPane().getTabCount(); i++) {
    		BasePanel bp = this.baseAt(i);
    		if ((bp.getFile() != null) && bp.getFile().equals(file)) {
    			//The file is already opened, so just raising its tab.
    			this.getTabbedPane().setSelectedComponent(bp);
    			return;
    		}
    	}
        
    	if (file.exists()) {
    		// Run the actual open in a thread to prevent the program
    		// locking until the file is loaded.
    		final File theFile = new File(filePath);
    		(new Thread() {
    			public void run() {
    				open.openIt(theFile, true);
    			}
    		}).start();
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
            catch (IOException ignored) {}
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
        if (baseAt(i).isBaseChanged()) {
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

      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
          if (baseAt(i).isSaving()) {
              // There is a database still being saved, so we need to wait.
              WaitForSaveOperation w = new WaitForSaveOperation(this);
              w.show(); // This method won't return until cancelled or the save operation is done.
              if (w.cancelled())
                  return; // The user clicked cancel.
          }
      }


      dispose();

      if (basePanel() != null)
        basePanel().saveDividerLocation();
      prefs.putInt("posX", JabRefFrame.this.getLocation().x);
      prefs.putInt("posY", JabRefFrame.this.getLocation().y);
      prefs.putInt("sizeX", JabRefFrame.this.getSize().width);
      prefs.putInt("sizeY", JabRefFrame.this.getSize().height);
      //prefs.putBoolean("windowMaximised", (getExtendedState()&MAXIMIZED_BOTH)>0);
      prefs.putBoolean("windowMaximised", (getExtendedState() == Frame.MAXIMIZED_BOTH));
      
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
		  
		  Class<?>[] defArgs = {Object.class, Method.class};
		  Class<?> thisClass = JabRefFrame.class;
		  Method registerMethod = osxAdapter.getDeclaredMethod("setAboutHandler", defArgs);
		  if (registerMethod != null) {
			  Object[] args = {this, thisClass.getDeclaredMethod("about", (Class[])null)};
			  registerMethod.invoke(osxAdapter, args);
		  }
		  registerMethod = osxAdapter.getDeclaredMethod("setPreferencesHandler", defArgs);
		  if (registerMethod != null) {
			  Object[] args = {this, thisClass.getDeclaredMethod("preferences", (Class[])null)};
			  registerMethod.invoke(osxAdapter, args);
		  }
		  registerMethod = osxAdapter.getDeclaredMethod("setQuitHandler", defArgs);
		  if (registerMethod != null) {
			  Object[] args = {this, thisClass.getDeclaredMethod("quit", (Class[])null)};
			  registerMethod.invoke(osxAdapter, args);
		  }
		  registerMethod = osxAdapter.getDeclaredMethod("setFileHandler", defArgs);
		  if (registerMethod != null) {
			  Object[] args = {this, thisClass.getDeclaredMethod("openAction", String.class)};
			  registerMethod.invoke(osxAdapter, args);
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
    con.insets = new Insets(2, 4, 2, 2);
    gbl.setConstraints(progressBar, con);
    status.add(progressBar);
    con.weightx = 1;
    con.gridwidth = GridBagConstraints.REMAINDER;
    statusLabel.setForeground(GUIGlobals.entryEditorLabelColor.darker());
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

      public GeneralAction(String command, String text, String description, KeyStroke key, String imageUrl) {
      this.command = command;
        ImageIcon icon = GUIGlobals.getImage(imageUrl);
        if (icon != null)
            putValue(SMALL_ICON, icon);
      putValue(NAME, text);
      putValue(SHORT_DESCRIPTION, Globals.lang(description));
        putValue(ACCELERATOR_KEY, key);
    }

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
              search = subMenu("Search"),
              bibtex = subMenu("BibTeX"),
              view = subMenu("View"),
              tools = subMenu("Tools"),
              //web = subMenu("Web search"),
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
      file.add(new MinimizeToSysTrayAction());
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
      edit.add(copyKeyAndTitle);
      //edit.add(exportToClipboard);
      edit.addSeparator();
      edit.add(mark);
      JMenu markSpecific = subMenu("Mark specific color");
      for (int i=0; i<Util.MAX_MARKING_LEVEL; i++)
          markSpecific.add(new MarkEntriesAction(this, i).getMenuItem());
      edit.add(markSpecific);
      edit.add(unmark);
      edit.add(unmarkAll); 
      edit.addSeparator();
      if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
    	  JMenu m;
    	  if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING)) {
	    	  m = new JMenu();
	    	  RightClickMenu.populateSpecialFieldMenu(m, Rank.getInstance(), this);
	    	  edit.add(m);
    	  }
    	  if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE)) {
    		  edit.add(toggleRelevance);
    	  }
    	  if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY)) {
    		  edit.add(toggleQualityAssured);
    	  }
    	  if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY)) {
    		  m = new JMenu();
    		  RightClickMenu.populateSpecialFieldMenu(m, Priority.getInstance(), this);
    		  edit.add(m);
    	  }
    	  if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED)) {
    		  edit.add(togglePrinted);
    	  }
    	  if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ)) {
    		  m = new JMenu();
    		  RightClickMenu.populateSpecialFieldMenu(m, ReadStatus.getInstance(), this);
    		  edit.add(m);
    	  }
      }
      edit.addSeparator();
      edit.add(manageKeywords);
      edit.add(selectAll);
      mb.add(edit);

      search.add(normalSearch);
      search.add(incrementalSearch);
      search.add(replaceAll);
      search.add(massSetField);
      search.addSeparator();
      search.add(dupliCheck);
      search.add(resolveDuplicateKeys);
      //search.add(strictDupliCheck);
      search.add(autoSetFile);
      search.addSeparator();
      GeneralFetcher generalFetcher = new GeneralFetcher(sidePaneManager, this, fetchers);
      search.add(generalFetcher.getAction());
      if (prefs.getBoolean("webSearchVisible")) {
          sidePaneManager.register(generalFetcher.getTitle(), generalFetcher);
          sidePaneManager.show(generalFetcher.getTitle());
      }
      mb.add(search);

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
      for (NewEntryAction aNewSpecificEntryAction : newSpecificEntryAction) {
          newSpec.add(aNewSpecificEntryAction);
      }
      bibtex.add(newSpec);
      bibtex.add(plainTextImport);
      bibtex.addSeparator();
      bibtex.add(editEntry);
      bibtex.add(editPreamble);
      bibtex.add(editStrings);
      mb.add(bibtex);
      
      tools.add(makeKeyAction);
      tools.add(Cleanup);
      tools.add(mergeEntries);
      //tools.add(downloadFullText);
      tools.add(newSubDatabaseAction);
      tools.add(writeXmpAction);
      OpenOfficePanel otp = OpenOfficePanel.getInstance();
      otp.init(this, sidePaneManager);
      tools.add(otp.getMenuItem());
      tools.add(pushExternalButton.getMenuAction());
      tools.addSeparator();
      tools.add(manageSelectors);
      tools.addSeparator();
      tools.add(openFolder);
      tools.add(openFile);
      tools.add(openPdf);
      tools.add(openUrl);
      //tools.add(openSpires);
      tools.add(findUnlinkedFiles);
      tools.add(autoLinkFile);
      tools.addSeparator();
      tools.add(abbreviateIso);
      tools.add(abbreviateMedline);
      tools.add(unabbreviate);
      tools.addSeparator();
      checkAndFix.add(autoSetPdf);
      checkAndFix.add(autoSetPs);
      checkAndFix.add(integrityCheckAction);
      tools.add(checkAndFix);

      mb.add(tools);


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


      options.add(selectKeys);
      mb.add(options);

      helpMenu.add(help);
      helpMenu.add(contents);
      helpMenu.addSeparator();
      helpMenu.add(errorConsole);
      helpMenu.addSeparator();
      helpMenu.add(about);
      mb.add(helpMenu);
  }

    public static JMenu subMenu(String name) {
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

    public void addParserResult(ParserResult pr, boolean raisePanel) {
        if (pr.toOpenTab()) {
            // Add the entries to the open tab.
            BasePanel panel = basePanel();
            if (panel == null) {
                // There is no open tab to add to, so we create a new tab:
                addTab(pr.getDatabase(), pr.getFile(), pr.getMetaData(), pr.getEncoding(), raisePanel);
            } else {
                List<BibtexEntry> entries = new ArrayList<BibtexEntry>(pr.getDatabase().getEntries());
                addImportedEntries(panel, entries, "", false);
            }
        } else {
            addTab(pr.getDatabase(), pr.getFile(), pr.getMetaData(), pr.getEncoding(), raisePanel);
        }
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
    tlb.addAction(Cleanup);
    tlb.addAction(mergeEntries);
    
    tlb.addSeparator();
    tlb.addAction(mark);
    tlb.addAction(unmark);
    tlb.addSeparator();
    if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
    	if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING)) {
    		tlb.add(net.sf.jabref.specialfields.SpecialFieldDropDown.generateSpecialFieldButtonWithDropDown(Rank.getInstance(), this));
    	}
    	if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE)) {
    		tlb.addAction(toggleRelevance);
    	}
    	if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY)) {
    		tlb.addAction(toggleQualityAssured);
    	}
    	if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY)) {
    		tlb.add(net.sf.jabref.specialfields.SpecialFieldDropDown.generateSpecialFieldButtonWithDropDown(Priority.getInstance(), this));
    	}
    	if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED)) {
    		tlb.addAction(togglePrinted);
    	}
    	if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ)) {
    		tlb.add(net.sf.jabref.specialfields.SpecialFieldDropDown.generateSpecialFieldButtonWithDropDown(ReadStatus.getInstance(), this));
    	}
    }

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

      tlb.addAction(openFolder);
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
        openDatabaseOnlyActions.addAll(Arrays.asList(manageSelectors,
                mergeDatabaseAction, newSubDatabaseAction, close, save, saveAs, saveSelectedAs, undo,
                redo, cut, delete, copy, paste, mark, unmark, unmarkAll, editEntry,
                selectAll, copyKey, copyCiteKey, copyKeyAndTitle, editPreamble, editStrings, toggleGroups, toggleSearch,
                makeKeyAction, normalSearch,
                incrementalSearch, replaceAll, importMenu, exportMenu,
			/* openSpires wasn't being supported so no point in supporting
			 * openInspire */
                openPdf, openUrl, openFolder, openFile, openSpires, /*openInspire,*/ togglePreview, dupliCheck, /*strictDupliCheck,*/ highlightAll,
                highlightAny, newEntryAction, plainTextImport, massSetField, manageKeywords,
                closeDatabaseAction, switchPreview, integrityCheckAction, autoSetPdf, autoSetPs,
                toggleHighlightAny, toggleHighlightAll, databaseProperties, abbreviateIso,
                abbreviateMedline, unabbreviate, exportAll, exportSelected,
                importCurrent, saveAll, dbConnect, dbExport, focusTable));
        
        openDatabaseOnlyActions.addAll(fetcherActions);

        openDatabaseOnlyActions.addAll(Arrays.asList(newSpecificEntryAction));

        severalDatabasesOnlyActions = new LinkedList<Object>();
        severalDatabasesOnlyActions.addAll(Arrays
            .asList(nextTab, prevTab, sortTabs));

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


    public BasePanel addTab(BibtexDatabase db, File file, MetaData metaData, String encoding, boolean raisePanel) {
        // ensure that non-null parameters are really non-null
        if (metaData == null) metaData = new MetaData();
        if (encoding == null) encoding = Globals.prefs.get("defaultEncoding");

        BasePanel bp = new BasePanel(JabRefFrame.this, db, file, metaData, encoding);
        addTab(bp, file, raisePanel);
        return bp;
    }


    public void addTab(BasePanel bp, File file, boolean raisePanel) {
        String title;
        if (file == null ) {
            title = Globals.lang(GUIGlobals.untitledTitle);
            if (!bp.database().getEntries().isEmpty()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                // This also happens internally at basepanel to ensure consistency
                title = title + "*";
            }
        } else {
            title = file.getName();
        }
        tabbedPane.add(title, bp);
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
      d.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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

            if (basePanel().isBaseChanged()) {
                int answer = JOptionPane.showConfirmDialog(JabRefFrame.this,
                    Globals.lang("Database has changed. Do you want to save before closing?"),
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
            setWindowTitle();
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
        addTab(database, null, new MetaData(), Globals.prefs.get("defaultEncoding"), true);
        output(Globals.lang("New database created."));
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

        if (dialog.generatePressed())
        {
          BasePanel bp = new BasePanel( JabRefFrame.this,
                                        dialog.getGenerateDB(),   // database
                                        null,                     // file
                                        new MetaData(), Globals.prefs.get("defaultEncoding"));                     // meta data
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
                    // On the one hand, the following statement could help at issues when JabRef is minimized to the systray
                    // On the other hand, users might dislake modality and this is not required to let the GUI work.
                    // Therefore, it is disabled.
                    // diag.setModal(true);
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
      // Metadata are only put in bibtex files, so we will not find it
      // in imported files. We therefore pass in an empty MetaData:
      BasePanel bp = new BasePanel(JabRefFrame.this, database, null, new MetaData(), Globals.prefs.get("defaultEncoding"));
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
      BasePanel basePanel = basePanel();
      BibtexDatabase database = basePanel.database;
      int oldCount = database.getEntryCount();
      NamedCompound ce = new NamedCompound(Globals.lang("Import entries"));

      mainLoop: 
      for (BibtexEntry entry : bibentries){
        boolean dupli = false;
        // Check for duplicates among the current entries:
          for (String s : database.getKeySet()) {
              BibtexEntry existingEntry = database.getEntryById(s);
              if (DuplicateCheck.isDuplicate(entry, existingEntry
              )) {
                  DuplicateResolverDialog drd = new DuplicateResolverDialog
                          (JabRefFrame.this, existingEntry, entry, DuplicateResolverDialog.IMPORT_CHECK);
                  drd.setVisible(true);
                  int res = drd.getSelected();
                  if (res == DuplicateResolverDialog.KEEP_LOWER) {
                      dupli = true;
                  } else if (res == DuplicateResolverDialog.KEEP_UPPER) {
                      database.removeEntry(existingEntry.getId());
                      ce.addEdit(new UndoableRemoveEntry
                              (database, existingEntry, basePanel));
                  } else if (res == DuplicateResolverDialog.BREAK) {
                      break mainLoop;
                  }
                  break;
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
              catch (Throwable ignored) {}
            }
          }
          if (baseAt(i).getFile() != null) {
            filenames.add(baseAt(i).getFile().getPath());
          }
        }
      }

      if (filenames.size() == 0) {
        output(Globals.lang("Not saved (empty session)") + ".");
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
    
    class BibtexKeyPatternAction extends MnemonicAwareAction {
        BibtexKeyPatternDialog bibtexKeyPatternDialog = null;
        public BibtexKeyPatternAction() {
            putValue(NAME, "Bibtex key patterns");
        }

        public void actionPerformed(ActionEvent e) {
        	JabRefPreferences.getInstance();
            if (bibtexKeyPatternDialog == null) {
                // if no instance of BibtexKeyPatternDialog exists, create new one
            	bibtexKeyPatternDialog = new BibtexKeyPatternDialog(JabRefFrame.this, basePanel());
            } else {
                // BibtexKeyPatternDialog allows for updating content based on currently selected panel
                bibtexKeyPatternDialog.setPanel(basePanel());
            }
            Util.placeDialog(bibtexKeyPatternDialog, JabRefFrame.this);
            bibtexKeyPatternDialog.setVisible(true);
        }
       
    }

    class IncreaseTableFontSizeAction extends MnemonicAwareAction {
        public IncreaseTableFontSizeAction() {
            putValue(NAME, "Increase table font size");
            putValue(ACCELERATOR_KEY, Globals.prefs.getKey("Increase table font size"));
        }
        public void actionPerformed(ActionEvent event) {
            int currentSize = GUIGlobals.CURRENTFONT.getSize();
            GUIGlobals.CURRENTFONT = new Font(GUIGlobals.CURRENTFONT.getFamily(), GUIGlobals.CURRENTFONT.getStyle(),
                    currentSize+1);
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
            GUIGlobals.CURRENTFONT = new Font(GUIGlobals.CURRENTFONT.getFamily(), GUIGlobals.CURRENTFONT.getStyle(),
                    currentSize-1);
            Globals.prefs.putInt("fontSize", currentSize-1);
            for (int i=0; i<baseCount(); i++) {
                baseAt(i).updateTableFont();
            }
        }
    }

    class MinimizeToSysTrayAction extends MnemonicAwareAction {
        public MinimizeToSysTrayAction() {
            putValue(NAME, "Minimize to system tray");
            putValue(ACCELERATOR_KEY, Globals.prefs.getKey("Minimize to system tray"));
        }
        public void actionPerformed(ActionEvent event) {
            if (sysTray == null)
                sysTray = new SysTray(JabRefFrame.this);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    sysTray.setTrayIconVisible(true);
                    JabRefFrame.this.setVisible(false);
                }
            });
        }
    }

    public void showIfMinimizedToSysTray() {
        // TODO: does not work correctly when a dialog is shown
        // Workaround: put into invokeLater queue before a dialog is added to that queue
        if (!this.isVisible()) {
            // isVisible() is false if minimized to systray
            if (sysTray != null) {
                sysTray.setTrayIconVisible(false);
            }
            setVisible(true);
            this.isActive();
            toFront();
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
  
  public SearchManager2 getSearchManager() {
		return searchManager;
	}

}
