package org.jabref.gui.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.maintable.CreateGroupAction;
import org.jabref.gui.maintable.PreferredGroupAdditionLocation;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class GroupTreeViewModel extends AbstractViewModel {

    private final ObjectProperty<GroupNodeViewModel> rootGroup = new SimpleObjectProperty<>();
    private final ListProperty<GroupTreeNode> selectedGroups = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final TaskExecutor taskExecutor;
    private final CustomLocalDragboard localDragboard;
    private final ObjectProperty<Predicate<GroupNodeViewModel>> filterPredicate = new SimpleObjectProperty<>();
    private final StringProperty filterText = new SimpleStringProperty();
    private final Comparator<GroupTreeNode> compAlphabetIgnoreCase = (GroupTreeNode v1, GroupTreeNode v2) -> v1
            .getName()
            .compareToIgnoreCase(v2.getName());
    private final Comparator<GroupTreeNode> compAlphabetIgnoreCaseReverse = (GroupTreeNode v1, GroupTreeNode v2) -> v2
            .getName()
            .compareToIgnoreCase(v1.getName());
    private final Comparator<GroupTreeNode> compEntries = (GroupTreeNode v1, GroupTreeNode v2) -> {
        if (this.currentDatabase.isPresent()) {
            int numChildren1 = v1.getEntriesInGroup(this.currentDatabase.get().getEntries()).size();
            int numChildren2 = v2.getEntriesInGroup(this.currentDatabase.get().getEntries()).size();
            return Integer.compare(numChildren2, numChildren1);
        }
            return 0;
    };
    private final Comparator<GroupTreeNode> compEntriesReverse = (GroupTreeNode v1, GroupTreeNode v2) -> {
        if (this.currentDatabase.isPresent()) {
            int numChildren1 = v1.getEntriesInGroup(this.currentDatabase.get().getEntries()).size();
            int numChildren2 = v2.getEntriesInGroup(this.currentDatabase.get().getEntries()).size();
            return Integer.compare(numChildren1, numChildren2);
        }
        return 0;
    };
    private Optional<BibDatabaseContext> currentDatabase = Optional.empty();

    public GroupTreeViewModel(StateManager stateManager,
                              DialogService dialogService,
                              PreferencesService preferencesService,
                              TaskExecutor taskExecutor,
                              CustomLocalDragboard localDragboard) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.dialogService = Objects.requireNonNull(dialogService);
        this.preferences = Objects.requireNonNull(preferencesService);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.localDragboard = Objects.requireNonNull(localDragboard);

        // Set-up bindings
        filterPredicate.bind(EasyBind.map(filterText, text -> group -> group.isMatchedBy(text)));

        ReadOnlyListProperty<GroupTreeNode> list = stateManager.activeGroupProperty();
        Optional<BibDatabaseContext> database = stateManager.getActiveDatabase();
        selectedGroups.setAll(list);
        list.addListener(this::onActiveGroupChange);

        // Init
        setNewDatabase(stateManager.getActiveDatabase());
        database.ifPresent(
            bibDatabaseContext ->
                stateManager.setSelectedGroups(
                    bibDatabaseContext,
                    Collections.singletonList(rootGroup.get().getGroupNode()
        )));
    }

    /**
     * Gets invoked if the user selects a different group.
     * We need to notify the {@link StateManager} about this change so that the main table gets updated.
     */
    private void onActiveGroupChange(ListChangeListener.Change<? extends GroupTreeNode> newValue) {
        if (!currentDatabase.equals(stateManager.activeDatabaseProperty().getValue())) {
            // Active database is not this one: do nothing
            return;
        }

        currentDatabase.ifPresent(database -> {
            if (newValue == null) {
                selectedGroups.clear();
            } else {
                selectedGroups.setAll(newValue.getList());
            }
        });
    }

    public void setNewDatabase(BibDatabaseContext database) {
        setNewDatabase(Optional.ofNullable(database));
    }

    public void setNewDatabase(Optional<BibDatabaseContext> database) {
        currentDatabase = database;
        database.ifPresentOrElse(
                this::onActiveDatabaseExists,
                this::onActiveDatabaseNull
        );
    }

    public GroupNodeViewModel rootGroup() {
        return rootGroup.get();
    }

    public ListProperty<GroupTreeNode> selectedGroupsProperty() {
        return selectedGroups;
    }

    public ObjectProperty<Predicate<GroupNodeViewModel>> filterPredicateProperty() {
        return filterPredicate;
    }

    public StringProperty filterTextProperty() {
        return filterText;
    }

    /**
     * Opens "New Group Dialog" and add the resulting group to the root
     */
    public void addNewGroupToRoot(boolean preferSelectedEntries) {
        CreateGroupAction groupMaker = new CreateGroupAction(
                dialogService,
                stateManager,
                preferSelectedEntries,
                PreferredGroupAdditionLocation.ADD_TO_ROOT,
                null
        );
        groupMaker.execute();
    }

    /**
     * Gets invoked if the user changes the active database.
     * We need to get the new group tree
     */
    private void onActiveDatabaseExists(BibDatabaseContext newDatabase) {
        GroupNodeViewModel newRoot = newDatabase.getMetaData().getGroups()
                .map(root -> new GroupNodeViewModel(newDatabase, stateManager, taskExecutor, root, localDragboard, preferences))
                .orElse(GroupNodeViewModel.getAllEntriesGroup(newDatabase, stateManager, taskExecutor, localDragboard, preferences));

        rootGroup.setValue(newRoot);
    }

    private void onActiveDatabaseNull() {
        rootGroup.setValue(null);
    }

    public void addNewSubgroup(GroupNodeViewModel parent, GroupDialogHeader groupDialogHeader) {
        addNewSubgroup(parent, groupDialogHeader, false);
    }

    /**
     * Opens "New Group Dialog" and adds the resulting group as subgroup to the specified group
     */
    public void addNewSubgroup(GroupNodeViewModel parent, GroupDialogHeader groupDialogHeader, boolean preferUseSelection) {
        CreateGroupAction groupMaker = new CreateGroupAction(dialogService, stateManager);
        groupMaker.execute();
    }

    public void writeGroupChangesToMetaData() {
        currentDatabase.ifPresent(database -> database.getMetaData().setGroups(rootGroup.get().getGroupNode()));
    }

    /**
     * Opens "Edit Group Dialog" and changes the given group to the edited one.
     */
    public void editGroup(GroupTreeNode oldGroup) {
        CreateGroupAction groupMaker = new CreateGroupAction(dialogService, stateManager, false, PreferredGroupAdditionLocation.ADD_BESIDE, oldGroup);
        groupMaker.execute();
    }

    public void removeSubgroups(GroupNodeViewModel group) {
        boolean confirmation = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove subgroups"),
                Localization.lang("Remove all subgroups of \"%0\"?", group.getDisplayName()));
        if (confirmation) {
            /// TODO: Add undo
            // final UndoableModifySubtree undo = new UndoableModifySubtree(getGroupTreeRoot(), node, "Remove subgroups");
            // panel.getUndoManager().addEdit(undo);
            for (GroupNodeViewModel child : group.getChildren()) {
                removeGroupsAndSubGroupsFromEntries(child.getGroupNode());
            }
            group.getGroupNode().removeAllChildren();
            dialogService.notify(Localization.lang("Removed all subgroups of group \"%0\".", group.getDisplayName()));
            writeGroupChangesToMetaData();
        }
    }

    public void removeGroupKeepSubgroups(GroupNodeViewModel group) {
        boolean confirmed;
        if (selectedGroups.size() <= 1) {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove group"),
                    Localization.lang("Remove group \"%0\" and keep its subgroups?", group.getDisplayName()),
                    Localization.lang("Remove"));
        } else {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove groups"),
                    Localization.lang("Remove all selected groups and keep their subgroups?"),
                    Localization.lang("Remove all"));
        }

        if (confirmed) {
            // TODO: Add undo
            // final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot, node, UndoableAddOrRemoveGroup.REMOVE_NODE_KEEP_CHILDREN);
            // panel.getUndoManager().addEdit(undo);

            List<GroupTreeNode> selectedGroupNodes = new ArrayList<>(selectedGroups);
            selectedGroupNodes.forEach(eachNode -> {
                eachNode.getParent()
                         .ifPresent(parent -> eachNode.moveAllChildrenTo(parent, parent.getIndexOfChild(eachNode).get()));
                eachNode.removeFromParent();
            });

            if (selectedGroupNodes.size() > 1) {
                dialogService.notify(Localization.lang("Removed all selected groups."));
            } else {
                dialogService.notify(Localization.lang("Removed group \"%0\".", group.getDisplayName()));
            }
            writeGroupChangesToMetaData();
        }
    }

    /**
     * Removes the specified group and its subgroups (after asking for confirmation).
     */
    public void removeGroupAndSubgroups(GroupNodeViewModel group) {
        boolean confirmed;
        if (selectedGroups.size() <= 1) {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove group and subgroups"),
                    Localization.lang("Remove group \"%0\" and its subgroups?", group.getDisplayName()),
                    Localization.lang("Remove"));
        } else {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove groups and subgroups"),
                    Localization.lang("Remove all selected groups and their subgroups?"),
                    Localization.lang("Remove all"));
        }

        if (confirmed) {
            // TODO: Add undo
            // final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot, node, UndoableAddOrRemoveGroup.REMOVE_NODE_AND_CHILDREN);
            // panel.getUndoManager().addEdit(undo);

            ArrayList<GroupTreeNode> selectedGroupNodes = new ArrayList<>(selectedGroups);
            selectedGroupNodes.forEach(eachNode -> {
                removeGroupsAndSubGroupsFromEntries(eachNode);
                eachNode.removeFromParent();
            });

            if (selectedGroupNodes.size() > 1) {
                dialogService.notify(Localization.lang("Removed all selected groups and their subgroups."));
            } else {
                dialogService.notify(Localization.lang("Removed group \"%0\" and its subgroups.", group.getDisplayName()));
            }
            writeGroupChangesToMetaData();
        }
    }

    /**
     * Removes the specified group (after asking for confirmation).
     */
    public void removeGroupNoSubgroups(GroupNodeViewModel group) {
        boolean confirmed;
        if (selectedGroups.size() <= 1) {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove group"),
                    Localization.lang("Remove group \"%0\"?", group.getDisplayName()),
                    Localization.lang("Remove"));
        } else {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove groups and subgroups"),
                    Localization.lang("Remove all selected groups and their subgroups?"),
                    Localization.lang("Remove all"));
        }

        if (confirmed) {
            // TODO: Add undo
            // final UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(groupsRoot, node, UndoableAddOrRemoveGroup.REMOVE_NODE_WITHOUT_CHILDREN);
            // panel.getUndoManager().addEdit(undo);

            ArrayList<GroupTreeNode> selectedGroupNodes = new ArrayList<>(selectedGroups);
            selectedGroupNodes.forEach(eachNode -> {
                removeGroupsAndSubGroupsFromEntries(eachNode);
                eachNode.removeFromParent();
            });

            if (selectedGroupNodes.size() > 1) {
                dialogService.notify(Localization.lang("Removed all selected groups."));
            } else {
                dialogService.notify(Localization.lang("Removed group \"%0\".", group.getDisplayName()));
            }
            writeGroupChangesToMetaData();
        }
    }

    void removeGroupsAndSubGroupsFromEntries(GroupTreeNode group) {
        for (GroupTreeNode child : group.getChildren()) {
            removeGroupsAndSubGroupsFromEntries(child);
        }

        // only remove explicit groups from the entries, keyword groups should not be deleted
        if (currentDatabase.isPresent() && group.getGroup() instanceof ExplicitGroup) {
            int groupsWithSameName = 0;
            String name = group.getGroup().getName();
            Optional<GroupTreeNode> rootGroup = currentDatabase.get().getMetaData().getGroups();
            if (rootGroup.isPresent()) {
                groupsWithSameName = rootGroup.get().findChildrenSatisfying(g -> g.getName().equals(name)).size();
            }
            if (groupsWithSameName < 2) {
                List<BibEntry> entriesInGroup = group.getEntriesInGroup(this.currentDatabase.get().getEntries());
                group.removeEntriesFromGroup(entriesInGroup);
            }
        }
    }

    public void addSelectedEntries(GroupNodeViewModel group) {
        // TODO: Warn
        // if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(node.getNode().getGroup(), panel.frame())) {
        //    return; // user aborted operation

        group.getGroupNode().addEntriesToGroup(stateManager.getSelectedEntries());

        // TODO: Add undo
        // NamedCompound undoAll = new NamedCompound(Localization.lang("change assignment of entries"));
        // if (!undoAdd.isEmpty()) { undo.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(node, undoAdd)); }
        // panel.getUndoManager().addEdit(undoAll);

        // TODO Display massages
        // if (undo == null) {
        //    frame.output(Localization.lang("The group \"%0\" already contains the selection.",
        //            node.getGroup().getName()));
        //    return;
        // }
        // panel.getUndoManager().addEdit(undo);
        // final String groupName = node.getGroup().getName();
        // if (assignedEntries == 1) {
        //    frame.output(Localization.lang("Assigned 1 entry to group \"%0\".", groupName));
        // } else {
        //    frame.output(Localization.lang("Assigned %0 entries to group \"%1\".", String.valueOf(assignedEntries),
        //            groupName));
        // }
    }

    public void removeSelectedEntries(GroupNodeViewModel group) {
        // TODO: warn if assignment has undesired side effects (modifies a field != keywords)
        // if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(mNode.getNode().getGroup(), mPanel.frame())) {
        //    return; // user aborted operation

        group.getGroupNode().removeEntriesFromGroup(stateManager.getSelectedEntries());

        // TODO: Add undo
        // if (!undo.isEmpty()) {
        //    mPanel.getUndoManager().addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(mNode, undo));
    }

    public void sortAlphabeticallyRecursive(GroupTreeNode group) {
        group.sortChildren(compAlphabetIgnoreCase, true);
    }

    public void sortReverseAlphabeticallyRecursive(GroupTreeNode group) {
        group.sortChildren(compAlphabetIgnoreCaseReverse, true);
    }

    public void sortEntriesRecursive(GroupTreeNode group) {
        group.sortChildren(compEntries, true);
    }

    public void sortReverseEntriesRecursive(GroupTreeNode group) {
        group.sortChildren(compEntriesReverse, true);
    }
}
