package org.jabref.gui.filelist;

import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.JabRefPreferences;

public class AttachFileAction extends SimpleCommand {

    private final BasePanel panel;
    private final DialogService dialogService;

    public AttachFileAction(BasePanel panel, DialogService dialogService) {
        this.panel = panel;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        if (panel.getSelectedEntries().size() != 1) {
            panel.output(Localization.lang("This operation requires exactly one item to be selected."));
            return;
        }
        BibEntry entry = panel.getSelectedEntries().get(0);

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
        .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY))
        .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(newFile -> {
            LinkedFile newLinkedFile = new LinkedFile("", newFile.toString(), "");

            LinkedFilesWrapper wrapper = new LinkedFilesWrapper();
            wrapper.setLinkedFile(newLinkedFile);
            LinkedFileEditDialogView dialog = new LinkedFileEditDialogView(wrapper);

            boolean applyPressed = dialog.showAndWait();
            if (applyPressed) {
                LinkedFilesEditDialogViewModel model = (LinkedFilesEditDialogViewModel) dialog.getController().get().getViewModel();
                newLinkedFile = model.getNewLinkedFile();

                Optional<FieldChange> fieldChange = entry.addFile(newLinkedFile);

                if (fieldChange.isPresent()) {
                    UndoableFieldChange ce = new UndoableFieldChange(entry, FieldName.FILE,
                    entry.getField(FieldName.FILE).orElse(null), fieldChange.get().getNewValue());
                    panel.getUndoManager().addEdit(ce);
                    panel.markBaseChanged();
                }
            }
        });
    }
}
