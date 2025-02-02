package org.jabref.gui.consistency;

import java.util.List;

import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ConsistencyCheckAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final UiTaskExecutor taskExecutor;

    public ConsistencyCheckAction(DialogService dialogService,
                                  StateManager stateManager,
                                  GuiPreferences preferences,
                                  BibEntryTypesManager entryTypesManager,
                                  UiTaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        Task<BibliographyConsistencyCheck.Result> task = new Task<>() {
            @Override
            protected BibliographyConsistencyCheck.Result call() {
             BibDatabaseContext databaseContext = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
             List<BibEntry> entries = databaseContext.getDatabase().getEntries();

             BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
             BibliographyConsistencyCheck.Result result = consistencyCheck.check(entries);

             return result;
            }
        };
        task.setOnSucceeded(value -> {
            BibliographyConsistencyCheck.Result result = task.getValue();
            if (result.entryTypeToResultMap().isEmpty()) {
                dialogService.notify(Localization.lang("No problems found."));
            } else {
                dialogService.showCustomDialogAndWait(new ConsistencyCheckDialog(dialogService, preferences, entryTypesManager, result));
            }
        });
        task.setOnFailed(event -> dialogService.showErrorDialogAndWait(Localization.lang("Consistency check failed."), task.getException()));

        dialogService.showProgressDialog(
                Localization.lang("Checking consistency..."),
                Localization.lang("Checking consistency..."),
                task);
        taskExecutor.execute(task);
    }
}
