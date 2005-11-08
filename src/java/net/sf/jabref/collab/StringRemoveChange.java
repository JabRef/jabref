package net.sf.jabref.collab;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.undo.*;
import net.sf.jabref.undo.UndoableStringChange;

public class StringRemoveChange extends Change {

  BibtexString string, inMem;

  InfoPane tp = new InfoPane();
  JScrollPane sp = new JScrollPane(tp);


  public StringRemoveChange(BibtexString string, BibtexString inMem) {
    name = Globals.lang("Removed string")+": '"+string.getName()+"'";
    this.string = string;
    this.inMem = inMem; // Holds the version in memory. Check if it has been modified...?

    StringBuffer sb = new StringBuffer();
    sb.append("<HTML><H2>");
    sb.append(Globals.lang("Removed string"));
    sb.append("</H2><H3>");
      sb.append(Globals.lang("Label")).append(":</H3>");
    sb.append(string.getName());
    sb.append("<H3>");
      sb.append(Globals.lang("Content")).append(":</H3>");
    sb.append(string.getContent());
    sb.append("</HTML>");
    tp.setText(sb.toString());

  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {

    try {
      panel.database().removeString(inMem.getId());
      undoEdit.addEdit(new UndoableRemoveString(panel, panel.database(), string));
    } catch (Exception ex) {
      Globals.logger("Error: could not add string '"+string.getName()+"': "+ex.getMessage());
    }

  }


  JComponent description() {
    return sp;
  }


}
