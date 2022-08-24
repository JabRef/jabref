package org.jabref.gui.collab.stringadd;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.ExternalChangeDetailsView;
import org.jabref.logic.l10n.Localization;

public final class StringAddDetailsView extends ExternalChangeDetailsView {

    public StringAddDetailsView(StringAdd stringAdd) {
        VBox container = new VBox();
        Label header = new Label(Localization.lang("Added string"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().addAll(
                header,
                new Label(Localization.lang("Label: %0", stringAdd.getAddedString().getName())),
                new Label(Localization.lang("Content: %0", stringAdd.getAddedString().getContent()))
        );
        setLeftAnchor(container, 8d);
        setTopAnchor(container, 8d);
        setRightAnchor(container, 8d);
        setBottomAnchor(container, 8d);

        getChildren().setAll(container);
    }
}
