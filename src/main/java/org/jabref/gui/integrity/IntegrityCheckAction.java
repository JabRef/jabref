package org.jabref.gui.integrity;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.jabref.Globals;
import org.jabref.gui.Dialog;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class IntegrityCheckAction extends SimpleCommand {

    private final JabRefFrame frame;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;

    public IntegrityCheckAction(JabRefFrame frame) {
        this.frame = frame;
        this.taskExecutor = Globals.TASK_EXECUTOR;
        this.dialogService = frame.getDialogService();
    }

    @Override
    public void execute() {
        BibDatabaseContext databaseContext = frame.getCurrentBasePanel().getBibDatabaseContext();
        IntegrityCheck check = new IntegrityCheck(databaseContext,
                Globals.prefs.getFilePreferences(),
                Globals.prefs.getBibtexKeyPatternPreferences(),
                Globals.journalAbbreviationLoader.getRepository(Globals.prefs.getJournalAbbreviationPreferences()),
                Globals.prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));

        Task<List<IntegrityMessage>> task = new Task<List<IntegrityMessage>>() {
            @Override
            protected List<IntegrityMessage> call() {
                List<IntegrityMessage> result = new ArrayList<>();

                ObservableList<BibEntry> entries = databaseContext.getDatabase().getEntries();
                for (int i = 0; i < entries.size(); i++) {
                    if (isCancelled()) {
                        break;
                    }

                    BibEntry entry = entries.get(i);
                    result.addAll(check.checkBibtexEntry(entry));
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
                Dialog<Void> dialog = new IntegrityCheckDialog(messages, frame.getCurrentBasePanel());
                dialog.showAndWait();
            }
        });
        task.setOnFailed(event -> dialogService.showErrorDialogAndWait("Integrity check failed."));

        dialogService.showProgressDialogAndWait(
                Localization.lang("Checking integrity..."),
                Localization.lang("Checking integrity..."),
                task);
        taskExecutor.execute(task);
    }
}
