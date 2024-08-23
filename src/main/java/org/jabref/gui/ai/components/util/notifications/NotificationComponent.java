package org.jabref.gui.ai.components.util.notifications;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;

public class NotificationComponent extends HBox {
    public NotificationComponent(Notification notification) {
        setSpacing(10);
        setPadding(new Insets(10));

        getChildren().addAll(
                getIcon(notification.type()),
                new VBox(10,
                        new Label(notification.title()),
                        new Label(notification.message())
                )
        );
    }

    private static Node getIcon(NotificationType type) {
        return switch (type) {
            case ERROR -> IconTheme.JabRefIcons.ERROR.getGraphicNode();
            case WARNING -> IconTheme.JabRefIcons.WARNING.getGraphicNode();
        };
    }
}
