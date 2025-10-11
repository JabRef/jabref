package org.jabref.gui.maintable;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

public class OpenSelectedEntriesFilesAction extends SimpleCommand {

    private static final int FILES_LIMIT = 10;

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;

    public OpenSelectedEntriesFilesAction(DialogService dialogService,
                                          StateManager stateManager,
                                          GuiPreferences preferences,
                                          TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;

        this.executable.bind(ActionHelper.hasLinkedFileForSelectedEntries(stateManager)
                                         .and(ActionHelper.needsEntriesSelected(stateManager)));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            List<LinkedFileViewModel> linkedFileViewModelList = stateManager
                    .getSelectedEntries().stream()
                    .flatMap(entry -> entry.getFiles().stream()
                                           .map(linkedFile -> new LinkedFileViewModel(
                                                   linkedFile,
                                                   entry,
                                                   databaseContext,
                                                   taskExecutor,
                                                   dialogService,
                                                   preferences)))
                    .toList();
            if (linkedFileViewModelList.size() > FILES_LIMIT) {
                boolean continueOpening = dialogService.showConfirmationDialogAndWait(
                        Localization.lang("Opening large number of files"),
                        Localization.lang("You are about to open %0 files. Continue?", linkedFileViewModelList.size()),
                        Localization.lang("Open all linked files"),
                        Localization.lang("Cancel file opening")
                );
                if (!continueOpening) {
                    return;
                }
            }

            linkedFileViewModelList.forEach(LinkedFileViewModel::open);
        });
    }
}
