package org.jabref.gui.ai;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

// [impl->req~ai.ingestion.clear-cache~1]
public class ClearEmbeddingsAction extends SimpleCommand {
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final AiService aiService;
    private final TaskExecutor taskExecutor;
    private final FilePreferences filePreferences;

    public ClearEmbeddingsAction(StateManager stateManager,
                                 DialogService dialogService,
                                 AiService aiService,
                                 TaskExecutor taskExecutor,
                                 FilePreferences filePreferences) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.aiService = aiService;
        this.filePreferences = filePreferences;
        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        boolean confirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Clear embeddings cache"),
                Localization.lang("Clear embeddings cache for current library?"));

        if (!confirmed) {
            return;
        }

        dialogService.notify(Localization.lang("Clearing embeddings cache..."));

        BibDatabaseContext bibDatabaseContext = stateManager.getActiveDatabase().get();

        List<LinkedFile> linkedFiles = bibDatabaseContext
                .getDatabase()
                .getEntries()
                .stream()
                .flatMap(entry -> entry.getFiles().stream())
                .toList();

        BackgroundTask.wrap(() ->
                              aiService
                                      .getEmbeddingsCleaner()
                                      .clearEmbeddingsFor(linkedFiles, bibDatabaseContext, filePreferences))
                      .executeWith(taskExecutor);
    }
}
