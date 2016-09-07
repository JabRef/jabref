package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoablePreambleChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;

class PreambleChange extends Change {

    private final String mem;
    private final String disk;
    private final InfoPane tp = new InfoPane();
    private final JScrollPane sp = new JScrollPane(tp);


    public PreambleChange(String mem, String disk) {
        super(Localization.lang("Changed preamble"));
        this.disk = disk;
        this.mem = mem;

        StringBuilder text = new StringBuilder(34);
        text.append("<FONT SIZE=3><H2>").append(Localization.lang("Changed preamble")).append("</H2>");

        if ((disk != null) && !disk.isEmpty()) {
            text.append("<H3>").append(Localization.lang("Value set externally")).append(":</H3>" + "<CODE>").append(disk).append("</CODE>");
        } else {
            text.append("<H3>").append(Localization.lang("Value cleared externally")).append("</H3>");
        }

        if ((mem != null) && !mem.isEmpty()) {
            text.append("<H3>").append(Localization.lang("Current value")).append(":</H3>" + "<CODE>").append(mem).append("</CODE>");
        }

        tp.setText(text.toString());
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        panel.getDatabase().setPreamble(disk);
        undoEdit.addEdit(new UndoablePreambleChange(panel.getDatabase(), panel, mem, disk));
        secondary.setPreamble(disk);
        return true;
    }

    @Override
    public JComponent description() {
        return sp;
    }
}
