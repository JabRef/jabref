package org.jabref.gui.collab.stringdelete;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.ExternalChangeDetailsView;
import org.jabref.logic.l10n.Localization;

public final class StringDeleteDetailsView extends ExternalChangeDetailsView {

    public StringDeleteDetailsView(StringDelete stringDelete) {
        VBox container = new VBox();
        Label header = new Label(Localization.lang("Deleted string"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().addAll(
                header,
                new Label(Localization.lang("Label") + ": " + stringDelete.getDeletedString().getName()),
                new Label(Localization.lang("Content") + ": " + stringDelete.getDeletedString().getContent())
        );
        setLeftAnchor(container, 8d);
        setTopAnchor(container, 8d);
        setRightAnchor(container, 8d);
        setBottomAnchor(container, 8d);

        getChildren().setAll(container);
    }
}
