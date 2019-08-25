package org.jabref.gui.specialfields;

import java.util.function.Function;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.OldCommandWrapper;
import org.jabref.gui.actions.OldCommandWrapperForActiveDatabase;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;

import de.saxsys.mvvmfx.utils.commands.Command;

public class SpecialFieldMenuItemFactory {
    public static MenuItem getSpecialFieldSingleItem(SpecialField field, ActionFactory factory, BasePanel panel) {
        SpecialFieldValueViewModel specialField = new SpecialFieldValueViewModel(field.getValues().get(0));
        return factory.createMenuItem(specialField.getAction(), new OldCommandWrapper(specialField.getCommand(), panel));
    }

    public static MenuItem getSpecialFieldSingleItemForActiveDatabase(SpecialField field, ActionFactory factory) {
        SpecialFieldValueViewModel specialField = new SpecialFieldValueViewModel(field.getValues().get(0));
        return factory.createMenuItem(specialField.getAction(), new OldCommandWrapperForActiveDatabase(specialField.getCommand()));
    }

    public static Menu createSpecialFieldMenu(SpecialField field, ActionFactory factory, BasePanel panel) {
        return createSpecialFieldMenu(field, factory, panel.getUndoManager(), specialField -> new OldCommandWrapper(specialField.getCommand(), panel));
    }

    public static Menu createSpecialFieldMenuForActiveDatabase(SpecialField field, ActionFactory factory, UndoManager undoManager) {
        return createSpecialFieldMenu(field, factory, undoManager, specialField -> new OldCommandWrapperForActiveDatabase(specialField.getCommand()));
    }

    public static Menu createSpecialFieldMenu(SpecialField field, ActionFactory factory, UndoManager undoManager, Function<SpecialFieldValueViewModel, Command> commandFactory) {
        SpecialFieldViewModel viewModel = new SpecialFieldViewModel(field, undoManager);
        Menu menu = factory.createMenu(viewModel.getAction());
        for (SpecialFieldValue val : field.getValues()) {
            SpecialFieldValueViewModel specialField = new SpecialFieldValueViewModel(val);
            menu.getItems().add(factory.createMenuItem(specialField.getAction(), commandFactory.apply(specialField)));
        }
        return menu;
    }
}
