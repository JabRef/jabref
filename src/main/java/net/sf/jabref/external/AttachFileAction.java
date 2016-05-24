package net.sf.jabref.external;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: 5/24/12
 * Time: 8:48 PM
 * To change this template use File | Settings | File Templates.
 */
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
            entry.getFieldOptional(Globals.FILE_FIELD).ifPresent(model::setContent);
            model.addEntry(model.getRowCount(), flEntry);
            String newVal = model.getStringRepresentation();

            UndoableFieldChange ce = new UndoableFieldChange(entry, Globals.FILE_FIELD,
                    entry.getField(Globals.FILE_FIELD), newVal);
            entry.setField(Globals.FILE_FIELD, newVal);
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
        }
    }

}
