package org.jabref.gui.groups;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.fxmisc.easybind.EasyBind;

public class GroupTreeViewModel extends AbstractViewModel {

    private final ObjectProperty<GroupNodeViewModel> rootGroup = new SimpleObjectProperty<>();
    private final ListProperty<GroupNodeViewModel> selectedGroups = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final CustomLocalDragboard localDragboard;
    private final ObjectProperty<Predicate<GroupNodeViewModel>> filterPredicate = new SimpleObjectProperty<>();
    private final StringProperty filterText = new SimpleStringProperty();
    private final Comparator<GroupTreeNode> compAlphabetIgnoreCase = (GroupTreeNode v1, GroupTreeNode v2) -> v1
            .getName()
            .compareToIgnoreCase(v2.getName());
    private Optional<BibDatabaseContext> currentDatabase;

    public GroupTreeViewModel(StateManager stateManager, DialogService dialogService, TaskExecutor taskExecutor, CustomLocalDragboard localDragboard) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.dialogService = Objects.requireNonNull(dialogService);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.localDragboard = Objects.requireNonNull(localDragboard);
        // Register listener
        EasyBind.subscribe(stateManager.activeDatabaseProperty(), this::onActiveDatabaseChanged);
        EasyBind.subscribe(selectedGroups, this::onSelectedGroupChanged);

        // Set-up bindings
        filterPredicate
                .bind(Bindings.createObjectBinding(() -> group -> group.isMatchedBy(filterText.get()), filterText));

        // Init
        onActiveDatabaseChanged(stateManager.activeDatabaseProperty().getValue());
    }

    public ObjectProperty<GroupNodeViewModel> rootGroupProperty() {
        return rootGroup;
    }

    public ListProperty<GroupNodeViewModel> selectedGroupsProperty() {
        return selectedGroups;
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
    private void onSelectedGroupChanged(ObservableList<GroupNodeViewModel> newValue) {
        if (!currentDatabase.equals(stateManager.activeDatabaseProperty().getValue())) {
            // Switch of database occurred -> do nothing
            return;
        }

        currentDatabase.ifPresent(database -> {
            if ((newValue == null) || newValue.isEmpty()) {
                stateManager.clearSelectedGroups(database);
            } else {
                stateManager.setSelectedGroups(database, newValue.stream().map(GroupNodeViewModel::getGroupNode).collect(Collectors.toList()));
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
                    .map(root -> new GroupNodeViewModel(newDatabase.get(), stateManager, taskExecutor, root, localDragboard))
                    .orElse(GroupNodeViewModel.getAllEntriesGroup(newDatabase.get(), stateManager, taskExecutor, localDragboard));

            rootGroup.setValue(newRoot);
            this.selectedGroups.setAll(
                    stateManager.getSelectedGroup(newDatabase.get()).stream()
                                .map(selectedGroup -> new GroupNodeViewModel(newDatabase.get(), stateManager, taskExecutor, selectedGroup, localDragboard))
                                .collect(Collectors.toList()));
        } else {
            rootGroup.setValue(GroupNodeViewModel.getAllEntriesGroup(new BibDatabaseContext(), stateManager, taskExecutor, localDragboard));
        }

        currentDatabase = newDatabase;
    }

    /**
     * Opens "New Group Dialog" and add the resulting group to the specified group
     */
    public void addNewSubgroup(GroupNodeViewModel parent) {
        Optional<AbstractGroup> newGroup = dialogService.showCustomDialogAndWait(new GroupDialog());
        newGroup.ifPresent(group -> {
            parent.addSubgroup(group);

            dialogService.notify(Localization.lang("Added group \"%0\".", group.getName()));
            writeGroupChangesToMetaData();
        });
    }

    private void writeGroupChangesToMetaData() {
        currentDatabase.get().getMetaData().setGroups(rootGroup.get().getGroupNode());
    }

    /**
     * Opens "Edit Group Dialog" and changes the given group to the edited one.
     */
    public void editGroup(GroupNodeViewModel oldGroup) {
        Optional<AbstractGroup> newGroup = dialogService
                .showCustomDialogAndWait(new GroupDialog(oldGroup.getGroupNode().getGroup()));
        newGroup.ifPresent(group -> {
            // TODO: Keep assignments
            boolean keepPreviousAssignments = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Change of Grouping Method"),
                    Localization.lang("Assign the original group's entries to this group?"));

            boolean removePreviousAssignents = (oldGroup.getGroupNode().getGroup() instanceof ExplicitGroup)
                    && (group instanceof ExplicitGroup);

            oldGroup.getGroupNode().setGroup(
                    group,
                    keepPreviousAssignments,
                    removePreviousAssignents,
                    stateManager.getEntriesInCurrentDatabase());

            dialogService.notify(Localization.lang("Modified group \"%0\".", group.getName()));
            writeGroupChangesToMetaData();
        });
    }

    public void removeSubgroups(GroupNodeViewModel group) {
        boolean confirmation = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove subgroups"),
                Localization.lang("Remove all subgroups of \"%0\"?", group.getDisplayName()));
        if (confirmation) {
            group.getGroupNode().removeAllChildren();
            dialogService.notify(Localization.lang("Removed all subgroups of group \"%0\".", group.getDisplayName()));
            writeGroupChangesToMetaData();
        }
    }

    public void removeGroupKeepSubgroups(GroupNodeViewModel group) {
        boolean confirmation = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove group"),
                Localization.lang("Remove group \"%0\"?", group.getDisplayName()));

        if (confirmation) {
            GroupTreeNode groupNode = group.getGroupNode();
            groupNode.getParent()
                     .ifPresent(parent -> groupNode.moveAllChildrenTo(parent, parent.getIndexOfChild(groupNode).get()));
            groupNode.removeFromParent();

            dialogService.notify(Localization.lang("Removed group \"%0\".", group.getDisplayName()));
            writeGroupChangesToMetaData();
        }
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
            removeGroupsAndSubGroupsFromEntries(group);
            group.getGroupNode().removeFromParent();

            dialogService.notify(Localization.lang("Removed group \"%0\" and its subgroups.", group.getDisplayName()));
            writeGroupChangesToMetaData();
        }
    }

    void removeGroupsAndSubGroupsFromEntries(GroupNodeViewModel group) {
        for (GroupNodeViewModel child : group.getChildren()) {
            removeGroupsAndSubGroupsFromEntries(child);
        }

        // only remove explicit groups from the entries, keyword groups should not be deleted
        if (group.getGroupNode().getGroup() instanceof ExplicitGroup) {
            List<BibEntry> entriesInGroup = group.getGroupNode().getEntriesInGroup(this.currentDatabase.get().getEntries());
            group.getGroupNode().removeEntriesFromGroup(entriesInGroup);
        }
    }

    public void addSelectedEntries(GroupNodeViewModel group) {
        group.getGroupNode().addEntriesToGroup(stateManager.getSelectedEntries());
    }

    public void removeSelectedEntries(GroupNodeViewModel group) {
        group.getGroupNode().removeEntriesFromGroup(stateManager.getSelectedEntries());
    }

    public void sortAlphabeticallyRecursive(GroupNodeViewModel group) {
        group.getGroupNode().sortChildren(compAlphabetIgnoreCase, true);
    }
}
