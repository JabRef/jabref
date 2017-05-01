package org.jabref.gui.fieldeditors.contextmenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;

import org.jabref.gui.actions.CopyDoiUrlAction;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.l10n.Localization;

public class EditorMenus {

    public static List<MenuItem> getDefaultMenu(TextArea textArea) {
        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new CaseChangeMenu(textArea.textProperty()));
        menuItems.add(new ConversionMenu(textArea.textProperty()));
        menuItems.add(new SeparatorMenuItem());
        menuItems.add(new ProtectedTermsMenu(textArea));
        menuItems.add(new SeparatorMenuItem());
        return menuItems;
    }

    public static List<MenuItem> getNameMenu(TextArea textArea) {
        MenuItem normalizeNames = new MenuItem(Localization.lang("Normalize to BibTeX name format"));
        //normalizeNames.setTooltip(Localization.lang("If possible, normalize this list of names to conform to standard BibTeX name formatting"))
        normalizeNames.setOnAction(event -> textArea.setText(new NormalizeNamesFormatter().format(textArea.getText())));

        return Collections.singletonList(normalizeNames);
    }

    public static List<MenuItem> getDOIMenu(TextArea textArea) {
        AbstractAction copyDoiUrlAction = new CopyDoiUrlAction(textArea);
        MenuItem copyDoiUrlMenuItem = new MenuItem((String) copyDoiUrlAction.getValue(Action.NAME));
        copyDoiUrlMenuItem.setOnAction(event -> copyDoiUrlAction.actionPerformed(null));

        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(copyDoiUrlMenuItem);
        menuItems.add(new SeparatorMenuItem());
        return menuItems;
    }
}
