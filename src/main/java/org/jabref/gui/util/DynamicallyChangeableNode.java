package org.jabref.gui.util;

import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/*
 * A node that can change it's content using a setContent(Node node) method, similar to {@link Tab}.
 */
public class DynamicallyChangeableNode extends VBox {
    protected void setContent(Node node) {
        getChildren().clear();
        VBox.setVgrow(node, Priority.ALWAYS);
        getChildren().add(node);
    }
}
