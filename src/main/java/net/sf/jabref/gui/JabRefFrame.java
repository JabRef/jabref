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
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.*;
import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.gui.actions.*;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.gui.menus.ChangeEntryTypeMenu;
import net.sf.jabref.gui.menus.help.DonateAction;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.worker.MarkEntriesAction;
import net.sf.jabref.gui.preftabs.PreferencesDialog;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fetcher.GeneralFetcher;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.integrity.IntegrityCheck;
import net.sf.jabref.logic.integrity.IntegrityMessage;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.exporter.AutoSaveManager;
import net.sf.jabref.exporter.ExportCustomizationDialog;
import net.sf.jabref.exporter.ExportFormats;
import net.sf.jabref.exporter.SaveAllAction;
import net.sf.jabref.exporter.SaveDatabaseAction;
import net.sf.jabref.external.ExternalFileTypeEditor;
import net.sf.jabref.external.push.PushToApplicationButton;
import net.sf.jabref.external.push.PushToApplications;
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
import net.sf.jabref.util.ManageKeywordsAction;
import net.sf.jabref.util.MassSetFieldAction;
import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;
import osx.macadapter.MacAdapter;

/**
 * The main window of the application.
 */
public class JabRefFrame extends JFrame implements OutputPrinter {
    private static final Log LOGGER = LogFactory.getLog(JabRefFrame.class);

    private static final boolean biblatexMode = Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE);

    final JSplitPane contentPane = new JSplitPane();

    final JabRefPreferences prefs = Globals.prefs;
    private PreferencesDialog prefsDialog;

    private int lastTabbedPanelSelectionIndex = -1;

    // The sidepane manager takes care of populating the sidepane.
    public SidePaneManager sidePaneManager;

    public JTabbedPane tabbedPane; // initialized at constructor
    final String htmlPadding = "<html><div style='padding:2px 5px;'>";

    private final Insets marg = new Insets(1, 0, 2, 0);
    private final JabRef jabRef;

    private PositionWindow pw;

    private final GeneralAction checkIntegrity = new GeneralAction(Actions.CHECK_INTEGRITY, Localization.menuTitle("Check integrity")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            IntegrityCheck check = new IntegrityCheck();
            List<IntegrityMessage> messages = check.checkBibtexDatabase(getCurrentBasePanel().database());

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
                        new Object[]{"key", "field", "message"}
                );

                table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                ListSelectionModel selectionModel = table.getSelectionModel();

                selectionModel.addListSelectionListener(new ListSelectionListener() {

                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            String citeKey = (String) model[table.getSelectedRow()][0];
                            String fieldName = (String) model[table.getSelectedRow()][1];
                            getCurrentBasePanel().editEntryByKeyAndFocusField(citeKey, fieldName);
                        }
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

    class ToolBar extends JToolBar {
        void addAction(Action a) {
            JButton b = new JButton(a);
            b.setText(null);
            if (!OS.OS_X) {
                b.setMargin(marg);
            }
            // create a disabled Icon for FontBasedIcons as Swing does not automatically create one
            Object obj = a.getValue(Action.LARGE_ICON_KEY);
            if ((obj instanceof IconTheme.FontBasedIcon)) {
                b.setDisabledIcon(((IconTheme.FontBasedIcon) obj).createDisabledIcon());
            }
            add(b);
        }

        void addJToogleButton(JToggleButton button) {
            button.setText(null);
            if (!OS.OS_X) {
                button.setMargin(marg);
            }
            Object obj = button.getAction().getValue(Action.LARGE_ICON_KEY);
            if ((obj instanceof IconTheme.FontBasedIcon)) {
                button.setDisabledIcon(((IconTheme.FontBasedIcon) obj).createDisabledIcon());
            }
            add(button);
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

    private final FileHistoryMenu fileHistory = new FileHistoryMenu(prefs, this);

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
    public JToggleButton previewToggle;
    public JToggleButton fetcherToggle;

    final OpenDatabaseAction open = new OpenDatabaseAction(this, true);
    private final AbstractAction editModeAction = new EditModeAction();
    private final AbstractAction quit = new CloseAction();
    private final AbstractAction selectKeys = new SelectKeysAction();
    private final AbstractAction newDatabaseAction = new NewDatabaseAction(this);
    private final AbstractAction newSubDatabaseAction = new NewSubDatabaseAction(this);
    private final AbstractAction forkMeOnGitHubAction = new ForkMeOnGitHubAction();
    private final AbstractAction donationAction = new DonateAction();
    private final AbstractAction help = new HelpAction(Localization.menuTitle("JabRef help"), helpDiag,
            GUIGlobals.baseFrameHelp, Localization.lang("JabRef help"),
            prefs.getKey(KeyBinds.HELP));
    private final AbstractAction contents = new HelpAction(Localization.menuTitle("Help contents"), helpDiag,
            GUIGlobals.helpContents, Localization.lang("Help contents"),
            IconTheme.JabRefIcon.HELP_CONTENTS.getIcon());
    private final AbstractAction about = new HelpAction(Localization.menuTitle("About JabRef"), helpDiag,
            GUIGlobals.aboutPage, Localization.lang("About JabRef"),
            IconTheme.getImage("about"));
    private final AbstractAction editEntry = new GeneralAction(Actions.EDIT, Localization.menuTitle("Edit entry"),
            Localization.lang("Edit entry"), prefs.getKey(KeyBinds.EDIT_ENTRY), IconTheme.JabRefIcon.EDIT_ENTRY.getIcon());
    private final AbstractAction focusTable = new GeneralAction(Actions.FOCUS_TABLE,
            Localization.menuTitle("Focus entry table"),
            Localization.lang("Move the keyboard focus to the entry table"), prefs.getKey(KeyBinds.FOCUS_ENTRY_TABLE));
    private final AbstractAction save = new GeneralAction(Actions.SAVE, Localization.menuTitle("Save database"),
            Localization.lang("Save database"), prefs.getKey(KeyBinds.SAVE_DATABASE), IconTheme.JabRefIcon.SAVE.getIcon());
    private final AbstractAction saveAs = new GeneralAction(Actions.SAVE_AS,
            Localization.menuTitle("Save database as ..."), Localization.lang("Save database as ..."),
            prefs.getKey(KeyBinds.SAVE_DATABASE_AS));
    private final AbstractAction saveAll = new SaveAllAction(JabRefFrame.this);
    private final AbstractAction saveSelectedAs = new GeneralAction(Actions.SAVE_SELECTED_AS,
            Localization.menuTitle("Save selected as ..."), Localization.lang("Save selected as ..."));
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
            Localization.lang("Undo"), prefs.getKey(KeyBinds.UNDO), IconTheme.JabRefIcon.UNDO.getIcon());
    private final AbstractAction redo = new GeneralAction(Actions.REDO, Localization.menuTitle("Redo"),
            Localization.lang("Redo"), prefs.getKey(KeyBinds.REDO), IconTheme.JabRefIcon.REDO.getIcon());
    final AbstractAction forward = new GeneralAction(Actions.FORWARD, Localization.menuTitle("Forward"),
            Localization.lang("Forward"), prefs.getKey(KeyBinds.FORWARD), IconTheme.JabRefIcon.RIGHT.getIcon());
    final AbstractAction back = new GeneralAction(Actions.BACK, Localization.menuTitle("Back"),
            Localization.lang("Back"), prefs.getKey(KeyBinds.BACK), IconTheme.JabRefIcon.LEFT.getIcon());
    final AbstractAction deleteEntry = new GeneralAction(Actions.DELETE, Localization.menuTitle("Delete entry"),
            Localization.lang("Delete entry"), prefs.getKey(KeyBinds.DELETE_ENTRY), IconTheme.JabRefIcon.DELETE_ENTRY.getIcon());
    private final AbstractAction copy = new EditAction(Actions.COPY, Localization.menuTitle("Copy"),
            Localization.lang("Copy"), prefs.getKey(KeyBinds.COPY), IconTheme.JabRefIcon.COPY.getIcon());
    private final AbstractAction paste = new EditAction(Actions.PASTE, Localization.menuTitle("Paste"),
            Localization.lang("Paste"), prefs.getKey(KeyBinds.PASTE), IconTheme.JabRefIcon.PASTE.getIcon());
    private final AbstractAction cut = new EditAction(Actions.CUT, Localization.menuTitle("Cut"),
            Localization.lang("Cut"), prefs.getKey(KeyBinds.CUT), IconTheme.JabRefIcon.CUT.getIcon());
    private final AbstractAction mark = new GeneralAction(Actions.MARK_ENTRIES, Localization.menuTitle("Mark entries"),
            Localization.lang("Mark entries"), prefs.getKey(KeyBinds.MARK_ENTRIES), IconTheme.JabRefIcon.MARK_ENTRIES.getIcon());
    private final AbstractAction unmark = new GeneralAction(Actions.UNMARK_ENTRIES,
            Localization.menuTitle("Unmark entries"), Localization.lang("Unmark entries"),
            prefs.getKey(KeyBinds.UNMARK_ENTRIES), IconTheme.JabRefIcon.UNMARK_ENTRIES.getIcon());
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
    private final AbstractAction saveSessionAction = new SaveSessionAction();
    public final AbstractAction loadSessionAction = new LoadSessionAction();
    private final AbstractAction normalSearch = new GeneralAction(Actions.SEARCH, Localization.menuTitle("Search"),
            Localization.lang("Search"), prefs.getKey(KeyBinds.SEARCH), IconTheme.JabRefIcon.SEARCH.getIcon());

    private final AbstractAction copyKey = new GeneralAction(Actions.COPY_KEY, Localization.menuTitle("Copy BibTeX key"),
            prefs.getKey(KeyBinds.COPY_BIB_TE_X_KEY));
    private final AbstractAction copyCiteKey = new GeneralAction(Actions.COPY_CITE_KEY, Localization.menuTitle(
            "Copy \\cite{BibTeX key}"),
            prefs.getKey(KeyBinds.COPY_CITE_BIB_TE_X_KEY));
    private final AbstractAction copyKeyAndTitle = new GeneralAction(Actions.COPY_KEY_AND_TITLE,
            Localization.menuTitle("Copy BibTeX key and title"),
            prefs.getKey(KeyBinds.COPY_BIB_TE_X_KEY_AND_TITLE));
    private final AbstractAction mergeDatabaseAction = new GeneralAction(Actions.MERGE_DATABASE,
            Localization.menuTitle("Append database"),
            Localization.lang("Append contents from a BibTeX database into the currently viewed database"));
    private final AbstractAction selectAll = new GeneralAction(Actions.SELECT_ALL, Localization.menuTitle("Select all"),
            prefs.getKey(KeyBinds.SELECT_ALL));
    private final AbstractAction replaceAll = new GeneralAction(Actions.REPLACE_ALL,
            Localization.menuTitle("Replace string"), prefs.getKey(KeyBinds.REPLACE_STRING));

    private final AbstractAction editPreamble = new GeneralAction(Actions.EDIT_PREAMBLE,
            Localization.menuTitle("Edit preamble"),
            Localization.lang("Edit preamble"),
            prefs.getKey(KeyBinds.EDIT_PREAMBLE));
    private final AbstractAction editStrings = new GeneralAction(Actions.EDIT_STRINGS,
            Localization.menuTitle("Edit strings"),
            Localization.lang("Edit strings"),
            prefs.getKey(KeyBinds.EDIT_STRINGS),
            IconTheme.JabRefIcon.EDIT_STRINGS.getIcon());
    private final AbstractAction toggleToolbar = new AbstractAction(Localization.menuTitle("Hide/show toolbar")) {
        {
            putValue(Action.ACCELERATOR_KEY, prefs.getKey(KeyBinds.HIDE_SHOW_TOOLBAR));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Hide/show toolbar"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tlb.setVisible(!tlb.isVisible());
        }
    };
    private final AbstractAction toggleGroups = new GeneralAction(Actions.TOGGLE_GROUPS,
            Localization.menuTitle("Toggle groups interface"),
            Localization.lang("Toggle groups interface"),
            prefs.getKey(KeyBinds.TOGGLE_GROUPS_INTERFACE),
            IconTheme.JabRefIcon.TOGGLE_GROUPS.getIcon());
    private final AbstractAction addToGroup = new GeneralAction(Actions.ADD_TO_GROUP, Localization.lang("Add to group"));
    private final AbstractAction removeFromGroup = new GeneralAction(Actions.REMOVE_FROM_GROUP,
            Localization.lang("Remove from group"));
    private final AbstractAction moveToGroup = new GeneralAction(Actions.MOVE_TO_GROUP, Localization.lang("Move to group"));


    private final AbstractAction togglePreview = new GeneralAction(Actions.TOGGLE_PREVIEW,
            Localization.menuTitle("Toggle entry preview"),
            Localization.lang("Toggle entry preview"),
            prefs.getKey(KeyBinds.TOGGLE_ENTRY_PREVIEW),
            IconTheme.JabRefIcon.TOGGLE_ENTRY_PREVIEW.getIcon());
    private final AbstractAction toggleHighlightAny = new GeneralAction(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ANY,
            Localization.menuTitle("Highlight groups matching any selected entry"),
            Localization.lang("Highlight groups matching any selected entry"));
    private final AbstractAction toggleHighlightAll = new GeneralAction(Actions.TOGGLE_HIGHLIGHTS_GROUPS_MATCHING_ALL,
            Localization.menuTitle("Highlight groups matching all selected entries"),
            Localization.lang("Highlight groups matching all selected entries"));
    final AbstractAction switchPreview = new GeneralAction(Actions.SWITCH_PREVIEW,
            Localization.menuTitle("Switch preview layout"),
            prefs.getKey(KeyBinds.SWITCH_PREVIEW_LAYOUT));
    private final AbstractAction makeKeyAction = new GeneralAction(Actions.MAKE_KEY,
            Localization.menuTitle("Autogenerate BibTeX keys"),
            Localization.lang("Autogenerate BibTeX keys"),
            prefs.getKey(KeyBinds.AUTOGENERATE_BIB_TE_X_KEYS),
            IconTheme.JabRefIcon.MAKE_KEY.getIcon());

    private final AbstractAction writeXmpAction = new GeneralAction(Actions.WRITE_XMP,
            Localization.menuTitle("Write XMP-metadata to PDFs"),
            Localization.lang("Will write XMP-metadata to the PDFs linked from selected entries."),
            prefs.getKey(KeyBinds.WRITE_XMP));

    private final AbstractAction openFolder = new GeneralAction(Actions.OPEN_FOLDER,
            Localization.menuTitle("Open folder"), Localization.lang("Open folder"),
            prefs.getKey(KeyBinds.OPEN_FOLDER));
    private final AbstractAction openFile = new GeneralAction(Actions.OPEN_EXTERNAL_FILE,
            Localization.menuTitle("Open file"),
            Localization.lang("Open file"),
            prefs.getKey(KeyBinds.OPEN_FILE),
            IconTheme.JabRefIcon.FILE.getIcon());
    private final AbstractAction openUrl = new GeneralAction(Actions.OPEN_URL,
            Localization.menuTitle("Open URL or DOI"),
            Localization.lang("Open URL or DOI"),
            prefs.getKey(KeyBinds.OPEN_URL_OR_DOI),
            IconTheme.JabRefIcon.WWW.getIcon());
    private final AbstractAction dupliCheck = new GeneralAction(Actions.DUPLI_CHECK,
            Localization.menuTitle("Find duplicates"), IconTheme.JabRefIcon.FIND_DUPLICATES.getIcon());
    private final AbstractAction plainTextImport = new GeneralAction(Actions.PLAIN_TEXT_IMPORT,
            Localization.menuTitle("New entry from plain text"),
            prefs.getKey(KeyBinds.NEW_FROM_PLAIN_TEXT));


    private final AbstractAction customExpAction = new CustomizeExportsAction();
    private final AbstractAction customImpAction = new CustomizeImportsAction();
    private final AbstractAction customFileTypesAction = ExternalFileTypeEditor.getAction(this);
    private final AbstractAction exportToClipboard = new GeneralAction(Actions.EXPORT_TO_CLIPBOARD,
            Localization.menuTitle("Export selected entries to clipboard"),
            IconTheme.JabRefIcon.EXPORT_TO_CLIPBOARD.getIcon());
    private final AbstractAction autoSetFile = new GeneralAction(Actions.AUTO_SET_FILE,
            Localization.lang("Synchronize file links"),
            Globals.prefs.getKey(KeyBinds.SYNCHRONIZE_FILES));

    private final AbstractAction abbreviateMedline = new GeneralAction(Actions.ABBREVIATE_MEDLINE,
            Localization.menuTitle("Abbreviate journal names (MEDLINE)"),
            Localization.lang("Abbreviate journal names of the selected entries (MEDLINE abbreviation)"));
    private final AbstractAction abbreviateIso = new GeneralAction(Actions.ABBREVIATE_ISO,
            Localization.menuTitle("Abbreviate journal names (ISO)"),
            Localization.lang("Abbreviate journal names of the selected entries (ISO abbreviation)"),
            Globals.prefs.getKey(KeyBinds.ABBREVIATE));

    private final AbstractAction unabbreviate = new GeneralAction(Actions.UNABBREVIATE,
            Localization.menuTitle("Unabbreviate journal names"),
            Localization.lang("Unabbreviate journal names of the selected entries"),
            Globals.prefs.getKey(KeyBinds.UNABBREVIATE));
    private final AbstractAction manageJournals = new ManageJournalsAction(this);
    private final AbstractAction databaseProperties = new DatabasePropertiesAction();
    private final AbstractAction bibtexKeyPattern = new BibtexKeyPatternAction();
    private final AbstractAction errorConsole = new ErrorConsoleAction(this, Globals.streamEavesdropper, Globals.handler);

    private final AbstractAction dbConnect = new GeneralAction(Actions.DB_CONNECT,
            Localization.menuTitle("Connect to external SQL database"),
            Localization.lang("Connect to external SQL database"));

    private final AbstractAction dbExport = new GeneralAction(Actions.DB_EXPORT,
            Localization.menuTitle("Export to external SQL database"),
            Localization.lang("Export to external SQL database"));

    private final AbstractAction cleanupEntries = new GeneralAction(Actions.CLEANUP,
            Localization.menuTitle("Cleanup entries"),
            Localization.lang("Cleanup entries"),
            prefs.getKey(KeyBinds.CLEANUP),
            IconTheme.JabRefIcon.CLEANUP_ENTRIES.getIcon());

    private final AbstractAction mergeEntries = new GeneralAction(Actions.MERGE_ENTRIES,
            Localization.menuTitle("Merge entries"),
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
            prefs.getKey(KeyBinds.RESOLVE_DUPLICATE_BIB_TE_X_KEYS));

    private final AbstractAction sendAsEmail = new GeneralAction(Actions.SEND_AS_EMAIL,
            Localization.lang("Send as email"), IconTheme.JabRefIcon.EMAIL.getIcon());

    final MassSetFieldAction massSetField = new MassSetFieldAction(this);
    final ManageKeywordsAction manageKeywords = new ManageKeywordsAction(this);

    private final GeneralAction findUnlinkedFiles = new GeneralAction(
            FindUnlinkedFilesDialog.ACTION_COMMAND,
            FindUnlinkedFilesDialog.ACTION_MENU_TITLE, FindUnlinkedFilesDialog.ACTION_SHORT_DESCRIPTION,
            prefs.getKey(FindUnlinkedFilesDialog.ACTION_KEYBINDING_ACTION)
    );

    private final AutoLinkFilesAction autoLinkFile = new AutoLinkFilesAction();

    private PushToApplicationButton pushExternalButton;

    GeneralFetcher generalFetcher;

    private final List<Action> fetcherActions = new LinkedList<>();

    public GroupSelector groupSelector;

    // The action for adding a new entry of unspecified type.
    private final NewEntryAction newEntryAction = new NewEntryAction(this, prefs.getKey(KeyBinds.NEW_ENTRY));
    // @formatter:off
    private final List<NewEntryAction> newSpecificEntryAction = getNewEntryActions();

    private List<NewEntryAction> getNewEntryActions() {
        List<NewEntryAction> actions = new ArrayList<>();

        if (biblatexMode) {
            for (String key : EntryTypes.getAllTypes()) {
                actions.add(new NewEntryAction(this, key));
            }
        } else {
            // Bibtex
            for (EntryType type : BibtexEntryTypes.ALL) {
                KeyStroke keyStroke = ChangeEntryTypeMenu.entryShortCuts.get(type.getName());
                if(keyStroke != null) {
                    actions.add(new NewEntryAction(this, type.getName(), keyStroke));
                } else {
                    actions.add(new NewEntryAction(this, type.getName()));
                }
            }
            // ieeetran
            for (EntryType type : IEEETranEntryTypes.ALL) {
                actions.add(new NewEntryAction(this, type.getName()));
            }
            // custom types
            for (EntryType type : CustomEntryTypesManager.ALL) {
                actions.add(new NewEntryAction(this, type.getName()));
            }
        }
        return actions;
    }

    public JabRefFrame(JabRef jabRef) {
        this.jabRef = jabRef;
        init();
        updateEnabledState();

    }

    private void init() {
        tabbedPane = new DragDropPopupPane(manageSelectors, databaseProperties, bibtexKeyPattern, closeDatabaseAction);

        MyGlassPane glassPane = new MyGlassPane();
        setGlassPane(glassPane);
        // glassPane.setVisible(true);

        setTitle(GUIGlobals.frameTitle);
        setIconImage(new ImageIcon(IconTheme.getIconUrl("jabrefIcon48")).getImage());
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
        pw = new PositionWindow(this, JabRefPreferences.POS_X, JabRefPreferences.POS_Y, JabRefPreferences.SIZE_X,
                JabRefPreferences.SIZE_Y);
        positionWindowOnScreen();

        // Set up a ComponentListener that saves the last size and position of the dialog
        this.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                // Save dialog position
                pw.storeWindowPosition();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // Save dialog position
                pw.storeWindowPosition();
            }
        });


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

                BasePanel bp = getCurrentBasePanel();
                if (bp != null) {
                    groupToggle.setSelected(sidePaneManager.isComponentVisible("groups"));
                    previewToggle.setSelected(Globals.prefs.getBoolean(JabRefPreferences.PREVIEW_ENABLED));
                    fetcherToggle.setSelected(sidePaneManager.isComponentVisible(generalFetcher.getTitle()));
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
                LOGGER.fatal("Could not interface with Mac OS X methods.", e);
            }
        }
    }

    private void positionWindowOnScreen() {
        if (!prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            pw.setWindowPosition();
        }
    }

    /**
     * Tries to open a browser with the given URL
     * <p>
     * All errors are logged
     *
     * @param url the url to open
     */
    public void openBrowser(String url) {
        try {
            JabRefDesktop.openBrowser(url);
            output(Localization.lang("External viewer called") + '.');
        } catch (IOException ex) {
            output(Localization.lang("Error") + ": " + ex.getMessage());
            LOGGER.debug("Cannot open browser.", ex);
        }
    }

    /**
     * Sets the title of the main window.
     */
    public void setWindowTitle() {
        BasePanel panel = getCurrentBasePanel();
        String mode = biblatexMode ? " (" + Localization.lang("%0 mode", "BibLaTeX") + ")" : " (" + Localization.lang("%0 mode", "BibTeX") + ")";

        // no database open
        if (panel == null) {
            setTitle(GUIGlobals.frameTitle + mode);
            return;
        }

        String changeFlag = panel.isBaseChanged() ? "*" : "";

        if (panel.getDatabaseFile() != null) {
            String databaseFile = panel.getDatabaseFile().getPath();
            setTitle(GUIGlobals.frameTitle + " - " + databaseFile + changeFlag + mode);
        } else {
            setTitle(GUIGlobals.frameTitle + " - " + GUIGlobals.untitledTitle + changeFlag + mode);
        }
    }

    private void initSidePane() {
        sidePaneManager = new SidePaneManager(this);

        GUIGlobals.sidePaneManager = this.sidePaneManager;
        GUIGlobals.helpDiag = this.helpDiag;

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
        open.openFile(file, true);
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
        //PrefsDialog.showPrefsDialog(JabRefFrame.this, prefs);
        AbstractWorker worker = new AbstractWorker() {

            @Override
            public void run() {
                output(Localization.lang("Opening preferences..."));
                if (prefsDialog == null) {
                    prefsDialog = new PreferencesDialog(JabRefFrame.this, jabRef);
                    PositionWindow.placeDialog(prefsDialog, JabRefFrame.this);
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
     * @param filenames the filenames of all currently opened files - used for storing them if prefs openLastEdited is set to true
     */
    private void tearDownJabRef(Vector<String> filenames) {
        JabRefExecutorService.INSTANCE.shutdownEverything();

        dispose();

        if (getCurrentBasePanel() != null) {
            getCurrentBasePanel().saveDividerLocation();
        }

        //prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, (getExtendedState()&MAXIMIZED_BOTH)>0);
        prefs.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, getExtendedState() == Frame.MAXIMIZED_BOTH);

        prefs.putBoolean(JabRefPreferences.TOOLBAR_VISIBLE, tlb.isVisible());
        // Store divider location for side pane:
        int width = contentPane.getDividerLocation();
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
        Vector<String> filenames = new Vector<>();
        if (tabbedPane.getTabCount() > 0) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (getBasePanelAt(i).isBaseChanged()) {
                    tabbedPane.setSelectedIndex(i);
                    Object[] options = {Localization.lang("Save changes"),
                            Localization.lang("Discard changes"),
                            Localization.lang("Return to JabRef")};
                    String filename;

                    if (getBasePanelAt(i).getDatabaseFile() != null) {
                        filename = getBasePanelAt(i).getDatabaseFile().getAbsolutePath();
                    } else {
                        filename = GUIGlobals.untitledTitle;
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

                if (getBasePanelAt(i).getDatabaseFile() != null) {
                    filenames.add(getBasePanelAt(i).getDatabaseFile().getAbsolutePath());
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

        pushExternalButton = new PushToApplicationButton(this, PushToApplications.applications);
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

        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0,0,0,0));

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
    public BasePanel getBasePanelAt(int i) {
        return (BasePanel) tabbedPane.getComponentAt(i);
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
        tabbedPane.setTitleAt(index, htmlPadding + title);
        tabbedPane.setToolTipTextAt(index, toolTip);
    }


    class GeneralAction extends MnemonicAwareAction {

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
                    ex.printStackTrace();
                }
            } else {
                LOGGER.info("Action '" + command + "' must be disabled when no database is open.");
            }
        }
    }

    private void fillMenu() {
        //mb.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
        mb.setBorder(null);
        JMenu file = JabRefFrame.subMenu(Localization.menuTitle("File"));
        JMenu sessions = JabRefFrame.subMenu(Localization.menuTitle("Sessions"));
        JMenu edit = JabRefFrame.subMenu(Localization.menuTitle("Edit"));
        JMenu search = JabRefFrame.subMenu(Localization.menuTitle("Search"));
        JMenu groups = JabRefFrame.subMenu(Localization.menuTitle("Groups"));
        JMenu bibtex = JabRefFrame.subMenu(Localization.menuTitle("BibTeX"));
        JMenu view = JabRefFrame.subMenu(Localization.menuTitle("View"));
        JMenu tools = JabRefFrame.subMenu(Localization.menuTitle("Tools"));
        JMenu options = JabRefFrame.subMenu(Localization.menuTitle("Options"));
        JMenu newSpec = JabRefFrame.subMenu(Localization.menuTitle("New entry..."));
        JMenu helpMenu = JabRefFrame.subMenu(Localization.menuTitle("Help"));

        file.add(newDatabaseAction);
        file.add(open); //opendatabaseaction
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
        file.addSeparator();

        sessions.add(loadSessionAction);
        sessions.add(saveSessionAction);
        file.add(sessions);
        file.add(fileHistory);
        file.addSeparator();
        file.add(editModeAction);
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

        edit.add(manageKeywords);
        edit.addSeparator();
        edit.add(selectAll);
        mb.add(edit);

        search.add(normalSearch);
        search.add(replaceAll);
        search.add(massSetField);
        search.addSeparator();
        search.add(dupliCheck);
        search.add(resolveDuplicateKeys);
        //search.add(strictDupliCheck);
        search.addSeparator();
        search.add(generalFetcher.getAction());
        if (prefs.getBoolean(JabRefPreferences.WEB_SEARCH_VISIBLE)) {
            sidePaneManager.register(generalFetcher.getTitle(), generalFetcher);
            sidePaneManager.show(generalFetcher.getTitle());
        }
        mb.add(search);

        groups.add(toggleGroups);
        groups.addSeparator();
        groups.add(addToGroup);
        groups.add(removeFromGroup);
        groups.add(moveToGroup);
        groups.addSeparator();
        groups.add(toggleHighlightAny);
        groups.add(toggleHighlightAll);
        mb.add(groups);

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
        view.add(generalFetcher.getAction());
        view.add(toggleGroups);
        view.add(togglePreview);
        view.add(switchPreview);

        mb.add(view);

        bibtex.add(newEntryAction);

        for(NewEntryAction a : newSpecificEntryAction) {
            newSpec.add(a);
        }

        bibtex.add(newSpec);

        bibtex.add(plainTextImport);
        bibtex.addSeparator();
        bibtex.add(editEntry);
        bibtex.add(editPreamble);
        bibtex.add(editStrings);
        bibtex.addSeparator();
        bibtex.add(deleteEntry);
        mb.add(bibtex);

        tools.add(makeKeyAction);
        tools.add(cleanupEntries);
        tools.add(mergeEntries);
        tools.add(downloadFullText);
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
        tools.add(openUrl);
        //tools.add(openSpires);
        tools.addSeparator();
        tools.add(autoSetFile);
        tools.add(findUnlinkedFiles);
        tools.add(autoLinkFile);
        tools.addSeparator();
        tools.add(abbreviateIso);
        tools.add(abbreviateMedline);
        tools.add(unabbreviate);
        tools.addSeparator();
        tools.add(checkIntegrity);
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
        helpMenu.addSeparator();
        helpMenu.add(forkMeOnGitHubAction);
        helpMenu.add(donationAction);
        helpMenu.addSeparator();
        helpMenu.add(about);
        mb.add(helpMenu);

        createDisabledIconsForMenuEntries(mb);
    }


    private static void createEntryTypeSection(JMenu menu, String title, java.util.List<NewEntryAction> actions) {
        // bibtex
        JMenuItem header = new JMenuItem(title);
        Font font = new Font(menu.getFont().getName(), Font.ITALIC, menu.getFont().getSize());
        header.setFont(font);
        header.setEnabled(false);
        menu.add(header);

        for (NewEntryAction action : actions) {
            menu.add(action);
        }
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
                addTab(pr.getDatabase(), pr.getFile(), pr.getMetaData(), pr.getEncoding(), raisePanel);
            } else {
                List<BibtexEntry> entries = new ArrayList<>(pr.getDatabase().getEntries());
                addImportedEntries(panel, entries, false);
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
        tlb.addAction(editStrings);
        tlb.addAction(deleteEntry);
        tlb.addSeparator();
        tlb.addAction(makeKeyAction);
        tlb.addAction(cleanupEntries);
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
            tlb.addSeparator();
        }

        fetcherToggle = new JToggleButton(generalFetcher.getAction());
        tlb.addJToogleButton(fetcherToggle);

        previewToggle = new JToggleButton(togglePreview);
        tlb.addJToogleButton(previewToggle);

        groupToggle = new JToggleButton(toggleGroups);
        tlb.addJToogleButton(groupToggle);

        tlb.addSeparator();

        // Removing the separate push-to buttons, replacing them by the
        // multipurpose button:
        //tlb.addAction(emacsPushAction);
        //tlb.addAction(lyxPushAction);
        //tlb.addAction(winEdtPushAction);
        tlb.add(pushExternalButton.getComponent());
        tlb.addSeparator();
        tlb.add(donationAction);
//        tlb.addAction(openFolder);
//        tlb.addAction(openFile);
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
            getBasePanelAt(i).getFilterSearchToggle().stop();
        }
    }


    private List<Object> openDatabaseOnlyActions = new LinkedList<>();
    private List<Object> severalDatabasesOnlyActions = new LinkedList<>();


    private void initActions() {
        openDatabaseOnlyActions = new LinkedList<>();
        openDatabaseOnlyActions.addAll(Arrays.asList(manageSelectors, mergeDatabaseAction, newSubDatabaseAction, save,
                saveAs, saveSelectedAs, saveSelectedAsPlain, undo, redo, cut, deleteEntry, copy, paste, mark, unmark,
                unmarkAll, editEntry, selectAll, copyKey, copyCiteKey, copyKeyAndTitle, editPreamble, editStrings,
                toggleGroups, makeKeyAction, normalSearch, mergeEntries, cleanupEntries, exportToClipboard,
                replaceAll, sendAsEmail, downloadFullText, writeXmpAction,
                findUnlinkedFiles, addToGroup, removeFromGroup, moveToGroup, autoLinkFile, resolveDuplicateKeys,
                openUrl, openFolder, openFile, togglePreview, dupliCheck, autoSetFile,
                newEntryAction, plainTextImport, massSetField, manageKeywords, pushExternalButton.getMenuAction(),
                closeDatabaseAction, switchPreview, checkIntegrity, toggleHighlightAny, toggleHighlightAll,
                databaseProperties, abbreviateIso, abbreviateMedline, unabbreviate, exportAll, exportSelected,
                importCurrent, saveAll, dbConnect, dbExport, focusTable));

        openDatabaseOnlyActions.addAll(fetcherActions);

        openDatabaseOnlyActions.addAll(newSpecificEntryAction);

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
            BasePanel bf = getBasePanelAt(i);

            // Update tables:
            if (bf.getDatabase() != null) {
                bf.setupMainPanel();

            }
        }
    }

    public BasePanel addTab(BibtexDatabase db, File file, MetaData metaData, Charset encoding, boolean raisePanel) {
        // ensure that non-null parameters are really non-null
        if (metaData == null) {
            metaData = new MetaData();
        }
        if (encoding == null) {
            encoding = Globals.prefs.getDefaultEncoding();
        }

        BasePanel bp = new BasePanel(JabRefFrame.this, db, file, metaData, encoding);
        addTab(bp, file, raisePanel);
        return bp;
    }


    private List<String> collectDatabaseFilePaths() {
        List<String> dbPaths = new ArrayList<>(getBasePanelCount());

        for (int i = 0; i < getBasePanelCount(); i++) {
            try {
                // db file exists
                if(getBasePanelAt(i).getDatabaseFile() == null) {
                    dbPaths.add("");
                } else {
                    dbPaths.add(getBasePanelAt(i).getDatabaseFile().getCanonicalPath());
                }
            } catch (IOException ex) {
                LOGGER.error("Invalid database file path: " + ex.getMessage());
            }
        }
        return dbPaths;
    }

    private List<String> getUniquePathParts() {
        List<String> dbPaths = collectDatabaseFilePaths();
        List<String> uniquePaths = FileUtil.uniquePathSubstrings(dbPaths);

        return uniquePaths;
    }

    public void updateAllTabTitles() {
        List<String> paths = getUniquePathParts();
        for (int i = 0; i < getBasePanelCount(); i++) {
            String uniqPath = paths.get(i);
            File file = getBasePanelAt(i).getDatabaseFile();

            if ((file != null) && !uniqPath.equals(file.getName())) {
                // remove filename
                uniqPath = uniqPath.substring(0, uniqPath.lastIndexOf(File.separator));
                tabbedPane.setTitleAt(i, getBasePanelAt(i).getTabTitle() + " \u2014 " + uniqPath);
            } else if((file != null) && uniqPath.equals(file.getName())) {
                // set original filename (again)
                tabbedPane.setTitleAt(i, getBasePanelAt(i).getTabTitle());
            }
        }
    }

    public void addTab(BasePanel bp, File file, boolean raisePanel) {
        // add tab
        tabbedPane.add(bp.getTabTitle(), bp);
        tabbedPane.setToolTipTextAt(tabbedPane.getTabCount() - 1, file != null ? file.getAbsolutePath() : null);
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


    class SelectKeysAction extends AbstractAction {
        public SelectKeysAction() {
            super(Localization.lang("Customize key bindings"));
            this.putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.KEY_BINDINGS.getSmallIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            KeyBindingsDialog d = new KeyBindingsDialog(new HashMap<>(prefs.getKeyBindings()), prefs.getDefaultKeys());
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.pack(); //setSize(300,500);
            PositionWindow.placeDialog(d, JabRefFrame.this);
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
            putValue(Action.NAME, Localization.menuTitle("Quit"));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Quit JabRef"));
            putValue(Action.ACCELERATOR_KEY, prefs.getKey(KeyBinds.QUIT_JAB_REF));
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
            super(IconTheme.JabRefIcon.CLOSE.getSmallIcon());
            putValue(Action.NAME, Localization.menuTitle("Close database"));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close the current database"));
            putValue(Action.ACCELERATOR_KEY, prefs.getKey(KeyBinds.CLOSE_DATABASE));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Ask here if the user really wants to close, if the base
            // has not been saved since last save.
            boolean close = true;
            if (getCurrentBasePanel() == null) { // when it is initially empty
                return; // nbatada nov 7
            }

            if (getCurrentBasePanel().isBaseChanged()) {

                String filename;

                if (getCurrentBasePanel().getDatabaseFile() != null) {
                    filename = getCurrentBasePanel().getDatabaseFile().getAbsolutePath();
                } else {
                    filename = GUIGlobals.untitledTitle;
                }

                int answer = showSaveDialog(filename);
                if ((answer == JOptionPane.CANCEL_OPTION) || (answer == JOptionPane.CLOSED_OPTION)) {
                    close = false; // The user has cancelled.
                }
                if (answer == JOptionPane.YES_OPTION) {
                    // The user wants to save.
                    try {
                        SaveDatabaseAction saveAction = new SaveDatabaseAction(getCurrentBasePanel());
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
            BasePanel pan = getCurrentBasePanel();
            pan.cleanUp();
            AutoSaveManager.deleteAutoSaveFile(pan); // Delete autosave
            tabbedPane.remove(pan);
            if (tabbedPane.getTabCount() > 0) {
                markActiveBasePanel();
            }
            setWindowTitle();
            updateEnabledState(); // FIXME: Man, this is what I call a bug that this is not called.
            output(Localization.lang("Closed database") + '.');
            // update tab titles
            updateAllTabTitles();
        }
    }

    // The action for opening the preferences dialog.
    private final AbstractAction showPrefs = new ShowPrefsAction();

    class ShowPrefsAction
            extends MnemonicAwareAction {

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
    private void addImportedEntries(final BasePanel panel, final List<BibtexEntry> entries, final boolean openInNew) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ImportInspectionDialog diag = new ImportInspectionDialog(JabRefFrame.this,
                        panel, BibtexFields.DEFAULT_INSPECTION_FIELDS, Localization.lang("Import"),
                        openInNew);
                diag.addEntries(entries);
                diag.entryListComplete();
                PositionWindow.placeDialog(diag, JabRefFrame.this);
                diag.setVisible(true);
                diag.toFront();
            }
        });
    }

    public FileHistoryMenu getFileHistory() {
        return fileHistory;
    }

    /**
     * Set the preview active state for all BasePanel instances.
     *
     * @param enabled
     */
    public void setPreviewActive(boolean enabled) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            getBasePanelAt(i).setPreviewActive(enabled);
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
            super();
            putValue(Action.NAME, Localization.menuTitle("Save session"));
            putValue(Action.ACCELERATOR_KEY, prefs.getKey(KeyBinds.SAVE_SESSION));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Here we store the names of all current files. If
            // there is no current file, we remove any
            // previously stored filename.
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
                                getCurrentBasePanel().runCommand(Actions.SAVE);
                            } catch (Throwable ignored) {
                                // Ignored
                            }
                        }
                    }
                    if (getBasePanelAt(i).getDatabaseFile() != null) {
                        filenames.add(getBasePanelAt(i).getDatabaseFile().getPath());
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
            super();
            putValue(Action.NAME, Localization.menuTitle("Load session"));
            putValue(Action.ACCELERATOR_KEY, prefs.getKey(KeyBinds.LOAD_SESSION));
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
                            if (getBasePanelAt(i).getDatabaseFile() != null) {
                                currentFiles.add(getBasePanelAt(i).getDatabaseFile().getPath());
                            }
                        }
                    }
                    String[] names = prefs.getStringArray("savedSession");
                    ArrayList<File> filesToOpen = new ArrayList<>();
                    for (int i = 0; i < names.length; i++) {
                        filesToOpen.add(new File(names[i]));
                    }
                    open.openFiles(filesToOpen, true);
                    running = false;
                }
            });

        }
    }

    class ChangeTabAction extends MnemonicAwareAction {

        private final boolean next;


        public ChangeTabAction(boolean next) {
            // @formatter:off
            putValue(Action.NAME, next ? Localization.menuTitle("Next tab") :
                    Localization.menuTitle("Previous tab"));
            // @formatter:on
            this.next = next;
            putValue(Action.ACCELERATOR_KEY,
                    next ? prefs.getKey(KeyBinds.NEXT_TAB) : prefs.getKey(KeyBinds.PREVIOUS_TAB));
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
            try {
                source.getActionMap().get(command).actionPerformed(new ActionEvent(source, 0, command));
            } catch (NullPointerException ex) {
                // No component is focused, so we do nothing.
            }
        }
    }

    class CustomizeExportsAction extends MnemonicAwareAction {

        public CustomizeExportsAction() {
            putValue(Action.NAME, Localization.menuTitle("Manage custom exports"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ExportCustomizationDialog ecd = new ExportCustomizationDialog(JabRefFrame.this);
            ecd.setVisible(true);
        }
    }

    class CustomizeImportsAction extends MnemonicAwareAction {

        public CustomizeImportsAction() {
            putValue(Action.NAME, Localization.menuTitle("Manage custom imports"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ImportCustomizationDialog ecd = new ImportCustomizationDialog(JabRefFrame.this);
            ecd.setVisible(true);
        }
    }

    class CustomizeEntryTypeAction extends MnemonicAwareAction {

        public CustomizeEntryTypeAction() {
            putValue(Action.NAME, Localization.menuTitle("Customize entry types"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JDialog dl = new EntryCustomizationDialog2(JabRefFrame.this);
            PositionWindow.placeDialog(dl, JabRefFrame.this);
            dl.setVisible(true);
        }
    }

    class GenFieldsCustomizationAction extends MnemonicAwareAction {

        public GenFieldsCustomizationAction() {
            putValue(Action.NAME, Localization.menuTitle("Set up general fields"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GenFieldsCustomizer gf = new GenFieldsCustomizer(JabRefFrame.this);
            PositionWindow.placeDialog(gf, JabRefFrame.this);
            gf.setVisible(true);

        }
    }

    class DatabasePropertiesAction extends MnemonicAwareAction {

        DatabasePropertiesDialog propertiesDialog;


        public DatabasePropertiesAction() {
            putValue(Action.NAME, Localization.menuTitle("Database properties"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (propertiesDialog == null) {
                propertiesDialog = new DatabasePropertiesDialog(JabRefFrame.this);
            }
            propertiesDialog.setPanel(getCurrentBasePanel());
            PositionWindow.placeDialog(propertiesDialog, JabRefFrame.this);
            propertiesDialog.setVisible(true);
        }

    }

    class BibtexKeyPatternAction extends MnemonicAwareAction {

        BibtexKeyPatternDialog bibtexKeyPatternDialog;


        public BibtexKeyPatternAction() {
            putValue(Action.NAME, Localization.lang("Bibtex key patterns"));
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
            PositionWindow.placeDialog(bibtexKeyPatternDialog, JabRefFrame.this);
            bibtexKeyPatternDialog.setVisible(true);
        }

    }

    class IncreaseTableFontSizeAction extends MnemonicAwareAction {

        public IncreaseTableFontSizeAction() {
            putValue(Action.NAME, Localization.menuTitle("Increase table font size"));
            putValue(Action.ACCELERATOR_KEY, Globals.prefs.getKey(KeyBinds.INCREASE_TABLE_FONT_SIZE));
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            int currentSize = GUIGlobals.CURRENTFONT.getSize();
            GUIGlobals.CURRENTFONT = new Font(GUIGlobals.CURRENTFONT.getFamily(), GUIGlobals.CURRENTFONT.getStyle(),
                    currentSize + 1);
            Globals.prefs.putInt(JabRefPreferences.FONT_SIZE, currentSize + 1);
            for (int i = 0; i < getBasePanelCount(); i++) {
                getBasePanelAt(i).updateTableFont();
            }
        }
    }

    class DecreaseTableFontSizeAction extends MnemonicAwareAction {

        public DecreaseTableFontSizeAction() {
            putValue(Action.NAME, Localization.menuTitle("Decrease table font size"));
            putValue(Action.ACCELERATOR_KEY, Globals.prefs.getKey(KeyBinds.DECREASE_TABLE_FONT_SIZE));
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
            for (int i = 0; i < getBasePanelCount(); i++) {
                getBasePanelAt(i).updateTableFont();
            }
        }
    }

    private static class MyGlassPane extends JPanel {

        //ForegroundLabel infoLabel = new ForegroundLabel("Showing search");
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
}
