package org.jabref.gui.collab;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

abstract class DatabaseChangeViewModel {
    private final StringProperty name = new SimpleStringProperty("");
    private BooleanProperty acceptedProperty = new SimpleBooleanProperty(true);

    DatabaseChangeViewModel(String name) {
        setName(name);
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isAccepted() {
        return acceptedProperty.getValue();
    }

    public BooleanProperty acceptedProperty() {
        return this.acceptedProperty;
    }

    public void setAccepted(boolean accepted) {
        this.acceptedProperty.setValue(accepted);
    }

    public ReadOnlyStringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return nameProperty().get();
    }

    private void setName(String str) {
        name.set(str);
    }

    /**
     * This method returns a {@link Node} detailing the nature and look of the change, e.g. how it is displayed
     *
     * @return Node the Node with the layout of the change
     */
    public abstract Node description();

    /**
     * Performs the change. This method is responsible for adding a proper undo edit to the NamedCompound, so the change can be undone.
     *
     * @param database the database that should be modified accordingly.
     * @param undoEdit NamedCompound The compound to hold the undo edits.
     */
    public abstract void makeChange(BibDatabaseContext database, NamedCompound undoEdit);

    public boolean hasAdvancedMergeDialog() {
        return false;
    }

    public Optional<DatabaseChangeViewModel> openAdvancedMergeDialog() {
        return Optional.empty();
    }
}
