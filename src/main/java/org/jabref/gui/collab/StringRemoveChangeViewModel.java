package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableRemoveString;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringRemoveChangeViewModel extends DatabaseChangeViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringRemoveChangeViewModel.class);
    private final BibtexString string;

    public StringRemoveChangeViewModel(BibtexString string) {
        super(Localization.lang("Removed string") + ": '" + string.getName() + '\'');
        this.string = string;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        try {
            database.getDatabase().removeString(string.getId());
            undoEdit.addEdit(new UndoableRemoveString(database.getDatabase(), string));
        } catch (Exception ex) {
            LOGGER.warn("Error: could not remove string '" + string.getName() + "': " + ex.getMessage(), ex);
        }
    }

    @Override
    public Node description() {
        VBox container = new VBox();
        Label header = new Label(Localization.lang("Removed string"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().addAll(
                header,
                new Label(Localization.lang("Label") + ": " + string.getName()),
                new Label(Localization.lang("Content") + ": " + string.getContent())
        );
        return container;
    }
}
