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

import java.awt.event.ActionEvent;
import java.util.Iterator;

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
        typeMenu = new JMenu(Globals.lang("Change entry type"));

    public RightClickMenu(BasePanel panel_, MetaData metaData_) {
        panel = panel_;
        metaData = metaData_;

        // Are multiple entries selected?
        boolean multiple = (panel.entryTable.getSelectedRowCount() > 1);

        // If only one entry is selected, get a reference to it for adapting the menu.
        BibtexEntry be = null;
        if (panel.entryTable.getSelectedRowCount() == 1)
          be = panel.entryTable.getSelectedEntries()[0];

        addPopupMenuListener(this);

        add(new AbstractAction(Globals.lang("Copy")) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("copy");
                    } catch (Throwable ex) {}
                }
            });
        add(new AbstractAction(Globals.lang("Paste")) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("paste");
                    } catch (Throwable ex) {}
                }
            });
        add(new AbstractAction(Globals.lang("Cut")) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("cut");
                    } catch (Throwable ex) {}
                }
            });

            addSeparator();

        if (multiple) {
          add(new AbstractAction(Globals.lang("Mark entries")) {
            public void actionPerformed(ActionEvent e) {
              try {
                panel.runCommand("markEntries");
              } catch (Throwable ex) {}
            }
          });
          add(new AbstractAction(Globals.lang("Unmark entries")) {
            public void actionPerformed(ActionEvent e) {
              try {
                panel.runCommand("unmarkEntries");
              } catch (Throwable ex) {}
            }
          });
          addSeparator();
        } else if (be != null) {
          if (be.getField(Globals.MARKED) == null)
            add(new AbstractAction(Globals.lang("Mark entry")) {
               public void actionPerformed(ActionEvent e) {
                 try {
                   panel.runCommand("markEntries");
                 } catch (Throwable ex) {}
               }
             });
           else
             add(new AbstractAction(Globals.lang("Unmark entry")) {
               public void actionPerformed(ActionEvent e) {
                 try {
                   panel.runCommand("unmarkEntries");
                 } catch (Throwable ex) {}
               }
             });
           addSeparator();
        }

        add(new AbstractAction(Globals.lang("Open PDF or PS")) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        panel.runCommand("openFile");
                    } catch (Throwable ex) {}
                }
            });

            add(new AbstractAction(Globals.lang("Open URL or DOI")) {
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

        add(new AbstractAction(Globals.lang("Copy \\cite{BibTeX key}")) {
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

        addSeparator();
//        add(groupMenu);
//        add(groupRemoveMenu);
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
      BibtexEntry[] bes = panel.entryTable.getSelectedEntries();
      GroupTreeNode groups = metaData.getGroups();
      if (groups == null) {
        groupAddMenu.setEnabled(false);
        groupRemoveMenu.setEnabled(false);
        return;
      }
      
      groupAddMenu.setEnabled(true);
      groupRemoveMenu.setEnabled(true);
      groupAddMenu.removeAll();
      groupRemoveMenu.removeAll();
      
      // JZ: I think having a special menu for only one selected entry
      // is rather non-intuitive because the user cannot see wheter a
      // certain menu item will add or remove the entry to/from
      // that group. IMHO, it is more consistent when the menu is always
      // the same.
      
      if (bes == null)
        return;
      add(groupAddMenu);
      add(groupRemoveMenu);

      insertNodes(groupAddMenu,metaData.getGroups(),true);
      insertNodes(groupRemoveMenu,metaData.getGroups(),false);
    }
    
    private void insertNodes(JMenu menu, GroupTreeNode node, boolean add) {
        AbstractAction action = getAction(node,add);
        
        if (node.getChildCount() == 0) {
            menu.add(action);
            return;
        }
        
        JMenu submenu = null;
        if (node.getGroup() instanceof AllEntriesGroup) {
            for (int i = 0; i < node.getChildCount(); ++i) {
                insertNodes(menu,(GroupTreeNode) node.getChildAt(i),add);
            }
        } else {
            submenu = new JMenu("["+node.getGroup().getName()+"]");
            submenu.add(action);
            submenu.add(new Separator());
            for (int i = 0; i < node.getChildCount(); ++i) {
                insertNodes(submenu,(GroupTreeNode) node.getChildAt(i),add);
            }
            menu.add(submenu);
        }
    }
    
    private AbstractAction getAction(GroupTreeNode node, boolean add) {
        AbstractAction action = add ? (AbstractAction) new AddToGroupAction(node)
                : (AbstractAction) new RemoveFromGroupAction(node);
        AbstractGroup group = node.getGroup();
        action.setEnabled(add ? group.supportsAdd() : group.supportsRemove());
        return action;
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      remove(groupAddMenu);
      remove(groupRemoveMenu);
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
        // nothing to do
    }

    class AddToGroupAction extends AbstractAction {
        GroupTreeNode m_node;
        public AddToGroupAction(GroupTreeNode node) {
            super(node.getGroup().getName());
            m_node = node;
        }
        public void actionPerformed(ActionEvent evt) {
            if (m_node.getGroup() instanceof ExplicitGroup) {
                // warn if any entry has an empty or duplicate key
                BibtexEntry[] entries = panel.getSelectedEntries();
                String key;
                int entriesWithoutKey = 0;
                int entriesWithDuplicateKey = 0;
                for (int i = 0; i < entries.length; ++i) {
                    key = entries[i].getCiteKey();
                    if (key == null)
                        key = "";
                    if (key.equals(""))
                        ++entriesWithoutKey;
                    else if (panel.database().getEntriesByKey(key).length > 1)
                        ++entriesWithDuplicateKey;
                }
                if (entriesWithoutKey > 0 || entriesWithDuplicateKey > 0) {
                    // JZTODO lyrics...
                    int i = JOptionPane.showConfirmDialog(panel.frame,"no key: " + entriesWithoutKey
                            + ", dupe key: " + entriesWithDuplicateKey,
                            "Warning",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (i == JOptionPane.CANCEL_OPTION)
                        return;
                }
            }
            AbstractUndoableEdit undo = m_node.getGroup().addSelection(panel);
            if (undo == null)
                return; // no changed made
            
            panel.undoManager.addEdit(undo);
            panel.refreshTable();
            panel.markBaseChanged();
            panel.updateEntryEditorIfShowing();
            panel.updateViewToSelected();
        }
    }
    
    class RemoveFromGroupAction extends AbstractAction {
        GroupTreeNode m_node;
        public RemoveFromGroupAction(GroupTreeNode node) {
            super(node.getGroup().getName());
            m_node = node;
        }
        public void actionPerformed(ActionEvent evt) {
            AbstractUndoableEdit undo = m_node.getGroup().removeSelection(panel);
            if (undo == null)
                return; // no changed made
            
            panel.undoManager.addEdit(undo);
            panel.refreshTable();
            panel.markBaseChanged();
            panel.updateEntryEditorIfShowing();
            panel.updateViewToSelected();
        }
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
