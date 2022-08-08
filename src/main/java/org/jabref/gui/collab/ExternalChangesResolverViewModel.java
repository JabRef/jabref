package org.jabref.gui.collab;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.tobiasdiez.easybind.EasyBind;

public class ExternalChangesResolverViewModel extends AbstractViewModel {
    private final NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));

    private final BibDatabaseContext databaseContext;
    private final ObservableList<DatabaseChangeViewModel> changes = FXCollections.observableArrayList();

    /**
     * Because the other changes list will be bound to the UI, certain changes can be removed. This list is used to keep
     * track of changes even when they're removed from the UI.
     */
    private final ObservableList<DatabaseChangeViewModel> immutableChanges = FXCollections.observableArrayList();

    private final ObservableList<DatabaseChangeViewModel> selectedChanges = FXCollections.observableArrayList();

    private final ObjectProperty<DatabaseChangeViewModel> lastSelectedChange = new SimpleObjectProperty<>();

    private final BooleanBinding areAllChangesResolved;

    private final BooleanBinding canOpenAdvancedMergeDialog;

    public ExternalChangesResolverViewModel(List<DatabaseChangeViewModel> externalChanges, BibDatabaseContext databaseContext) {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(externalChanges);
        assert !externalChanges.isEmpty();

        this.changes.addAll(externalChanges);
        this.immutableChanges.addAll(externalChanges);
        this.databaseContext = databaseContext;

        areAllChangesResolved = Bindings.createBooleanBinding(changes::isEmpty, changes);
        canOpenAdvancedMergeDialog = Bindings.createBooleanBinding(() -> selectedChanges.size() == 1 && lastSelectedChange.get() != null && lastSelectedChange.get().hasAdvancedMergeDialog(), selectedChanges);

        EasyBind.subscribe(areAllChangesResolved, isResolved -> {
            if (isResolved) {
                Platform.runLater(this::makeChanges);
            }
        });
    }

    public ObservableList<DatabaseChangeViewModel> getChanges() {
        return changes;
    }

    public ObservableList<DatabaseChangeViewModel> getSelectedChanges() {
        return selectedChanges;
    }

    public ObjectProperty<DatabaseChangeViewModel> lastSelectedChangeProperty() {
        return lastSelectedChange;
    }

    public Optional<DatabaseChangeViewModel> getLastSelectedChange() {
        return Optional.ofNullable(lastSelectedChangeProperty().get());
    }

    public BooleanBinding areAllChangesResolvedProperty() {
        return areAllChangesResolved;
    }

    public boolean areAllChangesResolved() {
        return areAllChangesResolvedProperty().get();
    }

    public BooleanBinding canOpenAdvancedMergeDialogProperty() {
        return canOpenAdvancedMergeDialog;
    }

    public void acceptSelectedChanges() {
        getSelectedChanges().forEach(DatabaseChangeViewModel::accept);
        getChanges().removeAll(getSelectedChanges());
    }

    public void denySelectedChanges() {
        getChanges().removeAll(getSelectedChanges());
    }

    private void makeChanges() {
        immutableChanges.stream().filter(DatabaseChangeViewModel::isAccepted).forEach(change -> change.makeChange(databaseContext, ce));
        ce.end();
    }
}
