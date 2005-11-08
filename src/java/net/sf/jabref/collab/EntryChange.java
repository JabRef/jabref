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

public class EntryChange extends Change {

  BibtexEntry memEntry, tmpEntry, diskEntry;
  boolean isModifiedLocally, modificationsAgree;

  public EntryChange(BibtexEntry memEntry, BibtexEntry tmpEntry, BibtexEntry diskEntry) {
    super();
    String key = tmpEntry.getCiteKey();
    if (key == null)
      name = "Modified entry";
    else
      name = "Modified entry: '"+key+"'";
    this.memEntry = memEntry;
    this.tmpEntry = tmpEntry;
    this.diskEntry = diskEntry;

    // We know that tmpEntry is not equal to diskEntry. Check if it has been modified
    // locally as well, since last tempfile was saved.
    isModifiedLocally = ! (Util.compareEntriesStrictly(memEntry, tmpEntry) > 1);

    // Another (unlikely?) possibility is that both disk and mem version has been modified
    // in the same way. Check for this, too.
    modificationsAgree = (Util.compareEntriesStrictly(memEntry, diskEntry) > 1);

    //Util.pr("Modified entry: "+memEntry.getCiteKey()+"\n Modified locally: "+isModifiedLocally
    //        +" Modifications agree: "+modificationsAgree);

    TreeSet allFields = new TreeSet(); //one.getAllFields());
    Object[] o = memEntry.getAllFields();
    for (int i = 0; i < o.length; i++)
      allFields.add(o[i]);
    o = tmpEntry.getAllFields();
    for (int i = 0; i < o.length; i++)
      allFields.add(o[i]);
    o = diskEntry.getAllFields();
    for (int i = 0; i < o.length; i++)
      allFields.add(o[i]);

    int score = 0;
    for (Iterator fld = allFields.iterator(); fld.hasNext();) {
      String field = (String)fld.next();
      String mem = (String)memEntry.getField(field),
          tmp = (String)tmpEntry.getField(field),
          disk = (String)diskEntry.getField(field);

      if ((tmp != null) && (disk != null)) {
        if (!tmp.equals(disk)) {
          // Modified externally.
          add(new FieldChange(field, memEntry, mem, tmp, disk));
        }
      } else if ((tmp == null) && (disk != null) && !disk.equals("")) {
        // Added externally.
        add(new FieldChange(field, memEntry, mem, tmp, disk));
      } else if ((disk == null) && (tmp != null) && !tmp.equals("")
                 && (mem != null) && !mem.equals("")) {
        // Deleted externally and not locally.
        add(new FieldChange(field, memEntry, mem, tmp, disk));
      }

      //Util.pr("Field: "+fld.next());
    }
  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {

    Enumeration e = children();
    for (; e.hasMoreElements();) {
      Change c = (Change)e.nextElement();
      if (c.isAcceptable() && c.isAccepted())
        c.makeChange(panel, undoEdit);
    }

    /*panel.database().removeEntry(memEntry.getId());
    try {
      diskEntry.setId(Util.createNeutralId());
    } catch (KeyCollisionException ex) {}
    panel.database().removeEntry(memEntry.getId());*/
  }

  JComponent description() {
    return new JLabel(name);
  }




  class FieldChange extends Change {

    BibtexEntry entry;
    String field, inMem, onTmp, onDisk;
    InfoPane tp = new InfoPane();
    JScrollPane sp = new JScrollPane(tp);

    public FieldChange(String field, BibtexEntry memEntry, String inMem, String onTmp, String onDisk) {
      entry = memEntry;
      name = field;
      this.field = field;
      this.inMem = inMem;
      this.onTmp = onTmp;
      this.onDisk = onDisk;

      StringBuffer text = new StringBuffer();
      text.append("<FONT SIZE=10>");
        text.append("<H2>").append(Globals.lang("Modification of field")).append(" <I>").append(field).append("</I></H2>");

      if ((onDisk != null) && !onDisk.equals(""))
          text.append("<H3>").append(Globals.lang("Value set externally")).append(":</H3>" + " ").append(onDisk);
      else
          text.append("<H3>").append(Globals.lang("Value cleared externally")).append("</H3>");

      if ((inMem != null) && !inMem.equals(""))
          text.append("<H3>").append(Globals.lang("Current value")).append(":</H3>" + " ").append(inMem);
      else {
        // No value in memory.
        /*if ((onTmp != null) && !onTmp.equals(inMem))
          text.append("<H2>"+Globals.lang("You have cleared this field. Original value")+":</H2>"
                      +" "+onTmp);*/
      }
      tp.setContentType("text/html");
      tp.setText(text.toString());
    }

    public void makeChange(BasePanel panel, NamedCompound undoEdit) {
      //System.out.println(field+" "+onDisk);
      entry.setField(field, onDisk);
      undoEdit.addEdit(new UndoableFieldChange(entry, field, inMem, onDisk));
    }

    JComponent description() {
      return sp;
    }

  }
}
