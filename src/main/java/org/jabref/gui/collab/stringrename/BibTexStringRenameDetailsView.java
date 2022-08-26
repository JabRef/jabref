package org.jabref.gui.collab.stringrename;

import javafx.scene.control.Label;

import org.jabref.gui.collab.ExternalChangeDetailsView;

public final class BibTexStringRenameDetailsView extends ExternalChangeDetailsView {

    public BibTexStringRenameDetailsView(BibTexStringRename stringRename) {
        Label label = new Label(stringRename.getNewString().getName() + " : " + stringRename.getOldString().getContent());
        setLeftAnchor(label, 8d);
        setTopAnchor(label, 8d);
        setRightAnchor(label, 8d);
        setBottomAnchor(label, 8d);

        getChildren().setAll(label);
    }
}
