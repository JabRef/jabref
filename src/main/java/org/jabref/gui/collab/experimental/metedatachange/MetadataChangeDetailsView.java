package org.jabref.gui.collab.experimental.metedatachange;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.experimental.ExternalChangeDetailsView;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

public final class MetadataChangeDetailsView extends ExternalChangeDetailsView {

    public MetadataChangeDetailsView(MetadataChange metadataChange, PreferencesService preferencesService) {
        VBox container = new VBox(15);

        Label header = new Label(Localization.lang("The following metadata changed:"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);

        for (String change : metadataChange.getMetaDataDiff().getDifferences(preferencesService)) {
            container.getChildren().add(new Label(change));
        }

        setLeftAnchor(container, 8d);
        setTopAnchor(container, 8d);
        setRightAnchor(container, 8d);
        setBottomAnchor(container, 8d);

        getChildren().setAll(container);
    }
}
