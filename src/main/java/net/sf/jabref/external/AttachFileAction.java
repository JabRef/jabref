/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.external;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.undo.UndoableFieldChange;

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
            String oldVal = entry.getField(GUIGlobals.FILE_FIELD);
            if (oldVal != null) {
                model.setContent(oldVal);
            }
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
