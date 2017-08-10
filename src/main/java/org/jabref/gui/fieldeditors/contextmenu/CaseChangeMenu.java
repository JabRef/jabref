package org.jabref.gui.fieldeditors.contextmenu;

import java.util.Objects;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

class CaseChangeMenu extends Menu {

    public CaseChangeMenu(final StringProperty text) {
        super(Localization.lang("Change case"));
        Objects.requireNonNull(text);

        // create menu items, one for each case changer
        for (final Formatter caseChanger : Formatters.CASE_CHANGERS) {
            MenuItem menuItem = new MenuItem(caseChanger.getName());
            //menuItem.setToolTip(caseChanger.getDescription());
            menuItem.setOnAction(event -> text.set(caseChanger.format(text.get())));

            this.getItems().add(menuItem);
        }
    }
}
