package org.jabref.gui.actions;

import java.util.Collections;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.collections.ObservableList;

import org.jabref.gui.StateManager;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class ActionHelper {
    public static BooleanExpression needsDatabase(StateManager stateManager) {
        return stateManager.activeDatabaseProperty().isPresent();
    }

    public static BooleanExpression needsEntriesSelected(StateManager stateManager) {
        return Bindings.isNotEmpty(stateManager.getSelectedEntries());
    }

    public static BooleanExpression needsEntriesSelected(int numberOfEntries, StateManager stateManager) {
        return Bindings.createBooleanBinding(
                () -> stateManager.getSelectedEntries().size() == numberOfEntries,
                stateManager.getSelectedEntries());
    }

    public static BooleanExpression isFieldSetForSelectedEntry(Field field, StateManager stateManager) {
        return isAnyFieldSetForSelectedEntry(Collections.singletonList(field), stateManager);
    }

    public static BooleanExpression isAnyFieldSetForSelectedEntry(List<Field> fields, StateManager stateManager) {
        ObservableList<BibEntry> selectedEntries = stateManager.getSelectedEntries();

        // binding should be recreated on every right click
        // not sure why selectedEntries might be empty, see https://github.com/JabRef/jabref/issues/6085
        if (selectedEntries.isEmpty()) {
            return Bindings.createBooleanBinding(() -> false, selectedEntries);
        }

        ObjectBinding<BibEntry> entry = Bindings.valueAt(selectedEntries, 0);
        return Bindings.createBooleanBinding(() -> {
            if (entry.get() == null)
                return false;
            else
               return entry.get().getFields().stream().anyMatch(fields::contains);
        }, entry, entry.getFieldsObserable());
        return Bindings.createBooleanBinding(
                () -> entry.getFields().stream().anyMatch(fields::contains),
                entry.getFieldsObservable(),
                selectedEntries);
    }
}
