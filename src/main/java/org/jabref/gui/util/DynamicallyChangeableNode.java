package org.jabref.gui.util;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.ai.components.privacynotice.AiPrivacyNoticeGuardedComponent;

/**
 * A node that can change its content using a setContent(Node) method, similar to {@link Tab}.
 * <p>
 * It is used in places where the content is changed dynamically, but you have to provide a one {@link Node} and set it
 * only once.
 * <p>
 * See {@link AiPrivacyNoticeGuardedComponent#rebuildUi()} for example.
 */
public class DynamicallyChangeableNode extends VBox {
    protected void setContent(Node node) {
        getChildren().clear();
        VBox.setVgrow(node, Priority.ALWAYS);
        getChildren().add(node);
    }
}
