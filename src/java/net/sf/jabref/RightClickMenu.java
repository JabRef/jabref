/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;

import java.awt.Font;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.groups.*;
import net.sf.jabref.undo.NamedCompound;

public class RightClickMenu extends JPopupMenu
    implements PopupMenuListener {

    BasePanel panel;
    MetaData metaData;
    JMenu groupAddMenu = new JMenu(Globals.lang("Add to group")),
        groupRemoveMenu = new JMenu(Globals.lang("Remove from group")),
        groupMoveMenu = new JMenu("Assign exclusively to group"), // JZTODO lyrics
        typeMenu = new JMenu(Globals.lang("Change entry type"));

    public RightClickMenu(BasePanel panel_, MetaData metaData_) {
        panel = panel_;
        metaData = metaData_;
        
        // Are multiple entries selected?
        boolean multiple = (panel.mainTable.getSelectedRowCount() > 1);

        // If only one entry is selected, get a reference to it for adapting the menu.
        BibtexEntry be = null;
        if (panel.mainTable.getSelectedRowCount() == 1)
          be = (BibtexEntry)panel.mainTable.getSelected().get(0);

        addPopupMenuListener(this);

        add(new AbstractAction(Globals.lang("Copy"), new ImageIcon(GUIGlobals.copyIconFile)) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("copy");
                    } catch (Throwable ex) {}
                }
            });
        add(new AbstractAction(Globals.lang("Paste"), new ImageIcon(GUIGlobals.pasteIconFile)) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("paste");
                    } catch (Throwable ex) {}
                }
            });
        add(new AbstractAction(Globals.lang("Cut"), new ImageIcon(GUIGlobals.cutIconFile)) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("cut");
                    } catch (Throwable ex) {}
                }
            });

        add(new AbstractAction(Globals.lang("Delete"), new ImageIcon(GUIGlobals.removeIconFile)) {
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
            addSeparator();
         
        if (multiple) {
          add(new AbstractAction(Globals.lang("Mark entries"), new ImageIcon(GUIGlobals.markIcon)) {
            public void actionPerformed(ActionEvent e) {
              try {
                panel.runCommand("markEntries");
              } catch (Throwable ex) {}
            }
          });
          add(new AbstractAction(Globals.lang("Unmark entries"), new ImageIcon(GUIGlobals.unmarkIcon)) {
            public void actionPerformed(ActionEvent e) {
              try {
                panel.runCommand("unmarkEntries");
              } catch (Throwable ex) {}
            }
          });
          addSeparator();
        } else if (be != null) {
          if (be.getField(Globals.MARKED) == null)
            add(new AbstractAction(Globals.lang("Mark entry"), new ImageIcon(GUIGlobals.markIcon)) {
               public void actionPerformed(ActionEvent e) {
                 try {
                   panel.runCommand("markEntries");
                 } catch (Throwable ex) {}
               }
             });
           else
             add(new AbstractAction(Globals.lang("Unmark entry"), new ImageIcon(GUIGlobals.unmarkIcon)) {
               public void actionPerformed(ActionEvent e) {
                 try {
                   panel.runCommand("unmarkEntries");
                 } catch (Throwable ex) {}
               }
             });
           addSeparator();
        }

        add(new AbstractAction(Globals.lang("Open PDF or PS"), new ImageIcon(GUIGlobals.pdfIcon)) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("openFile");
                    } catch (Throwable ex) {}
                }
            });

            add(new AbstractAction(Globals.lang("Open URL or DOI"), new ImageIcon(GUIGlobals.wwwIcon)) {
              public void actionPerformed(ActionEvent e) {
                try {
                  panel.runCommand("openUrl");
                } catch (Throwable ex) {}
              }
            });

        add(new AbstractAction(Globals.lang("Copy BibTeX key"), new ImageIcon(GUIGlobals.copyKeyIconFile)) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("copyKey");
                    } catch (Throwable ex) {}
                }
            });

        add(new AbstractAction(Globals.lang("Copy")+" \\cite{"+Globals.lang("BibTeX key")+"}", new ImageIcon(GUIGlobals.copyKeyIconFile)) {
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
        addSeparator(); // for "add/move/remove to/from group" entries (appended here)
    }

    /**
     * Remove all types from the menu. Then cycle through all available
     * types, and add them.
     */
    public void populateTypeMenu() {
        typeMenu.removeAll();
        for (Iterator i=BibtexEntryType.ALL_TYPES.keySet().iterator();
             i.hasNext();) {
            typeMenu.add(new ChangeTypeAction
                             (BibtexEntryType.getType((String)i.next()), panel));
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
    			menuItem.setIcon(GUIGlobals.groupIncludingIcon);
    			break;
    		case AbstractGroup.REFINING:
    			menuItem.setIcon(GUIGlobals.groupRefiningIcon);
    			break;
    		default:
    			menuItem.setIcon(GUIGlobals.groupRegularIcon);
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
