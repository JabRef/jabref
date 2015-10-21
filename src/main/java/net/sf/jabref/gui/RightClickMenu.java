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

        add(new AbstractAction(Localization.lang("Copy"), IconTheme.JabRefIcon.COPY.getIcon()) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.COPY);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not execute copy", ex);
                }
            }
        });
        add(new AbstractAction(Localization.lang("Paste"), IconTheme.JabRefIcon.PASTE.getIcon()) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.PASTE);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not execute paste", ex);
                }
            }
        });
        add(new AbstractAction(Localization.lang("Cut"), IconTheme.JabRefIcon.CUT.getIcon()) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.CUT);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not execute cut", ex);
                }
            }
        });

        add(new AbstractAction(Localization.lang("Delete"), IconTheme.JabRefIcon.DELETE.getIcon()) {

            @Override
            public void actionPerformed(ActionEvent e) {
                /*SwingUtilities.invokeLater(new Runnable () {
                public void run() {*/
                try {
                    panel.runCommand(Actions.DELETE);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not execute delete", ex);
                }
                /*}
                }); */
            }
        });
        addSeparator();

        add(new AbstractAction(Localization.lang("Export to clipboard"), IconTheme.JabRefIcon.EXPORT_TO_CLIPBOARD.getIcon()) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.EXPORT_TO_CLIPBOARD);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not execute exportToClipboard", ex);
                }
            }
        });
        add(new AbstractAction(Localization.lang("Send as email"), IconTheme.JabRefIcon.EMAIL.getIcon()) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.SEND_AS_EMAIL);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not execute sendAsEmail", ex);
                }
            }
        });
        addSeparator();

        JMenu markSpecific = JabRefFrame.subMenu("Mark specific color");
        JabRefFrame frame = panel.frame;
        for (int i = 0; i < EntryMarker.MAX_MARKING_LEVEL; i++) {
            markSpecific.add(new MarkEntriesAction(frame, i).getMenuItem());
        }

        if (multiple) {
            add(new AbstractAction(Localization.lang("Mark entries"), IconTheme.JabRefIcon.MARK_ENTRIES.getIcon()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand(Actions.MARK_ENTRIES);
                    } catch (Throwable ex) {
                        LOGGER.warn("Could not execute markEntries", ex);
                    }
                }
            });

            add(markSpecific);

            add(new AbstractAction(Localization.lang("Unmark entries"), IconTheme.JabRefIcon.UNMARK_ENTRIES.getIcon()) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand(Actions.UNMARK_ENTRIES);
                    } catch (Throwable ex) {
                        LOGGER.warn("Could not execute unmarkEntries", ex);
                    }
                }
            });
            addSeparator();
        } else if (be != null) {
            String marked = be.getField(BibtexFields.MARKED);
            // We have to check for "" too as the marked field may be empty
            if (marked == null || marked.isEmpty()) {
                add(new AbstractAction(Localization.lang("Mark entry"), IconTheme.JabRefIcon.MARK_ENTRIES.getIcon()) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            panel.runCommand(Actions.MARK_ENTRIES);
                        } catch (Throwable ex) {
                            LOGGER.warn("Could not execute markEntries", ex);
                        }
                    }
                });

                add(markSpecific);
            } else {
                add(markSpecific);
                add(new AbstractAction(Localization.lang("Unmark entry"), IconTheme.JabRefIcon.UNMARK_ENTRIES.getIcon()) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            panel.runCommand(Actions.UNMARK_ENTRIES);
                        } catch (Throwable ex) {
                            LOGGER.warn("Could not execute unmarkEntries", ex);
                        }
                    }
                });
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

        add(new AbstractAction(Localization.lang("Open folder")) {

            {
                if (!isFieldSetForSelectedEntry("file")) {
                    this.setEnabled(false);
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.OPEN_FOLDER);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not open folder", ex);
                }
            }
        });

        add(new AbstractAction(Localization.lang("Open file"), IconTheme.JabRefIcon.FILE.getIcon()) {

            {
                if(!isFieldSetForSelectedEntry("file")) {
                    this.setEnabled(false);
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.OPEN_EXTERNAL_FILE);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not open external file", ex);
                }
            }
        });

        add(new AbstractAction(Localization.lang("Attach file"), IconTheme.JabRefIcon.ATTACH_FILE.getIcon()) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.ADD_FILE_LINK);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not attach file", ex);
                }
            }
        });
        /*add(new AbstractAction(Globals.lang("Open PDF or PS"), GUIGlobals.getImage("openFile")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("openFile");
                } catch (Throwable ex) {}
            }
        });*/

        add(new AbstractAction(Localization.lang("Open URL or DOI"), IconTheme.JabRefIcon.WWW.getIcon()) {

            {
                if(!(isFieldSetForSelectedEntry("url") || isFieldSetForSelectedEntry("doi"))) {
                    this.setEnabled(false);
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.OPEN_URL);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not execute open URL", ex);
                }
            }
        });

        add(new AbstractAction(Localization.lang("Get BibTeX data from DOI"), IconTheme.JabRefIcon.DOI.getIcon()) {

            {
                if(!(isFieldSetForSelectedEntry("doi"))) {
                    this.setEnabled(false);
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.MERGE_DOI);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not merge with DOI data", ex);
                }
            }
        });

        add(new AbstractAction(Localization.lang("Copy BibTeX key")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.COPY_KEY);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not copy BibTex key", ex);
                }
            }
        });

        add(new AbstractAction(Localization.lang("Copy") + " \\cite{" + Localization.lang("BibTeX key") + '}') {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.COPY_CITE_KEY);
                } catch (Throwable ex) {
                    LOGGER.warn("Could not copy cite key", ex);
                }
            }
        });

        addSeparator();
        populateTypeMenu();

        add(typeMenu);
        add(new AbstractAction(Localization.lang("Plain text import"))
        {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.PLAIN_TEXT_IMPORT);
                } catch (Throwable ex) {
                    LOGGER.debug("Could not import plain text", ex);
                }
            }
        });

        add(JabRef.jrf.massSetField);
        add(JabRef.jrf.manageKeywords);

        addSeparator(); // for "add/move/remove to/from group" entries (appended here)

        groupAdd = new JMenuItem(new AbstractAction(Localization.lang("Add to group"))
        {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.ADD_TO_GROUP);

                    //BibtexEntry[] bes = panel.getSelectedEntries();
                    //JMenu groupMenu = buildGroupMenu(bes, true, false);

                } catch (Throwable ex) {
                    LOGGER.debug("Could not add to group", ex);
                }
            }
        });
        add(groupAdd);
        groupRemove = new JMenuItem(new AbstractAction(Localization.lang("Remove from group"))
        {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.REMOVE_FROM_GROUP);
                } catch (Throwable ex) {
                    LOGGER.debug("Could not remove from group", ex);
                }
            }
        });
        add(groupRemove);

        JMenuItem groupMoveTo = add(new AbstractAction(Localization.lang("Move to group")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand(Actions.MOVE_TO_GROUP);
                } catch (Throwable ex) {
                    LOGGER.debug("Could not execute move to group", ex);
                }
            }
        });
        add(groupMoveTo);

        floatMarked.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Globals.prefs.putBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES, floatMarked.isSelected());
                panel.mainTable.refreshSorting(); // Bad remote access
            }
        });


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
        menu.setIcon(field.getRepresentingIcon());
        for (SpecialFieldValue val : field.getValues()) {
            menu.add(val.getMenuAction(frame));
        }
    }

    /**
     * Set the dynamic contents of "Add to group ..." submenu.
     */
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        BibtexEntry[] bes = panel.getSelectedEntries();
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

        /*groupAddMenu.setEnabled(true);
        groupMoveMenu.setEnabled(true);
        groupRemoveMenu.setEnabled(true);
        groupAddMenu.removeAll();
        groupMoveMenu.removeAll();
        groupRemoveMenu.removeAll();

        add(groupAddMenu);
        add(groupMoveMenu);
        add(groupRemoveMenu);

        groupAddMenu.setEnabled(false);
        groupMoveMenu.setEnabled(false);
        groupRemoveMenu.setEnabled(false);*/

        /*insertNodes(groupAddMenu,metaData.getGroups(),bes,true,false);
        insertNodes(groupMoveMenu,metaData.getGroups(),bes,true,true);
        insertNodes(groupRemoveMenu,metaData.getGroups(),bes,false,false);*/

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
                menuItem.setIcon(IconTheme.JabRefIcon.GROUP_INCLUDING.getIcon());
                break;
            case REFINING:
                menuItem.setIcon(IconTheme.JabRefIcon.GROUP_REFINING.getIcon());
                break;
            default:
                menuItem.setIcon(IconTheme.JabRefIcon.GROUP_REGULAR.getIcon());
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
            if (entry.getAllFields().contains(fieldname)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
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
