package org.jabref.gui.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.jabref.gui.BasePanel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.gui.undo.UndoableStringChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringChangeViewModel extends ChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringChangeViewModel.class);
    private final BibtexString string;
    private final String disk;

    private final String label;
    private final InfoPane tp = new InfoPane();
    private final JScrollPane sp = new JScrollPane(tp);

    private final BibtexString tmpString;


    public StringChangeViewModel(BibtexString string, BibtexString tmpString, String disk) {
        super(Localization.lang("Modified string") + ": '" + tmpString.getName() + '\'');
        this.tmpString = tmpString;
        this.string = string;
        this.label = tmpString.getName();
        this.disk = disk;

        StringBuilder sb = new StringBuilder(46);
        sb.append("<HTML><H2>").append(Localization.lang("Modified string")).append("</H2><H3>")
                .append(Localization.lang("Label")).append(":</H3>").append(label).append("<H3>")
                .append(Localization.lang("New content")).append(":</H3>").append(disk);
        if (string == null) {
            sb.append("<P><I>");
            sb.append(Localization.lang("Cannot merge this change")).append(": ");
            sb.append(Localization.lang("The string has been removed locally")).append("</I>");
        } else {
            sb.append("<H3>");
            sb.append(Localization.lang("Current content")).append(":</H3>");
            sb.append(string.getContent());
        }
        sb.append("</HTML>");
        tp.setText(sb.toString());
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        if (string == null) {
            // The string was removed or renamed locally. We guess that it was removed.
            BibtexString bs = new BibtexString(label, disk);
            try {
                panel.getDatabase().addString(bs);
                undoEdit.addEdit(new UndoableInsertString(panel, panel.getDatabase(), bs));
            } catch (KeyCollisionException ex) {
                LOGGER.info("Error: could not add string '" + bs.getName() + "': " + ex.getMessage(), ex);
            }
        } else {
            String mem = string.getContent();
            string.setContent(disk);
            undoEdit.addEdit(new UndoableStringChange(panel, string, false, mem, disk));
        }

        // Update tmp database:
        if (tmpString == null) {
            BibtexString bs = new BibtexString(label, disk);
            secondary.addString(bs);
        } else {
            tmpString.setContent(disk);
        }

        return true;
    }

    @Override
    public JComponent description() {
        return sp;
    }

}
