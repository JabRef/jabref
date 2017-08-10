package org.jabref.gui.fieldeditors.contextmenu;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

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
        for (Formatter converter : Formatters.CONVERTERS) {
            MenuItem menuItem = new MenuItem(converter.getName());
            //menuItem.setToolTipText(converter.getDescription());
            menuItem.setOnAction(event -> text.set(converter.format(text.get())));
            this.getItems().add(menuItem);
        }
    }
}
