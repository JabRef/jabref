/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.undo;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibtexString;

public class UndoableStringChange extends AbstractUndoableJabRefEdit {

    private final BibtexString string;
    private final String oldValue;
    private final String newValue;
    private final boolean nameChange;
    private final BasePanel panel;


    public UndoableStringChange(BasePanel panel,
            BibtexString string, boolean nameChange,
            String oldValue, String newValue) {
        this.string = string;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.nameChange = nameChange;
        this.panel = panel;
    }

    @Override
    public String getPresentationName() {
        return (nameChange ? Localization.lang("change string name %0 to %1", StringUtil.boldHTML(oldValue),
                StringUtil.boldHTML(newValue)) : Localization.lang("change string content %0 to %1",
                        StringUtil.boldHTML(oldValue), StringUtil.boldHTML(newValue)));
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.

        panel.assureStringDialogNotEditing();

        if (nameChange) {
            string.setName(oldValue);
        } else {
            string.setContent(oldValue);
        }

        panel.updateStringDialog();
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.

        panel.assureStringDialogNotEditing();
        if (nameChange) {
            string.setName(newValue);
        } else {
            string.setContent(newValue);
        }

        panel.updateStringDialog();
    }

}
