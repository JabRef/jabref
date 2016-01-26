package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

/**
 * This Action may only be used in a menu or button.
 * Never in the entry editor. FileListEditor and EntryEditor have other ways to update the file links
 */
public class AutoLinkFilesAction extends AbstractAction {

    public AutoLinkFilesAction() {
        putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.AUTO_FILE_LINK.getSmallIcon());
        putValue(Action.LARGE_ICON_KEY, IconTheme.JabRefIcon.AUTO_FILE_LINK.getIcon());
        putValue(Action.NAME, Localization.lang("Automatically set file links"));
        putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.AUTOMATICALLY_LINK_FILES));
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        ArrayList<BibEntry> entries = new ArrayList<>();
        Collections.addAll(entries, JabRef.jrf.getCurrentBasePanel().getSelectedEntries());
        if (entries.isEmpty()) {
            JabRef.jrf.getCurrentBasePanel().output(Localization.lang("No entries selected."));
            return;
        }
        JDialog diag = new JDialog(JabRef.jrf, true);
        final NamedCompound nc = new NamedCompound(Localization.lang("Automatically set file links"));
        Runnable runnable = Util.autoSetLinks(entries, nc, null, null, JabRef.jrf.getCurrentBasePanel().loadedDatabase.getMetaData(), new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getID() > 0) {
                    // entry has been updated in Util.autoSetLinks, only treat nc and status message
                    if (nc.hasEdits()) {
                        nc.end();
                        JabRef.jrf.getCurrentBasePanel().undoManager.addEdit(nc);
                        JabRef.jrf.getCurrentBasePanel().markBaseChanged();
                    }
                    JabRef.jrf.output(Localization.lang("Finished autosetting external links."));
                } else {
                    JabRef.jrf.output(Localization.lang("Finished autosetting external links.")
                            + " " + Localization.lang("No files found."));
                }
            }
        }, diag);
        JabRefExecutorService.INSTANCE.execute(runnable);
    }
}
