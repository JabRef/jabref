package org.jabref.gui.git;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

public class ExternalChangesResolverViewModel {
    private StringProperty selectedChange;

    public ExternalChangesResolverViewModel(List<GitChange> changes, UndoManager undoManager) {
        this.selectedChange = new SimpleStringProperty();
    }

    public boolean areAllChangesResolved() {
        return false;
    }

    public ObservableList<GitChange> getVisibleChanges() {
        return null;
    }

    public BooleanExpression canAskUserToResolveChangeProperty() {
        return null;
    }

    public StringProperty selectedChangeProperty() {
        return this.selectedChange;
    }

    public void denyChange() {
    }

    public void acceptChange() {
    }

    public ObservableValue<String> getSelectedChange() {
        return this.selectedChange;
    }

    public ObservableValue areAllChangesResolvedProperty() {
        return null;
    }

    public void applyChanges() {
    }

    public void acceptMergedChange() {}
    
}
