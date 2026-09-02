package org.jabref.gui.collab.groupchange;

import javafx.scene.control.Label;

import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.logic.l10n.Localization;

public final class GroupChangeDetailsView extends DatabaseChangeDetailsView {

    public GroupChangeDetailsView(GroupChange groupChange) {
        this(groupChange, Localization.lang("%0. Accepting the change replaces the complete groups tree with the externally modified groups tree.", groupChange.getName()));
    }

    public GroupChangeDetailsView(GroupChange groupChange, String labelValue) {
        Label label = new Label(labelValue);
        label.setWrapText(true);

        this.setAllAnchorsAndAttachChild(label);
    }
}
