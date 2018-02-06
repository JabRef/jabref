package org.jabref.gui.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.jabref.gui.BasePanel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.logic.bibtex.comparator.PreambleDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.strings.StringUtil;

class PreambleChangeViewModel extends ChangeViewModel {

    private final String mem;
    private final String disk;
    private final InfoPane tp = new InfoPane();
    private final JScrollPane sp = new JScrollPane(tp);


    public PreambleChangeViewModel(String mem, PreambleDiff diff) {
        super(Localization.lang("Changed preamble"));
        this.disk = diff.getNewPreamble();
        this.mem = mem;

        StringBuilder text = new StringBuilder(34);
        text.append("<FONT SIZE=3><H2>").append(Localization.lang("Changed preamble")).append("</H2>");

        if (StringUtil.isNotBlank(disk)) {
            text.append("<H3>").append(Localization.lang("Value set externally")).append(":</H3>" + "<CODE>").append(disk).append("</CODE>");
        } else {
            text.append("<H3>").append(Localization.lang("Value cleared externally")).append("</H3>");
        }

        if (StringUtil.isNotBlank(mem)) {
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
