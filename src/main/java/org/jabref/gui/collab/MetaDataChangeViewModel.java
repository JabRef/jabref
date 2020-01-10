package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.bibtex.comparator.MetaDataDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

class MetaDataChangeViewModel extends DatabaseChangeViewModel {

    private final MetaDataDiff metaDataDiff;

    public MetaDataChangeViewModel(MetaDataDiff metaDataDiff) {
        super(Localization.lang("Metadata change"));
        this.metaDataDiff = metaDataDiff;
    }

    @Override
    public Node description() {
        VBox container = new VBox(15);

        container.getChildren().add(new Label(Localization.lang("The following metadata changed:")));

        for (String change : metaDataDiff.getDifferences()) {
            container.getChildren().add(new Label(change));
        }

        return container;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.setMetaData(metaDataDiff.getNewMetaData());
    }
}
