package org.jabref.gui.linkedfile;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

public class AttachFileAction extends SimpleCommand {

    private final LibraryTab libraryTab;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;

    public AttachFileAction(LibraryTab libraryTab,
                            DialogService dialogService,
                            StateManager stateManager,
                            FilePreferences filePreferences) {
        this.libraryTab = libraryTab;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;

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

        Path workingDirectory = databaseContext.getFirstExistingFileDir(filePreferences)
                                               .orElse(filePreferences.getWorkingDirectory());

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDirectory)
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(newFile -> {
            LinkedFile linkedFile = LinkedFilesEditorViewModel.fromFile(newFile,
                    databaseContext.getFileDirectories(filePreferences),
                    ExternalFileTypes.getInstance());

            LinkedFileEditDialogView dialog = new LinkedFileEditDialogView(linkedFile);

            dialogService.showCustomDialogAndWait(dialog)
                  .ifPresent(editedLinkedFile -> {
                      Optional<FieldChange> fieldChange = entry.addFile(editedLinkedFile);
                      fieldChange.ifPresent(change -> {
                          UndoableFieldChange ce = new UndoableFieldChange(change);
                          libraryTab.getUndoManager().addEdit(ce);
                          libraryTab.markBaseChanged();
                      });
                  });
        });
    }
}
