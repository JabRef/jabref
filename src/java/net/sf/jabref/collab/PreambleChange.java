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
    text.append("<H2>"+Globals.lang("Changed preamble")+"</H2>");

    if ((disk != null) && !disk.equals(""))
      text.append("<H3>"+Globals.lang("Value set externally")+":</H3>"
                  +"<CODE>"+disk+"</CODE>");
    else
      text.append("<H3>"+Globals.lang("Value cleared externally")+"</H3>");

    if ((mem != null) && !mem.equals(""))
        text.append("<H3>"+Globals.lang("Current value")+":</H3>"
                    +"<CODE>"+mem+"</CODE>");

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
