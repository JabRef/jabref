package net.sf.jabref;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;

import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.util.Util;

/**
 * This Action may only be used in a menu or button.
 * Never in the entry editor. FileListEditor and EntryEditor have other ways to update the file links
 */
class AutoLinkFilesAction extends AbstractAction {

    public AutoLinkFilesAction() {
        putValue(Action.SMALL_ICON, GUIGlobals.getImage("autoGroup"));
        putValue(Action.NAME, Globals.lang("Automatically set file links"));
        putValue(Action.ACCELERATOR_KEY, Globals.prefs.getKey("Automatically link files"));
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        ArrayList<BibtexEntry> entries = new ArrayList<BibtexEntry>();
        Collections.addAll(entries, JabRef.jrf.basePanel().getSelectedEntries());
        if (entries.isEmpty()) {
            JabRef.jrf.basePanel().output(Globals.lang("No entries selected."));
            return;
        }
        JDialog diag = new JDialog(JabRef.jrf, true);
        final NamedCompound nc = new NamedCompound(Globals.lang("Automatically set file links"));
        Runnable runnable = Util.autoSetLinks(entries, nc, null, null, JabRef.jrf.basePanel().metaData(), new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getID() > 0) {
                    // entry has been updated in Util.autoSetLinks, only treat nc and status message
                    if (nc.hasEdits()) {
                        nc.end();
                        JabRef.jrf.basePanel().undoManager.addEdit(nc);
                        JabRef.jrf.basePanel().markBaseChanged();
                    }
                    JabRef.jrf.output(Globals.lang("Finished autosetting external links."));
                } else {
                    JabRef.jrf.output(Globals.lang("Finished autosetting external links.")
                            + " " + Globals.lang("No files found."));
                }
            }
        }, diag);
        JabRefExecutorService.INSTANCE.execute(runnable);
    }
}
