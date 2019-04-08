package org.jabref.gui.actions;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;

import org.jabref.gui.StateManager;

public class ActionHelper {
    public static BooleanExpression needsDatabase(StateManager stateManager) {
        return stateManager.activeDatabaseProperty().isPresent();
    }

    public static BooleanExpression needsEntriesSelected(StateManager stateManager) {
        return Bindings.isNotEmpty(stateManager.getSelectedEntries());
    }
}
