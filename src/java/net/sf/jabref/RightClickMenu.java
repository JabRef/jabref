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
package net.sf.jabref;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.sf.jabref.groups.*;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.Relevance;
import net.sf.jabref.specialfields.SpecialField;
import net.sf.jabref.specialfields.SpecialFieldValue;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

public class RightClickMenu extends JPopupMenu
        implements PopupMenuListener {

    BasePanel panel;
    MetaData metaData;
    JMenu groupAddMenu = new JMenu(Globals.lang("Add to group")),
            groupRemoveMenu = new JMenu(Globals.lang("Remove from group")),
            groupMoveMenu = new JMenu(Globals.lang("Assign exclusively to group")), // JZTODO lyrics
            rankingMenu = new JMenu(),
            priorityMenu = new JMenu(),
            typeMenu = new JMenu(Globals.lang("Change entry type"));
    JCheckBoxMenuItem
            floatMarked = new JCheckBoxMenuItem(Globals.lang("Float marked entries"),
            Globals.prefs.getBoolean("floatMarkedEntries"));

    public RightClickMenu(BasePanel panel_, MetaData metaData_) {
        panel = panel_;
        metaData = metaData_;

        // Are multiple entries selected? 
        boolean multiple = (panel.mainTable.getSelectedRowCount() > 1);

        // If only one entry is selected, get a reference to it for adapting the menu.
        BibtexEntry be = null;
        if (panel.mainTable.getSelectedRowCount() == 1)
            be = panel.mainTable.getSelected().get(0);

        addPopupMenuListener(this);

        add(new AbstractAction(Globals.lang("Copy"), GUIGlobals.getImage("copy")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("copy");
                } catch (Throwable ex) {}
            }
        });
        add(new AbstractAction(Globals.lang("Paste"), GUIGlobals.getImage("paste")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("paste");
                } catch (Throwable ex) {}
            }
        });
        add(new AbstractAction(Globals.lang("Cut"), GUIGlobals.getImage("cut")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("cut");
                } catch (Throwable ex) {}
            }
        });

        add(new AbstractAction(Globals.lang("Delete"), GUIGlobals.getImage("delete")) {
            public void actionPerformed(ActionEvent e) {
                /*SwingUtilities.invokeLater(new Runnable () {
             public void run() {*/
                try {
                    panel.runCommand("delete");
                } catch (Throwable ex) {}
                /*}
               }); */

            }
        });
        addSeparator();

        add(new AbstractAction(Globals.lang("Export to clipboard")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("exportToClipboard");
                } catch (Throwable ex) {}
            }
        });
        add(new AbstractAction(Globals.lang("Send as email")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("sendAsEmail");
                } catch (Throwable ex) {}
            }
        });
        addSeparator();

        JMenu markSpecific = JabRefFrame.subMenu("Mark specific color");
        JabRefFrame frame = panel.frame;
        for (int i=0; i<Util.MAX_MARKING_LEVEL; i++)
            markSpecific.add(new MarkEntriesAction(frame, i).getMenuItem());

        if (multiple) {
            add(new AbstractAction(Globals.lang("Mark entries"), GUIGlobals.getImage("markEntries")) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("markEntries");
                    } catch (Throwable ex) {}
                }
            });

            add(markSpecific);

            add(new AbstractAction(Globals.lang("Unmark entries"), GUIGlobals.getImage("unmarkEntries")) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("unmarkEntries");
                    } catch (Throwable ex) {}
                }
            });
            addSeparator();
        } else if (be != null) {
            if (be.getField(BibtexFields.MARKED) == null) {

                add(new AbstractAction(Globals.lang("Mark entry"), GUIGlobals.getImage("markEntries")) {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            panel.runCommand("markEntries");
                        } catch (Throwable ex) {}
                    }
                });

                add(markSpecific);
            } else {
                add(markSpecific);
                add(new AbstractAction(Globals.lang("Unmark entry"), GUIGlobals.getImage("unmarkEntries")) {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            panel.runCommand("unmarkEntries");
                        } catch (Throwable ex) {}
                    }
                });
            }
            addSeparator();
        }

        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
        	if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING)) {
        		populateSpecialFieldMenu(this.rankingMenu, Rank.getInstance(), panel.frame);
    	        add(this.rankingMenu);
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

        	if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY)) {
		        populateSpecialFieldMenu(this.priorityMenu, Priority.getInstance(), panel.frame);
		        add(this.priorityMenu);
        	}
	        
	        addSeparator();
        }
        
        add(new AbstractAction(Globals.lang("Open file"), GUIGlobals.getImage("openExternalFile")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("openExternalFile");
                } catch (Throwable ex) {}
            }
        });

        add(new AbstractAction(Globals.lang("Attach file"), GUIGlobals.getImage("open")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("addFileLink");
                } catch (Throwable ex) {}
            }
        });
        /*add(new AbstractAction(Globals.lang("Open PDF or PS"), GUIGlobals.getImage("openFile")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("openFile");
                } catch (Throwable ex) {}
            }
        });*/

        add(new AbstractAction(Globals.lang("Open URL or DOI"), GUIGlobals.getImage("www")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("openUrl");
                } catch (Throwable ex) {}
            }
        });

        add(new AbstractAction(Globals.lang("Copy BibTeX key")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("copyKey");
                } catch (Throwable ex) {}
            }
        });

        add(new AbstractAction(Globals.lang("Copy")+" \\cite{"+Globals.lang("BibTeX key")+"}") {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("copyCiteKey");
                } catch (Throwable ex) {}
            }
        });

        addSeparator();
        populateTypeMenu();

        add(typeMenu);
        add(new AbstractAction(Globals.lang("Plain text import"))
        {
            public void actionPerformed(ActionEvent e) {
                try {
                    panel.runCommand("importPlainText");
                } catch (Throwable ex) {}
            }
        });
        
        add(JabRef.jrf.massSetField);
        add(JabRef.jrf.manageKeywords);
        
        addSeparator(); // for "add/move/remove to/from group" entries (appended here)

        floatMarked.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Globals.prefs.putBoolean("floatMarkedEntries", floatMarked.isSelected());
                panel.mainTable.refreshSorting(); // Bad remote access
            }
        });
    }
    
    /**
     * Remove all types from the menu. Then cycle through all available
     * types, and add them.
     */
    public void populateTypeMenu() {
        typeMenu.removeAll();
        for (String key : BibtexEntryType.ALL_TYPES.keySet()){
            typeMenu.add(new ChangeTypeAction
                    (BibtexEntryType.getType(key), panel));
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
        for (SpecialFieldValue val: field.getValues()) {
        	menu.add(val.getMenuAction(frame));
        }
    }
    
    /**
     * Set the dynamic contents of "Add to group ..." submenu.
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        BibtexEntry[] bes = panel.getSelectedEntries();
        panel.storeCurrentEdit();
        GroupTreeNode groups = metaData.getGroups();
        if (groups == null) {
            groupAddMenu.setEnabled(false);
            groupMoveMenu.setEnabled(false);
            groupRemoveMenu.setEnabled(false);
            return;
        }

        groupAddMenu.setEnabled(true);
        groupMoveMenu.setEnabled(true);
        groupRemoveMenu.setEnabled(true);
        groupAddMenu.removeAll();
        groupMoveMenu.removeAll();
        groupRemoveMenu.removeAll();

        if (bes == null)
            return;
        add(groupAddMenu);
        add(groupMoveMenu);
        add(groupRemoveMenu);

        groupAddMenu.setEnabled(false);
        groupMoveMenu.setEnabled(false);
        groupRemoveMenu.setEnabled(false);
        insertNodes(groupAddMenu,metaData.getGroups(),bes,true,false);
        insertNodes(groupMoveMenu,metaData.getGroups(),bes,true,true);
        insertNodes(groupRemoveMenu,metaData.getGroups(),bes,false,false);

        addSeparator();
        floatMarked.setSelected(Globals.prefs.getBoolean("floatMarkedEntries"));
        add(floatMarked);
    }

    /**
     * @param move For add: if true, remove from previous groups
     */
    public void insertNodes(JMenu menu, GroupTreeNode node, BibtexEntry[] selection,
                            boolean add, boolean move) {
        final AbstractAction action = getAction(node,selection,add,move);

        if (node.getChildCount() == 0) {
            JMenuItem menuItem = new JMenuItem(action);
            setGroupFontAndIcon(menuItem, node.getGroup());
            menu.add(menuItem);
            if (action.isEnabled())
                menu.setEnabled(true);
            return;
        }

        JMenu submenu = null;
        if (node.getGroup() instanceof AllEntriesGroup) {
            for (int i = 0; i < node.getChildCount(); ++i) {
                insertNodes(menu,(GroupTreeNode) node.getChildAt(i), selection, add, move);
            }
        } else {
            submenu = new JMenu("["+node.getGroup().getName()+"]");
            setGroupFontAndIcon(submenu, node.getGroup());
            // setEnabled(true) is done above/below if at least one menu
            // entry (item or submenu) is enabled
            submenu.setEnabled(action.isEnabled());
            JMenuItem menuItem = new JMenuItem(action);
            setGroupFontAndIcon(menuItem, node.getGroup());
            submenu.add(menuItem);
            submenu.add(new Separator());
            for (int i = 0; i < node.getChildCount(); ++i)
                insertNodes(submenu,(GroupTreeNode) node.getChildAt(i), selection, add, move);
            menu.add(submenu);
            if (submenu.isEnabled())
                menu.setEnabled(true);
        }
    }

    /** Sets the font and icon to be used, depending on the group */
    private void setGroupFontAndIcon(JMenuItem menuItem, AbstractGroup group) {
        if (Globals.prefs.getBoolean("groupShowDynamic")) {
            menuItem.setFont(menuItem.getFont().deriveFont(group.isDynamic() ?
                    Font.ITALIC : Font.PLAIN));
        }
        if (Globals.prefs.getBoolean("groupShowIcons")) {
            switch (group.getHierarchicalContext()) {
                case AbstractGroup.INCLUDING:
                    menuItem.setIcon(GUIGlobals.getImage("groupIncluding"));
                    break;
                case AbstractGroup.REFINING:
                    menuItem.setIcon(GUIGlobals.getImage("groupRefining"));
                    break;
                default:
                    menuItem.setIcon(GUIGlobals.getImage("groupRegular"));
                    break;
            }
        }
    }

    /**
     * @param move For add: if true, remove from all previous groups
     */
    private AbstractAction getAction(GroupTreeNode node, BibtexEntry[] selection,
                                     boolean add, boolean move) {
        AbstractAction action = add ? (AbstractAction) new AddToGroupAction(node, move,
                panel) : (AbstractAction) new RemoveFromGroupAction(node, panel);
        AbstractGroup group = node.getGroup();
        if (!move) {
            action.setEnabled(add ? group.supportsAdd() && !group.containsAll(selection)
                    : group.supportsRemove() && group.containsAny(selection));
        } else {
            action.setEnabled(group.supportsAdd());
        }
        return action;
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        remove(groupAddMenu);
        remove(groupMoveMenu);
        remove(groupRemoveMenu);
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
        // nothing to do
    }

    class ChangeTypeAction extends AbstractAction {
        BibtexEntryType type;
        BasePanel panel;

        public ChangeTypeAction(BibtexEntryType type, BasePanel bp) {
            super(type.getName());
            this.type = type;
            panel = bp;
        }
        public void actionPerformed(ActionEvent evt) {
            panel.changeType(type);
        }
    }
    
}
