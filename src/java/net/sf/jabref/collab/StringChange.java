package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertString;
import net.sf.jabref.undo.UndoableStringChange;

public class StringChange extends Change {

  BibtexString string;
  String mem, tmp, disk, label;

  InfoPane tp = new InfoPane();
  JScrollPane sp = new JScrollPane(tp);
    private BibtexString tmpString;

    public StringChange(BibtexString string, BibtexString tmpString, String label,
                      String mem, String tmp, String disk) {
        this.tmpString = tmpString;
        name = Globals.lang("Modified string")+": '"+label+"'";
    this.string = string;
    this.label = label;
    this.mem = mem;
    this.tmp = tmp;
    this.disk = disk;

    StringBuffer sb = new StringBuffer();
    sb.append("<HTML><H2>");
    sb.append(Globals.lang("Modified string"));
    sb.append("</H2><H3>");
      sb.append(Globals.lang("Label")).append(":</H3>");
    sb.append(label);
    sb.append("<H3>");
      sb.append(Globals.lang("New content")).append(":</H3>");
    sb.append(disk);
    if (string != null) {
      sb.append("<H3>");
        sb.append(Globals.lang("Current content")).append(":</H3>");
      sb.append(string.getContent());
    } else {
      sb.append("<P><I>");
        sb.append(Globals.lang("Cannot merge this change")).append(": ");
        sb.append(Globals.lang("The string has been removed locally")).append("</I>");
    }
    sb.append("</HTML>");
    tp.setText(sb.toString());
  }

  public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {
    if (string != null) {
      string.setContent(disk);
      undoEdit.addEdit(new UndoableStringChange(panel, string, false, mem, disk));
        // Update tmp databse:

    } else {
      // The string was removed or renamed locally. We guess that it was removed.
	String newId = Util.createNeutralId();
	BibtexString bs = new BibtexString(newId, label, disk);
	try {
	    panel.database().addString(bs);
	    undoEdit.addEdit(new UndoableInsertString(panel, panel.database(), bs));
	} catch (KeyCollisionException ex) {
	    Globals.logger("Error: could not add string '"+string.getName()+"': "+ex.getMessage());
	}
    }

      // Update tmp database:
      if (tmpString != null) {
          tmpString.setContent(disk);
      }
      else {
          BibtexString bs = new BibtexString(Util.createNeutralId(), label, disk);
          secondary.addString(bs);
      }

      return true;
  }


  JComponent description() {
    return sp;
  }


}
