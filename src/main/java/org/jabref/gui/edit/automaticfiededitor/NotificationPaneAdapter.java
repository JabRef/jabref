package org.jabref.gui.edit.automaticfiededitor;

import java.util.Collections;

import javafx.scene.Node;
import javafx.util.Duration;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.icon.IconTheme;

public class NotificationPaneAdapter extends LibraryTab.DatabaseNotification {

    public static final String STYLE_CLASS = "trans-notification-pane";

    public NotificationPaneAdapter(Node content) {
        super(content);
        getStyleClass().add(STYLE_CLASS);
    }

    public void notify(int affectedEntries, int totalEntries) {
        String notificationMessage = String.format("%d/%d affected entries", affectedEntries, totalEntries);
        Node notificationGraphic = IconTheme.JabRefIcons.INTEGRITY_INFO.getGraphicNode();

        notify(notificationGraphic, notificationMessage, Collections.emptyList(), Duration.millis(1750));
    }
}
