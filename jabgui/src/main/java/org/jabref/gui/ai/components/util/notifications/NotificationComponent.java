package org.jabref.gui.ai.components.util.notifications;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * Component used to display {@link Notification} in AI chat. See the documentation of {@link Notification} for more
 * details.
 */
public class NotificationComponent extends VBox {
    private final Label title = new Label("Title");
    private final Label message = new Label("Message");

    public NotificationComponent() {
        setSpacing(10);
        setPadding(new Insets(10));

        title.setFont(new Font("System Bold", title.getFont().getSize()));
        this.getChildren().addAll(title, message);
    }

    public NotificationComponent(Notification notification) {
        this();
        setNotification(notification);
    }

    public void setNotification(Notification notification) {
        title.setText(notification.title());
        message.setText(notification.message());
    }
}
