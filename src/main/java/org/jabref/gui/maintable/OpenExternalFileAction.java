package org.jabref.gui.maintable;

import java.util.LinkedList;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

public class OpenExternalFileAction extends SimpleCommand {

    private final int FILES_LIMIT = 10;

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    private final BibEntry entry;
    private final LinkedFile linkedFile;

    public OpenExternalFileAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this(dialogService, stateManager, preferencesService, null, null);
    }

    public OpenExternalFileAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService, BibEntry entry, LinkedFile linkedFile) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.entry = entry;
        this.linkedFile = linkedFile;

        if (this.linkedFile == null) {
            this.executable.bind(ActionHelper.hasLinkedFileForSelectedEntries(stateManager)
                                             .and(ActionHelper.needsEntriesSelected(stateManager))
            );
        } else {
            this.setExecutable(true);
        }
    }

    /**
     * Open all linked files of the selected entries. If opening too many files, pop out a dialog to ask the user if to continue.
     * <br>
     * If some selected entries have linked file and others do not, ignore the latter.
     */
    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            if (entry == null) {
                final List<BibEntry> selectedEntries = stateManager.getSelectedEntries();

                List<LinkedFileViewModel> linkedFileViewModelList = new LinkedList<>();
                LinkedFileViewModel linkedFileViewModel;

                for (BibEntry entry : selectedEntries) {
                    for (LinkedFile linkedFile : entry.getFiles()) {
                        linkedFileViewModel = new LinkedFileViewModel(
                                linkedFile,
                                entry,
                                databaseContext,
                                Globals.TASK_EXECUTOR,
                                dialogService,
                                preferencesService,
                                ExternalFileTypes.getInstance());

                        linkedFileViewModelList.add(linkedFileViewModel);
                    }
                }

                // ask the user when detecting # of files > FILES_LIMIT
                if (linkedFileViewModelList.size() > FILES_LIMIT) {
                    boolean continueOpening = dialogService.showConfirmationDialogAndWait(Localization.lang("Opening large number of files"),
                            Localization.lang("You are about to open %0 files. Continue?", linkedFileViewModelList.size()),
                            Localization.lang("Continue"), Localization.lang("Cancel"));
                    if (!continueOpening) {
                        return;
                    }
                }

                linkedFileViewModelList.forEach(LinkedFileViewModel::open);
            } else {
                LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                        linkedFile,
                        entry,
                        databaseContext,
                        Globals.TASK_EXECUTOR,
                        dialogService,
                        preferencesService,
                        ExternalFileTypes.getInstance());
                linkedFileViewModel.open();
            }
        });
    }
}
