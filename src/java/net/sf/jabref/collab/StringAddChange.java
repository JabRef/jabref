package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertString;

public class StringAddChange extends Change {

  BibtexString string;

  InfoPane tp = new InfoPane();
  JScrollPane sp = new JScrollPane(tp);

  public StringAddChange(BibtexString string) {
    name = Globals.lang("Added string")+": '"+string.getName()+"'";
    this.string = string;

    StringBuffer sb = new StringBuffer();
    sb.append("<HTML><H2>");
    sb.append(Globals.lang("Added string"));
    sb.append("</H2><H3>");
      sb.append(Globals.lang("Label")).append(":</H3>");
    sb.append(string.getName());
    sb.append("<H3>");
      sb.append(Globals.lang("Content")).append(":</H3>");
    sb.append(string.getContent());
    sb.append("</HTML>");
    tp.setText(sb.toString());

  }

  public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {

    if (panel.database().hasStringLabel(string.getName())) {
      // The name to change to is already in the database, so we can't comply.
      Globals.logger("Cannot add string '"+string.getName()+"' because the name "
                     +"is already in use.");
    }

    try {
      panel.database().addString(string);
      undoEdit.addEdit(new UndoableInsertString(panel, panel.database(), string));
    } catch (KeyCollisionException ex) {
      Globals.logger("Error: could not add string '"+string.getName()+"': "+ex.getMessage());
    }
    try {
        secondary.addString(new BibtexString(Util.createNeutralId(), string.getName(),
                string.getContent()));
    } catch (KeyCollisionException ex) {
        Globals.logger("Error: could not add string '"+string.getName()+"' to tmp database: "+ex.getMessage());
    }
    return true;
  }


  JComponent description() {
    return sp;
  }


}
