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

import net.sf.jabref.groups.GroupSelector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Iterator;
import net.sf.jabref.groups.QuickSearchRule;

public class RightClickMenu extends JPopupMenu
    implements PopupMenuListener {

    BasePanel panel;
    MetaData metaData;
    JMenu groupMenu = new JMenu(Globals.lang("Add to group")),
        groupRemoveMenu = new JMenu(Globals.lang("Remove from group")),
        typeMenu = new JMenu(Globals.lang("Change entry type")),
        setGroups = new JMenu(Globals.lang("Groups"));
    boolean forOneEntryOnly = false;

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
        add(new AbstractAction("Import plain text") {
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
      Vector groups = metaData.getData("groups");
      if ((groups == null) || (groups.size() == 0)) {
        groupMenu.setEnabled(false);
        groupRemoveMenu.setEnabled(false);
        return;
      } else {
        groupMenu.setEnabled(true);
        groupRemoveMenu.setEnabled(true);
      }
      groupMenu.removeAll();
      groupRemoveMenu.removeAll();
      setGroups.removeAll();
      if (bes == null)
        return;
      else if (bes.length < 2) {
        forOneEntryOnly = true;
        add(setGroups);
      }
      else {
        forOneEntryOnly = false;
        add(groupMenu);
        add(groupRemoveMenu);
      }

      for (int i=GroupSelector.OFFSET; i<groups.size()-2;
           i+=GroupSelector.DIM) {
        String name = (String)groups.elementAt(i+1),
            regexp = (String)groups.elementAt(i+2),
            field = (String)groups.elementAt(i);

        if (forOneEntryOnly) {
          // Only bes[0] is selected, so we can build specialized menus for it.
          QuickSearchRule qsr = new QuickSearchRule(field, regexp);
          int score = qsr.applyRule(null, bes[0]);
          //groupMenu.add(new JCheckBoxMenuItem(name, (score == 0)));
          ToggleGroupAction item = new ToggleGroupAction(name, regexp, field, (score > 0));
          setGroups.add(item);
        }
        else {
          groupMenu.add(new AddToGroupAction(name, regexp, field));
          groupRemoveMenu.add
              (new RemoveFromGroupAction(name, regexp, field));
        }

      }
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      remove(groupMenu);
      remove(groupRemoveMenu);
      remove(setGroups);
    }

    public void popupMenuCanceled(PopupMenuEvent e) {

    }

    class AddToGroupAction extends AbstractAction {
        String grp, regexp, field;
        public AddToGroupAction(String grp, String regexp, String field) {
            super(grp);
            this.grp = grp;
            this.regexp = regexp;
            this.field = field;
        }
        public void actionPerformed(ActionEvent evt) {
            panel.addToGroup(grp, regexp, field);
        }
    }

    class RemoveFromGroupAction extends AbstractAction {
        String grp, regexp, field;
        public RemoveFromGroupAction
            (String grp, String regexp, String field) {

            super(grp);
            this.grp = grp;
            this.regexp = regexp;
            this.field = field;
        }
        public void actionPerformed(ActionEvent evt) {
            panel.removeFromGroup(grp, regexp, field);
        }
    }

    class ToggleGroupAction extends JCheckBoxMenuItem implements ActionListener {
        String grp, regexp, field;
        boolean isIn;
        public ToggleGroupAction(String name, String regexp, String field, boolean isIn) {
            super(name, isIn);
            this.isIn = isIn;
            this.grp = grp;
            this.regexp = regexp;
            this.field = field;
            addActionListener(this);
        }
        public void actionPerformed(ActionEvent evt) {
          if (isIn)
            panel.removeFromGroup(grp, regexp, field);
          else
            panel.addToGroup(grp, regexp, field);
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
