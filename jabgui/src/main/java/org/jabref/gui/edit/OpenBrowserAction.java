package org.jabref.gui.edit;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;

public class OpenBrowserAction extends SimpleCommand {

    private final String urlToOpen;
    private final DialogService dialogService;
    private final NativeDesktop nativeDesktop;

    public OpenBrowserAction(String urlToOpen, DialogService dialogService, NativeDesktop nativeDesktop) {
        this.urlToOpen = urlToOpen;
        this.dialogService = dialogService;
        this.nativeDesktop = nativeDesktop;
    }

    @Override
    public void execute() {
        nativeDesktop.openBrowserShowPopup(urlToOpen, dialogService);
    }
}
