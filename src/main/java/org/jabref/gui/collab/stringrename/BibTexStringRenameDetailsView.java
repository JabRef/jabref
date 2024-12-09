package org.jabref.gui.collab.stringrename;

import javafx.scene.control.Label;

import org.jabref.gui.collab.DatabaseChangeDetailsView;

public final class BibTexStringRenameDetailsView extends DatabaseChangeDetailsView {

    public BibTexStringRenameDetailsView(BibTexStringRename stringRename) {
        Label label = new Label(stringRename.getNewString().getName() + " : " + stringRename.getOldString().getContent());

        this.setAllAnchorsAndAttachChild(label);
    }
}
