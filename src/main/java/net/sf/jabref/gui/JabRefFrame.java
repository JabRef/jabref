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

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;
import net.sf.jabref.*;
import net.sf.jabref.exporter.*;
import net.sf.jabref.external.ExternalFileTypeEditor;
import net.sf.jabref.external.push.PushToApplicationButton;
import net.sf.jabref.external.push.PushToApplications;
import net.sf.jabref.groups.EntryTableTransferHandler;
import net.sf.jabref.groups.GroupSelector;
import net.sf.jabref.gui.actions.*;
import net.sf.jabref.gui.dbproperties.DatabasePropertiesDialog;
import net.sf.jabref.gui.help.AboutAction;
import net.sf.jabref.gui.help.AboutDialog;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.journals.ManageJournalsAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.keyboard.KeyBindingRepository;
import net.sf.jabref.gui.keyboard.KeyBindingsDialog;
import net.sf.jabref.gui.menus.ChangeEntryTypeMenu;
import net.sf.jabref.gui.menus.FileHistoryMenu;
import net.sf.jabref.gui.menus.RightClickMenu;
import net.sf.jabref.gui.menus.help.DonateAction;
import net.sf.jabref.gui.menus.help.ForkMeOnGitHubAction;
import net.sf.jabref.gui.openoffice.OpenOfficePanel;
import net.sf.jabref.gui.preftabs.PreferencesDialog;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.gui.worker.MarkEntriesAction;
import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fetcher.GeneralFetcher;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.integrity.IntegrityCheck;
import net.sf.jabref.logic.integrity.IntegrityMessage;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.logging.GuiAppender;
import net.sf.jabref.logic.preferences.LastFocusedTabPreferences;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.specialfields.*;
import net.sf.jabref.sql.importer.DbImportAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import osx.macadapter.MacAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;

/**
 * The main window of the application.
 */
public class JabRefFrame extends JFrame implements OutputPrinter {

    private static final Log LOGGER = LogFactory.getLog(JabRefFrame.class);
    private static final String ELLIPSES = "...";

    private final JSplitPane splitPane = new JSplitPane();

    private final JabRefPreferences prefs = Globals.prefs;
    private PreferencesDialog prefsDialog;

    private int lastTabbedPanelSelectionIndex = -1;

    // The sidepane manager takes care of populating the sidepane.
    private SidePaneManager sidePaneManager;

    private JTabbedPane tabbedPane; // initialized at constructor

    private final Insets marg = new Insets(1, 0, 2, 0);
    private final JabRef jabRef;

    private PositionWindow pw;

    private final GeneralAction checkIntegrity = new GeneralAction(Actions.CHECK_INTEGRITY, Localization.menuTitle("Check integrity") + ELLIPSES) {

        @Override
        public void actionPerformed(ActionEvent e) {
            IntegrityCheck check = new IntegrityCheck(getCurrentBasePanel().getBibDatabaseContext());
            List<IntegrityMessage> messages = check.checkBibtexDatabase();

            if (messages.isEmpty()) {
                JOptionPane.showMessageDialog(getCurrentBasePanel(), Localization.lang("No problems found."));
            } else {
                // prepare data model
                Object[][] model = new Object[messages.size()][3];
                int i = 0;
                for (IntegrityMessage message : messages) {
                    model[i][0] = message.getEntry().getCiteKey();
                    model[i][1] = message.getFieldName();
                    model[i][2] = message.getMessage();
                    i++;
                }

                // construct view
                JTable table = new JTable(
                        model,
                        new Object[] {"key", "field", "message"}
                );

                table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                ListSelectionModel selectionModel = table.getSelectionModel();

                selectionModel.addListSelectionListener(event -> {
                    if (!event.getValueIsAdjusting()) {
                        String citeKey = (String) model[table.getSelectedRow()][0];
                        String fieldName = (String) model[table.getSelectedRow()][1];
                        getCurrentBasePanel().editEntryByKeyAndFocusField(citeKey, fieldName);
                    }
                });

                table.getColumnModel().getColumn(0).setPreferredWidth(80);
                table.getColumnModel().getColumn(1).setPreferredWidth(30);
                table.getColumnModel().getColumn(2).setPreferredWidth(250);
                table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
                JScrollPane scrollPane = new JScrollPane(table);
                String title = Localization.lang("%0 problem(s) found", String.valueOf(messages.size()));
                JDialog dialog = new JDialog(JabRefFrame.this, title, false);
                dialog.add(scrollPane);
                dialog.setSize(600, 500);

                // show view
                dialog.setVisible(true);
            }
        }
    };


    private final ToolBar tlb = new ToolBar();

    private final JMenuBar mb = new JMenuBar();

    private final GridBagLayout gbl = new GridBagLayout();
    private final GridBagConstraints con = new GridBagConstraints();

    private final JLabel statusLine = new JLabel("", SwingConstants.LEFT);
    private final JLabel statusLabel = new JLabel(
            Localization.lang("Status")
                    + ':', SwingConstants.LEFT);
    private final JProgressBar progressBar = new JProgressBar();

    private final FileHistoryMenu fileHistory = new FileHistoryMenu(prefs, this);

    // The help window.
    private final AboutDialog aboutDiag = new AboutDialog(this);

    // Here we instantiate menu/toolbar actions. Actions regarding
    // the currently open database are defined as a GeneralAction
    // with a unique command string. This causes the appropriate
    // BasePanel's runCommand() method to be called with that command.
    // Note: GeneralAction's constructor automatically gets translations
    // for the name and message strings.

    /* References to the toggle buttons in the toolbar */
    // the groups interface
    public JToggleButton groupToggle;
    private JToggleButton previewToggle;
    private JToggleButton fetcherToggle;


    private final OpenDatabaseAction open = new OpenDatabaseAction(this, true);
    private final EditModeAction editModeAction = new EditModeAction();
    private final AbstractAction quit = new CloseAction();
    private final AbstractAction selectKeys = new SelectKeysAction();
    private final AbstractAction newBibtexDatabaseAction = new NewDatabaseAction(this, BibDatabaseMode.BIBTEX);
    private final AbstractAction newBiblatexDatabaseAction = new NewDatabaseAction(this, BibDatabaseMode.BIBLATEX);
    private final AbstractAction newSubDatabaseAction = new NewSubDatabaseAction(this);
    private final AbstractAction forkMeOnGitHubAction = new ForkMeOnGitHubAction();
    private final AbstractAction donationAction = new DonateAction();
    private final AbstractAction help = new HelpAction(Localization.menuTitle("JabRef help"), Localization.lang("JabRef help"),
            HelpFiles.helpContents, Globals.getKeyPrefs().getKey(KeyBinding.HELP));
    private final AbstractAction about = new AboutAction(Localization.menuTitle("About JabRef"), aboutDiag,
            Localization.lang("About JabRef"), IconTheme.getImage("about"));
    private final AbstractAction editEntry = new GeneralAction(Actions.EDIT, Localization.menuTitle("Edit entry"),
            Localization.lang("Edit entry"), Globals.getKeyPrefs().getKey(KeyBinding.EDIT_ENTRY), IconTheme.JabRefIcon.EDIT_ENTRY.getIcon());
    private final AbstractAction focusTable = new GeneralAction(Actions.FOCUS_TABLE,
            Localization.menuTitle("Focus entry table"),
            Localization.lang("Move the keyboard focus to the entry table"), Globals.getKeyPrefs().getKey(KeyBinding.FOCUS_ENTRY_TABLE));
    private final AbstractAction save = new GeneralAction(Actions.SAVE, Localization.menuTitle("Save database"),
            Localization.lang("Save database"), Globals.getKeyPrefs().getKey(KeyBinding.SAVE_DATABASE), IconTheme.JabRefIcon.SAVE.getIcon());
    private final AbstractAction saveAs = new GeneralAction(Actions.SAVE_AS,
            Localization.menuTitle("Save database as..."), Localization.lang("Save database as..."),
            Globals.getKeyPrefs().getKey(KeyBinding.SAVE_DATABASE_AS));
    private final AbstractAction saveAll = new SaveAllAction(JabRefFrame.this);
    private final AbstractAction saveSelectedAs = new GeneralAction(Actions.SAVE_SELECTED_AS,
            Localization.menuTitle("Save selected as..."), Localization.lang("Save selected as..."));
    private final AbstractAction saveSelectedAsPlain = new GeneralAction(Actions.SAVE_SELECTED_AS_PLAIN,
            Localization.menuTitle("Save selected as plain BibTeX..."),
            Localization.lang("Save selected as plain BibTeX..."));
    private final AbstractAction exportAll = ExportFormats.getExportAction(this, false);
    private final AbstractAction exportSelected = ExportFormats.getExportAction(this, true);
    private final AbstractAction importCurrent = ImportFormats.getImportAction(this, false);
    private final AbstractAction importNew = ImportFormats.getImportAction(this, true);
    public final AbstractAction nextTab = new ChangeTabAction(true);
    public final AbstractAction prevTab = new ChangeTabAction(false);
    private final AbstractAction sortTabs = new SortTabsAction(this);
    private final AbstractAction undo = new GeneralAction(Actions.UNDO, Localization.menuTitle("Undo"),
            Localization.lang("Undo"), Globals.getKeyPrefs().getKey(KeyBinding.UNDO), IconTheme.JabRefIcon.UNDO.getIcon());
    private final AbstractAction redo = new GeneralAction(Actions.REDO, Localization.menuTitle("Redo"),
            Localization.lang("Redo"), Globals.getKeyPrefs().getKey(KeyBinding.REDO), IconTheme.JabRefIcon.REDO.getIcon());
    private final AbstractAction forward = new GeneralAction(Actions.FORWARD, Localization.menuTitle("Forward"),
            Localization.lang("Forward"), Globals.getKeyPrefs().getKey(KeyBinding.FORWARD), IconTheme.JabRefIcon.RIGHT.getIcon());
    private final AbstractAction back = new GeneralAction(Actions.BACK, Localization.menuTitle("Back"),
            Localization.lang("Back"), Globals.getKeyPrefs().getKey(KeyBinding.BACK), IconTheme.JabRefIcon.LEFT.getIcon());
    private final AbstractAction deleteEntry = new GeneralAction(Actions.DELETE, Localization.menuTitle("Delete entry"),
            Localization.lang("Delete entry"), Globals.getKeyPrefs().getKey(KeyBinding.DELETE_ENTRY), IconTheme.JabRefIcon.DELETE_ENTRY.getIcon());
    private final AbstractAction copy = new EditAction(Actions.COPY, Localization.menuTitle("Copy"),
            Localization.lang("Copy"), Globals.getKeyPrefs().getKey(KeyBinding.COPY), IconTheme.JabRefIcon.COPY.getIcon());
    private final AbstractAction paste = new EditAction(Actions.PASTE, Localization.menuTitle("Paste"),
            Localization.lang("Paste"), Globals.getKeyPrefs().getKey(KeyBinding.PASTE), IconTheme.JabRefIcon.PASTE.getIcon());
    private final AbstractAction cut = new EditAction(Actions.CUT, Localization.menuTitle("Cut"),
            Localization.lang("Cut"), Globals.getKeyPrefs().getKey(KeyBinding.CUT), IconTheme.JabRefIcon.CUT.getIcon());
    private final AbstractAction openConsole = new GeneralAction(Actions.OPEN_CONSOLE,
            Localization.menuTitle("Open terminal here"),
            Localization.lang("Open terminal here"),
            Globals.getKeyPrefs().getKey(KeyBinding.OPEN_CONSOLE),
            IconTheme.JabRefIcon.CONSOLE.getIcon());
    private final AbstractAction mark = new GeneralAction(Actions.MARK_ENTRIES, Localization.menuTitle("Mark entries"),
            Localization.lang("Mark entries"), Globals.getKeyPrefs().getKey(KeyBinding.MARK_ENTRIES), IconTheme.JabRefIcon.MARK_ENTRIES.getIcon());
    private final AbstractAction unmark = new GeneralAction(Actions.UNMARK_ENTRIES,
            Localization.menuTitle("Unmark entries"), Localization.lang("Unmark entries"),
            Globals.getKeyPrefs().getKey(KeyBinding.UNMARK_ENTRIES), IconTheme.JabRefIcon.UNMARK_ENTRIES.getIcon());
    private final AbstractAction unmarkAll = new GeneralAction(Actions.UNMARK_ALL, Localization.menuTitle("Unmark all"));
    private final AbstractAction toggleRelevance = new GeneralAction(
            Relevance.getInstance().getValues().get(0).getActionName(),
            Relevance.getInstance().getValues().get(0).getMenuString(),
            Relevance.getInstance().getValues().get(0).getToolTipText(),
            IconTheme.JabRefIcon.RELEVANCE.getIcon());
    private final AbstractAction toggleQualityAssured = new GeneralAction(
            Quality.getInstance().getValues().get(0).getActionName(),
            Quality.getInstance().getValues().get(0).getMenuString(),
            Quality.getInstance().getValues().get(0).getToolTipText(),
            IconTheme.JabRefIcon.QUALITY_ASSURED.getIcon());
    private final AbstractAction togglePrinted = new GeneralAction(
            Printed.getInstance().getValues().get(0).getActionName(),
            Printed.getInstance().getValues().get(0).getMenuString(),
            Printed.getInstance().getValues().get(0).getToolTipText(),
            IconTheme.JabRefIcon.PRINTED.getIcon());
    private final AbstractAction manageSelectors = new GeneralAction(Actions.MANAGE_SELECTORS,
            Localization.menuTitle("Manage content selectors"));
    private final AbstractAction normalSearch = new GeneralAction(Actions.SEARCH, Localization.menuTitle("Search"),
            Localization.lang("Search"), Globals.getKeyPrefs().getKey(KeyBinding.SEARCH), IconTheme.JabRefIcon.SEARCH.getIcon());

    private final AbstractAction copyKey = new GeneralAction(Actions.COPY_KEY, Localization.menuTitle("Copy BibTeX key"),
            Globals.getKeyPrefs().getKey(KeyBinding.COPY_BIBTEX_KEY));
    private final AbstractAction copyCiteKey = new GeneralAction(Actions.COPY_CITE_KEY, Localization.menuTitle(
            "Copy \\cite{BibTeX key}"),
            Globals.getKeyPrefs().getKey(KeyBinding.COPY_CITE_BIBTEX_KEY));
    private final AbstractAction copyKeyAndTitle = new GeneralAction(Actions.COPY_KEY_AND_TITLE,
            Localization.menuTitle("Copy BibTeX key and title"),
            Globals.getKeyPrefs().getKey(KeyBinding.COPY_BIBTEX_KEY_AND_TITLE));
    private final AbstractAction mergeDatabaseAction = new GeneralAction(Actions.MERGE_DATABASE,
            Localization.menuTitle("Append database"),
            Localization.lang("Append contents from a BibTeX database into the currently viewed database"));
    private final AbstractAction selectAll = new GeneralAction(Actions.SELECT_ALL, Localization.menuTitle("Select all"),
            Globals.getKeyPrefs().getKey(KeyBinding.SELECT_ALL));
    private final AbstractAction replaceAll = new GeneralAction(Actions.REPLACE_ALL,
            Localization.menuTitle("Replace string") + ELLIPSES, Globals.getKeyPrefs().getKey(KeyBinding.REPLACE_STRING));

    private final AbstractAction editPreamble = new GeneralAction(Actions.EDIT_PREAMBLE,
            Localization.menuTitle("Edit preamble"),
            Localization.lang("Edit preamble"),
            Globals.getKeyPrefs().getKey(KeyBinding.EDIT_PREAMBLE));
    private final AbstractAction editStrings = new GeneralAction(Actions.EDIT_STRINGS,
            Localization.menuTitle("Edit strings"),
            Localization.lang("Edit strings"),
            Globals.getKeyPrefs().getKey(KeyBinding.EDIT_STRINGS),
            IconTheme.JabRefIcon.EDIT_STRINGS.getIcon());
    private final AbstractAction customizeAction = new CustomizeEntryTypeAction();
    private final Action toggleToolbar = enableToggle(new AbstractAction(Localization.menuTitle("Hide/show toolbar")) {

        {
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.HIDE_SHOW_TOOLBAR));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Hide/show toolbar"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tlb.setVisible(!tlb.isVisible());
        }
    });

    private final Action toggleGroups = enableToggle(new GeneralAction(Actions.TOGGLE_GROUPS,
            Localization.menuTitle("Toggle groups interface"),
            Localization.lang("Toggle groups interface"),
            Globals.getKeyPrefs().getKey(KeyBinding.TOGGLE_GROUPS_INTERFACE),
            IconTheme.JabRefIcon.TOGGLE_GROUPS.getIcon()));
    private final AbstractAction addToGroup = new GeneralAction(Actions.ADD_TO_GROUP, Localization.lang("Add to group") + ELLIPSES);
    private final AbstractAction removeFromGroup = new GeneralAction(Actions.REMOVE_FROM_GROUP,
            Localization.lang("Remove from group") + ELLIPSES);
    private final AbstractAction moveToGroup = new GeneralAction(Actions.MOVE_TO_GROUP, Localization.lang("Move to group") + ELLIPSES);

    private final Action togglePreview = enableToggle(new GeneralAction(Actions.TOGGLE_PREVIEW,
            Localization.menuTitle("Toggle entry preview"),
            Localization.lang("Toggle entry preview"),
            Globals.getKeyPrefs().getKey(KeyBinding.TOGGLE_ENTRY_PREVIEW),
            IconTheme.JabRefIcon.TOGGLE_ENTRY_PREVIEW.getIcon()));
    private final Action toggleHighlightAny = enableToggle(new GeneralAction(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ANY,
            Localization.menuTitle("Highlight groups matching any selected entry"),
            Localization.lang("Highlight groups matching any selected entry")));
    private final Action toggleHighlightAll = enableToggle(new GeneralAction(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ALL,
            Localization.menuTitle("Highlight groups matching all selected entries"),
            Localization.lang("Highlight groups matching all selected entries")));
    private final Action toggleHighlightDisable = enableToggle(new GeneralAction(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_DISABLE,
            Localization.menuTitle("Disable highlight groups matching entries"),
            Localization.lang("Disable highlight groups matching entries")));


    private final AbstractAction switchPreview = new GeneralAction(Actions.SWITCH_PREVIEW,
            Localization.menuTitle("Switch preview layout"),
            Globals.getKeyPrefs().getKey(KeyBinding.SWITCH_PREVIEW_LAYOUT));
    private final AbstractAction makeKeyAction = new GeneralAction(Actions.MAKE_KEY,
            Localization.menuTitle("Autogenerate BibTeX keys"),
            Localization.lang("Autogenerate BibTeX keys"),
            Globals.getKeyPrefs().getKey(KeyBinding.AUTOGENERATE_BIBTEX_KEYS),
            IconTheme.JabRefIcon.MAKE_KEY.getIcon());

    private final AbstractAction writeXmpAction = new GeneralAction(Actions.WRITE_XMP,
            Localization.menuTitle("Write XMP-metadata to PDFs"),
            Localization.lang("Will write XMP-metadata to the PDFs linked from selected entries."),
            Globals.getKeyPrefs().getKey(KeyBinding.WRITE_XMP));

    private final AbstractAction openFolder = new GeneralAction(Actions.OPEN_FOLDER,
            Localization.menuTitle("Open folder"), Localization.lang("Open folder"),
            Globals.getKeyPrefs().getKey(KeyBinding.OPEN_FOLDER));
    private final AbstractAction openFile = new GeneralAction(Actions.OPEN_EXTERNAL_FILE,
            Localization.menuTitle("Open file"),
            Localization.lang("Open file"),
            Globals.getKeyPrefs().getKey(KeyBinding.OPEN_FILE),
            IconTheme.JabRefIcon.FILE.getIcon());
    private final AbstractAction openUrl = new GeneralAction(Actions.OPEN_URL,
            Localization.menuTitle("Open URL or DOI"),
            Localization.lang("Open URL or DOI"),
            Globals.getKeyPrefs().getKey(KeyBinding.OPEN_URL_OR_DOI),
            IconTheme.JabRefIcon.WWW.getIcon());
    private final AbstractAction dupliCheck = new GeneralAction(Actions.DUPLI_CHECK,
            Localization.menuTitle("Find duplicates"), IconTheme.JabRefIcon.FIND_DUPLICATES.getIcon());
    private final AbstractAction plainTextImport = new GeneralAction(Actions.PLAIN_TEXT_IMPORT,
            Localization.menuTitle("New entry from plain text") + ELLIPSES,
            Globals.getKeyPrefs().getKey(KeyBinding.NEW_FROM_PLAIN_TEXT));

    private final AbstractAction customExpAction = new CustomizeExportsAction();
    private final AbstractAction customImpAction = new CustomizeImportsAction();
    private final AbstractAction customFileTypesAction = ExternalFileTypeEditor.getAction(this);
    private final AbstractAction exportToClipboard = new GeneralAction(Actions.EXPORT_TO_CLIPBOARD,
            Localization.menuTitle("Export selected entries to clipboard"),
            IconTheme.JabRefIcon.EXPORT_TO_CLIPBOARD.getIcon());
    private final AbstractAction autoSetFile = new GeneralAction(Actions.AUTO_SET_FILE,
            Localization.lang("Synchronize file links") + ELLIPSES,
            Globals.getKeyPrefs().getKey(KeyBinding.SYNCHRONIZE_FILES));

    private final AbstractAction abbreviateMedline = new GeneralAction(Actions.ABBREVIATE_MEDLINE,
            Localization.menuTitle("Abbreviate journal names (MEDLINE)"),
            Localization.lang("Abbreviate journal names of the selected entries (MEDLINE abbreviation)"));
    private final AbstractAction abbreviateIso = new GeneralAction(Actions.ABBREVIATE_ISO,
            Localization.menuTitle("Abbreviate journal names (ISO)"),
            Localization.lang("Abbreviate journal names of the selected entries (ISO abbreviation)"),
            Globals.getKeyPrefs().getKey(KeyBinding.ABBREVIATE));

    private final AbstractAction unabbreviate = new GeneralAction(Actions.UNABBREVIATE,
            Localization.menuTitle("Unabbreviate journal names"),
            Localization.lang("Unabbreviate journal names of the selected entries"),
            Globals.getKeyPrefs().getKey(KeyBinding.UNABBREVIATE));
    private final AbstractAction manageJournals = new ManageJournalsAction(this);
    private final AbstractAction databaseProperties = new DatabasePropertiesAction();
    private final AbstractAction bibtexKeyPattern = new BibtexKeyPatternAction();
    private final AbstractAction errorConsole = new ErrorConsoleAction(this, Globals.streamEavesdropper, GuiAppender.CACHE);

    private final AbstractAction dbConnect = new GeneralAction(Actions.DB_CONNECT,
            Localization.menuTitle("Connect to external SQL database"),
            Localization.lang("Connect to external SQL database"));

    private final AbstractAction dbExport = new GeneralAction(Actions.DB_EXPORT,
            Localization.menuTitle("Export to external SQL database"),
            Localization.lang("Export to external SQL database"));

    private final AbstractAction cleanupEntries = new GeneralAction(Actions.CLEANUP,
            Localization.menuTitle("Cleanup entries") + ELLIPSES,
            Localization.lang("Cleanup entries"),
            Globals.getKeyPrefs().getKey(KeyBinding.CLEANUP),
            IconTheme.JabRefIcon.CLEANUP_ENTRIES.getIcon());

    private final AbstractAction mergeEntries = new GeneralAction(Actions.MERGE_ENTRIES,
            Localization.menuTitle("Merge entries") + ELLIPSES,
            Localization.lang("Merge entries"),
            IconTheme.JabRefIcon.MERGE_ENTRIES.getIcon());

    private final AbstractAction dbImport = new DbImportAction(this).getAction();
    private final AbstractAction downloadFullText = new GeneralAction(Actions.DOWNLOAD_FULL_TEXT,
            Localization.menuTitle("Look up full text document"),
            Localization.lang("Follow DOI or URL link and try to locate PDF full text document"));
    private final AbstractAction increaseFontSize = new IncreaseTableFontSizeAction();
    private final AbstractAction decreseFontSize = new DecreaseTableFontSizeAction();
    private final AbstractAction resolveDuplicateKeys = new GeneralAction(Actions.RESOLVE_DUPLICATE_KEYS,
            Localization.menuTitle("Resolve duplicate BibTeX keys"),
            Localization.lang("Find and remove duplicate BibTeX keys"),
            Globals.getKeyPrefs().getKey(KeyBinding.RESOLVE_DUPLICATE_BIBTEX_KEYS));

    private final AbstractAction sendAsEmail = new GeneralAction(Actions.SEND_AS_EMAIL,
            Localization.lang("Send as email"), IconTheme.JabRefIcon.EMAIL.getIcon());

    private final MassSetFieldAction massSetField = new MassSetFieldAction(this);
    private final ManageKeywordsAction manageKeywords = new ManageKeywordsAction(this);

    private final GeneralAction findUnlinkedFiles = new GeneralAction(
            FindUnlinkedFilesDialog.ACTION_COMMAND,
            FindUnlinkedFilesDialog.ACTION_MENU_TITLE, FindUnlinkedFilesDialog.ACTION_SHORT_DESCRIPTION,
            Globals.getKeyPrefs().getKey(KeyBinding.FIND_UNLINKED_FILES)
    );

    private final AutoLinkFilesAction autoLinkFile = new AutoLinkFilesAction();

    private PushToApplicationButton pushExternalButton;

    private GeneralFetcher generalFetcher;

    private GroupSelector groupSelector;

    private int previousTabCount = -1;

    // The action for adding a new entry of unspecified type.
    private final NewEntryAction newEntryAction = new NewEntryAction(this, Globals.getKeyPrefs().getKey(KeyBinding.NEW_ENTRY));
    private final List<NewEntryAction> newSpecificEntryAction = getNewEntryActions();

    // The action for closing the current database and leaving the window open.
    private final CloseDatabaseAction closeDatabaseAction = new CloseDatabaseAction();
    private final CloseAllDatabasesAction closeAllDatabasesAction = new CloseAllDatabasesAction();
    private final CloseOtherDatabasesAction closeOtherDatabasesAction = new CloseOtherDatabasesAction();

    // The action for opening the preferences dialog.
    private final AbstractAction showPrefs = new ShowPrefsAction();

    // Lists containing different subsets of actions for different purposes
    private final List<Object> specialFieldButtons = new LinkedList<>();
    private final List<Object> openDatabaseOnlyActions = new LinkedList<>();
    private final List<Object> severalDatabasesOnlyActions = new LinkedList<>();
    private final List<Object> openAndSavedDatabasesOnlyActions = new LinkedList<>();


    private class EditModeAction extends AbstractAction {

        public EditModeAction() {
            initName();
        }

        public void initName() {
            if (JabRefFrame.this.getCurrentBasePanel() == null) {
                putValue(Action.NAME, Localization.menuTitle("Switch to %0 mode", "BibTeX/BibLaTeX"));
            } else {
                BibDatabaseMode mode = JabRefFrame.this.getCurrentBasePanel().getBibDatabaseContext().getMode();
                String modeName = mode.getOppositeMode().getFormattedName();
                putValue(Action.NAME, Localization.menuTitle("Switch to %0 mode", modeName));
            }
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (JabRefFrame.this.getCurrentBasePanel() == null) {
                return;
            }

            BibDatabaseMode newMode = JabRefFrame.this.getCurrentBasePanel().getBibDatabaseContext().getMode()
                    .getOppositeMode();
            JabRefFrame.this.getCurrentBasePanel().getBibDatabaseContext().setMode(newMode);
            JabRefFrame.this.refreshTitleAndTabs();

            initName();

            // update all elements in current base panel
            JabRefFrame.this.getCurrentBasePanel().hideBottomComponent();
            JabRefFrame.this.getCurrentBasePanel().rebuildAllEntryEditors();
            JabRefFrame.this.getCurrentBasePanel().updateEntryEditorIfShowing();
        }

    }


    private List<NewEntryAction> getNewEntryActions() {
        // only Bibtex
        List<NewEntryAction> actions = new ArrayList<>();
        for (EntryType type : BibtexEntryTypes.ALL) {
            KeyStroke keyStroke = new ChangeEntryTypeMenu().entryShortCuts.get(type.getName());
            if (keyStroke == null) {
                actions.add(new NewEntryAction(this, type.getName()));
            } else {
                actions.add(new NewEntryAction(this, type.getName(), keyStroke));
            }
        }
        return actions;
    }

    public JabRefFrame(JabRef jabRef) {
        this.jabRef = jabRef;
        init();
        updateEnabledState();

    }

    private JPopupMenu tabPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        // Close actions
        JMenuItem close = new JMenuItem(Localization.lang("Close"));
        JMenuItem closeOthers = new JMenuItem(Localization.lang("Close Others"));
        JMenuItem closeAll = new JMenuItem(Localization.lang("Close All"));
        close.addActionListener(closeDatabaseAction);
        closeOthers.addActionListener(closeOtherDatabasesAction);
        closeAll.addActionListener(closeAllDatabasesAction);
        popupMenu.add(close);
        popupMenu.add(closeOthers);
        popupMenu.add(closeAll);

        popupMenu.addSeparator();

        JMenuItem databaseProperties = new JMenuItem(Localization.lang("Database properties"));
        databaseProperties.addActionListener(this.databaseProperties);
        popupMenu.add(databaseProperties);

        JMenuItem bibtexKeyPatternBtn = new JMenuItem(Localization.lang("BibTeX key patterns"));
        bibtexKeyPatternBtn.addActionListener(bibtexKeyPattern);
        popupMenu.add(bibtexKeyPatternBtn);

        JMenuItem manageSelectorsBtn = new JMenuItem(Localization.lang("Manage content selectors"));
        manageSelectorsBtn.addActionListener(manageSelectors);
        popupMenu.add(manageSelectorsBtn);

        return popupMenu;
    }

    private void init() {
        tabbedPane = new DragDropPopupPane(tabPopupMenu());

        MyGlassPane glassPane = new MyGlassPane();
        setGlassPane(glassPane);

        setTitle(GUIGlobals.FRAME_TITLE);
        setIconImage(new ImageIcon(IconTheme.getIconUrl("jabrefIcon48")).getImage());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {

                if (OS.OS_X) {
                    JabRefFrame.this.setVisible(false);
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
        pw = new PositionWindow(this, JabRefPreferences.POS_X, JabRefPreferences.POS_Y, JabRefPreferences.SIZE_X,
                JabRefPreferences.SIZE_Y);
        positionWindowOnScreen();

        tabbedPane.setBorder(null);
        tabbedPane.setForeground(GUIGlobals.INACTIVE_TABBED_COLOR);

        /*
         * The following state listener makes sure focus is registered with the
         * correct database when the user switches tabs. Without this,
         * cut/paste/copy operations would some times occur in the wrong tab.
         */
        tabbedPane.addChangeListener(e -> {
            markActiveBasePanel();

            BasePanel bp = getCurrentBasePanel();
            if (bp == null) {
                return;
            }

            groupToggle.setSelected(sidePaneManager.isComponentVisible("groups"));
            previewToggle.setSelected(Globals.prefs.getBoolean(JabRefPreferences.PREVIEW_ENABLED));
            fetcherToggle.setSelected(sidePaneManager.isComponentVisible(generalFetcher.getTitle()));
            Globals.focusListener.setFocused(bp.mainTable);
            setWindowTitle();
            editModeAction.initName();
            // Update search autocompleter with information for the correct database:
            bp.updateSearchManager();
            // Set correct enabled state for Back and Forward actions:
            bp.setBackAndForwardEnabledState();
            new FocusRequester(bp.mainTable);
        });

        //Note: The registration of Apple event is at the end of initialization, because
        //if the events happen too early (ie when the window is not initialized yet), the
        //opened (double-clicked) documents are not displayed.
        if (OS.OS_X) {
            try {
                new MacAdapter().registerMacEvents(this);
            } catch (Exception e) {
                LOGGER.fatal("Could not interface with Mac OS X methods.", e);
            }
        }
    }

    private void positionWindowOnScreen() {
        if (!prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            pw.setWindowPosition();
        }
    }

    private void refreshTitleAndTabs() {
        setWindowTitle();
        updateAllTabTitles();
    }

    /**
     * Sets the title of the main window.
     */
    public void setWindowTitle() {
        BasePanel panel = getCurrentBasePanel();

        // no database open
        if (panel == null) {
            setTitle(GUIGlobals.FRAME_TITLE);
            return;
        }

        String mode = panel.getBibDatabaseContext().getMode().getFormattedName();
        String modeInfo = String.format(" (%s)", Localization.lang("%0 mode", mode));
        String changeFlag = panel.isModified() ? "*" : "";

        if (panel.getBibDatabaseContext().getDatabaseFile() == null) {
            setTitle(GUIGlobals.FRAME_TITLE + " - " + GUIGlobals.UNTITLED_TITLE + changeFlag + modeInfo);
        } else {
            String databaseFile = panel.getBibDatabaseContext().getDatabaseFile().getPath();
            setTitle(GUIGlobals.FRAME_TITLE + " - " + databaseFile + changeFlag + modeInfo);
        }
    }

    private void initSidePane() {
        sidePaneManager = new SidePaneManager(this);

        groupSelector = new GroupSelector(this, sidePaneManager);

        generalFetcher = new GeneralFetcher(sidePaneManager, this);

        sidePaneManager.register("groups", groupSelector);
    }

    /**
     * The MacAdapter calls this method when a ".bib" file has been double-clicked from the Finder.
     */
    public void openAction(String filePath) {
        File file = new File(filePath);
        // all the logic is done in openIt. Even raising an existing panel
        getOpenDatabaseAction().openFile(file, true);
    }

    // General info dialog.  The MacAdapter calls this method when "About"
    // is selected from the application menu.
    public void about() {
        // reuse the normal about action
        // null as parameter is OK as the code of actionPerformed does not rely on the data sent in the event.
        about.actionPerformed(null);
    }

    // General preferences dialog.  The MacAdapter calls this method when "Preferences..."
    // is selected from the application menu.
    public void preferences() {
        output(Localization.lang("Opening preferences..."));
        if (prefsDialog == null) {
            prefsDialog = new PreferencesDialog(JabRefFrame.this, jabRef);
            prefsDialog.setLocationRelativeTo(JabRefFrame.this);
        } else {
            prefsDialog.setValues();
        }

        prefsDialog.setVisible(true);
        output("");
    }

    public JabRefPreferences prefs() {
        return prefs;
    }

    /**
     * Tears down all things started by JabRef
     * <p>
     * FIXME: Currently some threads remain and therefore hinder JabRef to be closed properly
     *
     * @param filenames the filenames of all currently opened files - used for storing them if prefs openLastEdited is set to true
     */
    private void tearDownJabRef(List<String> filenames) {
        JabRefExecutorService.INSTANCE.shutdownEverything();

        dispose();

        if (getCurrentBasePanel() != null) {
            getCurrentBasePanel().saveDividerLocation();
        }

        //prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, (getExtendedState()&MAXIMIZED_BOTH)>0);
        prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, getExtendedState() == Frame.MAXIMIZED_BOTH);

        prefs.putBoolean(JabRefPreferences.TOOLBAR_VISIBLE, tlb.isVisible());
        // Store divider location for side pane:
        int width = splitPane.getDividerLocation();
        if (width > 0) {
            prefs.putInt(JabRefPreferences.SIDE_PANE_WIDTH, width);
        }
        if (prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)) {
            // Here we store the names of all current files. If
            // there is no current file, we remove any
            // previously stored filename.
            if (filenames.isEmpty()) {
                prefs.remove(JabRefPreferences.LAST_EDITED);
            } else {
                prefs.putStringList(JabRefPreferences.LAST_EDITED, filenames);
                File focusedDatabase = getCurrentBasePanel().getBibDatabaseContext().getDatabaseFile();
                new LastFocusedTabPreferences(prefs).setLastFocusedTab(focusedDatabase);
            }

        }

        fileHistory.storeHistory();
        prefs.customExports.store();
        prefs.customImports.store();
        CustomEntryTypesManager.saveCustomEntryTypes(prefs);

        // Clear autosave files:
        if (Globals.autoSaveManager != null) {
            Globals.autoSaveManager.clearAutoSaves();
        }

        prefs.flush();

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
        List<String> filenames = new ArrayList<>();
        if (tabbedPane.getTabCount() > 0) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (getBasePanelAt(i).isModified()) {
                    tabbedPane.setSelectedIndex(i);
                    String filename;

                    if (getBasePanelAt(i).getBibDatabaseContext().getDatabaseFile() == null) {
                        filename = GUIGlobals.UNTITLED_TITLE;
                    } else {
                        filename = getBasePanelAt(i).getBibDatabaseContext().getDatabaseFile().getAbsolutePath();
                    }
                    int answer = showSaveDialog(filename);

                    if ((answer == JOptionPane.CANCEL_OPTION) ||
                            (answer == JOptionPane.CLOSED_OPTION)) {
                        return false;
                    }
                    if (answer == JOptionPane.YES_OPTION) {
                        // The user wants to save.
                        try {
                            //getCurrentBasePanel().runCommand("save");
                            SaveDatabaseAction saveAction = new SaveDatabaseAction(getCurrentBasePanel());
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

                if (getBasePanelAt(i).getBibDatabaseContext().getDatabaseFile() != null) {
                    filenames.add(getBasePanelAt(i).getBibDatabaseContext().getDatabaseFile().getAbsolutePath());
                }
            }
        }

        if (close) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (getBasePanelAt(i).isSaving()) {
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

        pushExternalButton = new PushToApplicationButton(this, PushToApplications.getApplications());
        fillMenu();
        createToolBar();
        getContentPane().setLayout(gbl);
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);
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
        gbl.setConstraints(splitPane, con);
        getContentPane().add(splitPane);

        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));

        splitPane.setRightComponent(tabbedPane);
        splitPane.setLeftComponent(sidePaneManager.getPanel());
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
        statusLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR.darker());
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
    public BasePanel getBasePanelAt(int i) {
        return (BasePanel) tabbedPane.getComponentAt(i);
    }

    /**
     * Returns a list of BasePanel.
     *
     */
    public List<BasePanel> getBasePanelList() {
        List<BasePanel> returnList = new ArrayList<>(getBasePanelCount());
        for (int i=0; i< getBasePanelCount(); i++) {
            returnList.add((BasePanel) tabbedPane.getComponentAt(i));
        }
        return returnList;
    }

    public void showBasePanelAt(int i) {
        tabbedPane.setSelectedIndex(i);
    }

    public void showBasePanel(BasePanel bp) {
        tabbedPane.setSelectedComponent(bp);
    }

    /**
     * Returns the currently viewed BasePanel.
     */
    public BasePanel getCurrentBasePanel() {
        if (tabbedPane == null) {
            return null;
        }
        return (BasePanel) tabbedPane.getSelectedComponent();
    }

    /**
     * @return the BasePanel count.
     */
    public int getBasePanelCount() {
        return tabbedPane.getComponentCount();
    }

    /**
     * handle the color of active and inactive JTabbedPane tabs
     */
    private void markActiveBasePanel() {
        int now = tabbedPane.getSelectedIndex();
        int len = tabbedPane.getTabCount();
        if ((lastTabbedPanelSelectionIndex > -1) && (lastTabbedPanelSelectionIndex < len)) {
            tabbedPane.setForegroundAt(lastTabbedPanelSelectionIndex, GUIGlobals.INACTIVE_TABBED_COLOR);
        }
        if ((now > -1) && (now < len)) {
            tabbedPane.setForegroundAt(now, GUIGlobals.ACTIVE_TABBED_COLOR);
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

    public void setTabTitle(JComponent comp, String title, String toolTip) {
        int index = getTabIndex(comp);
        tabbedPane.setTitleAt(index, title);
        tabbedPane.setToolTipTextAt(index, toolTip);
    }

    private static Action enableToggle(Action a, boolean initialValue) {
        // toggle only works correctly when the SELECTED_KEY is set to false or true explicitly upon start
        a.putValue(Action.SELECTED_KEY, String.valueOf(initialValue));

        return a;
    }

    private static Action enableToggle(Action a) {
        return enableToggle(a, false);
    }

    private class GeneralAction extends MnemonicAwareAction {

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

        public GeneralAction(String command, String text, Icon icon) {
            super(icon);

            this.command = command;
            putValue(Action.NAME, text);
        }

        public GeneralAction(String command, String text, String description, Icon icon) {
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

        public GeneralAction(String command, String text, String description, KeyStroke key, Icon icon) {
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
                    ((BasePanel) tabbedPane.getSelectedComponent()).runCommand(command);
                } catch (Throwable ex) {
                    LOGGER.error("Problem with executing command: " + command, ex);
                }
            } else {
                LOGGER.info("Action '" + command + "' must be disabled when no database is open.");
            }
        }
    }

    private void fillMenu() {
        mb.setBorder(null);
        JMenu file = JabRefFrame.subMenu(Localization.menuTitle("File"));
        JMenu edit = JabRefFrame.subMenu(Localization.menuTitle("Edit"));
        JMenu search = JabRefFrame.subMenu(Localization.menuTitle("Search"));
        JMenu groups = JabRefFrame.subMenu(Localization.menuTitle("Groups"));
        JMenu bibtex = JabRefFrame.subMenu("&BibTeX");
        JMenu quality = JabRefFrame.subMenu(Localization.menuTitle("Quality"));
        JMenu view = JabRefFrame.subMenu(Localization.menuTitle("View"));
        JMenu tools = JabRefFrame.subMenu(Localization.menuTitle("Tools"));
        JMenu options = JabRefFrame.subMenu(Localization.menuTitle("Options"));
        JMenu newSpec = JabRefFrame.subMenu(Localization.menuTitle("New entry by type..."));
        JMenu helpMenu = JabRefFrame.subMenu(Localization.menuTitle("Help"));

        file.add(newBibtexDatabaseAction);
        file.add(newBiblatexDatabaseAction);
        file.add(getOpenDatabaseAction());
        file.add(mergeDatabaseAction);
        file.add(save);
        file.add(saveAs);
        file.add(saveAll);
        file.add(saveSelectedAs);
        file.add(saveSelectedAsPlain);
        file.addSeparator();
        file.add(importNew);
        file.add(importCurrent);
        file.add(exportAll);
        file.add(exportSelected);
        file.addSeparator();
        file.add(dbConnect);
        file.add(dbImport);
        file.add(dbExport);

        file.addSeparator();
        file.add(databaseProperties);
        file.add(editModeAction);
        file.addSeparator();

        file.add(fileHistory);
        file.addSeparator();
        file.add(closeDatabaseAction);
        file.add(quit);
        mb.add(file);

        edit.add(undo);
        edit.add(redo);

        edit.addSeparator();

        edit.add(cut);
        edit.add(copy);
        edit.add(paste);

        edit.addSeparator();

        edit.add(copyKey);
        edit.add(copyCiteKey);
        edit.add(copyKeyAndTitle);
        edit.add(exportToClipboard);
        edit.add(sendAsEmail);

        edit.addSeparator();
        edit.add(mark);
        JMenu markSpecific = JabRefFrame.subMenu(Localization.menuTitle("Mark specific color"));
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
            edit.addSeparator();
        }

        edit.add(getManageKeywords());
        edit.add(getMassSetField());
        edit.addSeparator();
        edit.add(selectAll);
        mb.add(edit);

        search.add(normalSearch);
        search.add(replaceAll);
        search.addSeparator();
        search.add(new JCheckBoxMenuItem(generalFetcher.getAction()));
        if (prefs.getBoolean(JabRefPreferences.WEB_SEARCH_VISIBLE)) {
            sidePaneManager.register(generalFetcher.getTitle(), generalFetcher);
            sidePaneManager.show(generalFetcher.getTitle());
        }
        mb.add(search);

        groups.add(new JCheckBoxMenuItem(toggleGroups));
        groups.addSeparator();
        groups.add(addToGroup);
        groups.add(removeFromGroup);
        groups.add(moveToGroup);
        groups.addSeparator();
        JRadioButtonMenuItem toggleHighlightAnyItem = new JRadioButtonMenuItem(toggleHighlightAny);
        groups.add(toggleHighlightAnyItem);
        JRadioButtonMenuItem toggleHighlightAllItem = new JRadioButtonMenuItem(toggleHighlightAll);
        groups.add(toggleHighlightAllItem);
        JRadioButtonMenuItem toggleHighlightDisableItem = new JRadioButtonMenuItem(toggleHighlightDisable);
        groups.add(toggleHighlightDisableItem);
        ButtonGroup highlightButtonGroup = new ButtonGroup();
        highlightButtonGroup.add(toggleHighlightDisableItem);
        highlightButtonGroup.add(toggleHighlightAnyItem);
        highlightButtonGroup.add(toggleHighlightAllItem);

        HighlightMatchingGroupPreferences highlightMatchingGroupPreferences = new HighlightMatchingGroupPreferences(Globals.prefs);
        if(highlightMatchingGroupPreferences.isAll()) {
            toggleHighlightAllItem.setSelected(true);
        } else if(highlightMatchingGroupPreferences.isAny()) {
            toggleHighlightAnyItem.setSelected(true);
        } else {
            toggleHighlightDisableItem.setSelected(true);
        }

        mb.add(groups);

        view.add(getBackAction());
        view.add(getForwardAction());
        view.add(focusTable);
        view.add(nextTab);
        view.add(prevTab);
        view.add(sortTabs);
        view.addSeparator();
        view.add(increaseFontSize);
        view.add(decreseFontSize);
        view.addSeparator();
        view.add(new JCheckBoxMenuItem(toggleToolbar));
        view.add(new JCheckBoxMenuItem(enableToggle(generalFetcher.getAction())));
        view.add(new JCheckBoxMenuItem(toggleGroups));
        view.add(new JCheckBoxMenuItem(togglePreview));
        view.add(getSwitchPreviewAction());

        mb.add(view);

        bibtex.add(newEntryAction);

        for (NewEntryAction a : newSpecificEntryAction) {
            newSpec.add(a);
        }
        bibtex.add(newSpec);

        bibtex.add(plainTextImport);
        bibtex.addSeparator();
        bibtex.add(editEntry);
        bibtex.add(editPreamble);
        bibtex.add(editStrings);
        bibtex.addSeparator();
        bibtex.add(customizeAction);
        bibtex.addSeparator();
        bibtex.add(deleteEntry);
        mb.add(bibtex);

        quality.add(dupliCheck);
        quality.add(mergeEntries);
        quality.addSeparator();
        quality.add(resolveDuplicateKeys);
        quality.add(checkIntegrity);
        quality.add(cleanupEntries);
        quality.add(makeKeyAction);
        quality.addSeparator();
        quality.add(autoSetFile);
        quality.add(findUnlinkedFiles);
        quality.add(autoLinkFile);
        quality.add(downloadFullText);
        mb.add(quality);

        tools.add(newSubDatabaseAction);
        tools.add(writeXmpAction);
        OpenOfficePanel otp = OpenOfficePanel.getInstance();
        otp.init(this, sidePaneManager);
        tools.add(otp.getMenuItem());
        tools.add(pushExternalButton.getMenuAction());
        tools.addSeparator();
        tools.add(openFolder);
        tools.add(openFile);
        tools.add(openUrl);
        tools.add(openConsole);
        tools.addSeparator();
        tools.add(abbreviateIso);
        tools.add(abbreviateMedline);
        tools.add(unabbreviate);
        mb.add(tools);

        options.add(showPrefs);

        AbstractAction genFieldsCustomization = new GenFieldsCustomizationAction();
        options.add(genFieldsCustomization);
        options.add(customExpAction);
        options.add(customImpAction);
        options.add(customFileTypesAction);
        options.add(manageJournals);
        options.add(manageSelectors);
        options.add(selectKeys);
        mb.add(options);

        helpMenu.add(help);
        helpMenu.addSeparator();
        helpMenu.add(errorConsole);
        helpMenu.addSeparator();
        helpMenu.add(forkMeOnGitHubAction);
        helpMenu.add(donationAction);
        helpMenu.addSeparator();
        helpMenu.add(about);
        mb.add(helpMenu);

        createDisabledIconsForMenuEntries(mb);
    }

    public static JMenu subMenu(String name) {
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
            BasePanel panel = getCurrentBasePanel();
            if (panel == null) {
                // There is no open tab to add to, so we create a new tab:
                addTab(pr.getDatabaseContext(), pr.getEncoding(), raisePanel);
            } else {
                List<BibEntry> entries = new ArrayList<>(pr.getDatabase().getEntries());
                addImportedEntries(panel, entries, false);
            }
        } else {
            addTab(pr.getDatabaseContext(), pr.getEncoding(), raisePanel);
        }
    }

    private void createToolBar() {
        tlb.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
        tlb.setBorder(null);
        tlb.setRollover(true);

        tlb.setFloatable(false);
        if(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)) {
            tlb.addAction(newBiblatexDatabaseAction);
        } else {
            tlb.addAction(newBibtexDatabaseAction);
        }
        tlb.addAction(getOpenDatabaseAction());
        tlb.addAction(save);
        tlb.addAction(saveAll);

        tlb.addSeparator();
        tlb.addAction(cut);
        tlb.addAction(copy);
        tlb.addAction(paste);
        tlb.addAction(undo);
        tlb.addAction(redo);

        tlb.addSeparator();
        tlb.addAction(getBackAction());
        tlb.addAction(getForwardAction());
        tlb.addSeparator();
        tlb.addAction(newEntryAction);
        tlb.addAction(editEntry);
        tlb.addAction(editStrings);
        tlb.addAction(deleteEntry);
        tlb.addSeparator();
        tlb.addAction(makeKeyAction);
        tlb.addAction(cleanupEntries);
        tlb.addAction(mergeEntries);
        tlb.addAction(openConsole);

        tlb.addSeparator();
        tlb.addAction(mark);
        tlb.addAction(unmark);
        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING)) {
                JButton button = net.sf.jabref.specialfields.SpecialFieldDropDown
                        .generateSpecialFieldButtonWithDropDown(Rank.getInstance(), this);
                tlb.add(button);
                specialFieldButtons.add(button);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE)) {
                tlb.addAction(toggleRelevance);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY)) {
                tlb.addAction(toggleQualityAssured);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY)) {
                JButton button = net.sf.jabref.specialfields.SpecialFieldDropDown
                        .generateSpecialFieldButtonWithDropDown(Priority.getInstance(), this);
                tlb.add(button);
                specialFieldButtons.add(button);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED)) {
                tlb.addAction(togglePrinted);
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ)) {
                JButton button = net.sf.jabref.specialfields.SpecialFieldDropDown
                        .generateSpecialFieldButtonWithDropDown(ReadStatus.getInstance(), this);
                tlb.add(button);
                specialFieldButtons.add(button);
            }
        }
        tlb.addSeparator();

        fetcherToggle = new JToggleButton(generalFetcher.getAction());
        tlb.addJToogleButton(fetcherToggle);

        previewToggle = new JToggleButton(togglePreview);
        tlb.addJToogleButton(previewToggle);

        groupToggle = new JToggleButton(toggleGroups);
        tlb.addJToogleButton(groupToggle);

        tlb.addSeparator();

        tlb.add(pushExternalButton.getComponent());
        tlb.addSeparator();
        tlb.add(donationAction);
    }

    public void output(final String s) {
        SwingUtilities.invokeLater(() -> {
            statusLine.setText(s);
            statusLine.repaint();
        });
    }

    private void initActions() {
        openDatabaseOnlyActions.clear();
        openDatabaseOnlyActions.addAll(Arrays.asList(manageSelectors, mergeDatabaseAction, newSubDatabaseAction, save,
                saveAs, saveSelectedAs, saveSelectedAsPlain, undo, redo, cut, deleteEntry, copy, paste, mark, unmark,
                unmarkAll, editEntry, selectAll, copyKey, copyCiteKey, copyKeyAndTitle, editPreamble, editStrings,
                toggleGroups, makeKeyAction, normalSearch, mergeEntries, cleanupEntries, exportToClipboard, replaceAll,
                sendAsEmail, downloadFullText, writeXmpAction, findUnlinkedFiles, addToGroup, removeFromGroup,
                moveToGroup, autoLinkFile, resolveDuplicateKeys, openUrl, openFolder, openFile, togglePreview,
                dupliCheck, autoSetFile, newEntryAction, plainTextImport, getMassSetField(), getManageKeywords(),
                pushExternalButton.getMenuAction(), closeDatabaseAction, getSwitchPreviewAction(), checkIntegrity,
                toggleHighlightAny, toggleHighlightAll, toggleHighlightDisable, databaseProperties, abbreviateIso, abbreviateMedline,
                unabbreviate, exportAll, exportSelected, importCurrent, saveAll, dbConnect, dbExport, focusTable,
                toggleRelevance, toggleQualityAssured, togglePrinted, pushExternalButton.getComponent()));

        openDatabaseOnlyActions.addAll(newSpecificEntryAction);

        openDatabaseOnlyActions.addAll(specialFieldButtons);

        severalDatabasesOnlyActions.clear();
        severalDatabasesOnlyActions.addAll(Arrays
                .asList(nextTab, prevTab, sortTabs));

        openAndSavedDatabasesOnlyActions.addAll(Arrays.asList(openConsole));

        tabbedPane.addChangeListener(event -> updateEnabledState());

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

    /**
     * Enable or Disable all actions based on the number of open tabs.
     * <p>
     * The action that are affected are set in initActions.
     */
    public void updateEnabledState() {
        int tabCount = tabbedPane.getTabCount();
        if (tabCount != previousTabCount) {
            previousTabCount = tabCount;
            JabRefFrame.setEnabled(openDatabaseOnlyActions, tabCount > 0);
            JabRefFrame.setEnabled(severalDatabasesOnlyActions, tabCount > 1);
        }
        if (tabCount == 0) {
            getBackAction().setEnabled(false);
            getForwardAction().setEnabled(false);
        }

        boolean saved = false;

        if (tabCount > 0) {
            saved = getCurrentBasePanel().getBibDatabaseContext().getDatabaseFile() != null;
        }
        JabRefFrame.setEnabled(openAndSavedDatabasesOnlyActions, saved);
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
            BasePanel bf = getBasePanelAt(i);

            // Update tables:
            if (bf.getDatabase() != null) {
                bf.setupMainPanel();

            }
        }
    }

    public BasePanel addTab(BibDatabaseContext databaseContext, Charset encoding, boolean raisePanel) {
        Objects.requireNonNull(databaseContext);

        if (encoding == null) {
            encoding = Globals.prefs.getDefaultEncoding();
        }

        BasePanel bp = new BasePanel(JabRefFrame.this, databaseContext, encoding);
        addTab(bp, raisePanel);
        return bp;
    }

    private List<String> collectDatabaseFilePaths() {
        List<String> dbPaths = new ArrayList<>(getBasePanelCount());

        for (BasePanel basePanel : getBasePanelList()) {
            try {
                // db file exists
                if (basePanel.getBibDatabaseContext().getDatabaseFile() == null) {
                    dbPaths.add("");
                } else {
                    dbPaths.add(basePanel.getBibDatabaseContext().getDatabaseFile().getCanonicalPath());
                }
            } catch (IOException ex) {
                LOGGER.error("Invalid database file path: " + ex.getMessage());
            }
        }
        return dbPaths;
    }

    private List<String> getUniquePathParts() {
        List<String> dbPaths = collectDatabaseFilePaths();

        return FileUtil.uniquePathSubstrings(dbPaths);
    }

    public void updateAllTabTitles() {
        List<String> paths = getUniquePathParts();
        for (int i = 0; i < getBasePanelCount(); i++) {
            String uniqPath = paths.get(i);
            File file = getBasePanelAt(i).getBibDatabaseContext().getDatabaseFile();

            if ((file != null) && !uniqPath.equals(file.getName())) {
                // remove filename
                uniqPath = uniqPath.substring(0, uniqPath.lastIndexOf(File.separator));
                tabbedPane.setTitleAt(i, getBasePanelAt(i).getTabTitle() + " \u2014 " + uniqPath);
            } else if ((file != null) && uniqPath.equals(file.getName())) {
                // set original filename (again)
                tabbedPane.setTitleAt(i, getBasePanelAt(i).getTabTitle());
            }
            tabbedPane.setToolTipTextAt(i, file == null ? null : file.getAbsolutePath());
        }
    }

    public void addTab(BasePanel bp, boolean raisePanel) {
        // add tab
        tabbedPane.add(bp.getTabTitle(), bp);

        // update all tab titles
        updateAllTabTitles();

        if (raisePanel) {
            tabbedPane.setSelectedComponent(bp);
        }
    }

    /**
     * Creates icons for the disabled state for all JMenuItems with FontBasedIcons in the given menuElement.
     * This is necessary as Swing is not able to generate default disabled icons for font based icons.
     *
     * @param menuElement the menuElement for which disabled icons should be generated
     */
    public void createDisabledIconsForMenuEntries(MenuElement menuElement) {
        for (MenuElement subElement : menuElement.getSubElements()) {
            if ((subElement instanceof JMenu) || (subElement instanceof JPopupMenu)) {
                createDisabledIconsForMenuEntries(subElement);
            } else if (subElement instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) subElement;
                if (item.getIcon() instanceof IconTheme.FontBasedIcon) {
                    item.setDisabledIcon(((IconTheme.FontBasedIcon) item.getIcon()).createDisabledIcon());
                }
            }
        }
    }

    private class SelectKeysAction extends AbstractAction {

        public SelectKeysAction() {
            super(Localization.lang("Customize key bindings"));
            this.putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.KEY_BINDINGS.getSmallIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            KeyBindingsDialog d = new KeyBindingsDialog(new KeyBindingRepository(Globals.getKeyPrefs().getKeyBindings()));
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.pack(); //setSize(300,500);
            d.setLocationRelativeTo(JabRefFrame.this);
            d.setVisible(true);
        }
    }

    /**
     * The action concerned with closing the window.
     */
    private class CloseAction extends MnemonicAwareAction {

        public CloseAction() {
            putValue(Action.NAME, Localization.menuTitle("Quit"));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Quit JabRef"));
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.QUIT_JABREF));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            quit();
        }
    }

    private class ShowPrefsAction extends MnemonicAwareAction {

        public ShowPrefsAction() {
            super(IconTheme.JabRefIcon.PREFERENCES.getIcon());
            putValue(Action.NAME, Localization.menuTitle("Preferences"));
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
     * @param openInNew Should the entries be imported into a new database?
     */
    private void addImportedEntries(final BasePanel panel, final List<BibEntry> entries, final boolean openInNew) {
        SwingUtilities.invokeLater(() -> {
            ImportInspectionDialog diag = new ImportInspectionDialog(JabRefFrame.this, panel,
                    Localization.lang("Import"), openInNew);
            diag.addEntries(entries);
            diag.entryListComplete();
            diag.setLocationRelativeTo(JabRefFrame.this);
            diag.setVisible(true);
            diag.toFront();
        });
    }

    public FileHistoryMenu getFileHistory() {
        return fileHistory;
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
            SwingUtilities.invokeLater(() -> progressBar.setVisible(visible));
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
            SwingUtilities.invokeLater(() -> progressBar.setValue(value));
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
            SwingUtilities.invokeLater(() -> progressBar.setIndeterminate(value));
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
            SwingUtilities.invokeLater(() -> progressBar.setMaximum(value));
        }

    }

    private class ChangeTabAction extends MnemonicAwareAction {

        private final boolean next;

        public ChangeTabAction(boolean next) {
            putValue(Action.NAME, next ? Localization.menuTitle("Next tab") :
                    Localization.menuTitle("Previous tab"));
            this.next = next;
            putValue(Action.ACCELERATOR_KEY,
                    next ? Globals.getKeyPrefs().getKey(KeyBinding.NEXT_TAB) : Globals.getKeyPrefs().getKey(KeyBinding.PREVIOUS_TAB));
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
    private class EditAction extends MnemonicAwareAction {

        private final String command;

        public EditAction(String command, String menuTitle, String description, KeyStroke key, Icon icon) {
            super(icon);
            this.command = command;
            putValue(Action.NAME, menuTitle);
            putValue(Action.ACCELERATOR_KEY, key);
            putValue(Action.SHORT_DESCRIPTION, description);
        }

        @Override public void actionPerformed(ActionEvent e) {

            LOGGER.debug(Globals.focusListener.getFocused().toString());
            JComponent source = Globals.focusListener.getFocused();
            Action action = source.getActionMap().get(command);
            if (action != null) {
                action.actionPerformed(new ActionEvent(source, 0, command));
            }
        }
    }

    private class CustomizeExportsAction extends MnemonicAwareAction {

        public CustomizeExportsAction() {
            putValue(Action.NAME, Localization.menuTitle("Manage custom exports"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ExportCustomizationDialog ecd = new ExportCustomizationDialog(JabRefFrame.this);
            ecd.setVisible(true);
        }
    }

    private class CustomizeImportsAction extends MnemonicAwareAction {

        public CustomizeImportsAction() {
            putValue(Action.NAME, Localization.menuTitle("Manage custom imports"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ImportCustomizationDialog ecd = new ImportCustomizationDialog(JabRefFrame.this);
            ecd.setVisible(true);
        }
    }

    private class CustomizeEntryTypeAction extends MnemonicAwareAction {

        public CustomizeEntryTypeAction() {
            putValue(Action.NAME, Localization.menuTitle("Customize entry types"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JDialog dl = new EntryCustomizationDialog(JabRefFrame.this);
            dl.setLocationRelativeTo(JabRefFrame.this);
            dl.setVisible(true);
        }
    }

    private class GenFieldsCustomizationAction extends MnemonicAwareAction {

        public GenFieldsCustomizationAction() {
            putValue(Action.NAME, Localization.menuTitle("Set up general fields"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GenFieldsCustomizer gf = new GenFieldsCustomizer(JabRefFrame.this);
            gf.setLocationRelativeTo(JabRefFrame.this);
            gf.setVisible(true);

        }
    }

    private class DatabasePropertiesAction extends MnemonicAwareAction {

        private DatabasePropertiesDialog propertiesDialog;

        public DatabasePropertiesAction() {
            putValue(Action.NAME, Localization.menuTitle("Database properties"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (propertiesDialog == null) {
                propertiesDialog = new DatabasePropertiesDialog(JabRefFrame.this);
            }
            propertiesDialog.setPanel(getCurrentBasePanel());
            propertiesDialog.setLocationRelativeTo(JabRefFrame.this);
            propertiesDialog.setVisible(true);
        }

    }

    private class BibtexKeyPatternAction extends MnemonicAwareAction {

        private BibtexKeyPatternDialog bibtexKeyPatternDialog;

        public BibtexKeyPatternAction() {
            putValue(Action.NAME, Localization.lang("BibTeX key patterns"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JabRefPreferences.getInstance();
            if (bibtexKeyPatternDialog == null) {
                // if no instance of BibtexKeyPatternDialog exists, create new one
                bibtexKeyPatternDialog = new BibtexKeyPatternDialog(JabRefFrame.this, getCurrentBasePanel());
            } else {
                // BibtexKeyPatternDialog allows for updating content based on currently selected panel
                bibtexKeyPatternDialog.setPanel(getCurrentBasePanel());
            }
            bibtexKeyPatternDialog.setLocationRelativeTo(JabRefFrame.this);
            bibtexKeyPatternDialog.setVisible(true);
        }

    }

    private class IncreaseTableFontSizeAction extends MnemonicAwareAction {

        public IncreaseTableFontSizeAction() {
            putValue(Action.NAME, Localization.menuTitle("Increase table font size"));
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.INCREASE_TABLE_FONT_SIZE));
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            int currentSize = GUIGlobals.currentFont.getSize();
            GUIGlobals.currentFont = new Font(GUIGlobals.currentFont.getFamily(), GUIGlobals.currentFont.getStyle(),
                    currentSize + 1);
            Globals.prefs.putInt(JabRefPreferences.FONT_SIZE, currentSize + 1);
            for (BasePanel basePanel : getBasePanelList()) {
                basePanel.updateTableFont();
            }
            setStatus(Localization.lang("Table font size is %0", String.valueOf(GUIGlobals.currentFont.getSize())));
        }
    }

    private class DecreaseTableFontSizeAction extends MnemonicAwareAction {

        public DecreaseTableFontSizeAction() {
            putValue(Action.NAME, Localization.menuTitle("Decrease table font size"));
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.DECREASE_TABLE_FONT_SIZE));
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            int currentSize = GUIGlobals.currentFont.getSize();
            if (currentSize < 2) {
                return;
            }
            GUIGlobals.currentFont = new Font(GUIGlobals.currentFont.getFamily(), GUIGlobals.currentFont.getStyle(),
                    currentSize - 1);
            Globals.prefs.putInt(JabRefPreferences.FONT_SIZE, currentSize - 1);
            for (BasePanel basePanel : getBasePanelList()) {
                basePanel.updateTableFont();
            }
            setStatus(Localization.lang("Table font size is %0", String.valueOf(GUIGlobals.currentFont.getSize())));
        }
    }

    private static class MyGlassPane extends JPanel {
        public MyGlassPane() {
            addKeyListener(new KeyAdapter() {
                // Nothing
            });
            addMouseListener(new MouseAdapter() {
                // Nothing
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

    private int showSaveDialog(String filename) {
        Object[] options = {Localization.lang("Save changes"),
                Localization.lang("Discard changes"),
                Localization.lang("Return to JabRef")};

        return JOptionPane.showOptionDialog(JabRefFrame.this,
                Localization.lang("Database '%0' has changed.", filename),
                Localization.lang("Save before closing"), JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[2]);
    }

    private void closeTab(BasePanel panel) {
        // empty tab without database
        if (panel == null) {
            return;
        }

        if (panel.isModified()) {
            if (confirmClose(panel)) {
                removeTab(panel);
            }
        } else {
            removeTab(panel);
        }
    }

    // Ask if the user really wants to close, if the base has not been saved
    private boolean confirmClose(BasePanel panel) {
        boolean close = false;
        String filename;

        if (panel.getBibDatabaseContext().getDatabaseFile() == null) {
            filename = GUIGlobals.UNTITLED_TITLE;
        } else {
            filename = panel.getBibDatabaseContext().getDatabaseFile().getAbsolutePath();
        }

        int answer = showSaveDialog(filename);
        if (answer == JOptionPane.YES_OPTION) {
            // The user wants to save.
            try {
                SaveDatabaseAction saveAction = new SaveDatabaseAction(panel);
                saveAction.runCommand();
                if (saveAction.isSuccess()) {
                    close = true;
                }
            } catch (Throwable ex) {
                // do not close
            }

        } else if (answer == JOptionPane.NO_OPTION) {
            // discard changes
            close = true;
        }
        return close;
    }

    private void removeTab(BasePanel panel) {
        panel.cleanUp();
        AutoSaveManager.deleteAutoSaveFile(panel);
        tabbedPane.remove(panel);
        if (tabbedPane.getTabCount() > 0) {
            markActiveBasePanel();
        }
        setWindowTitle();
        updateEnabledState();
        output(Localization.lang("Closed database") + '.');
        // update tab titles
        updateAllTabTitles();
    }

    public ManageKeywordsAction getManageKeywords() {
        return manageKeywords;
    }

    public MassSetFieldAction getMassSetField() {
        return massSetField;
    }

    public OpenDatabaseAction getOpenDatabaseAction() {
        return open;
    }

    private class CloseDatabaseAction extends MnemonicAwareAction {

        public CloseDatabaseAction() {
            super(IconTheme.JabRefIcon.CLOSE.getSmallIcon());
            putValue(Action.NAME, Localization.menuTitle("Close database"));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close the current database"));
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DATABASE));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            closeTab(getCurrentBasePanel());
        }
    }

    private class CloseAllDatabasesAction extends MnemonicAwareAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            final Component[] panels = tabbedPane.getComponents();

            for (Component p : panels) {
                closeTab((BasePanel) p);
            }
        }
    }

    private class CloseOtherDatabasesAction extends MnemonicAwareAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            final BasePanel active = getCurrentBasePanel();
            final Component[] panels = tabbedPane.getComponents();

            for (Component p : panels) {
                if (!Objects.equals(p, active)) {
                    closeTab((BasePanel) p);
                }
            }
        }
    }

    private class ToolBar extends OSXCompatibleToolbar {

        public void addAction(Action a) {
            JButton b = new JButton(a);
            b.setText(null);
            if (!OS.OS_X) {
                b.setMargin(marg);
            }
            // create a disabled Icon for FontBasedIcons as Swing does not automatically create one
            Object obj = a.getValue(Action.LARGE_ICON_KEY);
            if (obj instanceof IconTheme.FontBasedIcon) {
                b.setDisabledIcon(((IconTheme.FontBasedIcon) obj).createDisabledIcon());
            }
            add(b);
        }

        public void addJToogleButton(JToggleButton button) {
            button.setText(null);
            if (!OS.OS_X) {
                button.setMargin(marg);
            }
            Object obj = button.getAction().getValue(Action.LARGE_ICON_KEY);
            if (obj instanceof IconTheme.FontBasedIcon) {
                button.setDisabledIcon(((IconTheme.FontBasedIcon) obj).createDisabledIcon());
            }
            add(button);
        }
    }


    public String getStatusLineText() {
        return statusLine.getText();
    }

    public AbstractAction getForwardAction() {
        return forward;
    }

    public AbstractAction getBackAction() {
        return back;
    }

    public AbstractAction getSwitchPreviewAction() {
        return switchPreview;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public SidePaneManager getSidePaneManager() {
        return sidePaneManager;
    }

    public GroupSelector getGroupSelector() {
        return groupSelector;
    }

    public void setFetcherToggle(boolean enabled) {
        fetcherToggle.setSelected(enabled);
    }

    public void setPreviewToggle(boolean enabled) {
        previewToggle.setSelected(enabled);
    }
}
