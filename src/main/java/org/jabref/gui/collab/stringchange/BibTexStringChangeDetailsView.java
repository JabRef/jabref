package org.jabref.gui.collab.stringchange;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.logic.l10n.Localization;

public final class BibTexStringChangeDetailsView extends DatabaseChangeDetailsView {

    public BibTexStringChangeDetailsView(BibTexStringChange stringChange) {
        VBox container = new VBox();
        Label header = new Label(Localization.lang("Modified string"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().addAll(
                header,
                new Label(Localization.lang("Label: %0", stringChange.getOldString().getName())),
                new Label(Localization.lang("Content: %0", stringChange.getNewString().getContent()))
        );

        container.getChildren().add(new Label(Localization.lang("Current content: %0", stringChange.getOldString().getContent())));
        setLeftAnchor(container, 8d);
        setTopAnchor(container, 8d);
        setRightAnchor(container, 8d);
        setBottomAnchor(container, 8d);

        getChildren().setAll(container);
    }
}
