package org.jabref.gui.shared;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

/**
 * Opens a shared database.
 */
public class ConnectToSharedDatabaseCommand extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public ConnectToSharedDatabaseCommand(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new SharedDatabaseLoginDialogView(jabRefFrame));
    }
}
