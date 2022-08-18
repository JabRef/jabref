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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalChangesResolverViewModel extends AbstractViewModel {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExternalChangesResolverViewModel.class);

    private final NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));

    private final BibDatabaseContext databaseContext;
    private final ObservableList<DatabaseChangeViewModel> visibleChanges = FXCollections.observableArrayList();

    /**
     * Because the other changes list will be bound to the UI, certain changes can be removed. This list is used to keep
     * track of changes even when they're removed from the UI.
     */
    private final ObservableList<DatabaseChangeViewModel> changes = FXCollections.observableArrayList();

    private final ObjectProperty<DatabaseChangeViewModel> selectedChange = new SimpleObjectProperty<>();

    private final BooleanBinding areAllChangesResolved;

    private final BooleanBinding canOpenAdvancedMergeDialog;

    public ExternalChangesResolverViewModel(List<DatabaseChangeViewModel> externalChanges, BibDatabaseContext databaseContext) {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(externalChanges);
        assert !externalChanges.isEmpty();

        this.visibleChanges.addAll(externalChanges);
        this.changes.addAll(externalChanges);
        this.databaseContext = databaseContext;

        areAllChangesResolved = Bindings.createBooleanBinding(visibleChanges::isEmpty, visibleChanges);
        canOpenAdvancedMergeDialog = Bindings.createBooleanBinding(() -> selectedChange.get() != null && selectedChange.get().hasAdvancedMergeDialog(), selectedChange);

        EasyBind.subscribe(areAllChangesResolved, isResolved -> {
            if (isResolved) {
                Platform.runLater(this::makeChanges);
            }
        });
    }

    public ObservableList<DatabaseChangeViewModel> getVisibleChanges() {
        return visibleChanges;
    }

    public ObjectProperty<DatabaseChangeViewModel> selectedChangeProperty() {
        return selectedChange;
    }

    public Optional<DatabaseChangeViewModel> getSelectedChange() {
        return Optional.ofNullable(selectedChangeProperty().get());
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

    public void acceptChange() {
        getSelectedChange().ifPresent(selectedChange -> {
            selectedChange.accept();
            getVisibleChanges().remove(selectedChange);
        });
    }

    public void denyChange() {
        getSelectedChange().ifPresent(getVisibleChanges()::remove);
    }

    public void acceptMergedChange(DatabaseChangeViewModel mergedChange) {
        Objects.requireNonNull(mergedChange);

        getSelectedChange().ifPresent(oldChange -> {
            changes.remove(oldChange);
            changes.add(mergedChange);
            getVisibleChanges().remove(oldChange);
            mergedChange.accept();
        });
    }

    private void makeChanges() {
        changes.stream().filter(DatabaseChangeViewModel::isAccepted).forEach(change -> change.makeChange(databaseContext, ce));
        ce.end();
    }
}
