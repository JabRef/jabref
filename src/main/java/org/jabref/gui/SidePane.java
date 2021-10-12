package org.jabref.gui;

import java.util.Collection;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * The side pane is displayed at the left side of JabRef and shows instances of {@link SidePaneComponent}.
 */
public class SidePane extends VBox {

    public SidePane() {
        setId("sidePane");
    }

    public void setComponents(Collection<SidePaneComponent> components) {
        getChildren().clear();

        for (SidePaneComponent component : components) {
            BorderPane node = new BorderPane();
            node.getStyleClass().add("sidePaneComponent");
            node.setTop(component.getHeader());
            node.setCenter(component.getContentPane());
            getChildren().add(node);
            VBox.setVgrow(node, component.getResizePolicy());
        }
    }
}
