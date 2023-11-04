package org.jabref.gui.linkedfile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

public class AttachFileFromURLAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;

    public AttachFileFromURLAction(DialogService dialogService,
                                   StateManager stateManager,
                                   TaskExecutor taskExecutor,
                                   PreferencesService preferencesService) {
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

        Optional<String> urlforDownload = getUrlForDownloadFromClipBoardOrEntry(dialogService, entry);

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

    public static Optional<String> getUrlForDownloadFromClipBoardOrEntry(DialogService dialogService, BibEntry entry) {
        String clipText = ClipBoardManager.getContents();
        Optional<String> urlText;
        String urlField = entry.getField(StandardField.URL).orElse("");
        if (clipText.startsWith("http://") || clipText.startsWith("https://") || clipText.startsWith("ftp://")) {
            urlText = dialogService.showInputDialogWithDefaultAndWait(
                    Localization.lang("Download file"), Localization.lang("Enter URL to download"), clipText);
        } else if (urlField.startsWith("http://") || urlField.startsWith("https://") || urlField.startsWith("ftp://")) {
            urlText = dialogService.showInputDialogWithDefaultAndWait(
                    Localization.lang("Download file"), Localization.lang("Enter URL to download"), urlField);
        } else {
            urlText = dialogService.showInputDialogAndWait(
                    Localization.lang("Download file"), Localization.lang("Enter URL to download"));
        }
        return urlText;
    }
}
