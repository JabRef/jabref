package org.jabref.gui.consistency;

import java.util.List;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ConsistencyCheckAction extends SimpleCommand {

    Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final UiTaskExecutor taskExecutor;

    public ConsistencyCheckAction(Supplier<LibraryTab> tabSupplier,
                                  DialogService dialogService,
                                  StateManager stateManager,
                                  GuiPreferences preferences,
                                  BibEntryTypesManager entryTypesManager,
                                  UiTaskExecutor taskExecutor) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        BackgroundTask.wrap(() -> {
            BibDatabaseContext databaseContext = stateManager.getActiveDatabase()
                                                             .orElseThrow(() -> new NullPointerException("Database null"));
            List<BibEntry> entries = databaseContext.getDatabase().getEntries();

            BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
            return consistencyCheck.check(entries);
        }).onSuccess(result -> {
            if (result.entryTypeToResultMap().isEmpty()) {
                dialogService.notify(Localization.lang("No problems found."));
            } else {
                dialogService.showCustomDialogAndWait(
                        new ConsistencyCheckDialog(tabSupplier.get(), dialogService, preferences, entryTypesManager, result));
            }
        }).onFailure(exception ->
                dialogService.showErrorDialogAndWait(Localization.lang("Consistency check failed."), exception)
        ).executeWith(taskExecutor);
    }
}
