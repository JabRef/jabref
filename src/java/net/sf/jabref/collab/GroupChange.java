package net.sf.jabref.collab;

import java.util.Vector;
import javax.swing.*;
import net.sf.jabref.*;
import net.sf.jabref.groups.*;
import net.sf.jabref.undo.NamedCompound;

public class GroupChange
    extends Change {

  AbstractGroup tmpGroup;
  AbstractGroup diskGroup;
  boolean removedLocally = false;
  MetaData md;

  public GroupChange(MetaData md, AbstractGroup tmpGroup, AbstractGroup diskGroup) {
    super("Modified group");
    // JZTODO
//    this.tmpGroup = tmpGroup;
//    this.diskGroup = diskGroup;
//    this.md = md;
//
//    if (md == null)
//      removedLocally = true;
//    else {
//      GroupTreeNode groups = md.getGroups();
//      if ((groups == null) || (GroupSelector.findGroupByName(groups, tmpGroup.getName()) == -1))
//        removedLocally = true;
//    }
  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {
      // JZTODO
//
//    GroupTreeNode groups = md.getGroups();
//    //if (groups == null)
//    // Error, no groups...
//
//    int pos = GroupSelector.findGroupByName(groups,tmpGroup.getName());
//    if (pos >= 0) {
//      groups.setElementAt(diskGroup, pos);
//    }
  }

  JComponent description() {
    return new JLabel(name);
  }


}
