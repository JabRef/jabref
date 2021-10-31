package org.jabref.gui.sidepane;

import javafx.scene.Node;
import javafx.scene.layout.Priority;

import org.jabref.gui.DialogService;
import org.jabref.preferences.PreferencesService;

public class GroupsSidePaneView extends SidePaneView {
    public GroupsSidePaneView(GroupsSidePaneHeaderView sidePaneHeaderView, Node content, Priority resizePolicy, PreferencesService preferences, DialogService dialogService) {
        super(sidePaneHeaderView, content, resizePolicy);
    }

    public void afterOpening() {
        ((GroupsSidePaneHeaderView) (getSidePaneHeaderView())).afterOpening();
    }
}
