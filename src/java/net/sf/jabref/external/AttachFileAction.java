package net.sf.jabref.external;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.undo.UndoableFieldChange;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: 5/24/12
 * Time: 8:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class AttachFileAction extends BaseAction {

    BibtexEntry entry = null;
    private BasePanel panel;

    public AttachFileAction(BasePanel panel) {
        this.panel = panel;
    }

    public void action() {
        if (panel.getSelectedEntries().length != 1)
            return; // TODO: display error message?
        entry = panel.getSelectedEntries()[0];
        FileListEntry flEntry = new FileListEntry("", "", null);
        FileListEntryEditor editor = new FileListEntryEditor(panel.frame(), flEntry, false, true,
                panel.metaData());
        editor.setVisible(true, true);
        if (editor.okPressed()) {
            FileListTableModel model = new FileListTableModel();
            String oldVal = entry.getField(GUIGlobals.FILE_FIELD);
            if (oldVal != null)
                model.setContent(oldVal);
            model.addEntry(model.getRowCount(), flEntry);
            String newVal = model.getStringRepresentation();

            UndoableFieldChange ce = new UndoableFieldChange(entry, GUIGlobals.FILE_FIELD,
                    oldVal, newVal);
            entry.setField(GUIGlobals.FILE_FIELD, newVal);
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
        }
    }


}
