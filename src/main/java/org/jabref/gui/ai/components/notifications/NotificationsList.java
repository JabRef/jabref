package org.jabref.gui.ai.components.notifications;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;

public class NotificationsList {
    private final List<Notification> list = new ArrayList<>();

    public void add(Notification notification) {
        list.add(notification);
    }

    public Node getIconNode() {
        return new JabRefIconView(getIcon());
    }

    private IconTheme.JabRefIcons getIcon() {
        if (has(NotificationType.ERROR)) {
            return IconTheme.JabRefIcons.ERROR;
        } else if (has(NotificationType.WARNING)) {
            return IconTheme.JabRefIcons.WARNING;
        } else {
            return IconTheme.JabRefIcons.INTEGRITY_INFO;
        }
    }

    private boolean has(NotificationType type) {
        return list.stream().anyMatch(notification -> notification.type() == type);
    }

    public NotificationsComponent toComponent() {
        return new NotificationsComponent(list);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
}
