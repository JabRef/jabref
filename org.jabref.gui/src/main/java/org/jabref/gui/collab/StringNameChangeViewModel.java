package org.jabref.gui.collab;

import javax.swing.JComponent;
import javax.swing.JLabel;

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

class StringNameChangeViewModel extends ChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringNameChangeViewModel.class);
    private final BibtexString string;
    private final String mem;
    private final String disk;
    private final String content;

    private final BibtexString tmpString;


    public StringNameChangeViewModel(BibtexString string, BibtexString tmpString, String mem, String disk) {
        super(Localization.lang("Renamed string") + ": '" + tmpString.getName() + '\'');
        this.tmpString = tmpString;
        this.string = string;
        this.content = tmpString.getContent();
        this.mem = mem;
        this.disk = disk;

    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {

        if (panel.getDatabase().hasStringLabel(disk)) {
            // The name to change to is already in the database, so we can't comply.
            LOGGER.info("Cannot rename string '" + mem + "' to '" + disk + "' because the name "
                    + "is already in use.");
        }

        if (string == null) {
            // The string was removed or renamed locally. We guess that it was removed.
            BibtexString bs = new BibtexString(disk, content);
            try {
                panel.getDatabase().addString(bs);
                undoEdit.addEdit(new UndoableInsertString(panel, panel.getDatabase(), bs));
            } catch (KeyCollisionException ex) {
                LOGGER.info("Error: could not add string '" + bs.getName() + "': " + ex.getMessage(), ex);
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
