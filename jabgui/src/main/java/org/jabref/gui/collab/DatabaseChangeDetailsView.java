package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

import org.jabref.gui.collab.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.entrychange.EntryWithPreviewAndSourceDetailsView;
import org.jabref.gui.collab.groupchange.GroupChangeDetailsView;
import org.jabref.gui.collab.metedatachange.MetadataChangeDetailsView;
import org.jabref.gui.collab.preamblechange.PreambleChangeDetailsView;
import org.jabref.gui.collab.stringadd.BibTexStringAddDetailsView;
import org.jabref.gui.collab.stringchange.BibTexStringChangeDetailsView;
import org.jabref.gui.collab.stringdelete.BibTexStringDeleteDetailsView;
import org.jabref.gui.collab.stringrename.BibTexStringRenameDetailsView;

public sealed abstract class DatabaseChangeDetailsView extends AnchorPane permits
        EntryWithPreviewAndSourceDetailsView,
        GroupChangeDetailsView,
        MetadataChangeDetailsView,
        PreambleChangeDetailsView,
        BibTexStringAddDetailsView,
        BibTexStringChangeDetailsView,
        BibTexStringDeleteDetailsView,
        BibTexStringRenameDetailsView,
        EntryChangeDetailsView {

    /**
     * Set left, top, right, bottom anchors based on common offset parameter for the given child
     * and attach it to children.
     *
     * @param child the child node of the implementation
     * @see AnchorPane#getChildren()
     * @see javafx.collections.ObservableList#setAll(Object[])
     */
    protected void setAllAnchorsAndAttachChild(Node child) {
        double ANCHOR_PANE_OFFSET = 8D;
        setLeftAnchor(child, ANCHOR_PANE_OFFSET);
        setTopAnchor(child, ANCHOR_PANE_OFFSET);
        setRightAnchor(child, ANCHOR_PANE_OFFSET);
        setBottomAnchor(child, ANCHOR_PANE_OFFSET);
        this.getChildren().setAll(child);
    }
}
