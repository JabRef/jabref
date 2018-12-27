package org.jabref.gui.fieldeditors.contextmenu;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;

import org.jabref.logic.l10n.Localization;

class ClearField extends MenuItem {

    public ClearField(TextInputControl opener) {
        super(Localization.lang("Clear"));
        setOnAction(event -> opener.setText(""));
    }
}
