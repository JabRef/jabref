package org.jabref.gui.collab.experimental.groupchange;

import javafx.scene.control.Label;

import org.jabref.gui.collab.experimental.ExternalChangeDetailsView;
import org.jabref.logic.l10n.Localization;

public final class GroupChangeDetailsView extends ExternalChangeDetailsView {

    public GroupChangeDetailsView(GroupChange groupChange) {
        String labelValue = "";
        if (groupChange.getGroupDiff().getNewGroupRoot() == null) {
            labelValue = groupChange.getName() + '.';
        } else {
            labelValue = groupChange.getName() + ". " + Localization.lang("Accepting the change replaces the complete groups tree with the externally modified groups tree.");
        }
        Label label = new Label(labelValue);
        setLeftAnchor(label, 8d);
        setTopAnchor(label, 8d);
        setRightAnchor(label, 8d);
        setBottomAnchor(label, 8d);

        getChildren().setAll(label);
    }
}
