package org.jabref.gui.auximport;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/**
 * The action concerned with generate a new (sub-)database from latex AUX file.
 *
 * A new library is created by {@link org.jabref.gui.importer.NewDatabaseAction}
 */
public class NewSubLibraryAction extends SimpleCommand {

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;

    public NewSubLibraryAction(LibraryTabContainer tabContainer, StateManager stateManager, DialogService dialogService) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new FromAuxDialog(tabContainer));
    }
}
