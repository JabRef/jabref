package org.jabref.gui.actions;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.libraryproperties.LibraryPropertiesDialogView;

public class LibraryPropertiesAction extends SimpleCommand {

    private final JabRefFrame frame;
    private final DialogService dialogService;

    public LibraryPropertiesAction(JabRefFrame frame, DialogService dialogService) {
        this.frame = frame;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        LibraryPropertiesDialogView propertiesDialog = new LibraryPropertiesDialogView(frame.getCurrentBasePanel(), dialogService);
        propertiesDialog.showAndWait();

    }

}
