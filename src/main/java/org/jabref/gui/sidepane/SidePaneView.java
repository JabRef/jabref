package org.jabref.gui.sidepane;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;

public class SidePaneView extends BorderPane {
    private final SidePaneHeaderView sidePaneHeaderView;
    private final Node content;
    private final Priority resizePolicy;

    public SidePaneView(SidePaneHeaderView sidePaneHeaderView, Node content, Priority resizePolicy) {
        this.sidePaneHeaderView = sidePaneHeaderView;
        this.content = content;
        this.resizePolicy = resizePolicy;
        initView();
    }

    private void initView() {
        getStyleClass().add("sidePaneComponent");
        setTop(sidePaneHeaderView);
        setCenter(content);
    }

    public Priority getResizePolicy() {
        return resizePolicy;
    }

    protected SidePaneHeaderView getSidePaneHeaderView() {
        return sidePaneHeaderView;
    }
}
