package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.scene.control.Label;

import org.jabref.model.entry.FieldName;

public class FieldNameLabel extends Label {

    public FieldNameLabel(String fieldName) {
        super(FieldName.getDisplayName(fieldName));

        setPadding(new Insets(4, 0, 0, 0));
        // TODO: style!
        //setVerticalAlignment(SwingConstants.TOP);
        //setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        //setBorder(BorderFactory.createEmptyBorder());
    }

}
