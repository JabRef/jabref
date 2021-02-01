package org.jabref.gui.collab;

import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.control.Label;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableStringChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringNameChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringNameChangeViewModel.class);
    private final BibtexString string;
    private final BibtexString newString;

    public StringNameChangeViewModel(BibtexString string, BibtexString newString) {
        super(Localization.lang("Renamed string") + ": '" + string.getName() + '\'');
        this.string = Objects.requireNonNull(string);
        this.newString = Objects.requireNonNull(newString);
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        if (database.getDatabase().hasStringByName(newString.getName())) {
            // The name to change to is already in the database, so we can't comply.
            LOGGER.info("Cannot rename string '" + string.getName() + "' to '" + newString.getName() + "' because the name "
                    + "is already in use.");
        }

        String currentName = string.getName();
        String newName = newString.getName();
        string.setName(newName);
        undoEdit.addEdit(new UndoableStringChange(string, true, currentName, newName));
    }

    @Override
    public Node description() {
        return new Label(newString.getName() + " : " + string.getContent());
    }
}
