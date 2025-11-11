package org.jabref.gui.fieldeditors.contextmenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.CopyDoiUrlAction;
import org.jabref.logic.formatter.bibtexfields.CleanupUrlFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;

/**
 * Provides context menus for the text fields of the entry editor. Note that we use {@link Supplier} to prevent an early
 * instantiation of the menus. Therefore, they are attached to each text field but instantiation happens on the first
 * right-click of the user in that field. The late instantiation is done by {@link
 * org.jabref.gui.fieldeditors.EditorTextArea#initContextMenu(java.util.function.Supplier, org.jabref.gui.keyboard.KeyBindingRepository) EditorTextArea#initContextMenu}.
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
            MenuItem normalizeNames = new MenuItem(Localization.lang("Normalize to BibTeX name format"));
            EasyBind.subscribe(textInput.textProperty(), value -> normalizeNames.setDisable(StringUtil.isNullOrEmpty(value)));
            normalizeNames.setOnAction(event -> textInput.setText(new NormalizeNamesFormatter().format(textInput.getText())));
            List<MenuItem> menuItems = new ArrayList<>(6);
            menuItems.add(normalizeNames);
            menuItems.addAll(new DefaultMenu(textInput).get());
            return menuItems;
        };
    }

    /**
     * The default context menu with a specific menu copying a DOI/ DOI URL.
     *
     * @param textField text-field that this menu will be connected to
     * @return menu containing items of the default menu and an item for copying a DOI/DOI URL
     */
    public static Supplier<List<MenuItem>> getDOIMenu(TextField textField, DialogService dialogService) {
        return () -> {
            ActionFactory factory = new ActionFactory();
            ClipBoardManager clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
            MenuItem copyDoiMenuItem = factory.createMenuItem(StandardActions.COPY_DOI, new CopyDoiUrlAction(textField, StandardActions.COPY_DOI, dialogService, clipBoardManager));
            MenuItem copyDoiUrlMenuItem = factory.createMenuItem(StandardActions.COPY_DOI_URL, new CopyDoiUrlAction(textField, StandardActions.COPY_DOI_URL, dialogService, clipBoardManager));
            List<MenuItem> menuItems = new ArrayList<>();
            menuItems.add(copyDoiMenuItem);
            menuItems.add(copyDoiUrlMenuItem);
            menuItems.add(new SeparatorMenuItem());
            menuItems.addAll(new DefaultMenu(textField).get());
            return menuItems;
        };
    }

    /**
     * The default context menu with a specific menu item to cleanup URL.
     *
     * @param textField text field that this menu will be connected to
     * @return menu containing items of the default menu and an item to cleanup a URL
     */
    public static Supplier<List<MenuItem>> getCleanupUrlMenu(TextField textField) {
        return () -> {
            MenuItem cleanupURL = new MenuItem(Localization.lang("Cleanup URL link"));
            cleanupURL.setDisable(textField.textProperty().isEmpty().get());
            cleanupURL.setOnAction(event -> textField.setText(new CleanupUrlFormatter().format(textField.getText())));
            List<MenuItem> menuItems = new ArrayList<>();
            menuItems.add(cleanupURL);
            return menuItems;
        };
    }
}
