package org.jabref.gui.exporter;

import java.util.List;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

public class SaveAllAction extends SimpleCommand {

    private final Supplier<List<LibraryTab>> tabsSupplier;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    public SaveAllAction(Supplier<List<LibraryTab>> tabsSupplier, PreferencesService preferencesService, DialogService dialogService) {
        this.tabsSupplier = tabsSupplier;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
    }

    @Override
    public void execute() {
        dialogService.notify(Localization.lang("Saving all libraries..."));

        for (LibraryTab libraryTab : tabsSupplier.get()) {
            SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction(libraryTab, dialogService, preferencesService, Globals.entryTypesManager);
            boolean saveResult = saveDatabaseAction.save();
            if (!saveResult) {
                dialogService.notify(Localization.lang("Could not save file."));
            }
        }

        dialogService.notify(Localization.lang("Save all finished."));
    }
}
