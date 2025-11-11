package org.jabref.gui.fieldeditors.contextmenu;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;

public class DefaultMenu implements Supplier<List<MenuItem>> {

    TextInputControl textInputControl;

    /**
     * The default menu that contains functions for changing the case of text and doing several conversions.
     *
     * @param textInputControl that this menu will be connected to
     */
    public DefaultMenu(TextInputControl textInputControl) {
        this.textInputControl = textInputControl;
    }

    public List<MenuItem> get() {
        return List.of(
                getCaseChangeMenu(textInputControl),
                getConversionMenu(textInputControl),
                new SeparatorMenuItem(),
                new ProtectedTermsMenu(textInputControl),
                new SeparatorMenuItem(),
                getClearFieldMenuItem(textInputControl));
    }

    private static Menu getCaseChangeMenu(TextInputControl textInputControl) {
        Objects.requireNonNull(textInputControl.textProperty());
        Menu submenu = new Menu(Localization.lang("Change case"));

        for (final Formatter caseChanger : Formatters.getCaseChangers()) {
            MenuItem menuItem = new MenuItem(caseChanger.getName());
            EasyBind.subscribe(textInputControl.textProperty(), value -> menuItem.setDisable(StringUtil.isNullOrEmpty(value)));
            menuItem.setOnAction(event ->
                    textInputControl.textProperty().set(caseChanger.format(textInputControl.textProperty().get())));
            submenu.getItems().add(menuItem);
        }

        return submenu;
    }

    private static Menu getConversionMenu(TextInputControl textInputControl) {
        Menu submenu = new Menu(Localization.lang("Convert"));

        for (Formatter converter : Formatters.getConverters()) {
            MenuItem menuItem = new MenuItem(converter.getName());
            EasyBind.subscribe(textInputControl.textProperty(), value -> menuItem.setDisable(StringUtil.isNullOrEmpty(value)));
            menuItem.setOnAction(event ->
                    textInputControl.textProperty().set(converter.format(textInputControl.textProperty().get())));
            submenu.getItems().add(menuItem);
        }

        return submenu;
    }

    // Icon: DELETE_SWEEP
    private static MenuItem getClearFieldMenuItem(TextInputControl textInputControl) {
        MenuItem menuItem = new MenuItem(Localization.lang("Clear"));
        menuItem.setOnAction(event -> textInputControl.setText(""));

        return menuItem;
    }
}
