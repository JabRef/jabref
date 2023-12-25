package org.jabref.gui.exporter;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

public class SaveAllAction extends SimpleCommand {

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    public SaveAllAction(LibraryTabContainer tabContainer, PreferencesService preferencesService, DialogService dialogService) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
    }

    @Override
    public void execute() {
        dialogService.notify(Localization.lang("Saving all libraries..."));

        for (LibraryTab libraryTab : tabContainer.getLibraryTabs()) {
            SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction(libraryTab, dialogService, preferencesService, Globals.entryTypesManager);
            boolean saveResult = saveDatabaseAction.save();
            if (!saveResult) {
                dialogService.notify(Localization.lang("Could not save file."));
            }
        }

        dialogService.notify(Localization.lang("Save all finished."));
    }
}
