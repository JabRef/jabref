package org.jabref.migrations;

import javafx.beans.property.BooleanProperty;

import org.jabref.gui.StateManager;

import com.airhacks.afterburner.injection.Injector;

public class MSCMigration {
    
    private static StateManager stateManager;

    static {
        stateManager = Injector.instantiateModelOrService(StateManager.class);
    }

    public BooleanProperty isEditorOpen() {
        return stateManager.getEditorShowing();
    }
}
