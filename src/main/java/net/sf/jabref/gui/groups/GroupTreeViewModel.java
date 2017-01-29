package net.sf.jabref.gui.groups;

import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import net.sf.jabref.gui.AbstractViewModel;
import net.sf.jabref.gui.StateManager;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.metadata.MetaData;

public class GroupTreeViewModel extends AbstractViewModel {

    private final ObjectProperty<GroupNodeViewModel> rootGroup = new SimpleObjectProperty<>();
    private final ObjectProperty<GroupNodeViewModel> selectedGroup = new SimpleObjectProperty<>();
    private final StateManager stateManager;
    private Optional<BibDatabaseContext> currentDatabase;

    public ObjectProperty<GroupNodeViewModel> rootGroupProperty() {
        return rootGroup;
    }

    public ObjectProperty<GroupNodeViewModel> selectedGroupProperty() {
        return selectedGroup;
    }

    public GroupTreeViewModel(StateManager stateManager) {
        this.stateManager = Objects.requireNonNull(stateManager);

        // Init
        onActiveDatabaseChanged(stateManager.activeDatabaseProperty().getValue());

        // Register listener
        stateManager.activeDatabaseProperty().addListener((observable, oldValue, newValue) -> onActiveDatabaseChanged(newValue));
        selectedGroup.addListener((observable, oldValue, newValue) -> onSelectedGroupChanged(newValue));
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
                    .map(root -> new GroupNodeViewModel(newDatabase.get(), root))
                    .orElse(GroupNodeViewModel.getAllEntriesGroup(newDatabase.get()));
            rootGroup.setValue(newRoot);
        }
    }
}
