package org.jabref.gui.groups;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
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
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

public class GroupTreeViewModel extends AbstractViewModel {

    private final ObjectProperty<GroupNodeViewModel> rootGroup = new SimpleObjectProperty<>();
    private final ListProperty<GroupNodeViewModel> selectedGroups = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final ObjectProperty<Predicate<GroupNodeViewModel>> filterPredicate = new SimpleObjectProperty<>();
    private final StringProperty filterText = new SimpleStringProperty();
    private final Comparator<GroupTreeNode> compAlphabetIgnoreCase = (GroupTreeNode v1, GroupTreeNode v2) -> v1
            .getName()
            .compareToIgnoreCase(v2.getName());
    private Optional<BibDatabaseContext> currentDatabase;

    public GroupTreeViewModel(StateManager stateManager, DialogService dialogService, TaskExecutor taskExecutor) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.dialogService = Objects.requireNonNull(dialogService);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);

        // Register listener
        stateManager.activeDatabaseProperty()
                .addListener((observable, oldValue, newValue) -> onActiveDatabaseChanged(newValue));
        selectedGroups.addListener((observable, oldValue, newValue) -> onSelectedGroupChanged(newValue));

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
            if (newValue == null || newValue.isEmpty()) {
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
                    .map(root -> new GroupNodeViewModel(newDatabase.get(), stateManager, taskExecutor, root))
                    .orElse(GroupNodeViewModel.getAllEntriesGroup(newDatabase.get(), stateManager, taskExecutor));

            rootGroup.setValue(newRoot);
            this.selectedGroups.setAll(
                    stateManager.getSelectedGroup(newDatabase.get()).stream()
                            .map(selectedGroup -> new GroupNodeViewModel(newDatabase.get(), stateManager, taskExecutor, selectedGroup))
                            .collect(Collectors.toList()));
        }

        currentDatabase = newDatabase;
    }

    /**
     * Opens "New Group Dialog" and add the resulting group to the specified group
     */
    public void addNewSubgroup(GroupNodeViewModel parent) {
        SwingUtilities.invokeLater(() -> {
            Optional<AbstractGroup> newGroup = dialogService.showCustomDialogAndWait(new GroupDialog());
            newGroup.ifPresent(group -> {
                GroupTreeNode newGroupNode = parent.addSubgroup(group);

                // TODO: Add undo
                //UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(parent, new GroupTreeNodeViewModel(newGroupNode), UndoableAddOrRemoveGroup.ADD_NODE);
                //panel.getUndoManager().addEdit(undo);

                // TODO: Expand parent to make new group visible
                //parent.expand();

                dialogService.notify(Localization.lang("Added group \"%0\".", group.getName()));
                writeGroupChangesToMetaData();
            });
        });
    }

    private void writeGroupChangesToMetaData() {
        currentDatabase.get().getMetaData().setGroups(rootGroup.get().getGroupNode());
    }

    /**
     * Opens "Edit Group Dialog" and changes the given group to the edited one.
     */
    public void editGroup(GroupNodeViewModel oldGroup) {
        SwingUtilities.invokeLater(() -> {
            Optional<AbstractGroup> newGroup = dialogService
                    .showCustomDialogAndWait(new GroupDialog(oldGroup.getGroupNode().getGroup()));
            newGroup.ifPresent(group -> {

                Platform.runLater(() -> {
                    // TODO: Keep assignments
                    boolean keepPreviousAssignments = dialogService.showConfirmationDialogAndWait(
                            Localization.lang("Change of Grouping Method"),
                            Localization.lang("Assign the original group's entries to this group?"));
                    //        WarnAssignmentSideEffects.warnAssignmentSideEffects(newGroup, panel.frame());
                    boolean removePreviousAssignents = (oldGroup.getGroupNode().getGroup() instanceof ExplicitGroup)
                            && (group instanceof ExplicitGroup);

                    List<FieldChange> addChange = oldGroup.getGroupNode().setGroup(
                            group,
                            keepPreviousAssignments,
                            removePreviousAssignents,
                            stateManager.getEntriesInCurrentDatabase());

                    // TODO: Add undo
                    // Store undo information.
                    // AbstractUndoableEdit undoAddPreviousEntries = null;
                    // UndoableModifyGroup undo = new UndoableModifyGroup(GroupSelector.this, groupsRoot, node, newGroup);
                    // if (undoAddPreviousEntries == null) {
                    //    panel.getUndoManager().addEdit(undo);
                    //} else {
                    //    NamedCompound nc = new NamedCompound("Modify Group");
                    //    nc.addEdit(undo);
                    //    nc.addEdit(undoAddPreviousEntries);
                    //    nc.end();/
                    //      panel.getUndoManager().addEdit(nc);
                    //}
                    //if (!addChange.isEmpty()) {
                    //    undoAddPreviousEntries = UndoableChangeEntriesOfGroup.getUndoableEdit(null, addChange);
                    //}

                    dialogService.notify(Localization.lang("Modified group \"%0\".", group.getName()));
                    writeGroupChangesToMetaData();
                });
            });
        });
    }

    public void removeSubgroups(GroupNodeViewModel group) {
        boolean confirmation = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove subgroups"),
                Localization.lang("Remove all subgroups of \"%0\"?", group.getDisplayName()));
        if (confirmation) {
            /// TODO: Add undo
            //final UndoableModifySubtree undo = new UndoableModifySubtree(getGroupTreeRoot(), node, "Remove subgroups");
            //panel.getUndoManager().addEdit(undo);
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
            // TODO: Add undo
            //final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot, node, UndoableAddOrRemoveGroup.REMOVE_NODE_KEEP_CHILDREN);
            //panel.getUndoManager().addEdit(undo);
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
            // TODO: Add undo
            //final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot, node, UndoableAddOrRemoveGroup.REMOVE_NODE_AND_CHILDREN);
            //panel.getUndoManager().addEdit(undo);

            removeGroupsAndSubGroupsFromEntries(group);

            group.getGroupNode().removeFromParent();

            dialogService.notify(Localization.lang("Removed group \"%0\" and its subgroups.", group.getDisplayName()));
            writeGroupChangesToMetaData();
        }
    }

    void removeGroupsAndSubGroupsFromEntries(GroupNodeViewModel group) {
        for (GroupNodeViewModel child: group.getChildren()) {
            removeGroupsAndSubGroupsFromEntries(child);
        }

        // only remove explicit groups from the entries, keyword groups should not be deleted
        if (group.getGroupNode().getGroup() instanceof ExplicitGroup) {
            List<BibEntry> entriesInGroup = group.getGroupNode().getEntriesInGroup(this.currentDatabase.get().getEntries());
            group.getGroupNode().removeEntriesFromGroup(entriesInGroup);
        }
    }

    public void addSelectedEntries(GroupNodeViewModel group) {
        // TODO: Warn
        // if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(node.getNode().getGroup(), panel.frame())) {
        //    return; // user aborted operation

        List<FieldChange> addChange = group.getGroupNode().addEntriesToGroup(stateManager.getSelectedEntries());

        // TODO: Add undo
        // NamedCompound undoAll = new NamedCompound(Localization.lang("change assignment of entries"));
        // if (!undoAdd.isEmpty()) { undo.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(node, undoAdd)); }
        // panel.getUndoManager().addEdit(undoAll);

        // TODO Display massages
        //if (undo == null) {
        //    frame.output(Localization.lang("The group \"%0\" already contains the selection.",
        //            node.getGroup().getName()));
        //    return;
        //}
        // panel.getUndoManager().addEdit(undo);
        // final String groupName = node.getGroup().getName();
        // if (assignedEntries == 1) {
        //    frame.output(Localization.lang("Assigned 1 entry to group \"%0\".", groupName));
        // } else {
        //    frame.output(Localization.lang("Assigned %0 entries to group \"%1\".", String.valueOf(assignedEntries),
        //            groupName));
        //}
    }

    public void removeSelectedEntries(GroupNodeViewModel group) {
        // TODO: warn if assignment has undesired side effects (modifies a field != keywords)
        // if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(mNode.getNode().getGroup(), mPanel.frame())) {
        //    return; // user aborted operation

        List<FieldChange> removeChange = group.getGroupNode().removeEntriesFromGroup(stateManager.getSelectedEntries());

        // TODO: Add undo
        // if (!undo.isEmpty()) {
        //    mPanel.getUndoManager().addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(mNode, undo));
    }

    public void sortAlphabeticallyRecursive(GroupNodeViewModel group) {
        group.getGroupNode().sortChildren(compAlphabetIgnoreCase, true);

    }
}
