package org.jabref.gui.edit;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

public class ReplaceStringAction extends SimpleCommand {
    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;

    public ReplaceStringAction(LibraryTabContainer tabContainer, StateManager stateManager, DialogService dialogService) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new ReplaceStringView(tabContainer.getCurrentLibraryTab()));
    }
}
