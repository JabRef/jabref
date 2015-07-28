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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.export.AutoSaveManager;
import net.sf.jabref.export.ExportCustomizationDialog;
import net.sf.jabref.export.ExportFormats;
import net.sf.jabref.export.SaveAllAction;
import net.sf.jabref.export.SaveDatabaseAction;
import net.sf.jabref.external.ExternalFileTypeEditor;
import net.sf.jabref.external.PushToApplicationButton;
import net.sf.jabref.groups.EntryTableTransferHandler;
import net.sf.jabref.groups.GroupSelector;
import net.sf.jabref.gui.BibtexKeyPatternDialog;
import net.sf.jabref.gui.DatabasePropertiesDialog;
import net.sf.jabref.gui.DragDropPopupPane;
import net.sf.jabref.gui.EntryCustomizationDialog2;
import net.sf.jabref.gui.ErrorConsoleAction;
import net.sf.jabref.gui.GenFieldsCustomizer;
import net.sf.jabref.gui.ImportInspectionDialog;
import net.sf.jabref.gui.SortTabsAction;
import net.sf.jabref.gui.SysTray;
import net.sf.jabref.gui.WaitForSaveOperation;
import net.sf.jabref.gui.menus.help.ForkMeOnGitHubAction;
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
import net.sf.jabref.util.StringUtil;
import net.sf.jabref.util.Util;
import net.sf.jabref.wizard.auximport.gui.FromAuxDialog;
import net.sf.jabref.wizard.integrity.gui.IntegrityWizard;

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;

/**
 * The main window of the application.
 */
public class JabRefFrame extends JFrame implements OutputPrinter {

    final JSplitPane contentPane = new JSplitPane();

    final JabRefPreferences prefs = Globals.prefs;
    private PrefsDialog3 prefsDialog = null;

    private int lastTabbedPanelSelectionIndex = -1;

    // The sidepane manager takes care of populating the sidepane. 
    public SidePaneManager sidePaneManager;

    JTabbedPane tabbedPane; // initialized at constructor

    private final Insets marg = new Insets(1, 0, 2, 0);
    private final JabRef jabRef;


    class ToolBar extends JToolBar {

        void addAction(Action a) {
            JButton b = new JButton(a);
            b.setText(null);
            if (!Globals.ON_MAC) {
                b.setMargin(marg);
            }
            add(b);
        }
    }


    final ToolBar tlb = new ToolBar();

    private final JMenuBar mb = new JMenuBar();
    private final JMenu pluginMenu = JabRefFrame.subMenu("Plugins");
    private boolean addedToPluginMenu = false;

    private final GridBagLayout gbl = new GridBagLayout();
    private final GridBagConstraints con = new GridBagConstraints();

    final JLabel statusLine = new JLabel("", SwingConstants.LEFT);
    private final JLabel statusLabel = new JLabel(
            Globals.lang("Status")
                    + ':', SwingConstants.LEFT);
    private final JProgressBar progressBar = new JProgressBar();

    private final FileHistory fileHistory = new FileHistory(prefs, this);

    private SysTray sysTray = null;

    // The help window.
    public final HelpDialog helpDiag = new HelpDialog(this);

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

    final OpenDatabaseAction open = new OpenDatabaseAction(this, true);
    private final AbstractAction
            close = new CloseDatabaseAction();
    private final AbstractAction quit = new CloseAction();
    private final AbstractAction selectKeys = new SelectKeysAction();
    private final AbstractAction newDatabaseAction = new NewDatabaseAction();
    private final AbstractAction newSubDatabaseAction = new NewSubDatabaseAction();
    private final AbstractAction integrityCheckAction = new IntegrityCheckAction();
    private final AbstractAction forkMeOnGitHubAction = new ForkMeOnGitHubAction();
    private final AbstractAction help = new HelpAction("JabRef help", helpDiag,
                    GUIGlobals.baseFrameHelp, Globals.lang("JabRef help"),
                    prefs.getKey("Help"));
    private final AbstractAction contents = new HelpAction("Help contents", helpDiag,
                    GUIGlobals.helpContents, Globals.lang("Help contents"),
                    GUIGlobals.getIconUrl("helpContents"));
    private final AbstractAction about = new HelpAction("About JabRef", helpDiag,
                    GUIGlobals.aboutPage, Globals.lang("About JabRef"),
                    GUIGlobals.getIconUrl("about"));
    private final AbstractAction editEntry = new GeneralAction("edit", "Edit entry",
                    Globals.lang("Edit entry"),
                    prefs.getKey("Edit entry"));
    private final AbstractAction focusTable = new GeneralAction("focusTable", "Focus entry table",
                    Globals.lang("Move the keyboard focus to the entry table"),
                    prefs.getKey("Focus entry table"));
    private final AbstractAction save = new GeneralAction("save", "Save database",
                    Globals.lang("Save database"),
                    prefs.getKey("Save database"));
    private final AbstractAction saveAs = new GeneralAction("saveAs", "Save database as ...",
                    Globals.lang("Save database as ..."),
                    prefs.getKey("Save database as ..."));
    private final AbstractAction saveAll = new SaveAllAction(JabRefFrame.this);
    private final AbstractAction saveSelectedAs = new GeneralAction("saveSelectedAs",
                    "Save selected as ...",
                    Globals.lang("Save selected as ..."),
                    GUIGlobals.getIconUrl("saveAs"));
    private final AbstractAction saveSelectedAsPlain = new GeneralAction("saveSelectedAsPlain",
                    "Save selected as plain BibTeX ...",
                    Globals.lang("Save selected as plain BibTeX ..."),
                    GUIGlobals.getIconUrl("saveAs"));
    private final AbstractAction exportAll = ExportFormats.getExportAction(this, false);
    private final AbstractAction exportSelected = ExportFormats.getExportAction(this, true);
    private final AbstractAction importCurrent = ImportFormats.getImportAction(this, false);
    private final AbstractAction importNew = ImportFormats.getImportAction(this, true);
    final AbstractAction nextTab = new ChangeTabAction(true);
    final AbstractAction prevTab = new ChangeTabAction(false);
    private final AbstractAction sortTabs = new SortTabsAction(this);
    private final AbstractAction undo = new GeneralAction("undo", "Undo", Globals.lang("Undo"),
                    prefs.getKey("Undo"));
    private final AbstractAction redo = new GeneralAction("redo", "Redo", Globals.lang("Redo"),
                    prefs.getKey("Redo"));
    final AbstractAction forward = new GeneralAction("forward", "Forward", Globals.lang("Forward"),
                    "right", prefs.getKey("Forward"));
    final AbstractAction back = new GeneralAction("back", "Back", Globals.lang("Back"),
                    "left", prefs.getKey("Back"));
    private final AbstractAction//cut = new GeneralAction("cut", "Cut", Globals.lang("Cut"),
            //   GUIGlobals.cutIconFile,
            //   prefs.getKey("Cut")),
            delete = new GeneralAction("delete", "Delete", Globals.lang("Delete"),
                    prefs.getKey("Delete"));
    private final AbstractAction//copy = new GeneralAction("copy", "Copy", Globals.lang("Copy"),
            //                         GUIGlobals.copyIconFile,
            //                         prefs.getKey("Copy")),
            copy = new EditAction("copy", GUIGlobals.getIconUrl("copy"));
    private final AbstractAction paste = new EditAction("paste", GUIGlobals.getIconUrl("paste"));
    private final AbstractAction cut = new EditAction("cut", GUIGlobals.getIconUrl("cut"));
    private final AbstractAction mark = new GeneralAction("markEntries", "Mark entries",
                    Globals.lang("Mark entries"),
                    prefs.getKey("Mark entries"));
    private final AbstractAction unmark = new GeneralAction("unmarkEntries", "Unmark entries",
                    Globals.lang("Unmark entries"),
                    prefs.getKey("Unmark entries"));
    private final AbstractAction unmarkAll = new GeneralAction("unmarkAll", "Unmark all");
    private final AbstractAction toggleRelevance = new GeneralAction(
                    Relevance.getInstance().getValues().get(0).getActionName(),
                    Relevance.getInstance().getValues().get(0).getMenuString(),
                    Relevance.getInstance().getValues().get(0).getToolTipText());
    private final AbstractAction toggleQualityAssured = new GeneralAction(
                    Quality.getInstance().getValues().get(0).getActionName(),
                    Quality.getInstance().getValues().get(0).getMenuString(),
                    Quality.getInstance().getValues().get(0).getToolTipText());
    private final AbstractAction togglePrinted = new GeneralAction(
                    Printed.getInstance().getValues().get(0).getActionName(),
                    Printed.getInstance().getValues().get(0).getMenuString(),
                    Printed.getInstance().getValues().get(0).getToolTipText());
    private final AbstractAction//    	priority = new GeneralAction("setPriority", "Set priority",
            //    			                                            Globals.lang("Set priority")),
            manageSelectors = new GeneralAction("manageSelectors", "Manage content selectors");
    private final AbstractAction saveSessionAction = new SaveSessionAction();
    final AbstractAction loadSessionAction = new LoadSessionAction();
    private final AbstractAction incrementalSearch = new GeneralAction("incSearch", "Incremental search",
                    Globals.lang("Start incremental search"),
                    prefs.getKey("Incremental search"));
    private final AbstractAction normalSearch = new GeneralAction("search", "Search", Globals.lang("Search"),
                    prefs.getKey("Search"));
    private final AbstractAction toggleSearch = new GeneralAction("toggleSearch", "Search", Globals.lang("Toggle search panel"));

    private final AbstractAction copyKey = new GeneralAction("copyKey", "Copy BibTeX key",
                    prefs.getKey("Copy BibTeX key"));
    private final AbstractAction//"Put a BibTeX reference to the selected entries on the clipboard",
            copyCiteKey = new GeneralAction("copyCiteKey", "Copy \\cite{BibTeX key}",
                    //"Put a BibTeX reference to the selected entries on the clipboard",
                    prefs.getKey("Copy \\cite{BibTeX key}"));
    private final AbstractAction copyKeyAndTitle = new GeneralAction("copyKeyAndTitle",
                    "Copy BibTeX key and title",
                    prefs.getKey("Copy BibTeX key and title"));
    private final AbstractAction mergeDatabaseAction = new GeneralAction("mergeDatabase",
                    "Append database",
                    Globals.lang("Append contents from a BibTeX database into the currently viewed database"),
                    GUIGlobals.getIconUrl("open"));
    private final AbstractAction//prefs.getKey("Open")),
            /*remove = new GeneralAction("remove", "Remove", "Remove selected entries",
              GUIGlobals.removeIconFile),*/
            selectAll = new GeneralAction("selectAll", "Select all",
                    prefs.getKey("Select all"));
    private final AbstractAction replaceAll = new GeneralAction("replaceAll", "Replace string",
                    prefs.getKey("Replace string"));

    private final AbstractAction editPreamble = new GeneralAction("editPreamble", "Edit preamble",
                    Globals.lang("Edit preamble"),
                    prefs.getKey("Edit preamble"));
    private final AbstractAction editStrings = new GeneralAction("editStrings", "Edit strings",
                    Globals.lang("Edit strings"),
                    prefs.getKey("Edit strings"));
    private final AbstractAction toggleToolbar = new GeneralAction("toggleToolbar", "Hide/show toolbar",
                    Globals.lang("Hide/show toolbar"),
                    prefs.getKey("Hide/show toolbar"));
    private final AbstractAction toggleGroups = new GeneralAction("toggleGroups",
                    "Toggle groups interface",
                    Globals.lang("Toggle groups interface"),
                    prefs.getKey("Toggle groups interface"));
    private final AbstractAction togglePreview = new GeneralAction("togglePreview",
                    "Toggle entry preview",
                    Globals.lang("Toggle entry preview"),
                    prefs.getKey("Toggle entry preview"));
    private final AbstractAction toggleHighlightAny = new GeneralAction("toggleHighlightGroupsMatchingAny",
                    "Highlight groups matching any selected entry",
                    Globals.lang("Highlight groups matching any selected entry"),
                    GUIGlobals.getIconUrl("groupsHighlightAny"));
    private final AbstractAction toggleHighlightAll = new GeneralAction("toggleHighlightGroupsMatchingAll",
                    "Highlight groups matching all selected entries",
                    Globals.lang("Highlight groups matching all selected entries"),
                    GUIGlobals.getIconUrl("groupsHighlightAll"));
    final AbstractAction switchPreview = new GeneralAction("switchPreview",
                    "Switch preview layout",
                    prefs.getKey("Switch preview layout"));
    private final AbstractAction makeKeyAction = new GeneralAction("makeKey", "Autogenerate BibTeX keys",
                    Globals.lang("Autogenerate BibTeX keys"),
                    prefs.getKey("Autogenerate BibTeX keys"));

    private final AbstractAction writeXmpAction = new GeneralAction("writeXMP", "Write XMP-metadata to PDFs",
                    Globals.lang("Will write XMP-metadata to the PDFs linked from selected entries."),
                    prefs.getKey("Write XMP"));

    private final AbstractAction openFolder = new GeneralAction("openFolder", "Open folder",
                    Globals.lang("Open folder"),
                    prefs.getKey("Open folder"));
    private final AbstractAction openFile = new GeneralAction("openExternalFile", "Open file",
                    Globals.lang("Open file"),
                    prefs.getKey("Open file"));
    private final AbstractAction openPdf = new GeneralAction("openFile", "Open PDF or PS",
                    Globals.lang("Open PDF or PS"),
                    prefs.getKey("Open PDF or PS"));
    private final AbstractAction openUrl = new GeneralAction("openUrl", "Open URL or DOI",
                    Globals.lang("Open URL or DOI"),
                    prefs.getKey("Open URL or DOI"));
    private final AbstractAction openSpires = new GeneralAction("openSpires", "Open SPIRES entry",
                    Globals.lang("Open SPIRES entry"),
                    prefs.getKey("Open SPIRES entry"));
    private final AbstractAction/*
             * It looks like this wasn't being implemented for spires anyway so we
             * comment it out for now.
             *
            openInspire = new GeneralAction("openInspire", "Open INSPIRE entry",
                                                Globals.lang("Open INSPIRE entry"),
                                                prefs.getKey("Open INSPIRE entry")),
            */
            dupliCheck = new GeneralAction("dupliCheck", "Find duplicates");
    private final AbstractAction//strictDupliCheck = new GeneralAction("strictDupliCheck", "Find and remove exact duplicates"),
            plainTextImport = new GeneralAction("plainTextImport",
                    "New entry from plain text",
                    prefs.getKey("New from plain text"));

    private final AbstractAction customExpAction = new CustomizeExportsAction();
    private final AbstractAction customImpAction = new CustomizeImportsAction();
    private final AbstractAction customFileTypesAction = ExternalFileTypeEditor.getAction(this);
    AbstractAction exportToClipboard = new GeneralAction("exportToClipboard", "Export selected entries to clipboard");
    private final AbstractAction//expandEndnoteZip = new ExpandEndnoteFilters(this),
            autoSetPdf = new GeneralAction("autoSetPdf", Globals.lang("Synchronize %0 links", "PDF"), Globals.prefs.getKey("Synchronize PDF"));
    private final AbstractAction autoSetPs = new GeneralAction("autoSetPs", Globals.lang("Synchronize %0 links", "PS"), Globals.prefs.getKey("Synchronize PS"));
    private final AbstractAction autoSetFile = new GeneralAction("autoSetFile", Globals.lang("Synchronize file links"), Globals.prefs.getKey("Synchronize files"));

    private final AbstractAction abbreviateMedline = new GeneralAction("abbreviateMedline", "Abbreviate journal names (MEDLINE)",
                    Globals.lang("Abbreviate journal names of the selected entries (MEDLINE abbreviation)"));
    private final AbstractAction abbreviateIso = new GeneralAction("abbreviateIso", "Abbreviate journal names (ISO)",
                    Globals.lang("Abbreviate journal names of the selected entries (ISO abbreviation)"),
                    Globals.prefs.getKey("Abbreviate"));

    private final AbstractAction unabbreviate = new GeneralAction("unabbreviate", "Unabbreviate journal names",
                    Globals.lang("Unabbreviate journal names of the selected entries"),
                    Globals.prefs.getKey("Unabbreviate"));
    private final AbstractAction manageJournals = new ManageJournalsAction(this);
    private final AbstractAction databaseProperties = new DatabasePropertiesAction();
    private final AbstractAction bibtexKeyPattern = new BibtexKeyPatternAction();
    private final AbstractAction errorConsole = new ErrorConsoleAction(this, Globals.streamEavesdropper, Globals.handler);
    AbstractAction test = new GeneralAction("test", "Test");

    private final AbstractAction dbConnect = new GeneralAction("dbConnect", "Connect to external SQL database",
                    Globals.lang("Connect to external SQL database"),
                    GUIGlobals.getIconUrl("dbConnect"));

    private final AbstractAction dbExport = new GeneralAction("dbExport", "Export to external SQL database",
                    Globals.lang("Export to external SQL database"),
                    GUIGlobals.getIconUrl("dbExport"));

    private final AbstractAction Cleanup = new GeneralAction("Cleanup", "Cleanup entries",
                    Globals.lang("Cleanup entries"),
                    prefs.getKey("Cleanup"),
                    ("cleanupentries"));

    private final AbstractAction mergeEntries = new GeneralAction("mergeEntries", "Merge entries",
                    Globals.lang("Merge entries"),
                    GUIGlobals.getIconUrl("mergeentries"));

    private final AbstractAction dbImport = new DbImportAction(this).getAction();
    private final AbstractAction//downloadFullText = new GeneralAction("downloadFullText", "Look up full text document",
            //        Globals.lang("Follow DOI or URL link and try to locate PDF full text document")),
            increaseFontSize = new IncreaseTableFontSizeAction();
    private final AbstractAction decreseFontSize = new DecreaseTableFontSizeAction();
    private final AbstractAction installPlugin = new PluginInstallerAction(this);
    private final AbstractAction resolveDuplicateKeys = new GeneralAction("resolveDuplicateKeys", "Resolve duplicate BibTeX keys",
                    Globals.lang("Find and remove duplicate BibTeX keys"),
                    prefs.getKey("Resolve duplicate BibTeX keys"));

    final MassSetFieldAction massSetField = new MassSetFieldAction(this);
    final ManageKeywordsAction manageKeywords = new ManageKeywordsAction(this);

    private final GeneralAction findUnlinkedFiles = new GeneralAction(
            FindUnlinkedFilesDialog.ACTION_COMMAND,
            FindUnlinkedFilesDialog.ACTION_TITLE,
            FindUnlinkedFilesDialog.ACTION_SHORT_DESCRIPTION,
            FindUnlinkedFilesDialog.ACTION_ICON,
            prefs.getKey(FindUnlinkedFilesDialog.ACTION_KEYBINDING_ACTION)
            );

    private final AutoLinkFilesAction autoLinkFile = new AutoLinkFilesAction();

    private PushToApplicationButton pushExternalButton;

    private final List<EntryFetcher> fetchers = new LinkedList<EntryFetcher>();
    private final List<Action> fetcherActions = new LinkedList<Action>();

    private SearchManager2 searchManager;

    public GroupSelector groupSelector;

    // The menus for importing/appending other formats
    private final JMenu importMenu = JabRefFrame.subMenu("Import into current database");
    private final JMenu importNewMenu = JabRefFrame.subMenu("Import into new database");
    private final JMenu exportMenu = JabRefFrame.subMenu("Export");
    JMenu customExportMenu = JabRefFrame.subMenu("Custom export");
    private final JMenu newDatabaseMenu = JabRefFrame.subMenu("New database");

    // Other submenus
    private final JMenu checkAndFix = JabRefFrame.subMenu("Legacy tools...");

    // The action for adding a new entry of unspecified type.
    private final NewEntryAction newEntryAction = new NewEntryAction(prefs.getKey("New entry"));
    private final NewEntryAction[] newSpecificEntryAction = new NewEntryAction[]
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


    public JabRefFrame(JabRef jabRef) {
        this.jabRef = jabRef;
        init();
        updateEnabledState();

    }

    private void init() {
        tabbedPane = new DragDropPopupPane(manageSelectors, databaseProperties, bibtexKeyPattern);

        UIManager.put("FileChooser.readOnly", Globals.prefs.getBoolean(JabRefPreferences.FILECHOOSER_DISABLE_RENAME));

        MyGlassPane glassPane = new MyGlassPane();
        setGlassPane(glassPane);
        // glassPane.setVisible(true);

        setTitle(GUIGlobals.frameTitle);
        //setIconImage(GUIGlobals.getImage("jabrefIcon").getImage());
        setIconImage(GUIGlobals.getImage("jabrefIcon48").getImage());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                if (Globals.ON_MAC) {
                    setState(Frame.ICONIFIED);
                } else {
                    (new CloseAction()).actionPerformed(null);
                }
            }
        });

        initSidePane();

        initLayout();

        initActions();

        // Show the toolbar if it was visible at last shutdown:
        tlb.setVisible(Globals.prefs.getBoolean(JabRefPreferences.TOOLBAR_VISIBLE));

        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        if (!prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {

            int sizeX = prefs.getInt(JabRefPreferences.SIZE_X);
            int sizeY = prefs.getInt(JabRefPreferences.SIZE_Y);
            int posX = prefs.getInt(JabRefPreferences.POS_X);
            int posY = prefs.getInt(JabRefPreferences.POS_Y);

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
            if (GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length == 1) {

                Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0]
                        .getDefaultConfiguration().getBounds();
                Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

                // Make sure we are not above or to the left of the screen bounds:
                if (posX < bounds.x) {
                    posX = bounds.x;
                }
                if (posY < bounds.y) {
                    posY = bounds.y;
                }

                int height = (int) dim.getHeight();
                int width = (int) dim.getWidth();

                //if (posX < )

                if ((posX + sizeX) > width) {
                    if (sizeX <= width) {
                        posX = width - sizeX;
                    } else {
                        posX = prefs.getIntDefault(JabRefPreferences.POS_X);
                        sizeX = prefs.getIntDefault(JabRefPreferences.SIZE_X);
                    }
                }

                if ((posY + sizeY) > height) {
                    if (sizeY <= height) {
                        posY = height - sizeY;
                    } else {
                        posY = prefs.getIntDefault(JabRefPreferences.POS_Y);
                        sizeY = prefs.getIntDefault(JabRefPreferences.SIZE_Y);
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

            @Override
            public void stateChanged(ChangeEvent e) {
                markActiveBasePanel();

                BasePanel bp = basePanel();
                if (bp != null) {
                    groupToggle.setSelected(sidePaneManager.isComponentVisible("groups"));
                    searchToggle.setSelected(sidePaneManager.isComponentVisible("search"));
                    previewToggle.setSelected(Globals.prefs.getBoolean(JabRefPreferences.PREVIEW_ENABLED));
                    highlightAny
                            .setSelected(Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ANY));
                    highlightAll
                            .setSelected(Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ALL));
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

        //Note: The registration of Apple event is at the end of initialization, because
        //if the events happen too early (ie when the window is not initialized yet), the
        //opened (double-clicked) documents are not displayed.
        if (Globals.ON_MAC) {
            try {
                Class<?> macreg = Class.forName("osx.macadapter.MacAdapter");
                Method method = macreg.getMethod("registerMacEvents", JabRefFrame.class);
                method.invoke(macreg.newInstance(), this);
            } catch (Exception e) {
                System.err.println("Exception (" + e.getClass().toString() + "): " + e.getMessage());
            }
        }
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
            setTitle(GUIGlobals.frameTitle + " - " + bp.getFile().getPath() + star);
        } else {
            setTitle(GUIGlobals.frameTitle + " - " + Globals.lang("untitled") + star);
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
        if (jabrefPlugin != null) {
            for (EntryFetcherExtension ext : jabrefPlugin.getEntryFetcherExtensions()) {
                try {
                    EntryFetcher fetcher = ext.getEntryFetcher();
                    if (fetcher != null) {
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
        if (Globals.prefs.getBoolean(JabRefPreferences.SEARCH_PANEL_VISIBLE)) {
            sidePaneManager.show("search");
        }
    }

    // The MacAdapter calls this method when a ".bib" file has been double-clicked from the Finder.
    public void openAction(String filePath) {
        File file = new File(filePath);

        // Check if the file is already open.
        for (int i = 0; i < this.getTabbedPane().getTabCount(); i++) {
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
            JabRefExecutorService.INSTANCE.execute(new Runnable() {

                @Override
                public void run() {
                    open.openIt(theFile, true);
                }
            });
        }
    }

    // General info dialog.  The MacAdapter calls this method when "About"
    // is selected from the application menu.
    public void about() {
        JDialog about = new JDialog(JabRefFrame.this, Globals.lang("About JabRef"),
                true);
        JEditorPane jp = new JEditorPane();
        JScrollPane sp = new JScrollPane
                (jp, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jp.setEditable(false);
        try {
            jp.setPage(GUIGlobals.class.getResource("/help/About.html"));//GUIGlobals.aboutPage);
            // We need a hyperlink listener to be able to switch to the license
            // terms and back.
            jp.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {

                @Override
                public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent e) {
                    if (e.getEventType()
                    == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            ((JEditorPane) e.getSource()).setPage(e.getURL());
                        }
                        catch (IOException ignored) {
                        }
                    }
                }
            });
            about.getContentPane().add(sp);
            about.setSize(GUIGlobals.aboutSize);
            Util.placeDialog(about, JabRefFrame.this);
            about.setVisible(true);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(JabRefFrame.this, "Could not load file 'About.html'",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    // General preferences dialog.  The MacAdapter calls this method when "Preferences..."
    // is selected from the application menu.
    public void preferences() {
        //PrefsDialog.showPrefsDialog(JabRefFrame.this, prefs);
        AbstractWorker worker = new AbstractWorker() {

            @Override
            public void run() {
                output(Globals.lang("Opening preferences..."));
                if (prefsDialog == null) {
                    prefsDialog = new PrefsDialog3(JabRefFrame.this, jabRef);
                    Util.placeDialog(prefsDialog, JabRefFrame.this);
                } else {
                    prefsDialog.setValues();
                }

            }

            @Override
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

    /**
     * Tears down all things started by JabRef
     * 
     * FIXME: Currently some threads remain and therefore hinder JabRef to be closed properly
     * 
     * @param filenames the file names of all currently opened files - used for storing them if prefs openLastEdited is set to true
     */
    private void tearDownJabRef(Vector<String> filenames) {
        JabRefExecutorService.INSTANCE.shutdownEverything();

        dispose();

        if (basePanel() != null) {
            basePanel().saveDividerLocation();
        }
        prefs.putInt(JabRefPreferences.POS_X, JabRefFrame.this.getLocation().x);
        prefs.putInt(JabRefPreferences.POS_Y, JabRefFrame.this.getLocation().y);
        prefs.putInt(JabRefPreferences.SIZE_X, JabRefFrame.this.getSize().width);
        prefs.putInt(JabRefPreferences.SIZE_Y, JabRefFrame.this.getSize().height);
        //prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, (getExtendedState()&MAXIMIZED_BOTH)>0);
        prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, (getExtendedState() == Frame.MAXIMIZED_BOTH));

        prefs.putBoolean(JabRefPreferences.TOOLBAR_VISIBLE, tlb.isVisible());
        prefs.putBoolean(JabRefPreferences.SEARCH_PANEL_VISIBLE, sidePaneManager.isComponentVisible("search"));
        // Store divider location for side pane:
        int width = contentPane.getDividerLocation();
        if (width > 0) {
            prefs.putInt(JabRefPreferences.SIDE_PANE_WIDTH, width);
        }
        if (prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)) {
            // Here we store the names of all current files. If
            // there is no current file, we remove any
            // previously stored file name.
            if (filenames.isEmpty()) {
                prefs.remove(JabRefPreferences.LAST_EDITED);
            }
            else {
                String[] names = new String[filenames.size()];
                for (int i = 0; i < filenames.size(); i++) {
                    names[i] = filenames.elementAt(i);
                }
                prefs.putStringArray(JabRefPreferences.LAST_EDITED, names);
            }

        }

        fileHistory.storeHistory();
        prefs.customExports.store();
        prefs.customImports.store();
        BibtexEntryType.saveCustomEntryTypes(prefs);

        // Clear autosave files:
        if (Globals.autoSaveManager != null) {
            Globals.autoSaveManager.clearAutoSaves();
        }

        // Let the search interface store changes to prefs.
        // But which one? Let's use the one that is visible.
        if (basePanel() != null) {
            (searchManager).updatePrefs();
        }

        prefs.flush();

        // hide systray because the JVM can only shut down when no systray icon is shown
        if(sysTray != null) {
            sysTray.hide();
        }

        // dispose all windows, even if they are not displayed anymore
        for(Window window: Window.getWindows()) {
            window.dispose();
        }

        // shutdown any timers that are may be active
        if (Globals.autoSaveManager != null) {
            Globals.stopAutoSaveManager();
        }
    }

    /**
     * General info dialog.  The MacAdapter calls this method when "Quit"
     * is selected from the application menu, Cmd-Q is pressed, or "Quit" is selected from the Dock.
     * The function returns a boolean indicating if quitting is ok or not.
     * 
     * Non-OSX JabRef calls this when choosing "Quit" from the menu
     * 
     * SIDE EFFECT: tears down JabRef
     * 
     * @return true if the user chose to quit; false otherwise
     */
    public boolean quit() {
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

                    if ((answer == JOptionPane.CANCEL_OPTION) ||
                            (answer == JOptionPane.CLOSED_OPTION)) {
                        return false;
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
                        } catch (Throwable ex) {
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
                     {
                        return false; // The user clicked cancel.
                    }
                }
            }

            tearDownJabRef(filenames);
            return true;
        }

        return false;
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
        int now = tabbedPane.getSelectedIndex();
        int len = tabbedPane.getTabCount();
        if ((lastTabbedPanelSelectionIndex > -1) && (lastTabbedPanelSelectionIndex < len)) {
            tabbedPane.setForegroundAt(lastTabbedPanelSelectionIndex, GUIGlobals.inActiveTabbed);
        }
        if ((now > -1) && (now < len)) {
            tabbedPane.setForegroundAt(now, GUIGlobals.activeTabbed);
        }
        lastTabbedPanelSelectionIndex = now;
    }

    private int getTabIndex(JComponent comp) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getComponentAt(i) == comp) {
                return i;
            }
        }
        return -1;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

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

        private final String command;


        public GeneralAction(String command, String text,
                String description, URL icon) {
            super(new ImageIcon(icon));
            this.command = command;
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, Globals.lang(description));
        }

        public GeneralAction(String command, String text,
                String description, String imageName,
                KeyStroke key) {
            super(GUIGlobals.getImage(imageName));
            this.command = command;
            putValue(Action.NAME, text);
            putValue(Action.ACCELERATOR_KEY, key);
            putValue(Action.SHORT_DESCRIPTION, Globals.lang(description));
        }

        public GeneralAction(String command, String text) {
            putValue(Action.NAME, text);
            this.command = command;
        }

        public GeneralAction(String command, String text, KeyStroke key) {
            this.command = command;
            putValue(Action.NAME, text);
            putValue(Action.ACCELERATOR_KEY, key);
        }

        public GeneralAction(String command, String text, String description) {
            this.command = command;
            ImageIcon icon = GUIGlobals.getImage(command);
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
            }
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, Globals.lang(description));
        }

        public GeneralAction(String command, String text, String description, KeyStroke key) {
            this.command = command;
            ImageIcon icon = GUIGlobals.getImage(command);
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
            }
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, Globals.lang(description));
            putValue(Action.ACCELERATOR_KEY, key);
        }

        public GeneralAction(String command, String text, String description, KeyStroke key, String imageUrl) {
            this.command = command;
            ImageIcon icon = GUIGlobals.getImage(imageUrl);
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
            }
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, Globals.lang(description));
            putValue(Action.ACCELERATOR_KEY, key);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (tabbedPane.getTabCount() > 0) {
                try {
                    ((BasePanel) (tabbedPane.getSelectedComponent()))
                            .runCommand(command);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
            else {
                // QUICK HACK to solve bug #1277
                if (e.getActionCommand().equals("Hide/show toolbar")) {
                    // code copied from BasePanel.java, action "toggleToolbar"
                    tlb.setVisible(!tlb.isVisible());
                } else {
                    Util.pr("Action '" + command + "' must be disabled when no database is open.");
                }
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
            putValue(Action.NAME, "New entry");
            putValue(Action.ACCELERATOR_KEY, key);
            putValue(Action.SHORT_DESCRIPTION, Globals.lang("New BibTeX entry"));
        }

        public NewEntryAction(String type_) {
            // This action leads to the creation of a specific entry.
            putValue(Action.NAME, StringUtil.nCase(type_));
            type = type_;
        }

        public NewEntryAction(String type_, KeyStroke key) {
            // This action leads to the creation of a specific entry.
            putValue(Action.NAME, StringUtil.nCase(type_));
            putValue(Action.ACCELERATOR_KEY, key);
            type = type_;
        }

        @Override
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
                ((BasePanel) (tabbedPane.getSelectedComponent()))
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
        JMenu file = JabRefFrame.subMenu("File"), sessions = JabRefFrame.subMenu("Sessions"), edit = JabRefFrame.subMenu("Edit"), search = JabRefFrame.subMenu("Search"), bibtex = JabRefFrame.subMenu("BibTeX"), view = JabRefFrame.subMenu("View"), tools = JabRefFrame.subMenu("Tools"),
        //web = subMenu("Web search"),
        options = JabRefFrame.subMenu("Options"), newSpec = JabRefFrame.subMenu("New entry..."), helpMenu = JabRefFrame.subMenu("Help");

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
        file.add(saveSelectedAsPlain);
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
        JMenu markSpecific = JabRefFrame.subMenu("Mark specific color");
        for (int i = 0; i < EntryMarker.MAX_MARKING_LEVEL; i++) {
            markSpecific.add(new MarkEntriesAction(this, i).getMenuItem());
        }
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
        if (prefs.getBoolean(JabRefPreferences.WEB_SEARCH_VISIBLE)) {
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
        view.add(toggleToolbar);
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
         prefs.put(JabRefPreferences.FONT_FAMILY, GUIGlobals.CURRENTFONT.getFamily());
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
        helpMenu.add(forkMeOnGitHubAction);
        helpMenu.addSeparator();
        helpMenu.add(about);
        mb.add(helpMenu);
    }

    public static JMenu subMenu(String name) {
        name = Globals.menuTitle(name);
        int i = name.indexOf('&');
        JMenu res;
        if (i >= 0) {
            res = new JMenu(name.substring(0, i) + name.substring(i + 1));
            char mnemonic = Character.toUpperCase(name.charAt(i + 1));
            res.setMnemonic((int) mnemonic);
        } else {
            res = new JMenu(name);
        }

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
        if (!Globals.ON_MAC) {
            searchToggle.setMargin(marg);
        }
        tlb.add(searchToggle);

        previewToggle = new JToggleButton(togglePreview);
        previewToggle.setText(null);
        if (!Globals.ON_MAC) {
            previewToggle.setMargin(marg);
        }
        tlb.add(previewToggle);
        tlb.addSeparator();

        groupToggle = new JToggleButton(toggleGroups);
        groupToggle.setText(null);
        if (!Globals.ON_MAC) {
            groupToggle.setMargin(marg);
        }
        tlb.add(groupToggle);

        highlightAny = new JToggleButton(toggleHighlightAny);
        highlightAny.setText(null);
        if (!Globals.ON_MAC) {
            highlightAny.setMargin(marg);
        }
        tlb.add(highlightAny);
        highlightAll = new JToggleButton(toggleHighlightAll);
        highlightAll.setText(null);
        if (!Globals.ON_MAC) {
            highlightAll.setMargin(marg);
        }
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

            @Override
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


    private List<Object> openDatabaseOnlyActions = new LinkedList<Object>();
    private List<Object> severalDatabasesOnlyActions = new LinkedList<Object>();


    private void initActions() {
        openDatabaseOnlyActions = new LinkedList<Object>();
        openDatabaseOnlyActions.addAll(Arrays.asList(manageSelectors,
                mergeDatabaseAction, newSubDatabaseAction, close, save, saveAs, saveSelectedAs, saveSelectedAsPlain, undo,
                redo, cut, delete, copy, paste, mark, unmark, unmarkAll, editEntry,
                selectAll, copyKey, copyCiteKey, copyKeyAndTitle, editPreamble, editStrings, toggleGroups, toggleSearch,
                makeKeyAction, normalSearch,
                incrementalSearch, replaceAll, importMenu, exportMenu,
                /* openSpires wasn't being supported so no point in supporting
                 * openInspire */
                openPdf, openUrl, openFolder, openFile, openSpires, /*openInspire,*/togglePreview, dupliCheck, /*strictDupliCheck,*/highlightAll,
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

            @Override
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
    private static void setEnabled(List<Object> list, boolean enabled) {
        for (Object o : list) {
            if (o instanceof Action) {
                ((Action) o).setEnabled(enabled);
            }
            if (o instanceof Component) {
                ((Component) o).setEnabled(enabled);
            }
        }
    }


    private int previousTabCount = -1;


    /**
     * Enable or Disable all actions based on the number of open tabs.
     * 
     * The action that are affected are set in initActions.
     */
    private void updateEnabledState() {
        int tabCount = tabbedPane.getTabCount();
        if (tabCount != previousTabCount) {
            previousTabCount = tabCount;
            JabRefFrame.setEnabled(openDatabaseOnlyActions, tabCount > 0);
            JabRefFrame.setEnabled(severalDatabasesOnlyActions, tabCount > 1);
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
        if (metaData == null) {
            metaData = new MetaData();
        }
        if (encoding == null) {
            encoding = Globals.prefs.get(JabRefPreferences.DEFAULT_ENCODING);
        }

        BasePanel bp = new BasePanel(JabRefFrame.this, db, file, metaData, encoding);
        addTab(bp, file, raisePanel);
        return bp;
    }

    public void addTab(BasePanel bp, File file, boolean raisePanel) {
        String title;
        if (file == null) {
            title = Globals.lang(GUIGlobals.untitledTitle);
            if (!bp.database().getEntries().isEmpty()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                // This also happens internally at basepanel to ensure consistency
                title = title + '*';
            }
        } else {
            title = file.getName();
        }
        tabbedPane.add("<html><div style='padding:2px 5px;'>" + title + "</div></html>", bp);
        tabbedPane.setToolTipTextAt(tabbedPane.getTabCount() - 1,
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

        @Override
        public void actionPerformed(ActionEvent e) {
            KeyBindingsDialog d = new KeyBindingsDialog
                    (new HashMap<String, String>(prefs.getKeyBindings()),
                            prefs.getDefaultKeys());
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.pack(); //setSize(300,500);
            Util.placeDialog(d, JabRefFrame.this);
            d.setVisible(true);
            if (d.getAction()) {
                prefs.setNewKeyBindings(d.getNewKeyBindings());
                JOptionPane.showMessageDialog
                        (JabRefFrame.this,
                                Globals.lang("Your new key bindings have been stored.") + '\n'
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
    class CloseAction  extends MnemonicAwareAction {

        public CloseAction() {
            putValue(Action.NAME, "Quit");
            putValue(Action.SHORT_DESCRIPTION, Globals.lang("Quit JabRef"));
            putValue(Action.ACCELERATOR_KEY, prefs.getKey("Quit JabRef"));
            //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q,
            //    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            quit();
        }
    }


    // The action for closing the current database and leaving the window open.
    private final CloseDatabaseAction closeDatabaseAction = new CloseDatabaseAction();


    class CloseDatabaseAction extends MnemonicAwareAction {

        public CloseDatabaseAction() {
            super(GUIGlobals.getImage("close"));
            putValue(Action.NAME, "Close database");
            putValue(Action.SHORT_DESCRIPTION, Globals.lang("Close the current database"));
            putValue(Action.ACCELERATOR_KEY, prefs.getKey("Close database"));
        }

        @Override
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
                        if (saveAction.isCancelled() || !saveAction.isSuccess()) {
                            // The action either not cancelled or unsuccessful.
                            // Break! 
                            close = false;
                        }

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
            output(Globals.lang("Closed database") + '.');
            System.gc(); // Test
        }
    }

    // The action concerned with opening a new database.
    class NewDatabaseAction
            extends MnemonicAwareAction {

        public NewDatabaseAction() {
            super(GUIGlobals.getImage("new"));
            putValue(Action.NAME, "New database");
            putValue(Action.SHORT_DESCRIPTION, Globals.lang("New BibTeX database"));
            //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Create a new, empty, database.
            BibtexDatabase database = new BibtexDatabase();
            addTab(database, null, new MetaData(), Globals.prefs.get(JabRefPreferences.DEFAULT_ENCODING), true);
            output(Globals.lang("New database created."));
        }
    }

    // The action concerned with generate a new (sub-)database from latex aux file.
    class NewSubDatabaseAction extends MnemonicAwareAction
    {

        public NewSubDatabaseAction()
        {
            super(GUIGlobals.getImage("new"));
            putValue(Action.NAME, "New subdatabase based on AUX file");
            putValue(Action.SHORT_DESCRIPTION, Globals.lang("New BibTeX subdatabase"));
            //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            // Create a new, empty, database.

            FromAuxDialog dialog = new FromAuxDialog(JabRefFrame.this, "", true, JabRefFrame.this.tabbedPane);

            Util.placeDialog(dialog, JabRefFrame.this);
            dialog.setVisible(true);

            if (dialog.generatePressed())
            {
                BasePanel bp = new BasePanel(JabRefFrame.this,
                        dialog.getGenerateDB(), // database
                        null, // file
                        new MetaData(), Globals.prefs.get(JabRefPreferences.DEFAULT_ENCODING)); // meta data
                tabbedPane.add(Globals.lang(GUIGlobals.untitledTitle), bp);
                tabbedPane.setSelectedComponent(bp);
                output(Globals.lang("New database created."));
            }
        }
    }

    // The action should test the database and report errors/warnings
    class IntegrityCheckAction extends AbstractAction
    {

        public IntegrityCheckAction()
        {
            super(Globals.menuTitle("Integrity check"),
                    GUIGlobals.getImage("integrityCheck"));
            //putValue( SHORT_DESCRIPTION, "integrity" ) ;  //Globals.lang( "integrity" ) ) ;
            //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            Object selComp = tabbedPane.getSelectedComponent();
            if (selComp != null)
            {
                BasePanel bp = (BasePanel) selComp;
                BibtexDatabase refBase = bp.getDatabase();
                if (refBase != null)
                {
                    IntegrityWizard wizard = new IntegrityWizard(JabRefFrame.this, basePanel());
                    Util.placeDialog(wizard, JabRefFrame.this);
                    wizard.setVisible(true);

                }
            }
        }
    }


    // The action for opening the preferences dialog.
    private final AbstractAction showPrefs = new ShowPrefsAction();


    class ShowPrefsAction
            extends MnemonicAwareAction {

        public ShowPrefsAction() {
            super(GUIGlobals.getImage("preferences"));
            putValue(Action.NAME, "Preferences");
            putValue(Action.SHORT_DESCRIPTION, Globals.lang("Preferences"));
        }

        @Override
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
    private void addImportedEntries(final BasePanel panel, final List<BibtexEntry> entries,
                                    String filename, final boolean openInNew) {
        /*
         * Use the import inspection dialog if it is enabled in preferences, and
         * (there are more than one entry or the inspection dialog is also
         * enabled for single entries):
         */
        if (Globals.prefs.getBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG) &&
                (Globals.prefs.getBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG_FOR_SINGLE) || (entries.size() > 1))) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
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

                    @Override
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
    private int addBibEntries(List<BibtexEntry> bibentries, String filename,
                              boolean intoNew) {
        if ((bibentries == null) || (bibentries.isEmpty())) {

            // No entries found. We need a message for this.
            JOptionPane.showMessageDialog(JabRefFrame.this, Globals.lang("No entries found. Please make sure you are "
                    + "using the correct import filter."), Globals.lang("Import failed"),
                    JOptionPane.ERROR_MESSAGE);
            return 0;
        }

        int addedEntries = 0;

        // Set owner and timestamp fields:
        Util.setAutomaticFields(bibentries, Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER),
                Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP), Globals.prefs.getBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES));

        if (intoNew || (tabbedPane.getTabCount() == 0)) {
            // Import into new database.
            BibtexDatabase database = new BibtexDatabase();
            for (BibtexEntry entry : bibentries) {
                try {
                    entry.setId(IdGenerator.next());
                    database.insertEntry(entry);
                } catch (KeyCollisionException ex) {
                    //ignore
                    System.err.println("KeyCollisionException [ addBibEntries(...) ]");
                }
            }
            // Metadata are only put in bibtex files, so we will not find it
            // in imported files. We therefore pass in an empty MetaData:
            BasePanel bp = new BasePanel(JabRefFrame.this, database, null, new MetaData(), Globals.prefs.get(JabRefPreferences.DEFAULT_ENCODING));
            /*
                  if (prefs.getBoolean("autoComplete")) {
                  db.setCompleters(autoCompleters);
                  }
             */
            addedEntries = database.getEntryCount();
            tabbedPane.add(GUIGlobals.untitledTitle, bp);
            bp.markBaseChanged();
            tabbedPane.setSelectedComponent(bp);
            if (filename != null) {
                output(Globals.lang("Imported database") + " '" + filename + "' " +
                        Globals.lang("with") + ' ' +
                        database.getEntryCount() + ' ' +
                        Globals.lang("entries into new database") + '.');
            }
        }
        else {
            // Import into current database.
            BasePanel basePanel = basePanel();
            BibtexDatabase database = basePanel.database;
            int oldCount = database.getEntryCount();
            NamedCompound ce = new NamedCompound(Globals.lang("Import entries"));

            mainLoop: for (BibtexEntry entry : bibentries) {
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
                        entry.setId(IdGenerator.next());
                        database.insertEntry(entry);
                        ce.addEdit(new UndoableInsertEntry
                                (database, entry, basePanel));
                        addedEntries++;
                    } catch (KeyCollisionException ex) {
                        //ignore
                        System.err.println("KeyCollisionException [ addBibEntries(...) ]");
                    }
                }
            }
            if (addedEntries > 0) {
                ce.end();
                basePanel.undoManager.addEdit(ce);
                basePanel.markBaseChanged();
                if (filename != null) {
                    output(Globals.lang("Imported database") + " '" + filename + "' " +
                            Globals.lang("with") + ' ' +
                            (database.getEntryCount() - oldCount) + ' ' +
                            Globals.lang("entries into new database") + '.');
                }
            }

        }

        return addedEntries;
    }

    private void setUpImportMenu(JMenu importMenu, boolean intoNew_) {
        importMenu.removeAll();

        // Add a menu item for autodetecting import format:
        importMenu.add(new ImportMenuItem(JabRefFrame.this, intoNew_));

        // Add custom importers
        importMenu.addSeparator();

        SortedSet<ImportFormat> customImporters = Globals.importFormatReader.getCustomImportFormats();
        JMenu submenu = new JMenu(Globals.lang("Custom importers"));
        submenu.setMnemonic(KeyEvent.VK_S);

        // Put in all formatters registered in ImportFormatReader:
        for (ImportFormat imFo : customImporters) {
            submenu.add(new ImportMenuItem(JabRefFrame.this, intoNew_, imFo));
        }

        if (!customImporters.isEmpty()) {
            submenu.addSeparator();
        }

        submenu.add(customImpAction);

        importMenu.add(submenu);
        importMenu.addSeparator();

        // Put in all formatters registered in ImportFormatReader:
        for (ImportFormat imFo : Globals.importFormatReader.getBuiltInInputFormats()) {
            importMenu.add(new ImportMenuItem(JabRefFrame.this, intoNew_, imFo));
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
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            baseAt(i).setPreviewActive(enabled);
        }
    }

    public void removeCachedEntryEditors() {
        for (int j = 0; j < tabbedPane.getTabCount(); j++) {
            BasePanel bp = (BasePanel) tabbedPane.getComponentAt(j);
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
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setVisible(visible);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    progressBar.setVisible(visible);
                }
            });
        }
    }

    /**
     * Sets the current value of the progress bar.
     *
     * If not called on the event dispatch thread, this method uses
     * SwingUtilities.invokeLater() to do the actual operation on the EDT.
     */
    public void setProgressBarValue(final int value) {
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setValue(value);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    progressBar.setValue(value);
                }
            });
        }

    }

    /**
     * Sets the indeterminate status of the progress bar.
     *
     * If not called on the event dispatch thread, this method uses
     * SwingUtilities.invokeLater() to do the actual operation on the EDT.
     */
    public void setProgressBarIndeterminate(final boolean value) {
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setIndeterminate(value);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    progressBar.setIndeterminate(value);
                }
            });
        }

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
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setMaximum(value);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    progressBar.setMaximum(value);
                }
            });
        }

    }


    class SaveSessionAction
            extends MnemonicAwareAction {

        public SaveSessionAction() {
            super(GUIGlobals.getImage("save"));
            putValue(Action.NAME, "Save session");
            putValue(Action.ACCELERATOR_KEY, prefs.getKey("Save session"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Here we store the names of all current files. If
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
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    if (baseAt(i).getFile() != null) {
                        filenames.add(baseAt(i).getFile().getPath());
                    }
                }
            }

            if (filenames.isEmpty()) {
                output(Globals.lang("Not saved (empty session)") + '.');
            }
            else {
                String[] names = new String[filenames.size()];
                for (int i = 0; i < filenames.size(); i++) {
                    names[i] = filenames.elementAt(i);
                }
                prefs.putStringArray("savedSession", names);
                output(Globals.lang("Saved session") + '.');
            }

        }
    }

    class LoadSessionAction extends MnemonicAwareAction {

        volatile boolean running = false;

        public LoadSessionAction() {
            super(GUIGlobals.getImage("loadSession"));
            putValue(Action.NAME, "Load session");
            putValue(Action.ACCELERATOR_KEY, prefs.getKey("Load session"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (prefs.get("savedSession") == null) {
                output(Globals.lang("No saved session found."));
                return;
            }
            if (running) {
                return;
            } else {
                running = true;
            }

            output(Globals.lang("Loading session..."));
            JabRefExecutorService.INSTANCE.execute(new Runnable() {

                @Override
                public void run() {
                    HashSet<String> currentFiles = new HashSet<String>();
                    if (tabbedPane.getTabCount() > 0) {
                        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                            if (baseAt(i).getFile() != null) {
                                currentFiles.add(baseAt(i).getFile().getPath());
                            }
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
            });

        }
    }

    class ChangeTabAction extends MnemonicAwareAction {

        private final boolean next;


        public ChangeTabAction(boolean next) {
            putValue(Action.NAME, next ? "Next tab" : "Previous tab");
            this.next = next;
            //Util.pr(""+prefs.getKey("Next tab"));
            putValue(Action.ACCELERATOR_KEY,
                    (next ? prefs.getKey("Next tab") : prefs.getKey("Previous tab")));
        }

        @Override
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
    class EditAction extends MnemonicAwareAction {

        private final String command;

        public EditAction(String command, URL icon) {
            super(new ImageIcon(icon));
            this.command = command;
            String nName = StringUtil.nCase(command);
            putValue(Action.NAME, nName);
            putValue(Action.ACCELERATOR_KEY, prefs.getKey(nName));
            putValue(Action.SHORT_DESCRIPTION, Globals.lang(nName));
            //putValue(ACCELERATOR_KEY,
            //         (next?prefs.getKey("Next tab"):prefs.getKey("Previous tab")));
        }

        @Override
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
            putValue(Action.NAME, "Manage custom exports");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ExportCustomizationDialog ecd = new ExportCustomizationDialog(JabRefFrame.this);
            ecd.setVisible(true);
        }
    }

    class CustomizeImportsAction extends MnemonicAwareAction {

        public CustomizeImportsAction() {
            putValue(Action.NAME, "Manage custom imports");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ImportCustomizationDialog ecd = new ImportCustomizationDialog(JabRefFrame.this);
            ecd.setVisible(true);
        }
    }

    class CustomizeEntryTypeAction extends MnemonicAwareAction {

        public CustomizeEntryTypeAction() {
            putValue(Action.NAME, "Customize entry types");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JDialog dl = new EntryCustomizationDialog2(JabRefFrame.this);
            Util.placeDialog(dl, JabRefFrame.this);
            dl.setVisible(true);
        }
    }

    class GenFieldsCustomizationAction extends MnemonicAwareAction {

        public GenFieldsCustomizationAction() {
            putValue(Action.NAME, "Set up general fields");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GenFieldsCustomizer gf = new GenFieldsCustomizer(JabRefFrame.this);
            Util.placeDialog(gf, JabRefFrame.this);
            gf.setVisible(true);

        }
    }

    class DatabasePropertiesAction extends MnemonicAwareAction {

        DatabasePropertiesDialog propertiesDialog = null;


        public DatabasePropertiesAction() {
            putValue(Action.NAME, "Database properties");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (propertiesDialog == null) {
                propertiesDialog = new DatabasePropertiesDialog(JabRefFrame.this);
            }
            propertiesDialog.setPanel(basePanel());
            Util.placeDialog(propertiesDialog, JabRefFrame.this);
            propertiesDialog.setVisible(true);
        }

    }

    class BibtexKeyPatternAction extends MnemonicAwareAction {

        BibtexKeyPatternDialog bibtexKeyPatternDialog = null;


        public BibtexKeyPatternAction() {
            putValue(Action.NAME, "Bibtex key patterns");
        }

        @Override
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
            putValue(Action.NAME, "Increase table font size");
            putValue(Action.ACCELERATOR_KEY, Globals.prefs.getKey("Increase table font size"));
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            int currentSize = GUIGlobals.CURRENTFONT.getSize();
            GUIGlobals.CURRENTFONT = new Font(GUIGlobals.CURRENTFONT.getFamily(), GUIGlobals.CURRENTFONT.getStyle(),
                    currentSize + 1);
            Globals.prefs.putInt(JabRefPreferences.FONT_SIZE, currentSize + 1);
            for (int i = 0; i < baseCount(); i++) {
                baseAt(i).updateTableFont();
            }
        }
    }

    class DecreaseTableFontSizeAction extends MnemonicAwareAction {

        public DecreaseTableFontSizeAction() {
            putValue(Action.NAME, "Decrease table font size");
            putValue(Action.ACCELERATOR_KEY, Globals.prefs.getKey("Decrease table font size"));
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            int currentSize = GUIGlobals.CURRENTFONT.getSize();
            if (currentSize < 2) {
                return;
            }
            GUIGlobals.CURRENTFONT = new Font(GUIGlobals.CURRENTFONT.getFamily(), GUIGlobals.CURRENTFONT.getStyle(),
                    currentSize - 1);
            Globals.prefs.putInt(JabRefPreferences.FONT_SIZE, currentSize - 1);
            for (int i = 0; i < baseCount(); i++) {
                baseAt(i).updateTableFont();
            }
        }
    }

    class MinimizeToSysTrayAction extends MnemonicAwareAction {

        public MinimizeToSysTrayAction() {
            putValue(Action.NAME, "Minimize to system tray");
            putValue(Action.ACCELERATOR_KEY, Globals.prefs.getKey("Minimize to system tray"));
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            if (sysTray == null) {
                sysTray = new SysTray(JabRefFrame.this);
            }
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    sysTray.show();
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
                sysTray.hide();
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

    private static class MyGlassPane extends JPanel {

        //ForegroundLabel infoLabel = new ForegroundLabel("Showing search");
        public MyGlassPane() {
            addKeyListener(new KeyAdapter() {
            });
            addMouseListener(new MouseAdapter() {
            });
            /*  infoLabel.setForeground(new Color(255, 100, 100, 124));

              setLayout(new BorderLayout());
              add(infoLabel, BorderLayout.CENTER);*/
            super.setCursor(
                    Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        // Override isOpaque() to prevent the glasspane from hiding the window contents:
        @Override
        public boolean isOpaque() {
            return false;
        }
    }


    @Override
    public void showMessage(Object message, String title, int msgType) {
        JOptionPane.showMessageDialog(this, message, title, msgType);
    }

    @Override
    public void setStatus(String s) {
        output(s);
    }

    @Override
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public SearchManager2 getSearchManager() {
        return searchManager;
    }

}
