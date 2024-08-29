package org.jabref.gui.ai.components.util.notifications;

import javafx.scene.paint.Color;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

public enum NotificationType {
    ERROR,
    WARNING;

    public JabRefIcon getIcon() {
        return switch (this) {
            case ERROR -> IconTheme.JabRefIcons.ERROR;
            case WARNING -> IconTheme.JabRefIcons.WARNING;
        };
    }

    public Color getIconColor() {
        return switch (this) {
            case ERROR -> Color.RED;
            case WARNING -> Color.YELLOW;
        };
    }
}
