package org.jabref.gui.collab.experimental.stringchange;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.experimental.ExternalChangeDetailsView;
import org.jabref.logic.l10n.Localization;

public final class StringChangeDetailsView extends ExternalChangeDetailsView {

    public StringChangeDetailsView(StringChange stringChange) {
        VBox container = new VBox();
        Label header = new Label(Localization.lang("Modified string"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().addAll(
                header,
                new Label(Localization.lang("Label") + ": " + stringChange.getOldString().getName()),
                new Label(Localization.lang("Content") + ": " + stringChange.getNewString().getContent())
        );

        container.getChildren().add(new Label(Localization.lang("Current content") + ": " + stringChange.getOldString().getContent()));
        setLeftAnchor(container, 8d);
        setTopAnchor(container, 8d);
        setRightAnchor(container, 8d);
        setBottomAnchor(container, 8d);

        getChildren().setAll(container);
    }
}
