package net.sf.jabref.collab;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BasePanel;
import net.sf.jabref.Util;
import net.sf.jabref.KeyCollisionException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.TreeSet;
import java.util.Iterator;
import javax.swing.JTextPane;
import net.sf.jabref.Globals;
import java.util.Enumeration;
import javax.swing.JScrollPane;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;
import net.sf.jabref.MetaData;
import java.util.Vector;

public class GroupChange
    extends Change {

  String tmpField, diskField, tmpRegexp, diskRegexp, gName;
  boolean removedLocally = false;
  MetaData md;

  public GroupChange(MetaData md, String gName, String tmpField, String diskField,
                     String tmpRegexp, String diskRegexp) {
    super("Modified group");
    this.gName = gName;
    this.tmpField = tmpField;
    this.diskField = diskField;
    this.tmpRegexp = tmpRegexp;
    this.diskRegexp = diskRegexp;
    this.md = md;

    if (md == null)
      removedLocally = true;
    else {
      Vector groups = md.getData("groups");
      if ((groups == null) || (Util.findGroup(gName, groups) == -1))
        removedLocally = true;
    }
  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {

    Vector groups = md.getData("groups");
    //if (groups == null)
    // Error, no groups...

    int pos = Util.findGroup(gName, groups);
    if (pos >= 0) {
      groups.setElementAt(diskField, pos);
      groups.setElementAt(diskRegexp, pos+2);
    }
  }

  JComponent description() {
    return new JLabel(name);
  }


}
