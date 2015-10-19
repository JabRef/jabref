package net.sf.jabref.external;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.model.entry.BibtexEntry;

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
        if (panel.getSelectedEntries().length != 1)
         {
            return; // TODO: display error message?
        }
        BibtexEntry entry = panel.getSelectedEntries()[0];
        FileListEntry flEntry = new FileListEntry("", "", null);
        FileListEntryEditor editor = new FileListEntryEditor(panel.frame(), flEntry, false, true,
                panel.metaData());
        editor.setVisible(true, true);
        if (editor.okPressed()) {
            FileListTableModel model = new FileListTableModel();
            String oldVal = entry.getField(Globals.FILE_FIELD);
            if (oldVal != null) {
                model.setContent(oldVal);
            }
            model.addEntry(model.getRowCount(), flEntry);
            String newVal = model.getStringRepresentation();

            UndoableFieldChange ce = new UndoableFieldChange(entry, Globals.FILE_FIELD,
                    oldVal, newVal);
            entry.setField(Globals.FILE_FIELD, newVal);
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
        }
    }

}
