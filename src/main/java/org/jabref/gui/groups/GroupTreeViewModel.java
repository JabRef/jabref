package org.jabref.gui.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class GroupTreeViewModel extends AbstractViewModel {

    private final ObjectProperty<GroupNodeViewModel> rootGroup = new SimpleObjectProperty<>();
    private final ListProperty<GroupNodeViewModel> selectedGroups = new SimpleListProperty<>(FXCollections.observableArrayList());
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
    private Optional<BibDatabaseContext> currentDatabase;

    public GroupTreeViewModel(StateManager stateManager, DialogService dialogService, PreferencesService preferencesService, TaskExecutor taskExecutor, CustomLocalDragboard localDragboard) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.dialogService = Objects.requireNonNull(dialogService);
        this.preferences = Objects.requireNonNull(preferencesService);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.localDragboard = Objects.requireNonNull(localDragboard);

        // Register listener
        EasyBind.subscribe(stateManager.activeDatabaseProperty(), this::onActiveDatabaseChanged);
        EasyBind.subscribe(selectedGroups, this::onSelectedGroupChanged);

        // Set-up bindings
        filterPredicate.bind(EasyBind.map(filterText, text -> group -> group.isMatchedBy(text)));

        // Init
        refresh();
    }

    private void refresh() {
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
        if (currentDatabase.isPresent()) {
            addNewSubgroup(rootGroup.get(), GroupDialogHeader.GROUP);
        } else {
            dialogService.showWarningDialogAndWait(Localization.lang("Cannot create group"), Localization.lang("Cannot create group. Please create a library first."));
        }
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
                    .map(root -> new GroupNodeViewModel(newDatabase.get(), stateManager, taskExecutor, root, localDragboard, preferences))
                    .orElse(GroupNodeViewModel.getAllEntriesGroup(newDatabase.get(), stateManager, taskExecutor, localDragboard, preferences));

            rootGroup.setValue(newRoot);
            if (stateManager.getSelectedGroup(newDatabase.get()).isEmpty()) {
                stateManager.setSelectedGroups(newDatabase.get(), Collections.singletonList(newRoot.getGroupNode()));
            }
            selectedGroups.setAll(
                    stateManager.getSelectedGroup(newDatabase.get()).stream()
                                .map(selectedGroup -> new GroupNodeViewModel(newDatabase.get(), stateManager, taskExecutor, selectedGroup, localDragboard, preferences))
                                .collect(Collectors.toList()));
        } else {
            rootGroup.setValue(null);
        }
        currentDatabase = newDatabase;
    }

    /**
     * Opens "New Group Dialog" and adds the resulting group as subgroup to the specified group while maintaining the
     * alphabetical order
     */
    public void addNewSubgroup(GroupNodeViewModel parent, GroupDialogHeader groupDialogHeader) {
        currentDatabase.ifPresent(database -> {
            Optional<AbstractGroup> newGroup = dialogService.showCustomDialogAndWait(new GroupDialogView(
                    dialogService,
                    database,
                    preferences,
                    null,
                    groupDialogHeader));

            newGroup.ifPresent(group -> {
                parent.addSubgroup(group);

                // TODO: Add undo
                // UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(parent, new GroupTreeNodeViewModel(newGroupNode), UndoableAddOrRemoveGroup.ADD_NODE);
                // panel.getUndoManager().addEdit(undo);

                // TODO: Expand parent to make new group visible
                // parent.expand();

                sortAlphabeticallyRecursive(parent.getGroupNode());

                dialogService.notify(Localization.lang("Added group \"%0\".", group.getName()));
                writeGroupChangesToMetaData();
            });
        });
    }

    private void writeGroupChangesToMetaData() {
        currentDatabase.ifPresent(database -> database.getMetaData().setGroups(rootGroup.get().getGroupNode()));
    }

    private boolean isGroupTypeEqual(AbstractGroup oldGroup, AbstractGroup newGroup) {
        return oldGroup.getClass().equals(newGroup.getClass());
    }

    /**
     * Check if it is necessary to show a group modified, reassign entry dialog <br>
     * Group name change is handled separately
     *
     * @param oldGroup Original Group
     * @param newGroup Edited group
     * @return true if just trivial modifications (e.g. color or description) or the relevant group properties are equal, false otherwise
     */
    boolean onlyMinorChanges(AbstractGroup oldGroup, AbstractGroup newGroup) {
        // we need to use getclass here because we have different subclass inheritance e.g. ExplicitGroup is a subclass of WordKeyWordGroup
        if (oldGroup.getClass() == WordKeywordGroup.class) {
            WordKeywordGroup oldWordKeywordGroup = (WordKeywordGroup) oldGroup;
            WordKeywordGroup newWordKeywordGroup = (WordKeywordGroup) newGroup;

            return Objects.equals(oldWordKeywordGroup.getSearchField().getName(), newWordKeywordGroup.getSearchField().getName())
                   && Objects.equals(oldWordKeywordGroup.getSearchExpression(), newWordKeywordGroup.getSearchExpression())
                   && Objects.equals(oldWordKeywordGroup.isCaseSensitive(), newWordKeywordGroup.isCaseSensitive());
        } else if (oldGroup.getClass() == RegexKeywordGroup.class) {
            RegexKeywordGroup oldRegexKeywordGroup = (RegexKeywordGroup) oldGroup;
            RegexKeywordGroup newRegexKeywordGroup = (RegexKeywordGroup) newGroup;

            return Objects.equals(oldRegexKeywordGroup.getSearchField().getName(), newRegexKeywordGroup.getSearchField().getName())
                   && Objects.equals(oldRegexKeywordGroup.getSearchExpression(), newRegexKeywordGroup.getSearchExpression())
                   && Objects.equals(oldRegexKeywordGroup.isCaseSensitive(), newRegexKeywordGroup.isCaseSensitive());
        } else if ((oldGroup.getClass() == SearchGroup.class)) {
            SearchGroup oldSearchGroup = (SearchGroup) oldGroup;
            SearchGroup newSearchGroup = (SearchGroup) newGroup;

            return Objects.equals(oldSearchGroup.getSearchExpression(), newSearchGroup.getSearchExpression())
                   && Objects.equals(oldSearchGroup.getSearchFlags(), newSearchGroup.getSearchFlags());
        } else if (oldGroup.getClass() == AutomaticKeywordGroup.class) {
            AutomaticKeywordGroup oldAutomaticKeywordGroup = (AutomaticKeywordGroup) oldGroup;
            AutomaticKeywordGroup newAutomaticKeywordGroup = (AutomaticKeywordGroup) oldGroup;

            return Objects.equals(oldAutomaticKeywordGroup.getKeywordDelimiter(), newAutomaticKeywordGroup.getKeywordDelimiter())
                   && Objects.equals(oldAutomaticKeywordGroup.getKeywordHierarchicalDelimiter(), newAutomaticKeywordGroup.getKeywordHierarchicalDelimiter())
                   && Objects.equals(oldAutomaticKeywordGroup.getField().getName(), newAutomaticKeywordGroup.getField().getName());
        } else if (oldGroup.getClass() == AutomaticPersonsGroup.class) {
            AutomaticPersonsGroup oldAutomaticPersonsGroup = (AutomaticPersonsGroup) oldGroup;
            AutomaticPersonsGroup newAutomaticPersonsGroup = (AutomaticPersonsGroup) newGroup;

            return Objects.equals(oldAutomaticPersonsGroup.getField().getName(), newAutomaticPersonsGroup.getField().getName());
        } else if (oldGroup.getClass() == TexGroup.class) {
            TexGroup oldTexGroup = (TexGroup) oldGroup;
            TexGroup newTexGroup = (TexGroup) newGroup;
            return Objects.equals(oldTexGroup.getFilePath().toString(), newTexGroup.getFilePath().toString());
        }
        return true;
    }

    /**
     * Opens "Edit Group Dialog" and changes the given group to the edited one.
     */
    public void editGroup(GroupNodeViewModel oldGroup) {
        currentDatabase.ifPresent(database -> {
            Optional<AbstractGroup> newGroup = dialogService.showCustomDialogAndWait(new GroupDialogView(
                     dialogService,
                     database,
                     preferences,
                     oldGroup.getGroupNode().getGroup(),
                     GroupDialogHeader.SUBGROUP));
            newGroup.ifPresent(group -> {

                AbstractGroup oldGroupDef = oldGroup.getGroupNode().getGroup();
                String oldGroupName = oldGroupDef.getName();

                boolean groupTypeEqual = isGroupTypeEqual(oldGroupDef, group);
                boolean onlyMinorModifications = groupTypeEqual && onlyMinorChanges(oldGroupDef, group);

                // dialog already warns us about this if the new group is named like another existing group
                // We need to check if only the name changed as this is relevant for the entry's group field
                if (groupTypeEqual && !group.getName().equals(oldGroupName) && onlyMinorModifications) {
                    int groupsWithSameName = 0;
                    Optional<GroupTreeNode> databaseRootGroup = currentDatabase.get().getMetaData().getGroups();
                    if (databaseRootGroup.isPresent()) {
                        // we need to check the old name for duplicates. If the new group name occurs more than once, it won't matter
                        groupsWithSameName = databaseRootGroup.get().findChildrenSatisfying(g -> g.getName().equals(oldGroupName)).size();
                    }
                    boolean removePreviousAssignments = true;
                    // We found more than 2 groups, so we cannot simply remove old assignment
                    if (groupsWithSameName >= 2) {
                        removePreviousAssignments = false;
                    }

                    oldGroup.getGroupNode().setGroup(
                                                     group,
                                                     true,
                                                     removePreviousAssignments,
                                                     database.getEntries());

                    dialogService.notify(Localization.lang("Modified group \"%0\".", group.getName()));
                    writeGroupChangesToMetaData();
                    // This is ugly but we have no proper update mechanism in place to propagate the changes, so redraw everything
                    refresh();
                    return;
                }

                if (groupTypeEqual && onlyMinorChanges(oldGroup.getGroupNode().getGroup(), group)) {
                    oldGroup.getGroupNode().setGroup(
                                     group,
                                     true,
                                     true,
                                     database.getEntries());

                    writeGroupChangesToMetaData();
                    refresh();
                    return;
                }

                // Major modifications

                String content = Localization.lang("Assign the original group's entries to this group?");
                ButtonType keepAssignments = new ButtonType(Localization.lang("Assign"), ButtonBar.ButtonData.YES);
                ButtonType removeAssignments = new ButtonType(Localization.lang("Do not assign"), ButtonBar.ButtonData.NO);
                ButtonType cancel = new ButtonType(Localization.lang("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

                if (newGroup.get().getClass() == WordKeywordGroup.class) {
                    content = content + "\n\n" +
                              Localization.lang("(Note: If original entries lack keywords to qualify for the new group configuration, confirming here will add them)");
                }
                Optional<ButtonType> previousAssignments = dialogService.showCustomButtonDialogAndWait(Alert.AlertType.WARNING,
                                                                                                       Localization.lang("Change of Grouping Method"),
                                                                                                       content,
                                                                                                       keepAssignments,
                                                                                                       removeAssignments,
                                                                                                       cancel);
                boolean removePreviousAssignments = (oldGroup.getGroupNode().getGroup() instanceof ExplicitGroup)
                                                    && (group instanceof ExplicitGroup);

                int groupsWithSameName = 0;
                Optional<GroupTreeNode> databaseRootGroup = currentDatabase.get().getMetaData().getGroups();
                if (databaseRootGroup.isPresent()) {
                    String name = oldGroup.getGroupNode().getGroup().getName();
                    groupsWithSameName = databaseRootGroup.get().findChildrenSatisfying(g -> g.getName().equals(name)).size();
                }
                // okay we found more than 2 groups with the same name
                // If we only found one we can still do it
                if (groupsWithSameName >= 2) {
                    removePreviousAssignments = false;
                }

                if (previousAssignments.isPresent() && (previousAssignments.get().getButtonData() == ButtonBar.ButtonData.YES)) {
                    oldGroup.getGroupNode().setGroup(
                                 group,
                                 true,
                                 removePreviousAssignments,
                                 database.getEntries());
                } else if (previousAssignments.isPresent() && (previousAssignments.get().getButtonData() == ButtonBar.ButtonData.NO)) {
                    oldGroup.getGroupNode().setGroup(
                                 group,
                                 false,
                                 removePreviousAssignments,
                                 database.getEntries());
                } else if (previousAssignments.isPresent() && (previousAssignments.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE)) {
                    return;
                }

                // stateManager.getEntriesInCurrentDatabase());

                // TODO: Add undo
                // Store undo information.
                // AbstractUndoableEdit undoAddPreviousEntries = null;
                // UndoableModifyGroup undo = new UndoableModifyGroup(GroupSelector.this, groupsRoot, node, newGroup);
                // if (undoAddPreviousEntries == null) {
                //    panel.getUndoManager().addEdit(undo);
                // } else {
                //    NamedCompound nc = new NamedCompound("Modify Group");
                //    nc.addEdit(undo);
                //    nc.addEdit(undoAddPreviousEntries);
                //    nc.end();/
                //      panel.getUndoManager().addEdit(nc);
                // }
                // if (!addChange.isEmpty()) {
                //    undoAddPreviousEntries = UndoableChangeEntriesOfGroup.getUndoableEdit(null, addChange);
                // }

                oldGroup.getParent().ifPresent(parent -> {
                    sortAlphabeticallyRecursive(parent);
                });

                dialogService.notify(Localization.lang("Modified group \"%0\".", group.getName()));
                writeGroupChangesToMetaData();
                // This is ugly but we have no proper update mechanism in place to propagate the changes, so redraw everything
                refresh();
            });
        });
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
                removeGroupsAndSubGroupsFromEntries(child);
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

            List<GroupNodeViewModel> selectedGroupNodes = new ArrayList<>(selectedGroups);
            selectedGroupNodes.forEach(eachNode -> {
                GroupTreeNode groupNode = eachNode.getGroupNode();

                groupNode.getParent()
                         .ifPresent(parent -> groupNode.moveAllChildrenTo(parent, parent.getIndexOfChild(groupNode).get()));
                groupNode.removeFromParent();
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

            ArrayList<GroupNodeViewModel> selectedGroupNodes = new ArrayList<>(selectedGroups);
            selectedGroupNodes.forEach(eachNode -> {
                removeGroupsAndSubGroupsFromEntries(eachNode);
                eachNode.getGroupNode().removeFromParent();
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

            ArrayList<GroupNodeViewModel> selectedGroupNodes = new ArrayList<>(selectedGroups);
            selectedGroupNodes.forEach(eachNode -> {
                removeGroupsAndSubGroupsFromEntries(eachNode);
                eachNode.getGroupNode().removeFromParent();
            });

            if (selectedGroupNodes.size() > 1) {
                dialogService.notify(Localization.lang("Removed all selected groups."));
            } else {
                dialogService.notify(Localization.lang("Removed group \"%0\".", group.getDisplayName()));
            }
            writeGroupChangesToMetaData();
        }
    }

    void removeGroupsAndSubGroupsFromEntries(GroupNodeViewModel group) {
        for (GroupNodeViewModel child : group.getChildren()) {
            removeGroupsAndSubGroupsFromEntries(child);
        }

        // only remove explicit groups from the entries, keyword groups should not be deleted
        if (group.getGroupNode().getGroup() instanceof ExplicitGroup) {
            int groupsWithSameName = 0;
            String name = group.getGroupNode().getGroup().getName();
            Optional<GroupTreeNode> rootGroup = currentDatabase.get().getMetaData().getGroups();
            if (rootGroup.isPresent()) {
                groupsWithSameName = rootGroup.get().findChildrenSatisfying(g -> g.getName().equals(name)).size();
            }
            if (groupsWithSameName < 2) {
                List<BibEntry> entriesInGroup = group.getGroupNode().getEntriesInGroup(this.currentDatabase.get().getEntries());
                group.getGroupNode().removeEntriesFromGroup(entriesInGroup);
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
}
