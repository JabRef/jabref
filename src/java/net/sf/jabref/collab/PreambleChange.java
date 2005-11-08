package net.sf.jabref.collab;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BasePanel;
import net.sf.jabref.Util;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoablePreambleChange;
import net.sf.jabref.KeyCollisionException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import net.sf.jabref.Globals;

public class PreambleChange extends Change {

  String tmp, mem, disk;
  InfoPane tp = new InfoPane();
  JScrollPane sp = new JScrollPane(tp);

  public PreambleChange(String tmp, String mem, String disk) {
    super("Changed preamble");
    this.disk = disk;
    this.mem = mem;
    this.tmp = tmp;

    StringBuffer text = new StringBuffer();
    text.append("<FONT SIZE=3>");
      text.append("<H2>").append(Globals.lang("Changed preamble")).append("</H2>");

    if ((disk != null) && !disk.equals(""))
        text.append("<H3>").append(Globals.lang("Value set externally")).append(":</H3>" + "<CODE>").append(disk).append("</CODE>");
    else
        text.append("<H3>").append(Globals.lang("Value cleared externally")).append("</H3>");

    if ((mem != null) && !mem.equals(""))
        text.append("<H3>").append(Globals.lang("Current value")).append(":</H3>" + "<CODE>").append(mem).append("</CODE>");

      //tp.setContentType("text/html");
      tp.setText(text.toString());
  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {
    panel.database().setPreamble(disk);
    undoEdit.addEdit(new UndoablePreambleChange(panel.database(), panel, mem, disk));
  }

  JComponent description() {
    return sp;
  }
}
