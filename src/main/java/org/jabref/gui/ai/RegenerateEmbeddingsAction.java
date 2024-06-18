package org.jabref.gui.ai;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class RegenerateEmbeddingsAction extends SimpleCommand {
    private final StateManager stateManager;
    private final GetCurrentLibraryTab currentLibraryTab;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    private boolean shouldContinue = true;

    public RegenerateEmbeddingsAction(StateManager stateManager,
                                        GetCurrentLibraryTab currentLibraryTab,
                                        DialogService dialogService,
                                        TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.currentLibraryTab = currentLibraryTab;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        init();
        BackgroundTask.wrap(this::rebuildIndex)
                      .executeWith(taskExecutor);
    }

    public void init() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        boolean confirm = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Regenerate embeddings cache"),
                Localization.lang("Regenerate embeddings cache for current library?"));

        if (!confirm) {
            shouldContinue = false;
            return;
        }

        dialogService.notify(Localization.lang("Regenerating embeddings cache..."));
    }

    private void rebuildIndex() {
        if (!shouldContinue || stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        currentLibraryTab.get().getEmbeddingsGenerationTaskManager().invalidate();
    }

    public interface GetCurrentLibraryTab {
        LibraryTab get();
    }
}
