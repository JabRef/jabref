package org.jabref.gui.contentselector;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ManageContentSelectorAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public ManageContentSelectorAction(JabRefFrame jabRefFrame, StateManager stateManager) {
        this.jabRefFrame = jabRefFrame;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        LibraryTab libraryTab = jabRefFrame.getCurrentLibraryTab();
        dialogService.showCustomDialogAndWait(new ContentSelectorDialogView(libraryTab));
    }
}
