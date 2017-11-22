package org.jabref.gui.filelist;

import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
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
        LinkedFile flEntry = new LinkedFile("", "", "");
        FileListEntryEditor editor = new FileListEntryEditor(flEntry, false, true,
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
        }
    }
}
