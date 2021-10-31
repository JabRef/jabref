package org.jabref.gui.sidepane;

import javafx.scene.Node;
import javafx.scene.layout.Priority;

public class GroupsSidePaneView extends SidePaneView {
    public GroupsSidePaneView(GroupsSidePaneHeaderView sidePaneHeaderView, Node content, Priority resizePolicy) {
        super(sidePaneHeaderView, content, resizePolicy);
    }

    public void afterOpening() {
        ((GroupsSidePaneHeaderView) (getSidePaneHeaderView())).afterOpening();
    }
}
