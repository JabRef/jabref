package org.jabref.gui.ai.components.util.notifications;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;

public class NotificationComponent extends HBox {
    private final Label title = new Label("Title");
    private final Label message = new Label("Message");

    public NotificationComponent() {
        setSpacing(10);
        setPadding(new Insets(10));

        this.getChildren().addAll(new VBox(10, title, message));
    }

    public NotificationComponent(Notification notification) {
        this();
        setNotification(notification);
    }

    public void setNotification(Notification notification) {
        if (this.getChildren().size() != 1) {
            this.getChildren().removeFirst();
        }

        this.getChildren().addFirst(notification.type().getIcon().withColor(notification.type().getIconColor()).getGraphicNode());

        title.setText(notification.title());
        message.setText(notification.message());
    }
}
