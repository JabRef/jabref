package org.jabref.gui.fieldeditors.contextmenu;

import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.JTextComponent;

import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

public class CaseChangeMenu extends JMenu {

    public CaseChangeMenu(final JTextComponent parent) {
        super(Localization.lang("Change case"));
        Objects.requireNonNull(parent);

        // create menu items, one for each case changer
        for (final Formatter caseChanger : Formatters.CASE_CHANGERS) {
            JMenuItem menuItem = new JMenuItem(caseChanger.getName());
            menuItem.setToolTipText(caseChanger.getDescription());
            menuItem.addActionListener(e -> parent.setText(caseChanger.format(parent.getText())));
            this.add(menuItem);
        }
    }
}
