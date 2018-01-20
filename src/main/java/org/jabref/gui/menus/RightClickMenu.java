package org.jabref.gui.menus;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.EntryMarker;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.copyfiles.CopyFilesAction;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.specialfields.SpecialFieldMenuAction;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.gui.worker.MarkEntriesAction;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.model.entry.specialfields.SpecialFieldValue;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreviewPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RightClickMenu extends JPopupMenu implements PopupMenuListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RightClickMenu.class);

    private final BasePanel panel;
    private final JMenuItem groupAdd;
    private final JMenuItem groupRemove;
    private final JMenuItem groupMoveTo;


    public RightClickMenu(JabRefFrame frame, BasePanel panel) {
        this.panel = panel;
        JMenu typeMenu = new ChangeEntryTypeMenu().getChangeEntryTypeMenu(panel);
        // Are multiple entries selected?
        boolean multiple = areMultipleEntriesSelected();

        // If only one entry is selected, get a reference to it for adapting the menu.
        BibEntry be = null;
        if (panel.getMainTable().getSelectedRowCount() == 1) {
            be = panel.getMainTable().getSelected().get(0);
        }

        addPopupMenuListener(this);

        JMenu copySpecialMenu = new JMenu(Localization.lang("Copy") + "...");
        copySpecialMenu.add(new GeneralAction(Actions.COPY_TITLE, Localization.lang("Copy title"), KeyBinding.COPY_TITLE));
        copySpecialMenu.add(new GeneralAction(Actions.COPY_KEY, Localization.lang("Copy BibTeX key"), KeyBinding.COPY_BIBTEX_KEY));
        copySpecialMenu.add(new GeneralAction(Actions.COPY_CITE_KEY, Localization.lang("Copy \\cite{BibTeX key}"), KeyBinding.COPY_CITE_BIBTEX_KEY));
        copySpecialMenu.add(new GeneralAction(Actions.COPY_KEY_AND_TITLE, Localization.lang("Copy BibTeX key and title"), KeyBinding.COPY_BIBTEX_KEY_AND_TITLE));
        copySpecialMenu.add(new GeneralAction(Actions.COPY_KEY_AND_LINK, Localization.lang("Copy BibTeX key and link"), KeyBinding.COPY_BIBTEX_KEY_AND_LINK));

        // the submenu will behave dependent on what style is currently selected (citation/preview)
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();
        String style = previewPreferences.getPreviewCycle().get(previewPreferences.getPreviewCyclePosition());
        if (CitationStyle.isCitationStyleFile(style)) {
            copySpecialMenu.add(new GeneralAction(Actions.COPY_CITATION_HTML, Localization.menuTitle("Copy citation") + " (HTML)", KeyBinding.COPY_PREVIEW));
            JMenu copyCitationMenu = new JMenu(Localization.menuTitle("Copy citation") + "...");
            copyCitationMenu.add(new GeneralAction(Actions.COPY_CITATION_TEXT, "Text"));
            copyCitationMenu.add(new GeneralAction(Actions.COPY_CITATION_RTF, "RTF"));
            copyCitationMenu.add(new GeneralAction(Actions.COPY_CITATION_ASCII_DOC, "AsciiDoc"));
            copyCitationMenu.add(new GeneralAction(Actions.COPY_CITATION_XSLFO, "XSL-FO"));
            copySpecialMenu.add(copyCitationMenu);
        } else {
            copySpecialMenu.add(new GeneralAction(Actions.COPY_CITATION_HTML, Localization.lang("Copy preview"), KeyBinding.COPY_PREVIEW));
        }

        copySpecialMenu.add(new GeneralAction(Actions.EXPORT_TO_CLIPBOARD, Localization.lang("Export to clipboard"),
                IconTheme.JabRefIcon.EXPORT_TO_CLIPBOARD.getSmallIcon()));

        add(new GeneralAction(Actions.COPY, Localization.lang("Copy"), IconTheme.JabRefIcon.COPY.getSmallIcon(), KeyBinding.COPY));
        add(copySpecialMenu);
        add(new GeneralAction(Actions.PASTE, Localization.lang("Paste"), IconTheme.JabRefIcon.PASTE.getSmallIcon(), KeyBinding.PASTE));
        add(new GeneralAction(Actions.CUT, Localization.lang("Cut"), IconTheme.JabRefIcon.CUT.getSmallIcon(), KeyBinding.CUT));
        add(new GeneralAction(Actions.DELETE, Localization.lang("Delete"), IconTheme.JabRefIcon.DELETE_ENTRY.getSmallIcon(), KeyBinding.DELETE_ENTRY));
        GeneralAction printPreviewAction = new GeneralAction(Actions.PRINT_PREVIEW, Localization.lang("Print entry preview"), IconTheme.JabRefIcon.PRINTED.getSmallIcon());
        printPreviewAction.setEnabled(!multiple);
        add(printPreviewAction);

        addSeparator();

        add(new GeneralAction(Actions.SEND_AS_EMAIL, Localization.lang("Send as email"), IconTheme.JabRefIcon.EMAIL.getSmallIcon()));
        addSeparator();
        add(new CopyFilesAction());

        JMenu markSpecific = JabRefFrame.subMenu(Localization.menuTitle("Mark specific color"));
        markSpecific.setIcon(IconTheme.JabRefIcon.MARK_ENTRIES.getSmallIcon());
        for (int i = 0; i < EntryMarker.MAX_MARKING_LEVEL; i++) {
            markSpecific.add(new MarkEntriesAction(frame, i).getMenuItem());
        }

        if (multiple) {
            add(new GeneralAction(Actions.MARK_ENTRIES, Localization.lang("Mark entries"), IconTheme.JabRefIcon.MARK_ENTRIES.getSmallIcon(), KeyBinding.MARK_ENTRIES));
            add(markSpecific);
            add(new GeneralAction(Actions.UNMARK_ENTRIES, Localization.lang("Unmark entries"), IconTheme.JabRefIcon.UNMARK_ENTRIES.getSmallIcon(), KeyBinding.UNMARK_ENTRIES));
        } else if (be != null) {
            Optional<String> marked = be.getField(FieldName.MARKED_INTERNAL);
            // We have to check for "" too as the marked field may be empty
            if ((!marked.isPresent()) || marked.get().isEmpty()) {
                add(new GeneralAction(Actions.MARK_ENTRIES, Localization.lang("Mark entry"), IconTheme.JabRefIcon.MARK_ENTRIES.getSmallIcon(), KeyBinding.MARK_ENTRIES));
                add(markSpecific);
            } else {
                add(markSpecific);
                add(new GeneralAction(Actions.UNMARK_ENTRIES, Localization.lang("Unmark entry"), IconTheme.JabRefIcon.UNMARK_ENTRIES.getSmallIcon(), KeyBinding.UNMARK_ENTRIES));
            }
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RANKING)) {
                JMenu rankingMenu = new JMenu();
                RightClickMenu.populateSpecialFieldMenu(rankingMenu, SpecialField.RANKING, frame);
                add(rankingMenu);
            }

            // TODO: multiple handling for relevance and quality-assurance
            // if multiple values are selected ("if (multiple)"), two options (set / clear) should be offered
            // if one value is selected either set or clear should be offered
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RELEVANCE)) {
                add(new SpecialFieldMenuAction(new SpecialFieldValueViewModel(SpecialField.RELEVANCE.getValues().get(0)), frame));
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_QUALITY)) {
                add(new SpecialFieldMenuAction(new SpecialFieldValueViewModel(SpecialField.QUALITY.getValues().get(0)), frame));
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRINTED)) {
                add(new SpecialFieldMenuAction(new SpecialFieldValueViewModel(SpecialField.PRINTED.getValues().get(0)), frame));
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRIORITY)) {
                JMenu priorityMenu = new JMenu();
                RightClickMenu.populateSpecialFieldMenu(priorityMenu, SpecialField.PRIORITY, frame);
                add(priorityMenu);
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_READ)) {
                JMenu readStatusMenu = new JMenu();
                RightClickMenu.populateSpecialFieldMenu(readStatusMenu, SpecialField.READ_STATUS, frame);
                add(readStatusMenu);
            }

        }

        addSeparator();

        GeneralAction openFolderAction = new GeneralAction(Actions.OPEN_FOLDER, Localization.lang("Open folder"),
                KeyBinding.OPEN_FOLDER);
        openFolderAction.setEnabled(isFieldSetForSelectedEntry(FieldName.FILE));
        add(openFolderAction);

        GeneralAction openFileAction = new GeneralAction(Actions.OPEN_EXTERNAL_FILE, Localization.lang("Open file"),
                getFileIconForSelectedEntry(), KeyBinding.OPEN_FILE);
        openFileAction.setEnabled(isFieldSetForSelectedEntry(FieldName.FILE));
        add(openFileAction);

        GeneralAction openUrlAction = new GeneralAction(Actions.OPEN_URL, Localization.lang("Open URL or DOI"),
                IconTheme.JabRefIcon.WWW.getSmallIcon(), KeyBinding.OPEN_URL_OR_DOI);
        openUrlAction.setEnabled(isFieldSetForSelectedEntry(FieldName.URL) || isFieldSetForSelectedEntry(FieldName.DOI));
        add(openUrlAction);

        addSeparator();

        add(typeMenu);

        GeneralAction mergeFetchedEntryAction = new GeneralAction(Actions.MERGE_WITH_FETCHED_ENTRY,
                Localization.lang("Get BibTeX data from %0", FetchAndMergeEntry.getDisplayNameOfSupportedFields()));
        mergeFetchedEntryAction.setEnabled(isAnyFieldSetForSelectedEntry(FetchAndMergeEntry.SUPPORTED_FIELDS));
        add(mergeFetchedEntryAction);

        add(frame.getMassSetField());

        GeneralAction attachFileAction = new GeneralAction(Actions.ADD_FILE_LINK, Localization.lang("Attach file"),
                IconTheme.JabRefIcon.ATTACH_FILE.getSmallIcon());
        attachFileAction.setEnabled(!multiple);
        add(attachFileAction);

        add(frame.getManageKeywords());

        GeneralAction mergeEntriesAction = new GeneralAction(Actions.MERGE_ENTRIES,
                Localization.lang("Merge entries") + "...", IconTheme.JabRefIcon.MERGE_ENTRIES.getSmallIcon());
        mergeEntriesAction.setEnabled(areExactlyTwoEntriesSelected());
        add(mergeEntriesAction);

        addSeparator(); // for "add/move/remove to/from group" entries (appended here)

        groupAdd = new JMenuItem(new GeneralAction(Actions.ADD_TO_GROUP, Localization.lang("Add to group")));
        add(groupAdd);
        groupRemove = new JMenuItem(new GeneralAction(Actions.REMOVE_FROM_GROUP, Localization.lang("Remove from group")));
        add(groupRemove);

        groupMoveTo = add(new GeneralAction(Actions.MOVE_TO_GROUP, Localization.lang("Move to group")));
        add(groupMoveTo);

        // create disabledIcons for all menu entries
        frame.createDisabledIconsForMenuEntries(this);
    }

    /**
     * Remove all types from the menu.
     * Then cycle through all available values, and add them.
     */
    public static void populateSpecialFieldMenu(JMenu menu, SpecialField field, JabRefFrame frame) {
        SpecialFieldViewModel viewModel = new SpecialFieldViewModel(field);
        menu.setText(viewModel.getLocalization());
        menu.setIcon(viewModel.getRepresentingIcon());
        for (SpecialFieldValue val : field.getValues()) {
            menu.add(new SpecialFieldMenuAction(new SpecialFieldValueViewModel(val), frame));
        }
    }

    private boolean areMultipleEntriesSelected() {
        return panel.getMainTable().getSelectedRowCount() > 1;
    }

    private boolean areExactlyTwoEntriesSelected() {
        return panel.getMainTable().getSelectedRowCount() == 2;
    }

    /**
     * Set the dynamic contents of "Add to group ..." submenu.
     */
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        boolean groupsPresent = panel.getBibDatabaseContext().getMetaData().getGroups().isPresent();
        groupAdd.setEnabled(groupsPresent);
        groupRemove.setEnabled(groupsPresent);
        groupMoveTo.setEnabled(groupsPresent);
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // Nothing to do
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
        // nothing to do
    }

    private boolean isFieldSetForSelectedEntry(String fieldname) {
        return isAnyFieldSetForSelectedEntry(Arrays.asList(fieldname));
    }

    private boolean isAnyFieldSetForSelectedEntry(List<String> fieldnames) {
        if (panel.getMainTable().getSelectedRowCount() == 1) {
            BibEntry entry = panel.getMainTable().getSelected().get(0);
            return !Collections.disjoint(fieldnames, entry.getFieldNames());
        }
        return false;
    }

    private Icon getFileIconForSelectedEntry() {
        if (panel.getMainTable().getSelectedRowCount() == 1) {
            BibEntry entry = panel.getMainTable().getSelected().get(0);
            if (entry.hasField(FieldName.FILE)) {
                JLabel label = FileListTableModel.getFirstLabel(entry.getField(FieldName.FILE).get());
                if (label != null) {
                    return label.getIcon();
                }
            }
        }
        return IconTheme.JabRefIcon.FILE.getSmallIcon();
    }

    class GeneralAction extends AbstractAction {

        private final String command;

        public GeneralAction(String command, String name) {
            super(name);
            this.command = command;
        }

        public GeneralAction(String command, String name, Icon icon) {
            super(name, icon);
            this.command = command;
        }

        public GeneralAction(String command, String name, KeyBinding key) {
            super(name);
            this.command = command;
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(key));
        }

        public GeneralAction(String command, String name, Icon icon, KeyBinding key) {
            super(name, icon);
            this.command = command;
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(key));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                panel.runCommand(command);
            } catch (Throwable ex) {
                LOGGER.debug("Cannot execute command " + command + ".", ex);
            }
        }
    }

}
