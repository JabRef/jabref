package org.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jabref.Logger;
import org.jabref.gui.BasePanel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.gui.undo.UndoableStringChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;

class StringNameChange extends Change {

    private final BibtexString string;
    private final String mem;
    private final String disk;
    private final String content;

    private final BibtexString tmpString;


    public StringNameChange(BibtexString string, BibtexString tmpString,
            String mem, String tmp, String disk, String content) {
        super(Localization.lang("Renamed string") + ": '" + tmp + '\'');
        this.tmpString = tmpString;
        this.string = string;
        this.content = content;
        this.mem = mem;
        this.disk = disk;

    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {

        if (panel.getDatabase().hasStringLabel(disk)) {
            // The name to change to is already in the database, so we can't comply.
            Logger.info(this, "Cannot rename string '" + mem + "' to '" + disk + "' because the name "
                    + "is already in use.");
        }

        if (string == null) {
            // The string was removed or renamed locally. We guess that it was removed.
            BibtexString bs = new BibtexString(disk, content);
            try {
                panel.getDatabase().addString(bs);
                undoEdit.addEdit(new UndoableInsertString(panel, panel.getDatabase(), bs));
            } catch (KeyCollisionException ex) {
                Logger.info(this, "Error: could not add string '" + bs.getName() + "'", ex);
            }
        } else {
            string.setName(disk);
            undoEdit.addEdit(new UndoableStringChange(panel, string, true, mem, disk));
        }

        // Update tmp database:
        if (tmpString == null) {
            BibtexString bs = new BibtexString(disk, content);
            secondary.addString(bs);
        } else {
            tmpString.setName(disk);
        }

        return true;
    }

    @Override
    public JComponent description() {
        return new JLabel(disk + " : " + content);
    }

}
