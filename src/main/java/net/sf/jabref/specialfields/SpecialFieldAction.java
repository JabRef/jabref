/*  Copyright (C) 2012-2016 JabRef contributors.
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
package net.sf.jabref.specialfields;

import java.util.List;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SpecialFieldAction implements BaseAction {

    private final JabRefFrame frame;
    private final String doneTextPattern;
    private final SpecialField c;
    private final String value;
    private final boolean nullFieldIfValueIsTheSame;
    private final String undoText;

    private static final Log LOGGER = LogFactory.getLog(SpecialFieldAction.class);


    /**
     *
     * @param nullFieldIfValueIsTheSame - false also causes that doneTextPattern has two place holders %0 for the value and %1 for the sum of entries
     * @param doneTextPattern - the pattern to use to update status information shown in MainFrame
     */
    public SpecialFieldAction(
            JabRefFrame frame,
            SpecialField c,
            String value,
            boolean nullFieldIfValueIsTheSame,
            String undoText,
            String doneTextPattern) {
        this.frame = frame;
        this.c = c;
        this.value = value;
        this.nullFieldIfValueIsTheSame = nullFieldIfValueIsTheSame;
        this.undoText = undoText;
        this.doneTextPattern = doneTextPattern;
    }

    @Override
    public void action() {
        try {
            List<BibEntry> bes = frame.getCurrentBasePanel().getSelectedEntries();
            if ((bes == null) || bes.isEmpty()) {
                return;
            }
            NamedCompound ce = new NamedCompound(undoText);
            for (BibEntry be : bes) {
                // if (value==null) and then call nullField has been omitted as updatefield also handles value==null
                SpecialFieldsUtils.updateField(c, value, be, ce, nullFieldIfValueIsTheSame);
            }
            ce.end();
            if (ce.hasEdits()) {
                frame.getCurrentBasePanel().undoManager.addEdit(ce);
                frame.getCurrentBasePanel().markBaseChanged();
                frame.getCurrentBasePanel().updateEntryEditorIfShowing();
                String outText;
                if (nullFieldIfValueIsTheSame) {
                    outText = Localization.lang(doneTextPattern, Integer.toString(bes.size()));
                } else {
                    outText = Localization.lang(doneTextPattern, value, Integer.toString(bes.size()));
                }
                frame.output(outText);
            } else {
                // if user does not change anything with his action, we do not do anything either
                // even no output message
            }
        } catch (Throwable ex) {
            LOGGER.error("Problem setting special fields", ex);
        }
    }

}
