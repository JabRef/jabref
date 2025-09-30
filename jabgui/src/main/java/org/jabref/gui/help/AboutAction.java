package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;

/**
 * Opens JabRef's “About” dialog from the GUI.
 * <p>
 * This action delegates dialog creation and display to {@link org.jabref.gui.DialogService}.
 * Keep this class UI-focused; no business logic belongs here.
 */

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
