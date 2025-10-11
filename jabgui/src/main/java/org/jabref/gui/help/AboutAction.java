package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;

public class AboutAction extends SimpleCommand {

    private final AboutDialogView aboutDialogView;
    private final DialogService dialogService;

    public AboutAction(final DialogService dialogService) {
        this.aboutDialogView = new AboutDialogView();
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        dialogService.showCustomDialog(aboutDialogView);
    }
}
