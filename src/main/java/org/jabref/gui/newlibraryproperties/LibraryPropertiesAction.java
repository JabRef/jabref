package org.jabref.gui.newlibraryproperties;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class LibraryPropertiesAction extends SimpleCommand {
    private final LibraryTab libraryTab;

    public LibraryPropertiesAction(LibraryTab libraryTab, StateManager stateManager) {
        this.libraryTab = libraryTab;
        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new LibraryPropertiesView(libraryTab));
    }
}
