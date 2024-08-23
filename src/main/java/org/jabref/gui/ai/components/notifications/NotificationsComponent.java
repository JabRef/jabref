package org.jabref.gui.ai.components.notifications;

import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;

public class NotificationsComponent extends Pane {
    public NotificationsComponent(List<Notification> notifications) {
        VBox vBox = new VBox(10);

        notifications
                .stream()
                .map(NotificationComponent::new)
                .forEach(vBox.getChildren()::add);

        ScrollPane scrollPane = new ScrollPane(vBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPadding(new Insets(10));

        getChildren().add(scrollPane);
    }

    public static IconTheme.JabRefIcons findSuitableIcon(List<Notification> notifications) {
        if (has(notifications, NotificationType.ERROR)) {
            return IconTheme.JabRefIcons.ERROR;
        } else if (has(notifications, NotificationType.WARNING)) {
            return IconTheme.JabRefIcons.WARNING;
        } else {
            return IconTheme.JabRefIcons.INTEGRITY_INFO;
        }
    }

    private static boolean has(List<Notification> notifications, NotificationType type) {
        return notifications.stream().anyMatch(notification -> notification.type() == type);
    }
}
