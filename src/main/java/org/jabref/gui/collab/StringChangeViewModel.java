package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.gui.undo.UndoableStringChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringChangeViewModel.class);
    private final BibtexString string;
    private final String disk;
    private final String label;

    public StringChangeViewModel(BibtexString string, BibtexString tmpString, String disk) {
        super(Localization.lang("Modified string") + ": '" + tmpString.getName() + '\'');
        this.string = string;
        this.label = tmpString.getName();
        this.disk = disk;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        if (string == null) {
            // The string was removed or renamed locally. We guess that it was removed.
            BibtexString bs = new BibtexString(label, disk);
            try {
                database.getDatabase().addString(bs);
                undoEdit.addEdit(new UndoableInsertString(database.getDatabase(), bs));
            } catch (KeyCollisionException ex) {
                LOGGER.warn("Error: could not add string '" + bs.getName() + "': " + ex.getMessage(), ex);
            }
        } else {
            String mem = string.getContent();
            string.setContent(disk);
            undoEdit.addEdit(new UndoableStringChange(string, false, mem, disk));
        }
    }

    @Override
    public Node description() {
        VBox container = new VBox();
        Label header = new Label(Localization.lang("Modified string"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().addAll(
                header,
                new Label(Localization.lang("Label") + ": " + label),
                new Label(Localization.lang("Content") + ": " + disk)
        );

        if (string == null) {
            container.getChildren().add(new Label(Localization.lang("Cannot merge this change") + ": " + Localization.lang("The string has been removed locally")));
        } else {
            container.getChildren().add(new Label(Localization.lang("Current content") + ": " + string.getContent()));
        }

        return container;
    }

}
