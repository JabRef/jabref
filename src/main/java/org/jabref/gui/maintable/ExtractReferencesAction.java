package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

public class ExtractReferencesAction extends SimpleCommand {
    private final int FILES_LIMIT = 10;

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;
    private final BibEntry entry;
    private final LinkedFile linkedFile;
    private final TaskExecutor taskExecutor;

    public ExtractReferencesAction(DialogService dialogService,
                                   StateManager stateManager,
                                   PreferencesService preferencesService,
                                   TaskExecutor taskExecutor) {
        this(dialogService, stateManager, preferencesService, null, null, taskExecutor);
    }

    public ExtractReferencesAction(DialogService dialogService,
                                   StateManager stateManager,
                                   PreferencesService preferencesService,
                                   BibEntry entry,
                                   LinkedFile linkedFile,
                                   TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.entry = entry;
        this.linkedFile = linkedFile;
        this.taskExecutor = taskExecutor;

        if (this.linkedFile == null) {
            this.executable.bind(
                    ActionHelper.needsEntriesSelected(stateManager)
                                .and(ActionHelper.hasLinkedFileForSelectedEntries(stateManager))
                                .and(this.preferencesService.getGrobidPreferences().grobidEnabledProperty())
            );
        } else {
            this.setExecutable(true);
        }
    }

    @Override
    public void execute() {
        extractReferences();
    }

    private void extractReferences() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            List<BibEntry> selectedEntries = new LinkedList<>();
            if (entry == null) {
                selectedEntries = stateManager.getSelectedEntries();
            } else {
                selectedEntries.add(entry);
            }

            List<Path> fileList = FileUtil.getListOfLinkedFiles(selectedEntries, databaseContext.getFileDirectories(preferencesService.getFilePreferences()));
            if (fileList.size() > FILES_LIMIT) {
                boolean continueOpening = dialogService.showConfirmationDialogAndWait(Localization.lang("Processing a large number of files"),
                        Localization.lang("You are about to process %0 files. Continue?", fileList.size()),
                        Localization.lang("Continue"), Localization.lang("Cancel"));
                if (!continueOpening) {
                    return;
                }
            }

            Callable<ParserResult> parserResultCallable = () -> new ParserResult(
                    new GrobidService(this.preferencesService.getGrobidPreferences()).processReferences(fileList, preferencesService.getImportFormatPreferences())
            );
            BackgroundTask<ParserResult> task = BackgroundTask.wrap(parserResultCallable)
                                                              .withInitialMessage(Localization.lang("Processing PDF(s)"));

            task.onFailure(dialogService::showErrorDialogAndWait);

            ImportEntriesDialog dialog = new ImportEntriesDialog(stateManager.getActiveDatabase().get(), task);
            dialog.setTitle(Localization.lang("Extract References"));
            dialogService.showCustomDialogAndWait(dialog);
        });
    }
}
