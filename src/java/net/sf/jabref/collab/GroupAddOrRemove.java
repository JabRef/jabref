package net.sf.jabref.collab;

import javax.swing.JComponent;
import net.sf.jabref.*;
import net.sf.jabref.groups.*;
import net.sf.jabref.undo.NamedCompound;

public class GroupAddOrRemove
    extends Change {

  AbstractGroup group;
  boolean add;
  InfoPane tp = new InfoPane();
  
  public GroupAddOrRemove(AbstractGroup group, boolean add) {
    super();
    if (add)
      name = "Added group";
    else
      name = "Removed group";
    this.group = group;
    this.add = add;
    
    StringBuffer text = new StringBuffer();
    text.append("<FONT SIZE=10>");
    text.append("<H2>"+
        (add ? Globals.lang("Added group") : Globals.lang("Removed group")));
    text.append("<H3>"+Globals.lang("Name")+":</H3>"
                +" "+group.getName());
    if (group instanceof KeywordGroup) {
        KeywordGroup keywordGroup = (KeywordGroup)group; 
        text.append("<H3>"+Globals.lang("Field")+":</H3>" 
                + " "+keywordGroup.getSearchField());
        text.append("<H3>" + Globals.lang("Regexp") + ":</H3>" + " "
                + keywordGroup.getSearchExpression());
    }
//  else if (group instanceof ...) JZTODO
    tp.setContentType("text/html");
    tp.setText(text.toString());
  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {
      // JZTODO
//    MetaData md = panel.metaData();
//    GroupTreeNode groups = null;
//    if (md != null)
//      groups = md.getGroups();
//
//      // Must report error if groups is null.
//
//    int pos = GroupSelector.findGroupByName(groups, group.getName());
//    if (add) {
//      // Add the group.
//      groups.add(group);
//    } else {
//      // Remove the group.
//      if (pos >= 0)
//        groups.removeElementAt(pos);
//    }
  }

  JComponent description() {
      return tp;
  }


}
