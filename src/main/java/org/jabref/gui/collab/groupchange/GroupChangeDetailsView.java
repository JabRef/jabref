package org.jabref.gui.collab.groupchange;

import javafx.scene.control.Label;

import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.logic.l10n.Localization;

public final class GroupChangeDetailsView extends DatabaseChangeDetailsView {

    public GroupChangeDetailsView(GroupChange groupChange) {
        String labelValue = "";
        if (groupChange.getGroupDiff().getNewGroupRoot() == null) {
            labelValue = groupChange.getName() + '.';
        } else {
            labelValue = Localization.lang("%0. Accepting the change replaces the complete groups tree with the externally modified groups tree.", groupChange.getName());
        }
        Label label = new Label(labelValue);
        label.setWrapText(true);

        this.setAllAnchorsAndAttachChild(label);
    }
}
