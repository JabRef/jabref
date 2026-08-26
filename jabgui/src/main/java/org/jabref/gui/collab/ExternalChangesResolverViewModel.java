package org.jabref.gui.collab;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.collab.entrychange.EntryChange;

import org.jspecify.annotations.NonNull;

public class ExternalChangesResolverViewModel extends AbstractViewModel {

    private final ObservableList<DatabaseChange> visibleChanges = FXCollections.observableArrayList();

    /// Because visible changes list will be bound to the UI, certain changes can be removed. This list is used to keep
    /// track of changes even when they're removed from the UI and to expose the final resolved change set.
    private final ObservableList<DatabaseChange> changes = FXCollections.observableArrayList();
    private final Map<DatabaseChange, Boolean> resolvedChangesMatchDisk = new IdentityHashMap<>();
    private final ObjectProperty<DatabaseChange> selectedChange = new SimpleObjectProperty<>();
    private final ReadOnlyBooleanWrapper areAllChangesResolved = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper areAllChangesAccepted = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper areAllChangesDenied = new ReadOnlyBooleanWrapper(false);
    private final BooleanExpression canAskUserToResolveChange;

    public ExternalChangesResolverViewModel(@NonNull List<DatabaseChange> externalChanges) {
        this.visibleChanges.addAll(externalChanges);
        this.changes.addAll(externalChanges);

        updateResolutionState();
        canAskUserToResolveChange = Bindings.createBooleanBinding(
                () -> getSelectedChange().flatMap(DatabaseChange::getExternalChangeResolver).isPresent(),
                selectedChange);
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

    public ReadOnlyBooleanProperty areAllChangesResolvedProperty() {
        return areAllChangesResolved.getReadOnlyProperty();
    }

    public boolean areAllChangesResolved() {
        return areAllChangesResolvedProperty().get();
    }

    public ReadOnlyBooleanProperty areAllChangesAcceptedProperty() {
        return areAllChangesAccepted.getReadOnlyProperty();
    }

    public boolean areAllChangesAccepted() {
        return areAllChangesAcceptedProperty().get();
    }

    public ReadOnlyBooleanProperty areAllChangesDeniedProperty() {
        return areAllChangesDenied.getReadOnlyProperty();
    }

    public boolean areAllChangesDenied() {
        return areAllChangesDeniedProperty().get();
    }

    public boolean resolvedChangesMatchDisk() {
        return !changes.isEmpty()
                && changes.stream().allMatch(DatabaseChange::isAccepted)
                && changes.stream().allMatch(change -> resolvedChangesMatchDisk.getOrDefault(change, false));
    }

    public BooleanExpression canAskUserToResolveChangeProperty() {
        return canAskUserToResolveChange;
    }

    public void acceptChange() {
        getSelectedChange().ifPresent(selectedChange -> {
            selectedChange.accept();
            resolvedChangesMatchDisk.put(selectedChange, true);
            getVisibleChanges().remove(selectedChange);
            updateResolutionState();
        });
    }

    public void denyChange() {
        getSelectedChange().ifPresent(selectedChange -> {
            resolvedChangesMatchDisk.put(selectedChange, false);
            getVisibleChanges().remove(selectedChange);
            updateResolutionState();
        });
    }

    public void acceptMergedChange(@NonNull DatabaseChange databaseChange) {
        getSelectedChange().ifPresent(oldChange -> {
            int oldChangeIndex = changes.indexOf(oldChange);
            if (oldChangeIndex >= 0) {
                changes.set(oldChangeIndex, databaseChange);
            } else {
                changes.add(databaseChange);
            }
            resolvedChangesMatchDisk.remove(oldChange);
            resolvedChangesMatchDisk.put(databaseChange, mergedChangeMatchesDiskVersion(oldChange, databaseChange));
            databaseChange.accept();
            getVisibleChanges().remove(oldChange);
            updateResolutionState();
        });
    }

    public List<DatabaseChange> getResolvedChanges() {
        return List.copyOf(changes);
    }

    private boolean mergedChangeMatchesDiskVersion(DatabaseChange oldChange, DatabaseChange mergedChange) {
        if (oldChange instanceof EntryChange oldEntryChange && mergedChange instanceof EntryChange mergedEntryChange) {
            return mergedEntryChange.getNewEntry().equals(oldEntryChange.getNewEntry());
        }

        return false;
    }

    private void updateResolutionState() {
        if (changes.isEmpty()) {
            areAllChangesResolved.set(false);
            areAllChangesAccepted.set(false);
            areAllChangesDenied.set(false);
            return;
        }

        areAllChangesResolved.set(visibleChanges.isEmpty());
        areAllChangesAccepted.set(changes.stream().allMatch(DatabaseChange::isAccepted));
        areAllChangesDenied.set(changes.stream().noneMatch(DatabaseChange::isAccepted));
    }
}
