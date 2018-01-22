package org.jabref.gui.maintable;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.ActionsFX;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreviewPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RightClickMenu {
    private static final Log LOGGER = LogFactory.getLog(RightClickMenu.class);

    public static ContextMenu create(BibEntryTableViewModel entry, KeyBindingRepository keyBindingRepository) {
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory(keyBindingRepository);

        // Are multiple entries selected?
        // boolean multiple = areMultipleEntriesSelected();

        // If only one entry is selected, get a reference to it for adapting the menu.
        // BibEntry be = null;
        //if (panel.getMainTable().getSelectedEntries().size() == 1) {
        //    be = panel.getMainTable().getSelectedEntries().get(0);
        //}

        Menu copySpecialMenu = factory.createMenu(ActionsFX.COPY_MORE);
        copySpecialMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_TITLE));
        copySpecialMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_KEY));
        copySpecialMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_CITE_KEY));
        copySpecialMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_KEY_AND_TITLE));
        copySpecialMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_KEY_AND_LINK));

        // the submenu will behave dependent on what style is currently selected (citation/preview)
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();
        String style = previewPreferences.getPreviewCycle().get(previewPreferences.getPreviewCyclePosition());
        if (CitationStyle.isCitationStyleFile(style)) {
            copySpecialMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_CITATION_HTML));
            Menu copyCitationMenu = factory.createMenu(ActionsFX.COPY_CITATION_MORE);
            copyCitationMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_CITATION_TEXT));
            copyCitationMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_CITATION_RTF));
            copyCitationMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_CITATION_ASCII_DOC));
            copyCitationMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_CITATION_XSLFO));
            copySpecialMenu.getItems().add(copyCitationMenu);
        } else {
            copySpecialMenu.getItems().add(factory.createMenuItem(ActionsFX.COPY_CITATION_HTML));
        }

        copySpecialMenu.getItems().add(factory.createMenuItem(ActionsFX.EXPORT_TO_CLIPBOARD));

        //add(factory.createMenuItem(ActionsFX.COPY));
        contextMenu.getItems().add(copySpecialMenu);
        contextMenu.getItems().add(factory.createMenuItem(ActionsFX.PASTE));
        contextMenu.getItems().add(factory.createMenuItem(ActionsFX.CUT));
        contextMenu.getItems().add(factory.createMenuItem(ActionsFX.DELETE));
        // TODO: Add somewhere else
        // GeneralAction printPreviewAction = factory.createMenuItem(ActionsFX.PRINT_PREVIEW);
        // printPreviewAction.setEnabled(!multiple);
        // add(printPreviewAction);

        contextMenu.getItems().add(new SeparatorMenuItem());
        contextMenu.getItems().add(factory.createMenuItem(ActionsFX.SEND_AS_EMAIL));
        contextMenu.getItems().add(new SeparatorMenuItem());

        // TODO: Add somewhere else
        // add(new CopyFilesAction());

        if (Globals.prefs.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RANKING)) {
                Menu rankingMenu = createSpecialFieldMenu(SpecialField.RANKING);
                contextMenu.getItems().add(rankingMenu);
            }

            // TODO: multiple handling for relevance and quality-assurance
            // if multiple values are selected ("if (multiple)"), two options (set / clear) should be offered
            // if one value is selected either set or clear should be offered
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RELEVANCE)) {
                // TODO contextMenu.getItems().add(new SpecialFieldMenuAction(new SpecialFieldValueViewModel(SpecialField.RELEVANCE.getValues().get(0)), frame));
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_QUALITY)) {
                // TODO contextMenu.getItems().add(new SpecialFieldMenuAction(new SpecialFieldValueViewModel(SpecialField.QUALITY.getValues().get(0)), frame));
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRINTED)) {
                // TODO contextMenu.getItems().add(new SpecialFieldMenuAction(new SpecialFieldValueViewModel(SpecialField.PRINTED.getValues().get(0)), frame));
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRIORITY)) {
                Menu priorityMenu = createSpecialFieldMenu(SpecialField.PRIORITY);
                contextMenu.getItems().add(priorityMenu);
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_READ)) {
                Menu readStatusMenu = createSpecialFieldMenu(SpecialField.READ_STATUS);
                contextMenu.getItems().add(readStatusMenu);
            }

        }

        contextMenu.getItems().add(new SeparatorMenuItem());

        MenuItem openFolderAction = factory.createMenuItem(ActionsFX.OPEN_FOLDER);
        // TODO: openFolderAction.setEnabled(isFieldSetForSelectedEntry(FieldName.FILE));
        contextMenu.getItems().add(openFolderAction);

        MenuItem openFileAction = factory.createMenuItem(ActionsFX.OPEN_EXTERNAL_FILE);
        // TODO: openFileAction.setEnabled(isFieldSetForSelectedEntry(FieldName.FILE));
        contextMenu.getItems().add(openFileAction);

        MenuItem openUrlAction = factory.createMenuItem(ActionsFX.OPEN_URL);
        // TODO: openUrlAction.setEnabled(isFieldSetForSelectedEntry(FieldName.URL) || isFieldSetForSelectedEntry(FieldName.DOI));
        contextMenu.getItems().add(openUrlAction);

        contextMenu.getItems().add(new SeparatorMenuItem());

        // TODO: JMenu typeMenu = new ChangeEntryTypeMenu(Globals.getKeyPrefs()).getChangeEntryTypeMenu(panel);
        //contextMenu.getItems().add(typeMenu);

        MenuItem mergeFetchedEntryAction = factory.createMenuItem(ActionsFX.MERGE_WITH_FETCHED_ENTRY);
        // TODO FetchAndMergeEntry.getDisplayNameOfSupportedFields()));
        // TODO mergeFetchedEntryAction.setEnabled(isAnyFieldSetForSelectedEntry(FetchAndMergeEntry.SUPPORTED_FIELDS));
        contextMenu.getItems().add(mergeFetchedEntryAction);

        // TODO:
        //contextMenu.getItems().add(frame.getMassSetField());

        MenuItem attachFileAction = factory.createMenuItem(ActionsFX.ADD_FILE_LINK);
        // TODO: attachFileAction.setEnabled(!multiple);
        contextMenu.getItems().add(attachFileAction);

        // TODO:
        //contextMenu.getItems().add(frame.getManageKeywords());

        MenuItem mergeEntriesAction = factory.createMenuItem(ActionsFX.MERGE_ENTRIES);
        // TODO: mergeEntriesAction.setEnabled(areExactlyTwoEntriesSelected());
        contextMenu.getItems().add(mergeEntriesAction);

        contextMenu.getItems().add(new SeparatorMenuItem()); // for "add/move/remove to/from group" entries (appended here)

        contextMenu.getItems().add(factory.createMenuItem(ActionsFX.ADD_TO_GROUP));
        contextMenu.getItems().add(factory.createMenuItem(ActionsFX.REMOVE_FROM_GROUP));

        contextMenu.getItems().add(factory.createMenuItem(ActionsFX.MOVE_TO_GROUP));

        // create disabledIcons for all menu entries
        // TODO: frame.createDisabledIconsForMenuEntries(this);

        return contextMenu;
    }

    /**
     * Remove all types from the menu.
     * Then cycle through all available values, and add them.
     */
    public static Menu createSpecialFieldMenu(SpecialField field) {
        Menu menu = new Menu();

        SpecialFieldViewModel viewModel = new SpecialFieldViewModel(field);
        menu.setText(viewModel.getLocalization());
        menu.setGraphic(viewModel.getIcon().getGraphicNode());
        // TODO:
        //for (SpecialFieldValue val : field.getValues()) {
        //    menu.add(new SpecialFieldMenuAction(new SpecialFieldValueViewModel(val), frame));
        //}
        return menu;
    }
/*

    private boolean areMultipleEntriesSelected() {
        return panel.getMainTable().getSelectedEntries().size() > 1;
    }

    private boolean areExactlyTwoEntriesSelected() {
        return panel.getMainTable().getSelectedEntries().size() == 2;
    }
*/

    /**
     * Set the dynamic contents of "Add to group ..." submenu.
     */
    /*
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
        if (panel.getMainTable().getSelectedEntries().size() == 1) {
            BibEntry entry = panel.getMainTable().getSelectedEntries().get(0);
            return !Collections.disjoint(fieldnames, entry.getFieldNames());
        }
        return false;
    }

    private Icon getFileIconForSelectedEntry() {
        if (panel.getMainTable().getSelectedEntries().size() == 1) {
            BibEntry entry = panel.getMainTable().getSelectedEntries().get(0);
            if (entry.hasField(FieldName.FILE)) {
                JLabel label = FileListTableModel.getFirstLabel(entry.getField(FieldName.FILE).get());
                if (label != null) {
                    return label.getIcon();
                }
            }
        }
        return IconTheme.JabRefIcons.FILE.getSmallIcon();
    }
    */
}
