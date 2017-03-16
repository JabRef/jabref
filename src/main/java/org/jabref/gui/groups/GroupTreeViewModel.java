package org.jabref.gui.groups;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

public class GroupTreeViewModel extends AbstractViewModel {

    private final ObjectProperty<GroupNodeViewModel> rootGroup = new SimpleObjectProperty<>();
    private final ObjectProperty<GroupNodeViewModel> selectedGroup = new SimpleObjectProperty<>();
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final ObjectProperty<Predicate<GroupNodeViewModel>> filterPredicate = new SimpleObjectProperty<>();
    private final StringProperty filterText = new SimpleStringProperty();
    private Optional<BibDatabaseContext> currentDatabase;

    public GroupTreeViewModel(StateManager stateManager, DialogService dialogService) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.dialogService = Objects.requireNonNull(dialogService);

        // Register listener
        stateManager.activeDatabaseProperty().addListener((observable, oldValue, newValue) -> onActiveDatabaseChanged(newValue));
        selectedGroup.addListener((observable, oldValue, newValue) -> onSelectedGroupChanged(newValue));

        // Set-up bindings
        filterPredicate.bind(Bindings.createObjectBinding(() -> group -> group.isMatchedBy(filterText.get()), filterText));

        // Init
        onActiveDatabaseChanged(stateManager.activeDatabaseProperty().getValue());
    }

    public ObjectProperty<GroupNodeViewModel> rootGroupProperty() {
        return rootGroup;
    }

    public ObjectProperty<GroupNodeViewModel> selectedGroupProperty() {
        return selectedGroup;
    }

    public ObjectProperty<Predicate<GroupNodeViewModel>> filterPredicateProperty() {
        return filterPredicate;
    }

    public StringProperty filterTextProperty() {
        return filterText;
    }

    /**
     * Gets invoked if the user selects a different group.
     * We need to notify the {@link StateManager} about this change so that the main table gets updated.
     */
    private void onSelectedGroupChanged(GroupNodeViewModel newValue) {
        if (!currentDatabase.equals(stateManager.activeDatabaseProperty().getValue())) {
            // Switch of database occurred -> do nothing
            return;
        }

        currentDatabase.ifPresent(database -> {
            if (newValue == null) {
                stateManager.clearSelectedGroup(database);
            } else {
                stateManager.setSelectedGroup(database, newValue.getGroupNode());
            }
        });
    }

    /**
     * Opens "New Group Dialog" and add the resulting group to the root
     */
    public void addNewGroupToRoot() {
        addNewSubgroup(rootGroup.get());
    }

    /**
     * Gets invoked if the user changes the active database.
     * We need to get the new group tree and update the view
     */
    private void onActiveDatabaseChanged(Optional<BibDatabaseContext> newDatabase) {
        if (newDatabase.isPresent()) {
            GroupNodeViewModel newRoot = newDatabase
                    .map(BibDatabaseContext::getMetaData)
                    .flatMap(MetaData::getGroups)
                    .map(root -> new GroupNodeViewModel(newDatabase.get(), stateManager, root))
                    .orElse(GroupNodeViewModel.getAllEntriesGroup(newDatabase.get(), stateManager));

            rootGroup.setValue(newRoot);
            stateManager.getSelectedGroup(newDatabase.get()).ifPresent(
                    selectedGroup -> this.selectedGroup
                            .setValue(new GroupNodeViewModel(newDatabase.get(), stateManager, selectedGroup)));
        }

        currentDatabase = newDatabase;
    }

    /**
     * Opens "New Group Dialog" and add the resulting group to the specified group
     */
    public void addNewSubgroup(GroupNodeViewModel parent) {
        Optional<AbstractGroup> newGroup = dialogService.showCustomDialogAndWait(new GroupDialog());
        newGroup.ifPresent(group -> {
            GroupTreeNode newGroupNode = parent.addSubgroup(group);

            // TODO: Add undo
            //UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(parent, new GroupTreeNodeViewModel(newGroupNode), UndoableAddOrRemoveGroup.ADD_NODE);
            //panel.getUndoManager().addEdit(undo);

            // TODO: Expand parent to make new group visible
            //parent.expand();

            dialogService.notify(Localization.lang("Added group \"%0\".", group.getName()));
        });
    }

    /**
     * Removes the specified group and its subgroups (after asking for confirmation).
     */
    public void removeGroupAndSubgroups(GroupNodeViewModel group) {
        boolean confirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove group and subgroups"),
                Localization.lang("Remove group \"%0\" and its subgroups?", group.getDisplayName()),
                Localization.lang("Remove"));
        if (confirmed) {
            // TODO: Add undo
            //final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot, node, UndoableAddOrRemoveGroup.REMOVE_NODE_AND_CHILDREN);
            //panel.getUndoManager().addEdit(undo);

            group.getGroupNode().removeFromParent();

            dialogService.notify(Localization.lang("Removed group \"%0\" and its subgroups.", group.getDisplayName()));
        }
    }
}
