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
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.journals.predatory.PredatoryJournalRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class IntegrityCheckAction extends SimpleCommand {

    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final Supplier<LibraryTab> tabSupplier;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final PredatoryJournalRepository predatoryJournalRepository;

    public IntegrityCheckAction(Supplier<LibraryTab> tabSupplier,
                                PreferencesService preferencesService,
                                DialogService dialogService,
                                StateManager stateManager,
                                TaskExecutor taskExecutor,
                                JournalAbbreviationRepository abbreviationRepository,
                                PredatoryJournalRepository predatoryJournalRepository) {
        this.tabSupplier = tabSupplier;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.abbreviationRepository = abbreviationRepository;
        this.predatoryJournalRepository = predatoryJournalRepository;

        this.executable.bind(needsDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        IntegrityCheck check = new IntegrityCheck(database,
                preferencesService.getFilePreferences(),
                preferencesService.getCitationKeyPatternPreferences(),
                abbreviationRepository,
                predatoryJournalRepository,
                preferencesService.getEntryEditorPreferences().shouldAllowIntegerEditionBibtex());

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
                Localization.lang("Checking integrity..."),
                task);
        taskExecutor.execute(task);
    }
}
