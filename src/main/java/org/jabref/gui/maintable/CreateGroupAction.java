package org.jabref.gui.maintable;

import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.groups.GroupDialogHeader;
import org.jabref.gui.groups.GroupDialogView;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.*;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class CreateGroupAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final boolean preferSelectedEntries;
    private final PreferredGroupAdditionLocation preferredAdditionLocation;
    private final GroupTreeNode editGroup;

    public CreateGroupAction(
            DialogService dialogService,
            StateManager stateManager,
            boolean preferSelectedEntries,
            PreferredGroupAdditionLocation preferredAdditionLocation,
            @Nullable GroupTreeNode editGroup
    ) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferSelectedEntries = preferSelectedEntries;
        this.preferredAdditionLocation = preferredAdditionLocation;
        this.editGroup = editGroup;

        this.executable.bind(needsEntriesSelected(stateManager));
    }

    public CreateGroupAction(
        DialogService dialogService,
        StateManager stateManager
    ) {
        this(dialogService, stateManager, false, PreferredGroupAdditionLocation.ADD_BESIDE, null);
    }

    public void writeGroupChangesToMetaData(BibDatabaseContext database, GroupTreeNode newGroup) {
        database.getMetaData().setGroups(newGroup.getRoot());
    }

    @Override
    public void execute() {
        Optional<BibDatabaseContext> database = stateManager.getActiveDatabase();
        Optional<GroupTreeNode> active;

        if (!stateManager.activeGroupProperty().isEmpty())
            active = Optional.of(stateManager.activeGroupProperty().getFirst());
        else if (database.isPresent())
            active = database.get().getMetaData().getGroups();
        else
            active = Optional.empty();

        if (database.isPresent() && active.isPresent()) {
            boolean isRoot = active.get().isRoot();

            final GroupTreeNode groupFallsUnder;
            if (editGroup != null && editGroup.getParent().isPresent()) {
                groupFallsUnder = editGroup.getParent().get();
            } else if (editGroup != null) {
                groupFallsUnder = editGroup;
            } else if (preferredAdditionLocation == PreferredGroupAdditionLocation.ADD_TO_ROOT) {
                groupFallsUnder = active.get().getRoot();
            } else if (preferredAdditionLocation == PreferredGroupAdditionLocation.ADD_BESIDE
                    && !isRoot) {
                groupFallsUnder = active.get().getParent().get();
            } else {
                groupFallsUnder = active.get();
            }

            Optional<AbstractGroup> newGroup = dialogService.showCustomDialogAndWait(
                    new GroupDialogView(
                            database.get(),
                            groupFallsUnder,
                            editGroup != null ? editGroup.getGroup() : null,
                            isRoot ? GroupDialogHeader.GROUP : GroupDialogHeader.SUBGROUP,
                            stateManager.getSelectedEntries(),
                            preferSelectedEntries
                    ));

            newGroup.ifPresent(group -> {
                GroupTreeNode newSubgroup;
                if (editGroup != null) {
                    Optional<GroupTreeNode> editedGroup = groupEditActions(editGroup, group, database.get());
                    if (editedGroup.isEmpty()) return;
                    newSubgroup = editedGroup.get();
                    dialogService.notify(Localization.lang("Modified group \"%0\".", newSubgroup.getName()));
                } else {
                    newSubgroup = groupFallsUnder.addSubgroup(group);
                    dialogService.notify(Localization.lang("Added group \"%0\".", newSubgroup.getName()));
                }
                stateManager.getSelectedGroups(database.get()).setAll(newSubgroup);
                // TODO: Add undo
                // UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(parent, new GroupTreeNodeViewModel(newGroupNode), UndoableAddOrRemoveGroup.ADD_NODE);
                // panel.getUndoManager().addEdit(undo);
                writeGroupChangesToMetaData(database.get(), newSubgroup);
            });
        } else {
            dialogService.showWarningDialogAndWait(Localization.lang("Cannot create group"), Localization.lang("Cannot create group. Please create a library first."));
        }

    }

    private Optional<GroupTreeNode> groupEditActions(GroupTreeNode node, AbstractGroup newGroup, BibDatabaseContext database) {
        AbstractGroup oldGroup = node.getGroup();
        String oldGroupName = oldGroup.getName();

        boolean groupTypeEqual = isGroupTypeEqual(oldGroup, newGroup);
        boolean onlyMinorModifications = groupTypeEqual && changesAreMinor(oldGroup, newGroup);

        // I inherited this from the previous maintainer. Haven't dug into which has better priority,
        // what default behavior is, or why this appears replicated -- all my changes are simply refactoring.
        boolean keepPreviousAssignments;
        boolean removePreviousAssignments;
        GroupTreeNode finalResults;

        // dialog already warns us about this if the new group is named like another existing group
        // We need to check if only the name changed as this is relevant for the entry's group field
        if (groupTypeEqual && !newGroup.getName().equals(oldGroupName) && onlyMinorModifications) {

            keepPreviousAssignments = true;
            // If the name is unique, we want to remove the old database with the same name.
            // Otherwise, we need to keep them.
            removePreviousAssignments = nameIsUnique(oldGroup, newGroup, database);

        } else if (groupTypeEqual && changesAreMinor(oldGroup, newGroup)) {

            keepPreviousAssignments = true;
            removePreviousAssignments = true;

        } else {
            // Major modifications
            Optional<ButtonType> reassignmentResponse = showConfirmationPanel(newGroup.getClass() == WordKeywordGroup.class);
            if (reassignmentResponse.isEmpty() || reassignmentResponse.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return Optional.empty();
            }
            removePreviousAssignments = (node.getGroup() instanceof ExplicitGroup)
                    && (newGroup instanceof ExplicitGroup)
                    && nameIsUnique(oldGroup, newGroup, database);
            keepPreviousAssignments = (reassignmentResponse.get().getButtonData() == ButtonBar.ButtonData.YES);
        }


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

        node.setGroup(
                newGroup,
                keepPreviousAssignments,
                removePreviousAssignments,
                database.getEntries());
        return Optional.of(node);
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
    public static boolean changesAreMinor(AbstractGroup oldGroup, AbstractGroup newGroup) {
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
        } else if (oldGroup.getClass() == SearchGroup.class) {
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

    private boolean nameIsUnique(AbstractGroup oldGroup, AbstractGroup newGroup, BibDatabaseContext database) {
        int groupsWithSameName = 0;
        Optional<GroupTreeNode> databaseRootGroup = database.getMetaData().getGroups();
        if (databaseRootGroup.isPresent()) {
            // we need to check the old name for duplicates. If the new group name occurs more than once, it won't matter
            groupsWithSameName = databaseRootGroup.get().findChildrenSatisfying(g -> g.getName().equals(oldGroup.getName())).size();
        }
        return !(groupsWithSameName >= 2);
    }

    private Optional<ButtonType> showConfirmationPanel(boolean isNowWordKeywordGroup) {
        String content = Localization.lang("Assign the original group's entries to this group?");
        ButtonType keepAssignments = new ButtonType(Localization.lang("Assign"), ButtonBar.ButtonData.YES);
        ButtonType removeAssignments = new ButtonType(Localization.lang("Do not assign"), ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType(Localization.lang("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        if (isNowWordKeywordGroup) {
            content = content + "\n\n" +
                    Localization.lang("(Note: If original entries lack keywords to qualify for the new group configuration, confirming here will add them)");
        }
        return dialogService.showCustomButtonDialogAndWait(Alert.AlertType.WARNING,
                Localization.lang("Change of Grouping Method"),
                content,
                keepAssignments,
                removeAssignments,
                cancel);
    }
}
