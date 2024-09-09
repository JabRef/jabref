package org.jabref.gui.exporter;

import java.util.List;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;

public class SaveAllAction extends SimpleCommand {

    private final Supplier<List<LibraryTab>> tabsSupplier;
    private final DialogService dialogService;
    private final GuiPreferences preferences;

    public SaveAllAction(Supplier<List<LibraryTab>> tabsSupplier, GuiPreferences preferences, DialogService dialogService) {
        this.tabsSupplier = tabsSupplier;
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void execute() {
        dialogService.notify(Localization.lang("Saving all libraries..."));

        for (LibraryTab libraryTab : tabsSupplier.get()) {
            SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction(libraryTab, dialogService, preferences, Injector.instantiateModelOrService(BibEntryTypesManager.class));
            boolean saveResult = saveDatabaseAction.save();
            if (!saveResult) {
                dialogService.notify(Localization.lang("Could not save file."));
            }
        }

        dialogService.notify(Localization.lang("Save all finished."));
    }
}
