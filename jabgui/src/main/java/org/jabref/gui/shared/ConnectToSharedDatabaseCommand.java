package org.jabref.gui.shared;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.actions.SimpleCommand;

/**
 * Opens a shared database.
 */
public class ConnectToSharedDatabaseCommand extends SimpleCommand {

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;

    public ConnectToSharedDatabaseCommand(LibraryTabContainer tabContainer, DialogService dialogService) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new SharedDatabaseLoginDialogView(tabContainer));
    }
}
