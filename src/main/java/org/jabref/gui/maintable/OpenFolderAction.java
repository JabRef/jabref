package org.jabref.gui.maintable;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

public class OpenFolderAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;

    private final BibEntry entry;
    private final LinkedFile linkedFile;

    public OpenFolderAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this(dialogService, stateManager, preferencesService, null, null);
    }

    public OpenFolderAction(DialogService dialogService, StateManager stateManager, PreferencesService preferencesService, BibEntry entry, LinkedFile linkedFile) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.entry = entry;
        this.linkedFile = linkedFile;

        if (this.linkedFile == null) {
            this.executable.bind(ActionHelper.isFilePresentForSelectedEntry(stateManager, preferencesService));
        } else {
            this.setExecutable(true);
        }
    }

    @Override
    public void execute() {
            stateManager.getActiveDatabase().ifPresent(databaseContext -> {
                if (entry == null) {
                    stateManager.getSelectedEntries().stream().filter((entry) -> !entry.getFiles().isEmpty()).forEach(entry -> {
                        LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                                entry.getFiles().get(0),
                                entry,
                                databaseContext,
                                Globals.TASK_EXECUTOR,
                                dialogService,
                                preferencesService,
                                ExternalFileTypes.getInstance()
                        );
                        linkedFileViewModel.openFolder();
                    });
                } else {
                    LinkedFileViewModel linkedFileViewModel = new LinkedFileViewModel(
                            linkedFile,
                            entry,
                            databaseContext,
                            Globals.TASK_EXECUTOR,
                            dialogService,
                            preferencesService,
                            ExternalFileTypes.getInstance()
                    );
                    linkedFileViewModel.openFolder();
                }
            });
    }
}
