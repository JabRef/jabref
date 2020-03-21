package org.jabref.gui.actions;

import java.util.Collections;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;

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
        BibEntry entry = stateManager.getSelectedEntries().get(0);
        return Bindings.createBooleanBinding(
                () -> entry.getFields().stream().anyMatch(fields::contains),
                entry.getFieldsObservable(),
                stateManager.getSelectedEntries());
    }
}
