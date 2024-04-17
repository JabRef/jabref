package org.jabref.gui.collab;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalChangesResolverViewModel extends AbstractViewModel {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExternalChangesResolverViewModel.class);

    private final ObservableList<DatabaseChange> visibleChanges = FXCollections.observableArrayList();

    /**
     * Because visible changes list will be bound to the UI, certain changes can be removed. This list is used to keep
     * track of changes even when they're removed from the UI.
     */
    private final ObservableList<DatabaseChange> changes = FXCollections.observableArrayList();

    private final ObjectProperty<DatabaseChange> selectedChange = new SimpleObjectProperty<>();

    private final BooleanBinding areAllChangesResolved;

    private BooleanBinding areAllChangesAccepted;

    private BooleanBinding areAllChangesDenied;

    private final BooleanBinding canAskUserToResolveChange;

    private final UndoManager undoManager;

    public ExternalChangesResolverViewModel(List<DatabaseChange> externalChanges, UndoManager undoManager) {
        Objects.requireNonNull(externalChanges);
        assert !externalChanges.isEmpty();

        this.visibleChanges.addAll(externalChanges);
        this.changes.addAll(externalChanges);
        this.undoManager = undoManager;

        areAllChangesResolved = Bindings.createBooleanBinding(visibleChanges::isEmpty, visibleChanges);
        areAllChangesAccepted = Bindings.createBooleanBinding(() -> changes.stream().allMatch(DatabaseChange::isAccepted));
        areAllChangesDenied = Bindings.createBooleanBinding(() -> changes.stream().noneMatch(DatabaseChange::isAccepted));
        canAskUserToResolveChange = Bindings.createBooleanBinding(() -> selectedChange.isNotNull().get() && selectedChange.get().getExternalChangeResolver().isPresent(), selectedChange);
    }

    public ObservableList<DatabaseChange> getVisibleChanges() {
        return visibleChanges;
    }

    public ObjectProperty<DatabaseChange> selectedChangeProperty() {
        return selectedChange;
    }

    public Optional<DatabaseChange> getSelectedChange() {
        return Optional.ofNullable(selectedChangeProperty().get());
    }

    public BooleanBinding areAllChangesResolvedProperty() {
        return areAllChangesResolved;
    }

    public boolean areAllChangesResolved() {
        return areAllChangesResolvedProperty().get();
    }

    public BooleanBinding areAllChangesAcceptedProperty() {
        return areAllChangesAccepted;
    }

    public boolean areAllChangesAccepted() {
        return areAllChangesAcceptedProperty().get();
    }

    public BooleanBinding areAllChangesDeniedProperty() {
        return areAllChangesDenied;
    }

    public boolean areAllChangesDenied() {
        return areAllChangesDeniedProperty().get();
    }

    public BooleanBinding canAskUserToResolveChangeProperty() {
        return canAskUserToResolveChange;
    }

    public void acceptChange() {
        getSelectedChange().ifPresent(selectedChange -> {
            selectedChange.accept();
            getVisibleChanges().remove(selectedChange);
        });
    }

    public void denyChange() {
        getSelectedChange().ifPresent(getVisibleChanges()::remove);
    }

    public void acceptMergedChange(DatabaseChange databaseChange) {
        Objects.requireNonNull(databaseChange);

        getSelectedChange().ifPresent(oldChange -> {
            changes.remove(oldChange);
            changes.add(databaseChange);
            databaseChange.accept();
            getVisibleChanges().remove(oldChange);
        });
    }
}
