package org.jabref.gui.fieldeditors.contextmenu;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;

import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.l10n.Localization;

class ClearField extends MenuItem {

    private final TextArea opener;

    public ClearField(TextArea opener) {
        super(Localization.lang("Clear text"));
        this.opener = opener;
        setOnAction(event -> opener.setText(new NormalizeNamesFormatter().format("")));
    }
}
