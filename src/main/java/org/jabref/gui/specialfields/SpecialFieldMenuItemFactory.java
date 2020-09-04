package org.jabref.gui.specialfields;

import java.util.function.Function;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;

import de.saxsys.mvvmfx.utils.commands.Command;

public class SpecialFieldMenuItemFactory {
    public static MenuItem getSpecialFieldSingleItem(SpecialField field, ActionFactory factory, JabRefFrame frame, DialogService dialogService, StateManager stateManager) {
        SpecialFieldValueViewModel specialField = new SpecialFieldValueViewModel(field.getValues().get(0));
        return factory.createMenuItem(specialField.getAction(),
                new SpecialFieldViewModel(field, Globals.undoManager).getSpecialFieldAction(field.getValues().get(0), frame, dialogService, stateManager));
    }

    public static Menu createSpecialFieldMenu(SpecialField field, ActionFactory factory, JabRefFrame frame, DialogService dialogService, StateManager stateManager) {
        return createSpecialFieldMenu(field, factory, Globals.undoManager, specialField ->
                new SpecialFieldViewModel(field, Globals.undoManager).getSpecialFieldAction(specialField.getValue(), frame, dialogService, stateManager));
    }

    public static Menu createSpecialFieldMenu(SpecialField field, ActionFactory factory, UndoManager undoManager, Function<SpecialFieldValueViewModel, Command> commandFactory) {
        SpecialFieldViewModel viewModel = new SpecialFieldViewModel(field, undoManager);
        Menu menu = factory.createMenu(viewModel.getAction());
        for (SpecialFieldValue Value : field.getValues()) {
            SpecialFieldValueViewModel valueViewModel = new SpecialFieldValueViewModel(Value);
            menu.getItems().add(factory.createMenuItem(valueViewModel.getAction(), commandFactory.apply(valueViewModel)));
        }
        return menu;
    }
}
