package org.jabref.gui.ai;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.LinkedFile;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ClearEmbeddingsAction extends SimpleCommand {
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final AiService aiService;
    private final TaskExecutor taskExecutor;

    public ClearEmbeddingsAction(StateManager stateManager,
                                 DialogService dialogService,
                                 AiService aiService,
                                 TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.aiService = aiService;
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

        List<LinkedFile> linkedFiles = stateManager
                .getActiveDatabase()
                .get()
                .getDatabase()
                .getEntries()
                .stream()
                .flatMap(entry -> entry.getFiles().stream())
                .toList();

        BackgroundTask.wrap(() -> aiService.getIngestionService().clearEmbeddingsFor(linkedFiles))
                      .executeWith(taskExecutor);
    }
}
