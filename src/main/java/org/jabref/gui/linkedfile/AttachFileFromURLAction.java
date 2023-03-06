package org.jabref.gui.linkedfile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModelUtil;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

public class AttachFileFromURLAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;

    public AttachFileFromURLAction(LibraryTab libraryTab,
                                   DialogService dialogService,
                                   StateManager stateManager,
                                   TaskExecutor taskExecutor, PreferencesService preferencesService) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.needsEntriesSelected(1, stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            dialogService.notify(Localization.lang("This operation requires an open library."));
            return;
        }

        if (stateManager.getSelectedEntries().size() != 1) {
            dialogService.notify(Localization.lang("This operation requires exactly one item to be selected."));
            return;
        }

        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();

        BibEntry entry = stateManager.getSelectedEntries().get(0);

        LinkedFilesEditorViewModelUtil util = new LinkedFilesEditorViewModelUtil(dialogService, entry);
        Optional<String> urlforDownload = util.getUrlForDownloadFromClipBoardOrEntry();

        if (urlforDownload.isEmpty()) {
            return;
        }

        try {
            URL url = new URL(urlforDownload.get());
            LinkedFileViewModel onlineFile = new LinkedFileViewModel(
                             new LinkedFile(url, ""),
                             entry,
                             databaseContext,
                             taskExecutor,
                             dialogService,
                             preferencesService);
            onlineFile.download();
        } catch (MalformedURLException exception) {
            dialogService.showErrorDialogAndWait(Localization.lang("Invalid URL"), exception);
        }
    }
}
