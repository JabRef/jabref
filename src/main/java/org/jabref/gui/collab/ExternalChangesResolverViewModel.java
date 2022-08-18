package org.jabref.gui.collab;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.collab.experimental.ExternalChange;
import org.jabref.gui.collab.experimental.entrychange.EntryChange;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalChangesResolverViewModel extends AbstractViewModel {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExternalChangesResolverViewModel.class);

    private final NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));
    private final ObservableList<ExternalChange> visibleChanges = FXCollections.observableArrayList();

    /**
     * Because visible changes list will be bound to the UI, certain changes can be removed. This list is used to keep
     * track of changes even when they're removed from the UI.
     */
    private final ObservableList<ExternalChange> changes = FXCollections.observableArrayList();

    private final ObjectProperty<ExternalChange> selectedChange = new SimpleObjectProperty<>();

    private final BooleanBinding areAllChangesResolved;

    private final BooleanBinding canAskUserToResolveChange;

    public ExternalChangesResolverViewModel(List<ExternalChange> externalChanges) {
        Objects.requireNonNull(externalChanges);
        assert !externalChanges.isEmpty();

        this.visibleChanges.addAll(externalChanges);
        this.changes.addAll(externalChanges);

        areAllChangesResolved = Bindings.createBooleanBinding(visibleChanges::isEmpty, visibleChanges);
        canAskUserToResolveChange = Bindings.createBooleanBinding(() -> selectedChange.isNotNull().get() && selectedChange.get().getExternalChangeResolver().isPresent(), selectedChange);
    }

    public ObservableList<ExternalChange> getVisibleChanges() {
        return visibleChanges;
    }

    public ObjectProperty<ExternalChange> selectedChangeProperty() {
        return selectedChange;
    }

    public Optional<ExternalChange> getSelectedChange() {
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
    }

    public void denyChange() {
        getSelectedChange().ifPresent(getVisibleChanges()::remove);
    }

    public void acceptMergedChange(ExternalChange externalChange) {
        Objects.requireNonNull(externalChange);

        getSelectedChange().ifPresent(oldChange -> {
            changes.remove(oldChange);
            changes.add(externalChange);
            getVisibleChanges().remove(oldChange);
            externalChange.accept();
        });
    }

    public void applyChanges() {
        changes.stream().filter(ExternalChange::isAccepted).forEach(change -> change.applyChange(ce));
        ce.end();
    }
}
