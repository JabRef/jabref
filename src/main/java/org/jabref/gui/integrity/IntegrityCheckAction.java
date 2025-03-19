package org.jabref.gui.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class IntegrityCheckAction extends SimpleCommand {

    private final UiTaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final Supplier<LibraryTab> tabSupplier;
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final JournalAbbreviationRepository abbreviationRepository;

    public IntegrityCheckAction(Supplier<LibraryTab> tabSupplier,
                                GuiPreferences preferences,
                                DialogService dialogService,
                                StateManager stateManager,
                                UiTaskExecutor taskExecutor,
                                JournalAbbreviationRepository abbreviationRepository) {
        this.tabSupplier = tabSupplier;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.abbreviationRepository = abbreviationRepository;
        this.executable.bind(needsDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        IntegrityCheck check = new IntegrityCheck(database,
                preferences.getFilePreferences(),
                preferences.getCitationKeyPatternPreferences(),
                abbreviationRepository,
                preferences.getEntryEditorPreferences().shouldAllowIntegerEditionBibtex());

        Task<List<IntegrityMessage>> task = new Task<>() {
            @Override
            protected List<IntegrityMessage> call() {
                ObservableList<BibEntry> entries = database.getDatabase().getEntries();
                List<IntegrityMessage> result = new ArrayList<>(check.checkDatabase(database.getDatabase()));
                for (int i = 0; i < entries.size(); i++) {
                    if (isCancelled()) {
                        break;
                    }

                    BibEntry entry = entries.get(i);
                    result.addAll(check.checkEntry(entry));
                    updateProgress(i, entries.size());
                }

                return result;
            }
        };
        task.setOnSucceeded(value -> {
            List<IntegrityMessage> messages = task.getValue();
            if (messages.isEmpty()) {
                dialogService.notify(Localization.lang("No problems found."));
            } else {
                dialogService.showCustomDialogAndWait(new IntegrityCheckDialog(messages, tabSupplier.get()));
            }
        });
        task.setOnFailed(event -> dialogService.showErrorDialogAndWait("Integrity check failed.", task.getException()));

        dialogService.showProgressDialog(
                Localization.lang("Checking integrity..."),
                Localization.lang("Waiting for the check to finish..."),
                task);
        taskExecutor.execute(task);
    }
}
