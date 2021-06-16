package org.jabref.gui.libraryproperties;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class LibraryPropertiesAction extends SimpleCommand {

    private final JabRefFrame frame;

    public LibraryPropertiesAction(JabRefFrame frame, StateManager stateManager) {
        this.frame = frame;
        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new LibraryPropertiesDialogView(frame.getCurrentLibraryTab()));
    }
}
