package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.stage.Screen;

import org.jabref.gui.util.FieldsUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;

public class FieldNameLabel extends Label {

    public FieldNameLabel(Field field) {
        setText(FieldTextMapper.getDisplayName(field));
        setPadding(new Insets(4, 0, 0, 0));
        setAlignment(Pos.CENTER);
        setPrefHeight(Double.POSITIVE_INFINITY);

        String description = FieldsUtil.getDescription(field);
        if (StringUtil.isNotBlank(description)) {
            Screen currentScreen = Screen.getPrimary();
            double maxWidth = currentScreen.getBounds().getWidth();
            Tooltip tooltip = new Tooltip(description);
            tooltip.setMaxWidth(maxWidth * 2 / 3);
            tooltip.setWrapText(true);
            this.setTooltip(tooltip);
        }
    }
}
