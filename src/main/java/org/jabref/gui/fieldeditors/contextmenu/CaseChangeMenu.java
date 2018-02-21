package org.jabref.gui.fieldeditors.contextmenu;

import java.util.Objects;

import javafx.beans.property.StringProperty;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tooltip;

import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

class CaseChangeMenu extends Menu {

    public CaseChangeMenu(final StringProperty text) {
        super(Localization.lang("Change case"));
        Objects.requireNonNull(text);

        // create menu items, one for each case changer
        for (final Formatter caseChanger : Formatters.CASE_CHANGERS) {
            CustomMenuItem menuItem = new CustomMenuItem(new Label(caseChanger.getName()));
            Tooltip toolTip = new Tooltip(caseChanger.getDescription());
            Tooltip.install(menuItem.getContent(), toolTip);
            menuItem.setOnAction(event -> text.set(caseChanger.format(text.get())));

            this.getItems().add(menuItem);
        }
    }
}
