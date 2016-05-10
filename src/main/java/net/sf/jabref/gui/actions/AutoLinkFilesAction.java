package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.external.AutoSetLinks;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

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
        List<BibEntry> entries = JabRefGUI.getMainFrame().getCurrentBasePanel().getSelectedEntries();
        if (entries.isEmpty()) {
            JabRefGUI.getMainFrame().getCurrentBasePanel()
                    .output(Localization.lang("This operation requires one or more entries to be selected."));
            return;
        }
        JDialog diag = new JDialog(JabRefGUI.getMainFrame(), true);
        final NamedCompound nc = new NamedCompound(Localization.lang("Automatically set file links"));
        Runnable runnable = AutoSetLinks.autoSetLinks(entries, nc, null, null,
                JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext(), e -> {
                    if (e.getID() > 0) {
                        // entry has been updated in Util.autoSetLinks, only treat nc and status message
                        if (nc.hasEdits()) {
                            nc.end();
                            JabRefGUI.getMainFrame().getCurrentBasePanel().undoManager.addEdit(nc);
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
