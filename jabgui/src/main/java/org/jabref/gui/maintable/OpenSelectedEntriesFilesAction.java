package org.jabref.gui.maintable;

import java.util.LinkedList;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

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
            final List<BibEntry> selectedEntries = stateManager.getSelectedEntries();

            if (selectedEntries.size() == 1) {
                BibEntry entry = selectedEntries.getFirst();
                List<LinkedFile> files = entry.getFiles();

                if (files.size() == 1) {
                    new OpenSingleExternalFileAction(
                            dialogService,
                            preferences,
                            entry,
                            files.getFirst(),
                            taskExecutor,
                            databaseContext
                    ).execute();
                    return;
                }
            }

            List<LinkedFileViewModel> linkedFileViewModelList = new LinkedList<>();

            for (BibEntry entry : selectedEntries) {
                for (LinkedFile linkedFile : entry.getFiles()) {
                    LinkedFileViewModel viewModel = new LinkedFileViewModel(
                            linkedFile,
                            entry,
                            databaseContext,
                            taskExecutor,
                            dialogService,
                            preferences);

                    linkedFileViewModelList.add(viewModel);
                }
            }

            if (linkedFileViewModelList.size() > FILES_LIMIT) {
                boolean continueOpening = dialogService.showConfirmationDialogAndWait(
                        Localization.lang("Opening large number of files"),
                        Localization.lang("You are about to open %0 files. Continue?", linkedFileViewModelList.size()),
                        Localization.lang("Open all files"),
                        Localization.lang("Don't open")
                );
                if (!continueOpening) {
                    return;
                }
            }

            linkedFileViewModelList.forEach(LinkedFileViewModel::open);
        });
    }
}
