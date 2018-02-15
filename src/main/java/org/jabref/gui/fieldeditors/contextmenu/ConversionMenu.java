package org.jabref.gui.fieldeditors.contextmenu;

import javafx.beans.property.StringProperty;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tooltip;

import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

/**
 * Menu to show up on right-click in a text field for converting text formats
 */
class ConversionMenu extends Menu {

    public ConversionMenu(StringProperty text) {
        super(Localization.lang("Convert"));

        // create menu items, one for each converter
        for (Formatter converter : Formatters.getConverters()) {
            CustomMenuItem menuItem = new CustomMenuItem(new Label(converter.getName()));
            Tooltip toolTip = new Tooltip(converter.getDescription());
            Tooltip.install(menuItem.getContent(), toolTip);
            menuItem.setOnAction(event -> text.set(converter.format(text.get())));
            this.getItems().add(menuItem);
        }
    }
}
