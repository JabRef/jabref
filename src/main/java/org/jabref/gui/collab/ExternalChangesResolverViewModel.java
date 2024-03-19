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
import org.jabref.gui.LibraryTab;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalChangesResolverViewModel extends AbstractViewModel {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExternalChangesResolverViewModel.class);

    private final NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));
    private final ObservableList<DatabaseChange> visibleChanges = FXCollections.observableArrayList();

    /**
     * Because visible changes list will be bound to the UI, certain changes can be removed. This list is used to keep
     * track of changes even when they're removed from the UI.
     */
    private final ObservableList<DatabaseChange> changes = FXCollections.observableArrayList();

    private final ObjectProperty<DatabaseChange> selectedChange = new SimpleObjectProperty<>();

    private final BooleanBinding areAllChangesResolved;

    private final BooleanBinding canAskUserToResolveChange;

    private final UndoManager undoManager;
    private final OptionalObjectProperty<LibraryTab> activeTab;

    public ExternalChangesResolverViewModel(List<DatabaseChange> externalChanges, UndoManager undoManager, OptionalObjectProperty<LibraryTab> activeTab) {
        Objects.requireNonNull(externalChanges);
        assert !externalChanges.isEmpty();

        this.visibleChanges.addAll(externalChanges);
        this.changes.addAll(externalChanges);
        this.undoManager = undoManager;
        this.activeTab = activeTab;

        areAllChangesResolved = Bindings.createBooleanBinding(visibleChanges::isEmpty, visibleChanges);
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

    public BooleanBinding canAskUserToResolveChangeProperty() {
        return canAskUserToResolveChange;
    }

    public void acceptChange() {
        getSelectedChange().ifPresent(selectedChange -> {
            selectedChange.accept();
            getVisibleChanges().remove(selectedChange);
        });
        if(activeTab!=null){
            activeTab.get().get().updateTabTitle(false);
            activeTab.get().get().resetChangedProperties();
        }
    }

    public void denyChange() {
        getSelectedChange().ifPresent(getVisibleChanges()::remove);
        if(activeTab!=null){
            activeTab.get().get().updateTabTitle(true);
            activeTab.get().get().markBaseChanged();
        }
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

    public void applyChanges() {
        changes.stream().filter(DatabaseChange::isAccepted).forEach(change -> change.applyChange(ce));
        ce.end();
        undoManager.addEdit(ce);
    }
}
