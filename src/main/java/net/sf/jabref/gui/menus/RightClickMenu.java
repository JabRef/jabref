package net.sf.jabref.gui.menus;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.EntryMarker;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.mergeentries.FetchAndMergeEntry;
import net.sf.jabref.gui.worker.MarkEntriesAction;
import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;
import net.sf.jabref.specialfields.SpecialField;
import net.sf.jabref.specialfields.SpecialFieldValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RightClickMenu extends JPopupMenu implements PopupMenuListener {
    private static final Log LOGGER = LogFactory.getLog(RightClickMenu.class);

    private final BasePanel panel;
    private final JMenuItem groupAdd;
    private final JMenuItem groupRemove;
    private final JMenuItem groupMoveTo;
    private final JCheckBoxMenuItem floatMarked = new JCheckBoxMenuItem(Localization.lang("Float marked entries"),
            Globals.prefs.getBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES));


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
        copySpecialMenu.add(new GeneralAction(Actions.COPY_KEY, Localization.lang("Copy BibTeX key")));
        copySpecialMenu.add(new GeneralAction(Actions.COPY_CITE_KEY, Localization.lang("Copy \\cite{BibTeX key}")));
        copySpecialMenu
                .add(new GeneralAction(Actions.COPY_KEY_AND_TITLE, Localization.lang("Copy BibTeX key and title")));
        copySpecialMenu.add(new GeneralAction(Actions.EXPORT_TO_CLIPBOARD, Localization.lang("Export to clipboard"),
                IconTheme.JabRefIcon.EXPORT_TO_CLIPBOARD.getSmallIcon()));

        add(new GeneralAction(Actions.COPY, Localization.lang("Copy"), IconTheme.JabRefIcon.COPY.getSmallIcon()));
        add(copySpecialMenu);
        add(new GeneralAction(Actions.PASTE, Localization.lang("Paste"), IconTheme.JabRefIcon.PASTE.getSmallIcon()));
        add(new GeneralAction(Actions.CUT, Localization.lang("Cut"), IconTheme.JabRefIcon.CUT.getSmallIcon()));
        add(new GeneralAction(Actions.DELETE, Localization.lang("Delete"), IconTheme.JabRefIcon.DELETE_ENTRY.getSmallIcon()));
        add(new GeneralAction(Actions.PRINT_PREVIEW, Localization.lang("Print entry preview"), IconTheme.JabRefIcon.PRINTED.getSmallIcon()));
        addSeparator();

        add(new GeneralAction(Actions.SEND_AS_EMAIL, Localization.lang("Send as email"), IconTheme.JabRefIcon.EMAIL.getSmallIcon()));
        addSeparator();

        JMenu markSpecific = JabRefFrame.subMenu(Localization.menuTitle("Mark specific color"));
        for (int i = 0; i < EntryMarker.MAX_MARKING_LEVEL; i++) {
            markSpecific.add(new MarkEntriesAction(frame, i).getMenuItem());
        }

        if (multiple) {
            add(new GeneralAction(Actions.MARK_ENTRIES, Localization.lang("Mark entries"), IconTheme.JabRefIcon.MARK_ENTRIES.getSmallIcon()));
            add(markSpecific);
            add(new GeneralAction(Actions.UNMARK_ENTRIES, Localization.lang("Unmark entries"), IconTheme.JabRefIcon.UNMARK_ENTRIES.getSmallIcon()));
        } else if (be != null) {
            Optional<String> marked = be.getFieldOptional(FieldName.MARKED_INTERNAL);
            // We have to check for "" too as the marked field may be empty
            if ((!marked.isPresent()) || marked.get().isEmpty()) {
                add(new GeneralAction(Actions.MARK_ENTRIES, Localization.lang("Mark entry"), IconTheme.JabRefIcon.MARK_ENTRIES.getSmallIcon()));
                add(markSpecific);
            } else {
                add(markSpecific);
                add(new GeneralAction(Actions.UNMARK_ENTRIES, Localization.lang("Unmark entry"), IconTheme.JabRefIcon.UNMARK_ENTRIES.getSmallIcon()));
            }
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RANKING)) {
                JMenu rankingMenu = new JMenu();
                RightClickMenu.populateSpecialFieldMenu(rankingMenu, Rank.getInstance(), frame);
                add(rankingMenu);
            }

            // TODO: multiple handling for relevance and quality-assurance
            // if multiple values are selected ("if (multiple)"), two options (set / clear) should be offered
            // if one value is selected either set or clear should be offered
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RELEVANCE)) {
                add(Relevance.getInstance().getValues().get(0).getMenuAction(frame));
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_QUALITY)) {
                add(Quality.getInstance().getValues().get(0).getMenuAction(frame));
            }
            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRINTED)) {
                add(Printed.getInstance().getValues().get(0).getMenuAction(frame));
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRIORITY)) {
                JMenu priorityMenu = new JMenu();
                RightClickMenu.populateSpecialFieldMenu(priorityMenu, Priority.getInstance(), frame);
                add(priorityMenu);
            }

            if (Globals.prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_READ)) {
                JMenu readStatusMenu = new JMenu();
                RightClickMenu.populateSpecialFieldMenu(readStatusMenu, ReadStatus.getInstance(), frame);
                add(readStatusMenu);
            }

        }

        addSeparator();

        add(new GeneralAction(Actions.OPEN_FOLDER, Localization.lang("Open folder")) {
            {
                if (!isFieldSetForSelectedEntry(FieldName.FILE)) {
                    this.setEnabled(false);
                }
            }
        });

        add(new GeneralAction(Actions.OPEN_EXTERNAL_FILE, Localization.lang("Open file"), getFileIconForSelectedEntry()) {
            {
                if (!isFieldSetForSelectedEntry(FieldName.FILE)) {
                    this.setEnabled(false);
                }
            }
        });

        add(new GeneralAction(Actions.OPEN_URL, Localization.lang("Open URL or DOI"), IconTheme.JabRefIcon.WWW.getSmallIcon()) {
            {
                if(!(isFieldSetForSelectedEntry(FieldName.URL) || isFieldSetForSelectedEntry(FieldName.DOI))) {
                    this.setEnabled(false);
                }
            }
        });

        addSeparator();

        add(typeMenu);

        add(new GeneralAction(Actions.MERGE_WITH_FETCHED_ENTRY,
                Localization.lang("Get BibTeX data from %0", FetchAndMergeEntry.getDisplayNameOfSupportedFields())) {
            {
                if (!(isAnyFieldSetForSelectedEntry(FetchAndMergeEntry.SUPPORTED_FIELDS))) {
                    this.setEnabled(false);
                }
            }
        });
        add(frame.getMassSetField());
        add(new GeneralAction(Actions.ADD_FILE_LINK, Localization.lang("Attach file"), IconTheme.JabRefIcon.ATTACH_FILE.getSmallIcon()));
        add(frame.getManageKeywords());
        add(new GeneralAction(Actions.MERGE_ENTRIES,
                Localization.lang("Merge entries") + "...",
                IconTheme.JabRefIcon.MERGE_ENTRIES.getSmallIcon()) {

            {
                if (!(areExactlyTwoEntriesSelected())) {
                    this.setEnabled(false);
                }
            }

        });

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

    private boolean areMultipleEntriesSelected() {
        return panel.getMainTable().getSelectedRowCount() > 1;
    }

    private boolean areExactlyTwoEntriesSelected() {
        return panel.getMainTable().getSelectedRowCount() == 2;
    }

    /**
     * Remove all types from the menu.
     * Then cycle through all available values, and add them.
     */
    public static void populateSpecialFieldMenu(JMenu menu, SpecialField field, JabRefFrame frame) {
        menu.setText(field.getMenuString());
        menu.setIcon(((IconTheme.FontBasedIcon) field.getRepresentingIcon()).createSmallIcon());
        for (SpecialFieldValue val : field.getValues()) {
            menu.add(val.getMenuAction(frame));
        }
    }

    /**
     * Set the dynamic contents of "Add to group ..." submenu.
     */
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        panel.storeCurrentEdit();
        GroupTreeNode groups = panel.getBibDatabaseContext().getMetaData().getGroups();
        if (groups == null) {
            groupAdd.setEnabled(false);
            groupRemove.setEnabled(false);
            groupMoveTo.setEnabled(false);
        } else {
            groupAdd.setEnabled(true);
            groupRemove.setEnabled(true);
            groupMoveTo.setEnabled(true);
        }
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
            if(entry.hasField(FieldName.FILE)) {
                JLabel label = FileListTableModel.getFirstLabel(entry.getFieldOptional(FieldName.FILE).get());
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
