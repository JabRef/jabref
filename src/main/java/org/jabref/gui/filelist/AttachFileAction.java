package org.jabref.gui.filelist;

import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

public class AttachFileAction implements BaseAction {

    private final BasePanel panel;

    public AttachFileAction(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public void action() {
        if (panel.getSelectedEntries().size() != 1) {
            panel.output(Localization.lang("This operation requires exactly one item to be selected."));
            return;
        }
        BibEntry entry = panel.getSelectedEntries().get(0);
        LinkedFile linkedFile = new LinkedFile("", "", "");
        LinkedFilesWrapper wrapper = new LinkedFilesWrapper();
        wrapper.setLinkedFile(linkedFile);
        LinkedFileEditDialogView dialog = new LinkedFileEditDialogView(wrapper);

        boolean applyPressed = DefaultTaskExecutor.runInJavaFXThread(() -> dialog.showAndWait());
        if (applyPressed) {
            LinkedFilesEditDialogViewModel model = (LinkedFilesEditDialogViewModel) dialog.getController().get().getViewModel();
            linkedFile = model.getNewLinkedFile();
            entry.addFile(linkedFile);
        }
        //

        /*   FileListEntryEditor editor = new FileListEntryEditor(flEntry, false, true,
                panel.getBibDatabaseContext());
        editor.setVisible(true, true);

        if (editor.okPressed()) {
            Optional<FieldChange> fieldChange = entry.addFile(flEntry);

            if (fieldChange.isPresent()) {
                UndoableFieldChange ce = new UndoableFieldChange(entry, FieldName.FILE,
                        entry.getField(FieldName.FILE).orElse(null), fieldChange.get().getNewValue());
                panel.getUndoManager().addEdit(ce);
                panel.markBaseChanged();
            }
        }*/
    }
}
