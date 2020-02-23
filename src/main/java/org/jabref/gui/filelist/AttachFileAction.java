package org.jabref.gui.filelist;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
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
import org.jabref.preferences.PreferencesService;

public class AttachFileAction extends SimpleCommand {

    private final BasePanel panel;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    public AttachFileAction(BasePanel panel, DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        this.panel = panel;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
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

        Path workingDirectory = databaseContext.getFirstExistingFileDir(preferencesService.getFilePreferences())
                                               .orElse(preferencesService.getWorkingDir());

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDirectory)
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(newFile -> {
            LinkedFile linkedFile = LinkedFilesEditorViewModel.fromFile(newFile,
                    databaseContext.getFileDirectoriesAsPaths(preferencesService.getFilePreferences()),
                    ExternalFileTypes.getInstance());

            LinkedFileEditDialogView dialog = new LinkedFileEditDialogView(linkedFile);

            dialog.showAndWait()
                  .ifPresent(editedLinkedFile -> {
                      Optional<FieldChange> fieldChange = entry.addFile(editedLinkedFile);
                      fieldChange.ifPresent(change -> {
                          UndoableFieldChange ce = new UndoableFieldChange(change);
                          panel.getUndoManager().addEdit(ce);
                          panel.markBaseChanged();
                      });
                  });
        });
    }
}
