package org.jabref.gui.welcome;

import javafx.scene.control.Button;

import org.jabref.gui.icon.IconTheme;

import org.jspecify.annotations.Nullable;

public class QuickSettingsButton extends Button {
    public QuickSettingsButton(String text, IconTheme.@Nullable JabRefIcons icon, Runnable action) {
        super(text);
        if (icon != null) {
            setGraphic(icon.getGraphicNode());
        }
        getStyleClass().add("quick-settings-button");
        setMaxWidth(Double.MAX_VALUE);
        setOnAction(_ -> action.run());
    }
}
