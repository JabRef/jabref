package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.logic.bibtex.comparator.PreambleDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

class PreambleChangeViewModel extends DatabaseChangeViewModel {

    private final PreambleDiff change;

    public PreambleChangeViewModel(PreambleDiff change) {
        super(Localization.lang("Changed preamble"));
        this.change = change;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().setPreamble(change.getNewPreamble());
        undoEdit.addEdit(new UndoablePreambleChange(database.getDatabase(), change.getOriginalPreamble(), change.getNewPreamble()));
    }

    @Override
    public Node description() {
        VBox container = new VBox();
        Label header = new Label(Localization.lang("Changed preamble"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);

        if (StringUtil.isNotBlank(change.getOriginalPreamble())) {
            container.getChildren().add(new Label(Localization.lang("Current value") + ": " + change.getOriginalPreamble()));
        }

        if (StringUtil.isNotBlank(change.getNewPreamble())) {
            container.getChildren().add(new Label(Localization.lang("Value set externally") + ": " + change.getNewPreamble()));
        } else {
            container.getChildren().add(new Label(Localization.lang("Value cleared externally")));
        }

        return container;
    }
}
