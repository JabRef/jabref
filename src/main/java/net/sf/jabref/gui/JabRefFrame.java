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
package net.sf.jabref.gui;

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

import net.sf.jabref.*;
import net.sf.jabref.gui.actions.*;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.worker.MarkEntriesAction;
import net.sf.jabref.gui.preftabs.PreferencesDialog;
import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fetcher.GeneralFetcher;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.bibtex.DuplicateCheck;
import net.sf.jabref.logic.id.IdGenerator;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.exporter.AutoSaveManager;
import net.sf.jabref.exporter.ExportCustomizationDialog;
import net.sf.jabref.exporter.ExportFormats;
import net.sf.jabref.exporter.SaveAllAction;
import net.sf.jabref.exporter.SaveDatabaseAction;
import net.sf.jabref.external.ExternalFileTypeEditor;
import net.sf.jabref.external.PushToApplicationButton;
import net.sf.jabref.groups.EntryTableTransferHandler;
import net.sf.jabref.groups.GroupSelector;
import net.sf.jabref.gui.menus.help.ForkMeOnGitHubAction;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpDialog;
import net.sf.jabref.gui.journals.ManageJournalsAction;
import net.sf.jabref.openoffice.OpenOfficePanel;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;
import net.sf.jabref.specialfields.SpecialFieldsUtils;
import net.sf.jabref.sql.importer.DbImportAction;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.util.ManageKeywordsAction;
import net.sf.jabref.util.MassSetFieldAction;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.util.Util;

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;
import osx.macadapter.MacAdapter;

/**
 * The main window of the application.
 */
public class JabRefFrame extends JFrame implements OutputPrinter {
    private static final long serialVersionUID = 1L;
    private static final Log LOGGER = LogFactory.getLog(JabRefFrame.class);

    final JSplitPane contentPane = new JSplitPane();

    final JabRefPreferences prefs = Globals.prefs;
    private PreferencesDialog prefsDialog;

    private int lastTabbedPanelSelectionIndex = -1;

    // The sidepane manager takes care of populating the sidepane. 
    public SidePaneManager sidePaneManager;

    public JTabbedPane tabbedPane; // initialized at constructor

    private final Insets marg = new Insets(1, 0, 2, 0);
    private final JabRef jabRef;

    class ToolBar extends JToolBar {

        private static final long serialVersionUID = 1L;

        void addAction(Action a) {
            JButton b = new JButton(a);
            b.setText(null);
            if (!OS.OS_X) {
                b.setMargin(marg);
            }
            add(b);
        }
    }


    final ToolBar tlb = new ToolBar();

    private final JMenuBar mb = new JMenuBar();

    private final GridBagLayout gbl = new GridBagLayout();
    private final GridBagConstraints con = new GridBagConstraints();

    final JLabel statusLine = new JLabel("", SwingConstants.LEFT);
    private final JLabel statusLabel = new JLabel(
            Localization.lang("Status")
                    + ':', SwingConstants.LEFT);
    private final JProgressBar progressBar = new JProgressBar();

    private final FileHistory fileHistory = new FileHistory(prefs, this);

    private SysTray sysTray;

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
    public JToggleButton searchToggle;
    public JToggleButton previewToggle;
    public JToggleButton highlightAny;
    public JToggleButton highlightAll;

    final OpenDatabaseAction open = new OpenDatabaseAction(this, true);
    private final AbstractAction
            close = new CloseDatabaseAction();
    private final AbstractAction quit = new CloseAction();
    private final AbstractAction selectKeys = new SelectKeysAction();
    private final AbstractAction newDatabaseAction = new NewDatabaseAction(this);
    private final AbstractAction newSubDatabaseAction = new NewSubDatabaseAction(this);
    private final AbstractAction integrityCheckAction = new IntegrityCheckAction(this);
    private final AbstractAction forkMeOnGitHubAction = new ForkMeOnGitHubAction();
    private final AbstractAction help = new HelpAction("JabRef help", helpDiag,
            GUIGlobals.baseFrameHelp, Localization.lang("JabRef help"),
            prefs.getKey("Help"));
    private final AbstractAction contents = new HelpAction("Help contents", helpDiag,
            GUIGlobals.helpContents, Localization.lang("Help contents"),
            IconTheme.getImage("helpContents"));
    private final AbstractAction about = new HelpAction("About JabRef", helpDiag,
            GUIGlobals.aboutPage, Localization.lang("About JabRef"),
            IconTheme.getImage("about"));
    private final AbstractAction editEntry = new GeneralAction(Actions.EDIT, "Edit entry",
            Localization.lang("Edit entry"),
            prefs.getKey(KeyBinds.EDIT_ENTRY),
            IconTheme.getImage("edit"));
    private final AbstractAction focusTable = new GeneralAction(Actions.FOCUS_TABLE, "Focus entry table",
            Localization.lang("Move the keyboard focus to the entry table"),
            prefs.getKey(KeyBinds.FOCUS_ENTRY_TABLE));
    private final AbstractAction save = new GeneralAction(Actions.SAVE, "Save database",
            Localization.lang("Save database"),
            prefs.getKey(KeyBinds.SAVE_DATABASE),
            IconTheme.getImage("save"));
    private final AbstractAction saveAs = new GeneralAction(Actions.SAVE_AS, "Save database as ...",
            Localization.lang("Save database as ..."),
            prefs.getKey(KeyBinds.SAVE_DATABASE_AS),
            IconTheme.getImage("saveAs"));
    private final AbstractAction saveAll = new SaveAllAction(JabRefFrame.this);
    private final AbstractAction saveSelectedAs = new GeneralAction(Actions.SAVE_SELECTED_AS,
            "Save selected as ...",
            Localization.lang("Save selected as ..."),
            IconTheme.getImage("saveAs"));
    private final AbstractAction saveSelectedAsPlain = new GeneralAction(Actions.SAVE_SELECTED_AS_PLAIN,
            "Save selected as plain BibTeX ...",
            Localization.lang("Save selected as plain BibTeX ..."),
            IconTheme.getImage("saveAs"));
    private final AbstractAction exportAll = ExportFormats.getExportAction(this, false);
    private final AbstractAction exportSelected = ExportFormats.getExportAction(this, true);
    private final AbstractAction importCurrent = ImportFormats.getImportAction(this, false);
    private final AbstractAction importNew = ImportFormats.getImportAction(this, true);
    public final AbstractAction nextTab = new ChangeTabAction(true);
    public final AbstractAction prevTab = new ChangeTabAction(false);
    private final AbstractAction sortTabs = new SortTabsAction(this);
    private final AbstractAction undo = new GeneralAction(Actions.UNDO, "Undo", Localization.lang("Undo"),
            prefs.getKey(KeyBinds.UNDO),
            IconTheme.getImage("undo"));
    private final AbstractAction redo = new GeneralAction(Actions.REDO, "Redo", Localization.lang("Redo"),
            prefs.getKey(KeyBinds.REDO),
            IconTheme.getImage("redo"));
    final AbstractAction forward = new GeneralAction(Actions.FORWARD, "Forward", Localization.lang("Forward"),
            prefs.getKey(KeyBinds.FORWARD), IconTheme.getImage("right"));
    final AbstractAction back = new GeneralAction(Actions.BACK, "Back", Localization.lang("Back"),
            prefs.getKey(KeyBinds.BACK), IconTheme.getImage("left"));
    private final AbstractAction delete = new GeneralAction(Actions.DELETE, "Delete", Localization.lang("Delete"),
            prefs.getKey(KeyBinds.DELETE),
            IconTheme.getImage("delete"));
    private final AbstractAction copy = new EditAction(Actions.COPY, IconTheme.getImage("copy"));
    private final AbstractAction paste = new EditAction(Actions.PASTE, IconTheme.getImage("paste"));
    private final AbstractAction cut = new EditAction(Actions.CUT, IconTheme.getImage("cut"));
    private final AbstractAction mark = new GeneralAction(Actions.MARK_ENTRIES, "Mark entries",
            Localization.lang("Mark entries"),
            prefs.getKey(KeyBinds.MARK_ENTRIES),
            IconTheme.getImage("markEntries"));
    private final AbstractAction unmark = new GeneralAction(Actions.UNMARK_ENTRIES, "Unmark entries",
            Localization.lang("Unmark entries"),
            prefs.getKey(KeyBinds.UNMARK_ENTRIES),
            IconTheme.getImage("unmarkEntries"));
    private final AbstractAction unmarkAll = new GeneralAction(Actions.UNMARK_ALL, "Unmark all");
    private final AbstractAction toggleRelevance = new GeneralAction(
            Relevance.getInstance().getValues().get(0).getActionName(),
            Relevance.getInstance().getValues().get(0).getMenuString(),
            Relevance.getInstance().getValues().get(0).getToolTipText(),
            IconTheme.getImage(Relevance.getInstance().getValues().get(0).getActionName()));
    private final AbstractAction toggleQualityAssured = new GeneralAction(
            Quality.getInstance().getValues().get(0).getActionName(),
            Quality.getInstance().getValues().get(0).getMenuString(),
            Quality.getInstance().getValues().get(0).getToolTipText(),
            IconTheme.getImage(Quality.getInstance().getValues().get(0).getActionName()));
    private final AbstractAction togglePrinted = new GeneralAction(
            Printed.getInstance().getValues().get(0).getActionName(),
            Printed.getInstance().getValues().get(0).getMenuString(),
            Printed.getInstance().getValues().get(0).getToolTipText(),
            IconTheme.getImage(Printed.getInstance().getValues().get(0).getActionName()));
    private final AbstractAction manageSelectors = new GeneralAction(Actions.MANAGE_SELECTORS, "Manage content selectors");
    private final AbstractAction saveSessionAction = new SaveSessionAction();
    public final AbstractAction loadSessionAction = new LoadSessionAction();
    private final AbstractAction incrementalSearch = new GeneralAction(Actions.INC_SEARCH, "Incremental search",
            Localization.lang("Start incremental search"),
            prefs.getKey(KeyBinds.INCREMENTAL_SEARCH),
            IconTheme.getImage("incSearch"));
    private final AbstractAction normalSearch = new GeneralAction(Actions.SEARCH, "Search", Localization.lang("Search"),
            prefs.getKey(KeyBinds.SEARCH),
            IconTheme.getImage("search"));
    private final AbstractAction toggleSearch = new GeneralAction(Actions.TOGGLE_SEARCH, "Search",
            Localization.lang("Toggle search panel"),IconTheme.getImage("toggleSearch"));

    private final AbstractAction copyKey = new GeneralAction(Actions.COPY_KEY, "Copy BibTeX key",
            prefs.getKey(KeyBinds.COPY_BIB_TE_X_KEY));
    private final AbstractAction//"Put a BibTeX reference to the selected entries on the clipboard",
            copyCiteKey = new GeneralAction(Actions.COPY_CITE_KEY, "Copy \\cite{BibTeX key}",
            //"Put a BibTeX reference to the selected entries on the clipboard",
            prefs.getKey(KeyBinds.COPY_CITE_BIB_TE_X_KEY));
    private final AbstractAction copyKeyAndTitle = new GeneralAction(Actions.COPY_KEY_AND_TITLE,
            "Copy BibTeX key and title",
            prefs.getKey(KeyBinds.COPY_BIB_TE_X_KEY_AND_TITLE));
    private final AbstractAction mergeDatabaseAction = new GeneralAction(Actions.MERGE_DATABASE,
            "Append database",
            Localization.lang("Append contents from a BibTeX database into the currently viewed database"),
            IconTheme.getImage("open"));
    private final AbstractAction selectAll = new GeneralAction(Actions.SELECT_ALL, "Select all",
            prefs.getKey(KeyBinds.SELECT_ALL));
    private final AbstractAction replaceAll = new GeneralAction(Actions.REPLACE_ALL, "Replace string",
            prefs.getKey(KeyBinds.REPLACE_STRING));

    private final AbstractAction editPreamble = new GeneralAction(Actions.EDIT_PREAMBLE, "Edit preamble",
            Localization.lang("Edit preamble"),
            prefs.getKey(KeyBinds.EDIT_PREAMBLE),
            IconTheme.getImage("editPreamble"));
    private final AbstractAction editStrings = new GeneralAction(Actions.EDIT_STRINGS, "Edit strings",
            Localization.lang("Edit strings"),
            prefs.getKey(KeyBinds.EDIT_STRINGS),
            IconTheme.getImage("editStrings"));
    private final AbstractAction toggleToolbar = new AbstractAction("Hide/show toolbar") {
        {
            putValue(Action.ACCELERATOR_KEY, prefs.getKey(KeyBinds.HIDE_SHOW_TOOLBAR));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Hide/show toolbar"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            tlb.setVisible(!tlb.isVisible());
        }};
    private final AbstractAction toggleGroups = new GeneralAction(Actions.TOGGLE_GROUPS,
            "Toggle groups interface",
            Localization.lang("Toggle groups interface"),
            prefs.getKey(KeyBinds.TOGGLE_GROUPS_INTERFACE),
            IconTheme.getImage("toggleGroups"));
    private final AbstractAction togglePreview = new GeneralAction(Actions.TOGGLE_PREVIEW,
            "Toggle entry preview",
            Localization.lang("Toggle entry preview"),
            prefs.getKey(KeyBinds.TOGGLE_ENTRY_PREVIEW),
            IconTheme.getImage("togglePreview"));
    private final AbstractAction toggleHighlightAny = new GeneralAction(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ANY,
            "Highlight groups matching any selected entry",
            Localization.lang("Highlight groups matching any selected entry"),
            IconTheme.getImage("groupsHighlightAny"));
    private final AbstractAction toggleHighlightAll = new GeneralAction(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ALL,
            "Highlight groups matching all selected entries",
            Localization.lang("Highlight groups matching all selected entries"),
            IconTheme.getImage("groupsHighlightAll"));
    final AbstractAction switchPreview = new GeneralAction(Actions.SWITCH_PREVIEW,
            "Switch preview layout",
            prefs.getKey(KeyBinds.SWITCH_PREVIEW_LAYOUT));
    private final AbstractAction makeKeyAction = new GeneralAction(Actions.MAKE_KEY, "Autogenerate BibTeX keys",
            Localization.lang("Autogenerate BibTeX keys"),
            prefs.getKey(KeyBinds.AUTOGENERATE_BIB_TE_X_KEYS),
            IconTheme.getImage("makeKey"));

    private final AbstractAction writeXmpAction = new GeneralAction(Actions.WRITE_XMP, "Write XMP-metadata to PDFs",
            Localization.lang("Will write XMP-metadata to the PDFs linked from selected entries."),
            prefs.getKey(KeyBinds.WRITE_XMP));

    private final AbstractAction openFolder = new GeneralAction(Actions.OPEN_FOLDER, "Open folder",
            Localization.lang("Open folder"),
            prefs.getKey(KeyBinds.OPEN_FOLDER));
    private final AbstractAction openFile = new GeneralAction(Actions.OPEN_EXTERNAL_FILE, "Open file",
            Localization.lang("Open file"),
            prefs.getKey(KeyBinds.OPEN_FILE),
            IconTheme.getImage("openExternalFile"));
    private final AbstractAction openPdf = new GeneralAction(Actions.OPEN_FILE, "Open PDF or PS",
            Localization.lang("Open PDF or PS"),
            prefs.getKey(KeyBinds.OPEN_PDF_OR_PS),
            IconTheme.getImage("openFile"));
    private final AbstractAction openUrl = new GeneralAction(Actions.OPEN_URL, "Open URL or DOI",
            Localization.lang("Open URL or DOI"),
            prefs.getKey(KeyBinds.OPEN_URL_OR_DOI),
            IconTheme.getImage("openUrl"));
    private final AbstractAction openSpires = new GeneralAction(Actions.OPEN_SPIRES, "Open SPIRES entry",
            Localization.lang("Open SPIRES entry"),
            prefs.getKey(KeyBinds.OPEN_SPIRES_ENTRY));
    private final AbstractAction dupliCheck = new GeneralAction(Actions.DUPLI_CHECK, "Find duplicates");
    private final AbstractAction plainTextImport = new GeneralAction(Actions.PLAIN_TEXT_IMPORT,
            "New entry from plain text",
            prefs.getKey(KeyBinds.NEW_FROM_PLAIN_TEXT));

    private final AbstractAction customExpAction = new CustomizeExportsAction();
    private final AbstractAction customImpAction = new CustomizeImportsAction();
    private final AbstractAction customFileTypesAction = ExternalFileTypeEditor.getAction(this);
    AbstractAction exportToClipboard = new GeneralAction("exportToClipboard", "Export selected entries to clipboard");
    private final AbstractAction autoSetPdf = new GeneralAction(Actions.AUTO_SET_PDF,
            "Synchronize PDF links",
            prefs.getKey(KeyBinds.SYNCHRONIZE_PDF));
    private final AbstractAction autoSetPs = new GeneralAction(Actions.AUTO_SET_PS,
            "Synchronize PS links",
            prefs.getKey(KeyBinds.SYNCHRONIZE_PS));
    private final AbstractAction autoSetFile = new GeneralAction(Actions.AUTO_SET_FILE,
            Localization.lang("Synchronize file links"),
            Globals.prefs.getKey(KeyBinds.SYNCHRONIZE_FILES));

    private final AbstractAction abbreviateMedline = new GeneralAction(Actions.ABBREVIATE_MEDLINE, "Abbreviate journal names (MEDLINE)",
            Localization.lang("Abbreviate journal names of the selected entries (MEDLINE abbreviation)"));
    private final AbstractAction abbreviateIso = new GeneralAction(Actions.ABBREVIATE_ISO, "Abbreviate journal names (ISO)",
            Localization.lang("Abbreviate journal names of the selected entries (ISO abbreviation)"),
            Globals.prefs.getKey(KeyBinds.ABBREVIATE));

    private final AbstractAction unabbreviate = new GeneralAction(Actions.UNABBREVIATE, "Unabbreviate journal names",
            Localization.lang("Unabbreviate journal names of the selected entries"),
            Globals.prefs.getKey(KeyBinds.UNABBREVIATE));
    private final AbstractAction manageJournals = new ManageJournalsAction(this);
    private final AbstractAction databaseProperties = new DatabasePropertiesAction();
    private final AbstractAction bibtexKeyPattern = new BibtexKeyPatternAction();
    private final AbstractAction errorConsole = new ErrorConsoleAction(this, Globals.streamEavesdropper, Globals.handler);

    private final AbstractAction dbConnect = new GeneralAction(Actions.DB_CONNECT, "Connect to external SQL database",
            Localization.lang("Connect to external SQL database"),
            IconTheme.getImage("dbConnect"));

    private final AbstractAction dbExport = new GeneralAction(Actions.DB_EXPORT, "Export to external SQL database",
            Localization.lang("Export to external SQL database"),
            IconTheme.getImage("dbExport"));

    private final AbstractAction Cleanup = new GeneralAction(Actions.CLEANUP, "Cleanup entries",
            Localization.lang("Cleanup entries"),
            prefs.getKey(KeyBinds.CLEANUP),
            IconTheme.getImage("cleanupentries"));

    private final AbstractAction mergeEntries = new GeneralAction(Actions.MERGE_ENTRIES, "Merge entries",
            Localization.lang("Merge entries"),
            IconTheme.getImage("mergeentries"));

    private final AbstractAction dbImport = new DbImportAction(this).getAction();
    private final AbstractAction increaseFontSize = new IncreaseTableFontSizeAction();
    private final AbstractAction decreseFontSize = new DecreaseTableFontSizeAction();
    private final AbstractAction resolveDuplicateKeys = new GeneralAction(Actions.RESOLVE_DUPLICATE_KEYS, "Resolve duplicate BibTeX keys",
            Localization.lang("Find and remove duplicate BibTeX keys"),
            prefs.getKey(KeyBinds.RESOLVE_DUPLICATE_BIB_TE_X_KEYS));

    final MassSetFieldAction massSetField = new MassSetFieldAction(this);
    final ManageKeywordsAction manageKeywords = new ManageKeywordsAction(this);

    private final GeneralAction findUnlinkedFiles = new GeneralAction(
            FindUnlinkedFilesDialog.ACTION_COMMAND,
            FindUnlinkedFilesDialog.ACTION_MENU_TITLE,
            Localization.lang(FindUnlinkedFilesDialog.ACTION_SHORT_DESCRIPTION),
            prefs.getKey(FindUnlinkedFilesDialog.ACTION_KEYBINDING_ACTION),
            IconTheme.getImage("toggleSearch")
    );

    private final AutoLinkFilesAction autoLinkFile = new AutoLinkFilesAction();

    private PushToApplicationButton pushExternalButton;

    private final List<Action> fetcherActions = new LinkedList<>();

    private SearchManager searchManager;

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
    private final NewEntryAction newEntryAction = new NewEntryAction(this, prefs.getKey("New entry"));
    private final NewEntryAction[] newSpecificEntryAction = new NewEntryAction[]
            {
                    new NewEntryAction(this, "article", prefs.getKey("New article")),
                    new NewEntryAction(this, "book", prefs.getKey("New book")),
                    new NewEntryAction(this, "phdthesis", prefs.getKey("New phdthesis")),
                    new NewEntryAction(this, "inbook", prefs.getKey("New inbook")),
                    new NewEntryAction(this, "mastersthesis", prefs.getKey("New mastersthesis")),
                    new NewEntryAction(this, "proceedings", prefs.getKey("New proceedings")),
                    new NewEntryAction(this, "inproceedings"),
                    new NewEntryAction(this, "conference"),
                    new NewEntryAction(this, "incollection"),
                    new NewEntryAction(this, "booklet"),
                    new NewEntryAction(this, "manual"),
                    new NewEntryAction(this, "techreport"),
                    new NewEntryAction(this, "unpublished",
                            prefs.getKey("New unpublished")),
                    new NewEntryAction(this, "misc"),
                    new NewEntryAction(this, "other")
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
        setIconImage(IconTheme.getImage("jabrefIcon48").getImage());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                if (OS.OS_X) {
                    setState(Frame.ICONIFIED);
                } else {
                    new CloseAction().actionPerformed(null);
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

                if (posX + sizeX > width) {
                    if (sizeX <= width) {
                        posX = width - sizeX;
                    } else {
                        posX = prefs.getIntDefault(JabRefPreferences.POS_X);
                        sizeX = prefs.getIntDefault(JabRefPreferences.SIZE_X);
                    }
                }

                if (posY + sizeY > height) {
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
        if (OS.OS_X) {
            try {
                new MacAdapter().registerMacEvents(this);
            } catch (Exception e) {
                LOGGER.fatal("could not interface with Mac OS X methods", e);
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
            setTitle(GUIGlobals.frameTitle + " - " + Localization.lang("untitled") + star);
        }
    }

    private void initSidePane() {
        sidePaneManager = new SidePaneManager(this);

        GUIGlobals.sidePaneManager = this.sidePaneManager;
        GUIGlobals.helpDiag = this.helpDiag;

        groupSelector = new GroupSelector(this, sidePaneManager);
        searchManager = new SearchManager(this, sidePaneManager);

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
            if (bp.getFile() != null && bp.getFile().equals(file)) {
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
        JDialog about = new JDialog(JabRefFrame.this, Localization.lang("About JabRef"),
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
                        } catch (IOException ignored) {
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
                output(Localization.lang("Opening preferences..."));
                if (prefsDialog == null) {
                    prefsDialog = new PreferencesDialog(JabRefFrame.this, jabRef);
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
     * <p>
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
        prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, getExtendedState() == Frame.MAXIMIZED_BOTH);

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
            } else {
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
            searchManager.updatePrefs();
        }

        prefs.flush();

        // hide systray because the JVM can only shut down when no systray icon is shown
        if (sysTray != null) {
            sysTray.hide();
        }

        // dispose all windows, even if they are not displayed anymore
        for (Window window : Window.getWindows()) {
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
     * <p>
     * Non-OSX JabRef calls this when choosing "Quit" from the menu
     * <p>
     * SIDE EFFECT: tears down JabRef
     *
     * @return true if the user chose to quit; false otherwise
     */
    public boolean quit() {
        // Ask here if the user really wants to close, if the base
        // has not been saved since last save.
        boolean close = true;
        Vector<String> filenames = new Vector<>();
        if (tabbedPane.getTabCount() > 0) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (baseAt(i).isBaseChanged()) {
                    tabbedPane.setSelectedIndex(i);
                    int answer = JOptionPane.showConfirmDialog
                            (JabRefFrame.this, Localization.lang
                                            ("Database has changed. Do you "
                                                    + "want to save before closing?"),
                                    Localization.lang("Save before closing"),
                                    JOptionPane.YES_NO_CANCEL_OPTION);

                    if (answer == JOptionPane.CANCEL_OPTION ||
                            answer == JOptionPane.CLOSED_OPTION) {
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
                                output(Localization.lang("Unable to save database"));
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
                    if (w.cancelled()) {
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
     *
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
    private void markActiveBasePanel() {
        int now = tabbedPane.getSelectedIndex();
        int len = tabbedPane.getTabCount();
        if (lastTabbedPanelSelectionIndex > -1 && lastTabbedPanelSelectionIndex < len) {
            tabbedPane.setForegroundAt(lastTabbedPanelSelectionIndex, GUIGlobals.inActiveTabbed);
        }
        if (now > -1 && now < len) {
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


    class GeneralAction extends MnemonicAwareAction {
        private final Log LOGGER = LogFactory.getLog(JabRefFrame.class);

        private final String command;

        public GeneralAction(String command, String text) {
            this.command = command;
            putValue(Action.NAME, text);
        }

        public GeneralAction(String command, String text, String description) {
            this.command = command;
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, description);
        }

        public GeneralAction(String command, String text, String description, ImageIcon icon) {
            super(icon);

            this.command = command;
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, description);
        }

        public GeneralAction(String command, String text, KeyStroke key) {
            this.command = command;
            putValue(Action.NAME, text);
            putValue(Action.ACCELERATOR_KEY, key);
        }

        public GeneralAction(String command, String text, String description, KeyStroke key) {
            this.command = command;
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, description);
            putValue(Action.ACCELERATOR_KEY, key);
        }

        public GeneralAction(String command, String text, String description, KeyStroke key, ImageIcon icon) {
            super(icon);

            this.command = command;
            putValue(Action.NAME, text);
            putValue(Action.SHORT_DESCRIPTION, description);
            putValue(Action.ACCELERATOR_KEY, key);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (tabbedPane.getTabCount() > 0) {
                try {
                    ((BasePanel) tabbedPane.getSelectedComponent())
                            .runCommand(command);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            } else {
                    LOGGER.info("Action '" + command + "' must be disabled when no database is open.");
            }
        }
    }

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
        JMenu file = JabRefFrame.subMenu("File");
        JMenu sessions = JabRefFrame.subMenu("Sessions");
        JMenu edit = JabRefFrame.subMenu("Edit");
        JMenu search = JabRefFrame.subMenu("Search");
        JMenu bibtex = JabRefFrame.subMenu("BibTeX");
        JMenu view = JabRefFrame.subMenu("View");
        JMenu tools = JabRefFrame.subMenu("Tools");
        JMenu options = JabRefFrame.subMenu("Options");
        JMenu newSpec = JabRefFrame.subMenu("New entry...");
        JMenu helpMenu = JabRefFrame.subMenu("Help");

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
        GeneralFetcher generalFetcher = new GeneralFetcher(sidePaneManager, this);
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
        name = Localization.menuTitle(name);
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
                List<BibtexEntry> entries = new ArrayList<>(pr.getDatabase().getEntries());
                addImportedEntries(panel, entries, "", false);
            }
        } else {
            addTab(pr.getDatabase(), pr.getFile(), pr.getMetaData(), pr.getEncoding(), raisePanel);
        }
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
        if (!OS.OS_X) {
            searchToggle.setMargin(marg);
        }
        tlb.add(searchToggle);

        previewToggle = new JToggleButton(togglePreview);
        previewToggle.setText(null);
        if (!OS.OS_X) {
            previewToggle.setMargin(marg);
        }
        tlb.add(previewToggle);
        tlb.addSeparator();

        groupToggle = new JToggleButton(toggleGroups);
        groupToggle.setText(null);
        if (!OS.OS_X) {
            groupToggle.setMargin(marg);
        }
        tlb.add(groupToggle);

        highlightAny = new JToggleButton(toggleHighlightAny);
        highlightAny.setText(null);
        if (!OS.OS_X) {
            highlightAny.setMargin(marg);
        }
        tlb.add(highlightAny);
        highlightAll = new JToggleButton(toggleHighlightAll);
        highlightAll.setText(null);
        if (!OS.OS_X) {
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


    private List<Object> openDatabaseOnlyActions = new LinkedList<>();
    private List<Object> severalDatabasesOnlyActions = new LinkedList<>();


    private void initActions() {
        openDatabaseOnlyActions = new LinkedList<>();
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

        severalDatabasesOnlyActions = new LinkedList<>();
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
     *
     * @param list    List that should contain Actions and Components.
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
     * <p>
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
            title = Localization.lang(GUIGlobals.untitledTitle);
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
        // idea: "<html><div style='padding:2px 5px;'>" + title + "</div></html>" instead of "title" to get some space around.
        // However, this causes https://sourceforge.net/p/jabref/bugs/1293/
        // Therefore, plain "title" is used
        tabbedPane.add(title, bp);
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


    class SelectKeysAction extends AbstractAction {
        public SelectKeysAction() {
            super(Localization.lang("Customize key bindings"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            KeyBindingsDialog d = new KeyBindingsDialog(new HashMap<>(prefs.getKeyBindings()), prefs.getDefaultKeys());
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.pack(); //setSize(300,500);
            Util.placeDialog(d, JabRefFrame.this);
            d.setVisible(true);
            if (d.getAction()) {
                prefs.setNewKeyBindings(d.getNewKeyBindings());
                JOptionPane.showMessageDialog
                        (JabRefFrame.this,
                                Localization.lang("Your new key bindings have been stored.") + '\n'
                                        + Localization.lang("You must restart JabRef for the new key "
                                        + "bindings to work properly."),
                                Localization.lang("Key bindings changed"),
                                JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * The action concerned with closing the window.
     */
    class CloseAction extends MnemonicAwareAction {
        public CloseAction() {
            putValue(Action.NAME, "Quit");
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Quit JabRef"));
            putValue(Action.ACCELERATOR_KEY, prefs.getKey("Quit JabRef"));
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
            super(IconTheme.getImage("close"));
            putValue(Action.NAME, "Close database");
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close the current database"));
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
                        Localization.lang("Database has changed. Do you want to save before closing?"),
                        Localization.lang("Save before closing"), JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) {
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
            updateEnabledState(); // FIXME: Man, this is what I call a bug that this is not called.
            output(Localization.lang("Closed database") + '.');
            // FIXME: why?
            System.gc(); // Test
        }
    }

    // The action for opening the preferences dialog.
    private final AbstractAction showPrefs = new ShowPrefsAction();

    class ShowPrefsAction
            extends MnemonicAwareAction {

        public ShowPrefsAction() {
            super(IconTheme.getImage("preferences"));
            putValue(Action.NAME, "Preferences");
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Preferences"));
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
     * @param panel     The BasePanel to add to.
     * @param entries   The entries to add.
     * @param filename  Name of the file where the import came from.
     * @param openInNew Should the entries be imported into a new database?
     */
    private void addImportedEntries(final BasePanel panel, final List<BibtexEntry> entries,
                                    String filename, final boolean openInNew) {
        /*
         * Use the import inspection dialog if it is enabled in preferences, and
         * (there are more than one entry or the inspection dialog is also
         * enabled for single entries):
         */
        if (Globals.prefs.getBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG) &&
                (Globals.prefs.getBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG_FOR_SINGLE) || entries.size() > 1)) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    ImportInspectionDialog diag = new ImportInspectionDialog(JabRefFrame.this,
                            panel, BibtexFields.DEFAULT_INSPECTION_FIELDS, Localization.lang("Import"),
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
            if (panel != null && entries.size() == 1) {
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
     *
     * @param filename If non-null, a message is printed to the status line describing
     *                 how many entries were imported, and from which file. If null, the message will not
     *                 be printed.
     * @param intoNew  Determines if the entries will be put in a new database or in the current
     *                 one.
     */
    private int addBibEntries(List<BibtexEntry> bibentries, String filename,
                              boolean intoNew) {
        if (bibentries == null || bibentries.isEmpty()) {

            // No entries found. We need a message for this.
            JOptionPane.showMessageDialog(JabRefFrame.this, Localization.lang("No entries found. Please make sure you are "
                            + "using the correct import filter."), Localization.lang("Import failed"),
                    JOptionPane.ERROR_MESSAGE);
            return 0;
        }

        int addedEntries = 0;

        // Set owner and timestamp fields:
        Util.setAutomaticFields(bibentries, Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER),
                Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP), Globals.prefs.getBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES));

        if (intoNew || tabbedPane.getTabCount() == 0) {
            // Import into new database.
            BibtexDatabase database = new BibtexDatabase();
            for (BibtexEntry entry : bibentries) {

                entry.setId(IdGenerator.next());
                database.insertEntry(entry);

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
                output(Localization.lang("Imported database") + " '" + filename + "' " +
                        Localization.lang("with") + ' ' +
                        database.getEntryCount() + ' ' +
                        Localization.lang("entries into new database") + '.');
            }
        } else {
            // Import into current database.
            BasePanel basePanel = basePanel();
            BibtexDatabase database = basePanel.database;
            int oldCount = database.getEntryCount();
            NamedCompound ce = new NamedCompound(Localization.lang("Import entries"));

            mainLoop:
            for (BibtexEntry entry : bibentries) {
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

                    entry.setId(IdGenerator.next());
                    database.insertEntry(entry);
                    ce.addEdit(new UndoableInsertEntry
                            (database, entry, basePanel));
                    addedEntries++;

                }
            }
            if (addedEntries > 0) {
                ce.end();
                basePanel.undoManager.addEdit(ce);
                basePanel.markBaseChanged();
                if (filename != null) {
                    output(Localization.lang("Imported database") + " '" + filename + "' " +
                            Localization.lang("with") + ' ' +
                            (database.getEntryCount() - oldCount) + ' ' +
                            Localization.lang("entries into new database") + '.');
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
        JMenu submenu = new JMenu(Localization.lang("Custom importers"));
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
     *
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

    /**
     * Set the visibility of the progress bar in the right end of the
     * status line at the bottom of the frame.
     * <p>
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
     * <p>
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
     * <p>
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
     * <p>
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
            super(IconTheme.getImage("save"));
            putValue(Action.NAME, "Save session");
            putValue(Action.ACCELERATOR_KEY, prefs.getKey("Save session"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Here we store the names of all current files. If
            // there is no current file, we remove any
            // previously stored file name.
            Vector<String> filenames = new Vector<>();
            if (tabbedPane.getTabCount() > 0) {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    if (tabbedPane.getTitleAt(i).equals(GUIGlobals.untitledTitle)) {
                        tabbedPane.setSelectedIndex(i);
                        int answer = JOptionPane.showConfirmDialog
                                (JabRefFrame.this, Localization.lang
                                                ("This untitled database must be saved first to be "
                                                        + "included in the saved session. Save now?"),
                                        Localization.lang("Save database"),
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
                output(Localization.lang("Not saved (empty session)") + '.');
            } else {
                String[] names = new String[filenames.size()];
                for (int i = 0; i < filenames.size(); i++) {
                    names[i] = filenames.elementAt(i);
                }
                prefs.putStringArray("savedSession", names);
                output(Localization.lang("Saved session") + '.');
            }

        }
    }

    public class LoadSessionAction extends MnemonicAwareAction {

        volatile boolean running;

        public LoadSessionAction() {
            super(IconTheme.getImage("loadSession"));
            putValue(Action.NAME, "Load session");
            putValue(Action.ACCELERATOR_KEY, prefs.getKey("Load session"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (prefs.get("savedSession") == null) {
                output(Localization.lang("No saved session found."));
                return;
            }
            if (running) {
                return;
            } else {
                running = true;
            }

            output(Localization.lang("Loading session..."));
            JabRefExecutorService.INSTANCE.execute(new Runnable() {

                @Override
                public void run() {
                    HashSet<String> currentFiles = new HashSet<>();
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
                    output(Localization.lang("Files opened") + ": " +
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
                    next ? prefs.getKey("Next tab") : prefs.getKey("Previous tab"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = tabbedPane.getSelectedIndex();
            int newI = next ? i + 1 : i - 1;
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

        public EditAction(String command, ImageIcon icon) {
            super(icon);
            this.command = command;
            String nName = StringUtil.capitalizeFirst(command);
            putValue(Action.NAME, nName);
            putValue(Action.ACCELERATOR_KEY, prefs.getKey(nName));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang(nName));
            //putValue(ACCELERATOR_KEY,
            //         (next?prefs.getKey("Next tab"):prefs.getKey("Previous tab")));
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            //Util.pr(Globals.focusListener.getFocused().toString());
            JComponent source = Globals.focusListener.getFocused();
            try {
                source.getActionMap().get(command).actionPerformed(new ActionEvent(source, 0, command));
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

        DatabasePropertiesDialog propertiesDialog;


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

        BibtexKeyPatternDialog bibtexKeyPatternDialog;


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

    public SearchManager getSearchManager() {
        return searchManager;
    }

}
