package org.jabref.gui.actions;

import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.jabref.JabRefExecutorService;
import org.jabref.JabRefGUI;
import org.jabref.gui.externalfiles.AutoSetLinks;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

/**
 * This Action may only be used in a menu or button.
 * Never in the entry editor. FileListEditor and EntryEditor have other ways to update the file links
 */
public class AutoLinkFilesAction extends SimpleCommand {

    public AutoLinkFilesAction() {

    }

    @Override
    public void execute() {
        List<BibEntry> entries = JabRefGUI.getMainFrame().getCurrentBasePanel().getSelectedEntries();
        if (entries.isEmpty()) {
            JabRefGUI.getMainFrame().getCurrentBasePanel()
                    .output(Localization.lang("This operation requires one or more entries to be selected."));
            return;
        }
        JDialog diag = new JDialog((JFrame) null, true);
        final NamedCompound nc = new NamedCompound(Localization.lang("Automatically set file links"));
        Runnable runnable = AutoSetLinks.autoSetLinks(entries, nc, null,
                JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext(), e -> {
                    if (e.getID() > 0) {
                        // entry has been updated in Util.autoSetLinks, only treat nc and status message
                        if (nc.hasEdits()) {
                            nc.end();
                            JabRefGUI.getMainFrame().getCurrentBasePanel().getUndoManager().addEdit(nc);
                            JabRefGUI.getMainFrame().getCurrentBasePanel().markBaseChanged();
                        }
                        JabRefGUI.getMainFrame().output(Localization.lang("Finished automatically setting external links."));
                    } else {
                        JabRefGUI.getMainFrame().output(Localization.lang("Finished automatically setting external links.") + " "
                                + Localization.lang("No files found."));
                    }
                } , diag);
        JabRefExecutorService.INSTANCE.execute(runnable);
    }
}
