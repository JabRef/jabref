package org.jabref.gui.collab.preamblechange;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.logic.bibtex.comparator.PreambleDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public final class PreambleChangeDetailsView extends DatabaseChangeDetailsView {

    public PreambleChangeDetailsView(PreambleChange preambleChange) {
        PreambleDiff preambleDiff = preambleChange.getPreambleDiff();

        VBox container = new VBox();
        Label header = new Label(Localization.lang("Changed preamble"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);

        if (StringUtil.isNotBlank(preambleDiff.getOriginalPreamble())) {
            container.getChildren().add(new Label(Localization.lang("Current value: %0", preambleDiff.getOriginalPreamble())));
        }

        if (StringUtil.isNotBlank(preambleDiff.getNewPreamble())) {
            container.getChildren().add(new Label(Localization.lang("Value set externally: %0", preambleDiff.getNewPreamble())));
        } else {
            container.getChildren().add(new Label(Localization.lang("Value cleared externally")));
        }
        setLeftAnchor(container, 8d);
        setTopAnchor(container, 8d);
        setRightAnchor(container, 8d);
        setBottomAnchor(container, 8d);

        getChildren().setAll(container);
    }
}
