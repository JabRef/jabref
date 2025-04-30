package org.jabref.gui.libraryproperties.preamble;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.model.database.BibDatabaseContext;

public class PreamblePropertiesViewModel implements PropertiesTabViewModel {
    private final StringProperty preambleProperty = new SimpleStringProperty("");

    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;

    PreamblePropertiesViewModel(BibDatabaseContext databaseContext, UndoManager undoManager) {
        this.undoManager = undoManager;
        this.databaseContext = databaseContext;
    }

    @Override
    public void setValues() {
        preambleProperty.setValue(databaseContext.getDatabase().getPreamble().orElse(""));
    }

    @Override
    public void storeSettings() {
        String newPreamble = preambleProperty.getValue();
        if (!databaseContext.getDatabase().getPreamble().orElse("").equals(newPreamble)) {
            undoManager.addEdit(new UndoablePreambleChange(databaseContext.getDatabase(), databaseContext.getDatabase().getPreamble().orElse(null), newPreamble));
            databaseContext.getDatabase().setPreamble(newPreamble);
        }
    }

    public StringProperty preambleProperty() {
        return this.preambleProperty;
    }
}
