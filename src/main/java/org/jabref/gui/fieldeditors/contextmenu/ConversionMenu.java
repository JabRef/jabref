package org.jabref.gui.fieldeditors.contextmenu;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.JTextComponent;

import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

/**
 * @author Oscar Gustafsson
 *
 * Menu to show up on right-click in a text field for converting text formats
 */
public class ConversionMenu extends JMenu {

    public ConversionMenu(JTextComponent opener) {
        super(Localization.lang("Convert"));
        // create menu items, one for each case changer
        for (Formatter converter : Formatters.CONVERTERS) {
            JMenuItem menuItem = new JMenuItem(converter.getName());
            menuItem.setToolTipText(converter.getDescription());
            menuItem.addActionListener(e -> opener.setText(converter.format(opener.getText())));
            this.add(menuItem);
        }
    }
}
