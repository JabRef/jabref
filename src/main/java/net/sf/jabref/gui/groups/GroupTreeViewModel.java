package net.sf.jabref.gui.groups;

import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import net.sf.jabref.gui.AbstractViewModel;
import net.sf.jabref.gui.DialogService;
import net.sf.jabref.gui.StateManager;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.metadata.MetaData;

public class GroupTreeViewModel extends AbstractViewModel {

    private final ObjectProperty<GroupNodeViewModel> rootGroup = new SimpleObjectProperty<>();
    private final ObjectProperty<GroupNodeViewModel> selectedGroup = new SimpleObjectProperty<>();
    private final StateManager stateManager;
    private final DialogService dialogService;
    private Optional<BibDatabaseContext> currentDatabase;

    public GroupTreeViewModel(StateManager stateManager, DialogService dialogService) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.dialogService = Objects.requireNonNull(dialogService);

        // Init
        onActiveDatabaseChanged(stateManager.activeDatabaseProperty().getValue());

        // Register listener
        stateManager.activeDatabaseProperty().addListener((observable, oldValue, newValue) -> onActiveDatabaseChanged(newValue));
        selectedGroup.addListener((observable, oldValue, newValue) -> onSelectedGroupChanged(newValue));
    }

    public ObjectProperty<GroupNodeViewModel> rootGroupProperty() {
        return rootGroup;
    }

    public ObjectProperty<GroupNodeViewModel> selectedGroupProperty() {
        return selectedGroup;
    }

    /**
     * Gets invoked if the user selects a different group.
     * We need to notify the {@link StateManager} about this change so that the main table gets updated.
     */
    private void onSelectedGroupChanged(GroupNodeViewModel newValue) {
        stateManager.activeGroupProperty().setValue(
                Optional.ofNullable(newValue).map(GroupNodeViewModel::getGroupNode));
    }

    /**
     * Gets invoked if the user changes the active database.
     * We need to get the new group tree and update the view
     */
    private void onActiveDatabaseChanged(Optional<BibDatabaseContext> newDatabase) {
        currentDatabase = newDatabase;

        if (newDatabase.isPresent()) {
            GroupNodeViewModel newRoot = newDatabase
                    .map(BibDatabaseContext::getMetaData)
                    .flatMap(MetaData::getGroups)
                    .map(root -> new GroupNodeViewModel(newDatabase.get(), stateManager, root))
                    .orElse(GroupNodeViewModel.getAllEntriesGroup(newDatabase.get(), stateManager));
            rootGroup.setValue(newRoot);
        }
    }

    /**
     * Opens "New Group Dialog" and add the resulting group to the root
     */
    public void addNewGroupToRoot() {
        addNewSubgroup(rootGroup.get());
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
}
