package org.jabref.gui.specialfields;

import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

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
                                                     UndoManager undoManager,
                                                     StateManager stateManager) {
        SpecialFieldValueViewModel specialField = new SpecialFieldValueViewModel(field.getValues().getFirst());
        MenuItem menuItem = factory.createMenuItem(specialField.getAction(),
                new SpecialFieldViewModel(field, preferences, undoManager)
                        .getSpecialFieldAction(field.getValues().getFirst(), tabSupplier, dialogService, stateManager));
        menuItem.visibleProperty().bind(preferences.getSpecialFieldsPreferences().specialFieldsEnabledProperty());
        return menuItem;
    }

    public static Menu createSpecialFieldMenu(SpecialField field,
                                              ActionFactory factory,
                                              Supplier<LibraryTab> tabSupplier,
                                              DialogService dialogService,
                                              GuiPreferences preferences,
                                              UndoManager undoManager,
                                              StateManager stateManager) {

        return createSpecialFieldMenu(field, factory, preferences, undoManager, specialField ->
                new SpecialFieldViewModel(field, preferences, undoManager)
                        .getSpecialFieldAction(specialField.getValue(), tabSupplier, dialogService, stateManager));
    }

    public static Menu createSpecialFieldMenu(SpecialField field,
                                              ActionFactory factory,
                                              GuiPreferences preferences,
                                              UndoManager undoManager,
                                              Function<SpecialFieldValueViewModel, Command> commandFactory) {
        SpecialFieldViewModel viewModel = new SpecialFieldViewModel(field, preferences, undoManager);
        Menu menu = factory.createMenu(viewModel.getAction());

        for (SpecialFieldValue Value : field.getValues()) {
            SpecialFieldValueViewModel valueViewModel = new SpecialFieldValueViewModel(Value);
            menu.getItems().add(factory.createMenuItem(valueViewModel.getAction(), commandFactory.apply(valueViewModel)));
        }

        menu.visibleProperty().bind(preferences.getSpecialFieldsPreferences().specialFieldsEnabledProperty());
        return menu;
    }
}
