/*  Copyright (C) 2003-2011 JabRef contributors.
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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.sf.jabref.*;
import net.sf.jabref.groups.*;
import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.groups.structure.AllEntriesGroup;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.worker.MarkEntriesAction;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryType;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;
import net.sf.jabref.specialfields.SpecialField;
import net.sf.jabref.specialfields.SpecialFieldValue;
import net.sf.jabref.specialfields.SpecialFieldsUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RightClickMenu extends JPopupMenu implements PopupMenuListener {
    private static final Log LOGGER = LogFactory.getLog(RightClickMenu.class);

    private final BasePanel panel;
    private final MetaData metaData;
    private final JMenu groupAddMenu = new JMenu(Localization.lang("Add to group"));
    private final JMenu groupRemoveMenu = new JMenu(Localization.lang("Remove from group"));
    private final JMenu groupMoveMenu = new JMenu(Localization.lang("Assign exclusively to group")); // JZTODO lyrics
    private final JMenu typeMenu = new JMenu(Localization.lang("Change entry type"));
    private final JMenuItem groupAdd;
    private final JMenuItem groupRemove;
    private final JCheckBoxMenuItem floatMarked = new JCheckBoxMenuItem(Localization.lang("Float marked entries"),
            Globals.prefs.getBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES));


    public RightClickMenu(BasePanel panel_, MetaData metaData_) {
        panel = panel_;
        metaData = metaData_;

        // Are multiple entries selected?
        boolean multiple = panel.mainTable.getSelectedRowCount() > 1;

        // If only one entry is selected, get a reference to it for adapting the menu.
        BibtexEntry be = null;
        if (panel.mainTable.getSelectedRowCount() == 1) {
            be = panel.mainTable.getSelected().get(0);
        }

        addPopupMenuListener(this);

        add(new GeneralAction(Actions.COPY, Localization.lang("Copy"), IconTheme.JabRefIcon.COPY.getSmallIcon()));
        add(new GeneralAction(Actions.PASTE, Localization.lang("Paste"), IconTheme.JabRefIcon.PASTE.getSmallIcon()));
        add(new GeneralAction(Actions.CUT, Localization.lang("Cut"), IconTheme.JabRefIcon.CUT.getSmallIcon()));
        add(new GeneralAction(Actions.DELETE, Localization.lang("Delete"), IconTheme.JabRefIcon.DELETE.getSmallIcon()));
        addSeparator();

        add(new GeneralAction(Actions.COPY_KEY, Localization.lang("Copy BibTeX key")));
        add(new GeneralAction(Actions.COPY_CITE_KEY, Localization.lang("Copy \\cite{BibTeX key}")));
        add(new GeneralAction(Actions.COPY_KEY_AND_TITLE, Localization.lang("Copy BibTeX key and title")));

        add(new GeneralAction(Actions.EXPORT_TO_CLIPBOARD, Localization.lang("Export to clipboard"), IconTheme.JabRefIcon.EXPORT_TO_CLIPBOARD.getSmallIcon()));
        add(new GeneralAction(Actions.SEND_AS_EMAIL, Localization.lang("Send as email"), IconTheme.JabRefIcon.EMAIL.getSmallIcon()));
        addSeparator();

        JMenu markSpecific = JabRefFrame.subMenu("Mark specific color");
        JabRefFrame frame = panel.frame;
        for (int i = 0; i < EntryMarker.MAX_MARKING_LEVEL; i++) {
            markSpecific.add(new MarkEntriesAction(frame, i).getMenuItem());
        }

        if (multiple) {
            add(new GeneralAction(Actions.MARK_ENTRIES, Localization.lang("Mark entries"), IconTheme.JabRefIcon.MARK_ENTRIES.getSmallIcon()));
            add(markSpecific);
            add(new GeneralAction(Actions.UNMARK_ENTRIES, Localization.lang("Unmark entries"), IconTheme.JabRefIcon.UNMARK_ENTRIES.getSmallIcon()));
            addSeparator();
        } else if (be != null) {
            String marked = be.getField(BibtexFields.MARKED);
            // We have to check for "" too as the marked field may be empty
            if ((marked == null) || marked.isEmpty()) {
                add(new GeneralAction(Actions.MARK_ENTRIES, Localization.lang("Mark entry"), IconTheme.JabRefIcon.MARK_ENTRIES.getSmallIcon()));
                add(markSpecific);
            } else {
                add(markSpecific);
                add(new GeneralAction(Actions.UNMARK_ENTRIES, Localization.lang("Unmark entry"), IconTheme.JabRefIcon.UNMARK_ENTRIES.getSmallIcon()));
            }
            addSeparator();
        }

        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING)) {
                JMenu rankingMenu = new JMenu();
                RightClickMenu.populateSpecialFieldMenu(rankingMenu, Rank.getInstance(), panel.frame);
                add(rankingMenu);
            }

            // TODO: multiple handling for relevance and quality-assurance
            // if multiple values are selected ("if (multiple)"), two options (set / clear) should be offered
            // if one value is selected either set or clear should be offered
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE)) {
                add(Relevance.getInstance().getValues().get(0).getMenuAction(panel.frame));
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY)) {
                add(Quality.getInstance().getValues().get(0).getMenuAction(panel.frame));
            }
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED)) {
                add(Printed.getInstance().getValues().get(0).getMenuAction(panel.frame));
            }

            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY)) {
                JMenu priorityMenu = new JMenu();
                RightClickMenu.populateSpecialFieldMenu(priorityMenu, Priority.getInstance(), panel.frame);
                add(priorityMenu);
            }

            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ)) {
                JMenu readStatusMenu = new JMenu();
                RightClickMenu.populateSpecialFieldMenu(readStatusMenu, ReadStatus.getInstance(), panel.frame);
                add(readStatusMenu);
            }

            addSeparator();
        }

        add(new GeneralAction(Actions.OPEN_FOLDER, Localization.lang("Open folder")) {
            {
                if (!isFieldSetForSelectedEntry("file")) {
                    this.setEnabled(false);
                }
            }
        });

        add(new GeneralAction(Actions.OPEN_EXTERNAL_FILE, Localization.lang("Open file"), getFileIconForSelectedEntry()) {
            {
                if(!isFieldSetForSelectedEntry("file")) {
                    this.setEnabled(false);
                }
            }
        });

        add(new GeneralAction(Actions.ADD_FILE_LINK, Localization.lang("Attach file"), IconTheme.JabRefIcon.ATTACH_FILE.getSmallIcon()));

        add(new GeneralAction(Actions.OPEN_URL, Localization.lang("Open URL or DOI"), IconTheme.JabRefIcon.WWW.getSmallIcon()) {
            {
                if(!(isFieldSetForSelectedEntry("url") || isFieldSetForSelectedEntry("doi"))) {
                    this.setEnabled(false);
                }
            }
        });

        add(new GeneralAction(Actions.MERGE_DOI, Localization.lang("Get BibTeX data from DOI")) {
            {
                if(!(isFieldSetForSelectedEntry("doi"))) {
                    this.setEnabled(false);
                }
            }
        });

        addSeparator();
        populateTypeMenu();

        add(typeMenu);
        add(new GeneralAction(Actions.PLAIN_TEXT_IMPORT, Localization.lang("Plain text import")));

        add(JabRef.jrf.massSetField);
        add(JabRef.jrf.manageKeywords);

        addSeparator(); // for "add/move/remove to/from group" entries (appended here)

        groupAdd = new JMenuItem(new GeneralAction(Actions.ADD_TO_GROUP, Localization.lang("Add to group")));
        add(groupAdd);
        groupRemove = new JMenuItem(new GeneralAction(Actions.REMOVE_FROM_GROUP, Localization.lang("Remove from group")));
        add(groupRemove);

        JMenuItem groupMoveTo = add(new GeneralAction(Actions.MOVE_TO_GROUP, Localization.lang("Move to group")));
        add(groupMoveTo);

        floatMarked.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Globals.prefs.putBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES, floatMarked.isSelected());
                panel.mainTable.refreshSorting(); // Bad remote access
            }
        });

        // create disabledIcons for all menu entries
        frame.createDisabledIconsForMenuEntries(this);
    }

    /**
     * Remove all types from the menu. Then cycle through all available
     * types, and add them.
     */
    private void populateTypeMenu() {
        typeMenu.removeAll();
        for (String key : BibtexEntryType.getAllTypes()) {
            typeMenu.add(new ChangeTypeAction(BibtexEntryType.getType(key), panel));
        }
    }

    /**
     * Remove all types from the menu.
     * Then cycle through all available values, and add them.
     */
    public static void populateSpecialFieldMenu(JMenu menu, SpecialField field, JabRefFrame frame) {
        //menu.removeAll();
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
        GroupTreeNode groups = panel.metaData().getGroups();
        if (groups == null) {
            groupAdd.setEnabled(false);
            groupRemove.setEnabled(false);
        } else {
            groupAdd.setEnabled(true);
            groupRemove.setEnabled(true);
        }

        addSeparator();
        floatMarked.setSelected(Globals.prefs.getBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES));
        add(floatMarked);
    }

    private JMenu buildGroupMenu(BibtexEntry[] bes, boolean add, boolean move) {
        if (bes == null) {
            return null;
        }
        JMenu groupMenu = new JMenu();
        GroupTreeNode groups = metaData.getGroups();
        if (groups == null) {
            groupAddMenu.setEnabled(false);
            groupMoveMenu.setEnabled(false);
            groupRemoveMenu.setEnabled(false);
            return null;
        }

        insertNodes(groupMenu, metaData.getGroups(), bes, add, move);

        return groupMenu;

    }

    /**
     * @param move For add: if true, remove from previous groups
     */
    private void insertNodes(JMenu menu, GroupTreeNode node, BibtexEntry[] selection,
                             boolean add, boolean move) {
        final AbstractAction action = getAction(node, selection, add, move);

        if (node.getChildCount() == 0) {
            JMenuItem menuItem = new JMenuItem(action);
            setGroupFontAndIcon(menuItem, node.getGroup());
            menu.add(menuItem);
            if (action.isEnabled()) {
                menu.setEnabled(true);
            }
            return;
        }

        JMenu submenu;
        if (node.getGroup() instanceof AllEntriesGroup) {
            for (int i = 0; i < node.getChildCount(); ++i) {
                insertNodes(menu, (GroupTreeNode) node.getChildAt(i), selection, add, move);
            }
        } else {
            submenu = new JMenu('[' + node.getGroup().getName() + ']');
            setGroupFontAndIcon(submenu, node.getGroup());
            // setEnabled(true) is done above/below if at least one menu
            // entry (item or submenu) is enabled
            submenu.setEnabled(action.isEnabled());
            JMenuItem menuItem = new JMenuItem(action);
            setGroupFontAndIcon(menuItem, node.getGroup());
            submenu.add(menuItem);
            submenu.add(new Separator());
            for (int i = 0; i < node.getChildCount(); ++i) {
                insertNodes(submenu, (GroupTreeNode) node.getChildAt(i), selection, add, move);
            }
            menu.add(submenu);
            if (submenu.isEnabled()) {
                menu.setEnabled(true);
            }
        }
    }

    /** Sets the font and icon to be used, depending on the group */
    private void setGroupFontAndIcon(JMenuItem menuItem, AbstractGroup group) {
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_DYNAMIC)) {
            menuItem.setFont(menuItem.getFont().deriveFont(group.isDynamic() ?
                    Font.ITALIC : Font.PLAIN));
        }
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_ICONS)) {
            switch (group.getHierarchicalContext()) {
            case INCLUDING:
                menuItem.setIcon(IconTheme.JabRefIcon.GROUP_INCLUDING.getSmallIcon());
                break;
            case REFINING:
                menuItem.setIcon(IconTheme.JabRefIcon.GROUP_REFINING.getSmallIcon());
                break;
            default:
                menuItem.setIcon(IconTheme.JabRefIcon.GROUP_REGULAR.getSmallIcon());
                break;
            }
        }
    }

    /**
     * @param move For add: if true, remove from all previous groups
     */
    private AbstractAction getAction(GroupTreeNode node, BibtexEntry[] selection,
            boolean add, boolean move) {
        AbstractAction action = add ? new AddToGroupAction(node, move,
                panel) : new RemoveFromGroupAction(node, panel);
        AbstractGroup group = node.getGroup();
        if (!move) {
            action.setEnabled(add ? group.supportsAdd() && !group.containsAll(selection)
                    : group.supportsRemove() && group.containsAny(selection));
        } else {
            action.setEnabled(group.supportsAdd());
        }
        return action;
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        remove(groupAddMenu);
        remove(groupMoveMenu);
        remove(groupRemoveMenu);
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
        // nothing to do
    }

    private boolean isFieldSetForSelectedEntry(String fieldname) {
        if (panel.mainTable.getSelectedRowCount() == 1) {
            BibtexEntry entry = panel.mainTable.getSelected().get(0);
            if (entry.getFieldNames().contains(fieldname)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private Icon getFileIconForSelectedEntry() {
        if (panel.mainTable.getSelectedRowCount() == 1) {
            BibtexEntry entry = panel.mainTable.getSelected().get(0);
            String file = entry.getField(Globals.FILE_FIELD);
            if(file!=null) {
                return FileListTableModel.getFirstLabel(file).getIcon();
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


    static class ChangeTypeAction extends AbstractAction {

        final BibtexEntryType type;
        final BasePanel panel;


        public ChangeTypeAction(BibtexEntryType type, BasePanel bp) {
            super(type.getName());
            this.type = type;
            panel = bp;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            panel.changeType(type);
        }
    }

}
