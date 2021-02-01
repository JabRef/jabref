package org.jabref.gui.collab;

import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableStringChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringChangeViewModel.class);
    private final BibtexString string;
    private final BibtexString newString;

    public StringChangeViewModel(BibtexString string, BibtexString newString) {
        super(Localization.lang("Modified string") + ": '" + string.getName() + '\'');
        this.string = Objects.requireNonNull(string);
        this.newString = Objects.requireNonNull(newString);
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        String currentValue = string.getContent();
        String newValue = newString.getContent();
        string.setContent(newValue);
        undoEdit.addEdit(new UndoableStringChange(string, false, currentValue, newValue));
    }

    @Override
    public Node description() {
        VBox container = new VBox();
        Label header = new Label(Localization.lang("Modified string"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().addAll(
                header,
                new Label(Localization.lang("Label") + ": " + newString.getName()),
                new Label(Localization.lang("Content") + ": " + newString.getContent())
        );

        container.getChildren().add(new Label(Localization.lang("Current content") + ": " + string.getContent()));

        return container;
    }
}
