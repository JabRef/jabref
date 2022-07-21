package org.jabref.gui.edit.automaticfiededitor;

import java.util.Collections;

import javafx.scene.Node;
import javafx.util.Duration;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.icon.IconTheme;

public class NotificationPaneAdapter extends LibraryTab.DatabaseNotification {

    public NotificationPaneAdapter(Node content) {
        super(content);
    }

    public void notify(int affectedEntries, int totalEntries) {
        String notificationMessage = String.format("%d/%d affected entries", affectedEntries, totalEntries);
        Node notificationGraphic = IconTheme.JabRefIcons.INTEGRITY_INFO.getGraphicNode();

        notify(notificationGraphic, notificationMessage, Collections.emptyList(), Duration.millis(1750));
    }
}
