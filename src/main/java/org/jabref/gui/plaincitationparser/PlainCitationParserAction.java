package org.jabref.gui.plaincitationparser;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;

public class PlainCitationParserAction extends SimpleCommand {
    private final DialogService dialogService;

    public PlainCitationParserAction(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new PlainCitationParserDialog());
    }
}
