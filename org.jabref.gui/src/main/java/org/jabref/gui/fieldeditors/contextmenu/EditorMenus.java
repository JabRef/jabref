package org.jabref.gui.fieldeditors.contextmenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Action;

import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import org.jabref.gui.actions.CopyDoiUrlAction;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.l10n.Localization;

/**
 * Provides context menus for the text fields of the entry editor. Note that we use {@link Supplier} to prevent an early
 * instantiation of the menus. Therefore, they are attached to each text field but instantiation happens on the first
 * right-click of the user in that field. The late instantiation is done by {@link
 * org.jabref.gui.fieldeditors.EditorTextArea#addToContextMenu(java.util.function.Supplier)}.
 */
public class EditorMenus {

    /**
     * The default menu that contains functions for changing the case of text and doing several conversions.
     *
     * @param textArea text-area that this menu will be connected to
     * @return default context menu available for most text fields
     */
    public static Supplier<List<MenuItem>> getDefaultMenu(TextArea textArea) {
        return () -> {
            List<MenuItem> menuItems = new ArrayList<>(6);
            menuItems.add(new CaseChangeMenu(textArea.textProperty()));
            menuItems.add(new ConversionMenu(textArea.textProperty()));
            menuItems.add(new SeparatorMenuItem());
            menuItems.add(new ProtectedTermsMenu(textArea));
            menuItems.add(new SeparatorMenuItem());
            menuItems.add(new ClearField(textArea));
            return menuItems;
        };
    }

    /**
     * The default context menu with a specific menu for normalizing person names regarding to BibTex rules.
     *
     * @param textArea text-area that this menu will be connected to
     * @return menu containing items of the default menu and an item for normalizing person names
     */
    public static Supplier<List<MenuItem>> getNameMenu(TextArea textArea) {
        return () -> {
            CustomMenuItem normalizeNames = new CustomMenuItem(new Label(Localization.lang("Normalize to BibTeX name format")));
            normalizeNames.setOnAction(event -> textArea.setText(new NormalizeNamesFormatter().format(textArea.getText())));
            Tooltip toolTip = new Tooltip(Localization.lang("If possible, normalize this list of names to conform to standard BibTeX name formatting"));
            Tooltip.install(normalizeNames.getContent(), toolTip);
            List<MenuItem> menuItems = new ArrayList<>(6);
            menuItems.add(normalizeNames);
            menuItems.addAll(getDefaultMenu(textArea).get());
            return menuItems;
        };
    }

    /**
     * The default context menu with a specific menu copying a DOI URL.
     *
     * @param textArea text-area that this menu will be connected to
     * @return menu containing items of the default menu and an item for copying a DOI URL
     */
    public static Supplier<List<MenuItem>> getDOIMenu(TextArea textArea) {
        return () -> {
            AbstractAction copyDoiUrlAction = new CopyDoiUrlAction(textArea);
            MenuItem copyDoiUrlMenuItem = new MenuItem((String) copyDoiUrlAction.getValue(Action.NAME));
            copyDoiUrlMenuItem.setOnAction(event -> copyDoiUrlAction.actionPerformed(null));

            List<MenuItem> menuItems = new ArrayList<>();
            menuItems.add(copyDoiUrlMenuItem);
            menuItems.add(new SeparatorMenuItem());
            menuItems.addAll(getDefaultMenu(textArea).get());
            return menuItems;
        };
    }
}
