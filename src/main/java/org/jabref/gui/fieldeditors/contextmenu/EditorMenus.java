package org.jabref.gui.fieldeditors.contextmenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.CopyDoiUrlAction;
import org.jabref.logic.formatter.bibtexfields.CleanupUrlFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.l10n.Localization;

/**
 * Provides context menus for the text fields of the entry editor. Note that we use {@link Supplier} to prevent an early
 * instantiation of the menus. Therefore, they are attached to each text field but instantiation happens on the first
 * right-click of the user in that field. The late instantiation is done by {@link
 * org.jabref.gui.fieldeditors.EditorTextArea#initContextMenu(java.util.function.Supplier)}.
 */
public class EditorMenus {

    /**
     * The default context menu with a specific menu for normalizing person names regarding to BibTex rules.
     *
     * @param textInput text-input-control that this menu will be connected to
     * @return menu containing items of the default menu and an item for normalizing person names
     */
    public static Supplier<List<MenuItem>> getNameMenu(final TextInputControl textInput) {
        return () -> {
            CustomMenuItem normalizeNames = new CustomMenuItem(new Label(Localization.lang("Normalize to BibTeX name format")));
            normalizeNames.setOnAction(event -> textInput.setText(new NormalizeNamesFormatter().format(textInput.getText())));
            Tooltip toolTip = new Tooltip(Localization.lang("If possible, normalize this list of names to conform to standard BibTeX name formatting"));
            Tooltip.install(normalizeNames.getContent(), toolTip);
            List<MenuItem> menuItems = new ArrayList<>(6);
            menuItems.add(normalizeNames);
            menuItems.addAll(new DefaultMenu(textInput).get());
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
            ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());
            MenuItem copyDoiUrlMenuItem = factory.createMenuItem(StandardActions.COPY_DOI, new CopyDoiUrlAction(textArea));
            List<MenuItem> menuItems = new ArrayList<>();
            menuItems.add(copyDoiUrlMenuItem);
            menuItems.add(new SeparatorMenuItem());
            menuItems.addAll(new DefaultMenu(textArea).get());
            return menuItems;
        };
    }

    /**
     * The default context menu with a specific menu item to cleanup URL.
     *
     * @param textArea text-area that this menu will be connected to
     * @return menu containing items of the default menu and an item to cleanup a URL
     */
    public static Supplier<List<MenuItem>> getCleanupUrlMenu(TextArea textArea) {
        return () -> {
            CustomMenuItem cleanupURL = new CustomMenuItem(new Label(Localization.lang("Cleanup URL link")));
            cleanupURL.setDisable(textArea.textProperty().isEmpty().get());
            cleanupURL.setOnAction(event -> textArea.setText(new CleanupUrlFormatter().format(textArea.getText())));
            List<MenuItem> menuItems = new ArrayList<>();
            menuItems.add(cleanupURL);
            return menuItems;
        };
    }
}
