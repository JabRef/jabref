package org.jabref.gui.integrity;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class IntegrityCheckAction extends SimpleCommand {

    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final JabRefFrame frame;
    private final StateManager stateManager;

    public IntegrityCheckAction(JabRefFrame frame, StateManager stateManager, TaskExecutor taskExecutor) {
        this.frame = frame;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.dialogService = frame.getDialogService();

        this.executable.bind(needsDatabase(this.stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        IntegrityCheck check = new IntegrityCheck(database,
                Globals.prefs.getFilePreferences(),
                Globals.prefs.getCitationKeyPatternPreferences(),
                Globals.journalAbbreviationRepository,
                Globals.prefs.getEntryEditorPreferences().shouldAllowIntegerEditionBibtex());

        Task<List<IntegrityMessage>> task = new Task<>() {
            @Override
            protected List<IntegrityMessage> call() {
                List<IntegrityMessage> result = new ArrayList<>();

                ObservableList<BibEntry> entries = database.getDatabase().getEntries();
                result.addAll(check.checkDatabase(database.getDatabase()));
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
                dialogService.showCustomDialogAndWait(new IntegrityCheckDialog(messages, frame.getCurrentLibraryTab()));
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
