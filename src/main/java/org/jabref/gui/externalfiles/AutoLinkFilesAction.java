package org.jabref.gui.externalfiles;

import java.util.List;

import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

/**
 * This Action may only be used in a menu or button.
 * Never in the entry editor. FileListEditor and EntryEditor have other ways to update the file links
 */
public class AutoLinkFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private final JabRefFrame frame;
    private final JabRefPreferences preferences;

    public AutoLinkFilesAction(JabRefFrame frame, JabRefPreferences preferences) {
        this.frame = frame;
        this.dialogService = frame.getDialogService();
        this.preferences = preferences;
    }

    @Override
    public void execute() {
        List<BibEntry> entries = frame.getCurrentBasePanel().getSelectedEntries();
        if (entries.isEmpty()) {
            dialogService.notify(Localization.lang("This operation requires one or more entries to be selected."));
            return;
        }

        final NamedCompound nc = new NamedCompound(Localization.lang("Automatically set file links"));
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(frame.getCurrentBasePanel().getBibDatabaseContext(), preferences.getFilePreferences(), preferences.getAutoLinkPreferences(), ExternalFileTypes.getInstance());
        Task<List<BibEntry>> linkFilesTask = new Task<List<BibEntry>>() {
            @Override
            protected List<BibEntry> call() {
                return util.linkAssociatedFiles(entries, nc);
            }

            @Override
            protected void succeeded() {
                if (!getValue().isEmpty()) {
                    if (nc.hasEdits()) {
                        nc.end();
                        frame.getCurrentBasePanel().getUndoManager().addEdit(nc);
                    }
                    dialogService.notify(Localization.lang("Finished automatically setting external links."));
                } else {
                    dialogService.notify(Localization.lang("Finished automatically setting external links.") + " " + Localization.lang("No files found."));
                }
            }
        };

        dialogService.showProgressDialogAndWait(
                Localization.lang("Automatically setting file links"),
                Localization.lang("Searching for files"),
                linkFilesTask
        );
    }
}
