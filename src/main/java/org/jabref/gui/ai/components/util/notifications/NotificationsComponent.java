package org.jabref.gui.ai.components.util.notifications;

import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

/**
 * A {@link ScrollPane} for displaying AI chat {@link Notification}s. See the documentation of {@link Notification} for
 * more details.
 */
public class NotificationsComponent extends ScrollPane {
    private static final double SCROLL_PANE_MAX_HEIGHT = 300;

    private final VBox vBox = new VBox(10);

    public NotificationsComponent(ObservableList<Notification> notifications) {
        setContent(vBox);
        setMaxHeight(SCROLL_PANE_MAX_HEIGHT);

        fill(notifications);
        notifications.addListener((ListChangeListener<? super Notification>) change -> fill(notifications));
    }

    private void fill(List<Notification> notifications) {
        vBox.getChildren().clear();
        notifications.stream().map(NotificationComponent::new).forEach(vBox.getChildren()::add);
    }

    public static JabRefIcon findSuitableIcon(List<Notification> notifications) {
        if (has(notifications, NotificationType.ERROR)) {
            return IconTheme.JabRefIcons.ERROR.withColor(Color.RED);
        } else if (has(notifications, NotificationType.WARNING)) {
            return IconTheme.JabRefIcons.WARNING.withColor(Color.YELLOW);
        } else {
            return IconTheme.JabRefIcons.INTEGRITY_INFO.withColor(Color.BLUE);
        }
    }

    private static boolean has(List<Notification> notifications, NotificationType type) {
        return notifications.stream().anyMatch(notification -> notification.type() == type);
    }
}
