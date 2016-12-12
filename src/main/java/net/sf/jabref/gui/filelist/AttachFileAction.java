package net.sf.jabref.gui.filelist;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

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
        FileListEntry flEntry = new FileListEntry("", "");
        FileListEntryEditor editor = new FileListEntryEditor(panel.frame(), flEntry, false, true,
                panel.getBibDatabaseContext());
        editor.setVisible(true, true);
        if (editor.okPressed()) {
            FileListTableModel model = new FileListTableModel();
            entry.getField(FieldName.FILE).ifPresent(model::setContent);
            model.addEntry(model.getRowCount(), flEntry);
            String newVal = model.getStringRepresentation();

            UndoableFieldChange ce = new UndoableFieldChange(entry, FieldName.FILE,
                    entry.getField(FieldName.FILE).orElse(null), newVal);
            entry.setField(FieldName.FILE, newVal);
            panel.getUndoManager().addEdit(ce);
            panel.markBaseChanged();
        }
    }

}
