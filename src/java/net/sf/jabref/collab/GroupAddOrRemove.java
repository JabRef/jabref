package net.sf.jabref.collab;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BasePanel;
import net.sf.jabref.groups.GroupSelector;
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

public class GroupAddOrRemove
    extends Change {

  String field, gName, regexp;
  boolean add;
  InfoPane tp = new InfoPane();
  
  public GroupAddOrRemove(String field, String gName, String regexp, boolean add) {
    super();
    if (add)
      name = "Added group";
    else
      name = "Removed group";
    this.field = field;
    this.gName = gName;
    this.regexp = regexp;
    this.add = add;
    
    StringBuffer text = new StringBuffer();
    text.append("<FONT SIZE=10>");
    text.append("<H2>"+
        (add ? Globals.lang("Added group") : Globals.lang("Removed group")));
    text.append("<H3>"+Globals.lang("Name")+":</H3>"
                +" "+gName);
    text.append("<H3>"+Globals.lang("Field")+":</H3>"
                 +" "+field);
    text.append("<H3>"+Globals.lang("Regexp")+":</H3>"
                +" "+regexp);
    tp.setContentType("text/html");
    tp.setText(text.toString());
  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {
    MetaData md = panel.metaData();
    Vector groups = null;
    if (md != null)
      groups = md.getData("groups");

      // Must report error if groups is null.

    if (add) {
      // Add the group.
      int pos = GroupSelector.findPos(groups, gName);
      groups.add(pos, regexp);
      groups.add(pos, gName);
      groups.add(pos, field);
    } else {
      // Remove the group.
      int pos = Util.findGroup(gName, groups);
      if (pos >= 0) {
        groups.removeElementAt(pos);
        groups.removeElementAt(pos);
        groups.removeElementAt(pos);
      }

    }
  }

  JComponent description() {
      return tp;
  }


}
