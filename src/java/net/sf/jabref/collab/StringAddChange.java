package net.sf.jabref.collab;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.undo.*;
import net.sf.jabref.undo.UndoableStringChange;

public class StringAddChange extends Change {

  BibtexString string;
  int pos;

  InfoPane tp = new InfoPane();
  JScrollPane sp = new JScrollPane(tp);

  public StringAddChange(BibtexString string, int pos) {
    name = Globals.lang("Added string")+": '"+string.getName()+"'";
    this.string = string;
    this.pos = pos;

    StringBuffer sb = new StringBuffer();
    sb.append("<HTML><H2>");
    sb.append(Globals.lang("Added string"));
    sb.append("</H2><H3>");
    sb.append(Globals.lang("Label")+":</H3>");
    sb.append(string.getName());
    sb.append("<H3>");
    sb.append(Globals.lang("Content")+":</H3>");
    sb.append(string.getContent());
    sb.append("</HTML>");
    tp.setText(sb.toString());

  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {

    if (panel.database().hasStringLabel(string.getName())) {
      // The name to change to is already in the database, so we can't comply.
      Globals.logger("Cannot add string '"+string.getName()+"' because the name "
                     +"is already in use.");
    }

    try {
      int goodPos = Math.min(pos, panel.database().getStringCount());
      panel.database().addString(string, goodPos);
      undoEdit.addEdit(new UndoableInsertString(panel, panel.database(), string, goodPos));
    } catch (KeyCollisionException ex) {
      Globals.logger("Error: could not add string '"+string.getName()+"': "+ex.getMessage());
    }

  }


  JComponent description() {
    return sp;
  }


}
