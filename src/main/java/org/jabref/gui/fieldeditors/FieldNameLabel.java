package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.scene.control.Label;

import javafx.scene.text.Font;
import org.jabref.Globals;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

public class FieldNameLabel extends Label {

    public FieldNameLabel(String fieldName) {
        super(FieldName.getDisplayName(fieldName));

        setPadding(new Insets(4, 0, 0, 0));
        // TODO: style!
        setFont(Font.font("Verdana", Globals.prefs.getInt(JabRefPreferences.MENU_FONT_SIZE)));
        //setVerticalAlignment(SwingConstants.TOP);
        //setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        //setBorder(BorderFactory.createEmptyBorder());
    }

}
