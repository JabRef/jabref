package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.bibtex.comparator.MetaDataDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

class MetaDataChangeViewModel extends DatabaseChangeViewModel {

    private final MetaDataDiff metaDataDiff;
    private final PreferencesService preferences;

    public MetaDataChangeViewModel(MetaDataDiff metaDataDiff, PreferencesService preferences) {
        super(Localization.lang("Metadata change"));
        this.metaDataDiff = metaDataDiff;
        this.preferences = preferences;
    }

    @Override
    public Node description() {
        VBox container = new VBox(15);

        Label header = new Label(Localization.lang("The following metadata changed:"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);

        for (String change : metaDataDiff.getDifferences(preferences)) {
            container.getChildren().add(new Label(change));
        }

        return container;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.setMetaData(metaDataDiff.getNewMetaData());
    }
}
