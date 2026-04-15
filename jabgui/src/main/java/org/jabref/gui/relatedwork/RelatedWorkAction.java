package org.jabref.gui.relatedwork;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

public class RelatedWorkAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;

    public RelatedWorkAction(DialogService dialogService, StateManager stateManager, CliPreferences preferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        executable.bind(ActionHelper.needsDatabase(stateManager)
                                    .and(ActionHelper.needsEntriesSelected(1, stateManager))
                                    .and(ActionHelper.isPdfFilePresentForSelectedEntry(stateManager, preferences)));
    }

    @Override
    public void execute() {
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().orElseThrow();
        BibEntry sourceEntry = stateManager.getSelectedEntries().getFirst();
        Optional<LinkedFile> linkedPDFFile = sourceEntry.getFiles().stream()
                                                        .filter(this::isPDFLinkedFile)
                                                        .findFirst();

        if (linkedPDFFile.isEmpty()) {
            dialogService.showWarningDialogAndWait(
                    Localization.lang("No PDF files available"),
                    Localization.lang("Please attach PDF files")
            );
            return;
        }

        Optional<String> citationKey = sourceEntry.getCitationKey();
        if (citationKey.isEmpty()) {
            dialogService.showWarningDialogAndWait(
                    Localization.lang("Insert related work text"),
                    Localization.lang("Selected entry does not have an associated citation key.")
            );
            return;
        }

        dialogService.showCustomDialogAndWait(new RelatedWorkDialogView(
                databaseContext,
                sourceEntry,
                linkedPDFFile.get(),
                citationKey.get()
        ));
    }

    private boolean isPDFLinkedFile(LinkedFile linkedFile) {
        return linkedFile.getFileName()
                         .map(Path::of)
                         .map(FileUtil::isPDFFile)
                         .orElse(false);
    }
}
