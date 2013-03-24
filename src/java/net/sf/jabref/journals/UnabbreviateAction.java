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
package net.sf.jabref.journals;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.BasePanel;
import net.sf.jabref.AbstractWorker;
import net.sf.jabref.undo.NamedCompound;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Sep 17, 2005
 * Time: 12:48:23 AM
 * To browseOld this template use File | Settings | File Templates.
 */
public class UnabbreviateAction extends AbstractWorker {
    BasePanel panel;
    String message = "";

    public UnabbreviateAction(BasePanel panel) {
        this.panel = panel;
    }


    public void init() {
        //  new FieldWeightDialog(frame).setVisible(true);
        panel.output("Unabbreviating...");
    }

    public void run() {
        //net.sf.jabref.journals.JournalList.downloadJournalList(frame);


        BibtexEntry[] entries = panel.getSelectedEntries();
        if (entries == null)
            return;
        NamedCompound ce = new NamedCompound("Unabbreviate journal names");
        int count = 0;
        for (int i = 0; i < entries.length; i++) {
            if (Globals.journalAbbrev.unabbreviate(panel.database(), entries[i], "journal", ce))
                count++;
        }
        if (count > 0) {
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
            message = Globals.lang("Unabbreviated %0 journal names.", String.valueOf(count));
        } else {
            message = Globals.lang("No journal names could be unabbreviated.");
        }
    }

    public void update() {
        panel.output(message);
    }
}
