package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.control.Label;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.gui.undo.UndoableStringChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringNameChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringNameChangeViewModel.class);
    private final BibtexString string;
    private final String mem;
    private final String disk;
    private final String content;

    public StringNameChangeViewModel(BibtexString string, BibtexString tmpString, String mem, String disk) {
        super(Localization.lang("Renamed string") + ": '" + tmpString.getName() + '\'');
        this.string = string;
        this.content = tmpString.getContent();
        this.mem = mem;
        this.disk = disk;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        if (database.getDatabase().hasStringLabel(disk)) {
            // The name to change to is already in the database, so we can't comply.
            LOGGER.info("Cannot rename string '" + mem + "' to '" + disk + "' because the name "
                    + "is already in use.");
        }

        if (string == null) {
            // The string was removed or renamed locally. We guess that it was removed.
            BibtexString bs = new BibtexString(disk, content);
            try {
                database.getDatabase().addString(bs);
                undoEdit.addEdit(new UndoableInsertString(database.getDatabase(), bs));
            } catch (KeyCollisionException ex) {
                LOGGER.info("Error: could not add string '" + bs.getName() + "': " + ex.getMessage(), ex);
            }
        } else {
            string.setName(disk);
            undoEdit.addEdit(new UndoableStringChange(string, true, mem, disk));
        }
    }

    @Override
    public Node description() {
        return new Label(disk + " : " + content);
    }

}
