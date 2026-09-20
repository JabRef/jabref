package org.jabref.gui.specialfields;

import java.util.function.Function;
import java.util.function.Supplier;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;

import de.saxsys.mvvmfx.utils.commands.Command;

public class SpecialFieldMenuItemFactory {
    public static MenuItem getSpecialFieldSingleItem(SpecialField field,
                                                     ActionFactory factory,
                                                     Supplier<LibraryTab> tabSupplier,
                                                     DialogService dialogService,
                                                     GuiPreferences preferences,
                                                     StateManager stateManager) {
        SpecialFieldValueViewModel specialField = new SpecialFieldValueViewModel(field.getValues().getFirst());
        MenuItem menuItem = factory.createMenuItem(specialField.getAction(),
                SpecialFieldViewModel.getSpecialFieldAction(field, field.getValues().getFirst(), tabSupplier, dialogService, preferences, stateManager));
        menuItem.visibleProperty().bind(preferences.getSpecialFieldsPreferences().specialFieldsEnabledProperty());
        return menuItem;
    }

    public static Menu createSpecialFieldMenu(SpecialField field,
                                              ActionFactory factory,
                                              Supplier<LibraryTab> tabSupplier,
                                              DialogService dialogService,
                                              GuiPreferences preferences,
                                              StateManager stateManager) {

        return createSpecialFieldMenu(field, factory, preferences, specialField ->
                SpecialFieldViewModel.getSpecialFieldAction(field, specialField.getValue(), tabSupplier, dialogService, preferences, stateManager));
    }

    public static Menu createSpecialFieldMenu(SpecialField field,
                                              ActionFactory factory,
                                              GuiPreferences preferences,
                                              Function<SpecialFieldValueViewModel, Command> commandFactory) {
        Menu menu = factory.createMenu(SpecialFieldViewModel.getAction(field));

        for (SpecialFieldValue Value : field.getValues()) {
            SpecialFieldValueViewModel valueViewModel = new SpecialFieldValueViewModel(Value);
            menu.getItems().add(factory.createMenuItem(valueViewModel.getAction(), commandFactory.apply(valueViewModel)));
        }

        menu.visibleProperty().bind(preferences.getSpecialFieldsPreferences().specialFieldsEnabledProperty());
        return menu;
    }
}
