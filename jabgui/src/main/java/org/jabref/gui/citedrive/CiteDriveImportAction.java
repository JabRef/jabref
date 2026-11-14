package org.jabref.gui.citedrive;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class CiteDriveImportAction extends SimpleCommand {
    private final DialogService dialogService;
    private final GuiPreferences preferences;

    public CiteDriveImportAction(DialogService dialogService, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    /// [impl->req~citedrive.import.open-export-page~1]
    @Override
    public void execute() {
        NativeDesktop.openBrowserShowPopup(
                preferences.getCiteDrivePreferences().getExportPage().toASCIIString(),
                dialogService,
                preferences.getExternalApplicationsPreferences());
    }
}
