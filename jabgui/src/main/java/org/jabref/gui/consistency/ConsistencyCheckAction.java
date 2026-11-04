package org.jabref.gui.consistency;

import java.util.Optional;
import java.util.function.Supplier;

import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ConsistencyCheckAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckAction.class);
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
        Optional<BibDatabaseContext> databaseContext = stateManager.getActiveDatabase();
        if (databaseContext.isEmpty()) {
            LOGGER.debug("Consistency check invoked with no library opened.");
            dialogService.notify(Localization.lang("No library open"));
            return;
        }
        BibDatabaseContext bibContext = databaseContext.orElseThrow();
        LibraryTab libraryTab = tabSupplier.get();
        Task<ConsistencyCheckDialogViewModel> task = new Task<>() {
            @Override
            public ConsistencyCheckDialogViewModel call() {
                BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
                BibliographyConsistencyCheck.Result result = consistencyCheck.check(bibContext, entryTypesManager, (count, total) ->
                        UiTaskExecutor.runInJavaFXThread(() -> {
                            updateProgress(count, total);
                            updateMessage(Localization.lang("%0/%1 entry types", count + 1, total));
                        }));
                // [impl->req~ux.consistency.report-background~1]
                return new ConsistencyCheckDialogViewModel(dialogService, preferences, entryTypesManager, bibContext.getMode(), result, taskExecutor);
            }
        };

        task.setOnFailed(_ -> dialogService.showErrorDialogAndWait(Localization.lang("Consistency check failed."), task.getException()));
        task.setOnSucceeded(_ -> {
            if (task.getValue().getTableData().isEmpty()) {
                dialogService.notify(Localization.lang("No problems found."));
            } else {
                dialogService.showCustomDialogAndWait(
                        new ConsistencyCheckDialog(libraryTab, dialogService, task.getValue()));
            }
        });
        taskExecutor.execute(task);

        dialogService.showProgressDialog(
                Localization.lang("Check consistency"),
                Localization.lang("Checking consistency..."),
                task);
    }
}
