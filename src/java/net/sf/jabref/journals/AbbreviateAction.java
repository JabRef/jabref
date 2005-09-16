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
 * To change this template use File | Settings | File Templates.
 */
public class AbbreviateAction extends AbstractWorker {
    BasePanel panel;
    String message = "";

    public AbbreviateAction(BasePanel panel) {
        this.panel = panel;
    }


    public void init() {
        //  new FieldWeightDialog(frame).setVisible(true);
        panel.output("Abbreviating...");
    }

    public void run() {
        //net.sf.jabref.journals.JournalList.downloadJournalList(frame);


        BibtexEntry[] entries = panel.entryTable.getSelectedEntries();
        if (entries == null)
            return;
        NamedCompound ce = new NamedCompound("Abbreviate journal names");
        int count = 0;
        for (int i = 0; i < entries.length; i++) {
            if (Globals.journalAbbrev.abbreviate(entries[i], "journal", true, ce))
                count++;
        }
        if (count > 0) {
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.refreshTable();
            panel.markBaseChanged();
            message = Globals.lang("Abbreviated %0 journal names.", String.valueOf(count));
        } else {
            message = Globals.lang("No journal names could be abbreviated.");
        }
    }

    public void update() {
        panel.output(message);
    }
}
