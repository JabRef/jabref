package org.jabref.gui.maintable;

import javafx.beans.binding.BooleanExpression;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.groups.GroupDialogView;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.actions.ActionHelper.isFieldSetForSelectedEntry;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class CreateGroupFromSelectionAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    public CreateGroupFromSelectionAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

        this.executable.bind(needsEntriesSelected(stateManager));

        this.setExecutable(stateManager.activeGroupProperty().size() == 1);
    }

    @Override
    public void execute() {
        GroupTreeNode active = stateManager.activeGroupProperty().getFirst();
        active.addSubgroup()
    }

}
