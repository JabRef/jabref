package org.jabref.gui.externalfiles;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/**
 * This Action may only be used in a menu or button.
 * Never in the entry editor. FileListEditor and EntryEditor have other ways to update the file links
 */
public class AutoLinkFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final StateManager stateManager;
    private UndoManager undoManager;
    private TaskExecutor taskExecutor;

    public AutoLinkFilesAction(JabRefFrame frame, JabRefPreferences preferences, StateManager stateManager, UndoManager undoManager, TaskExecutor taskExecutor) {
        this.dialogService = frame.getDialogService();
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(this.stateManager).and(needsEntriesSelected(stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(executable, "", Localization.lang("This operation requires one or more entries to be selected.")));

    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        List<BibEntry> entries = stateManager.getSelectedEntries();

        final NamedCompound nc = new NamedCompound(Localization.lang("Automatically set file links"));
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(
                database,
                preferences.getFilePreferences(),
                preferences.getAutoLinkPreferences(),
                ExternalFileTypes.getInstance());
        Task<List<BibEntry>> linkFilesTask = new Task<>() {

            @Override
            protected List<BibEntry> call() {
                return util.linkAssociatedFiles(entries, nc);
            }

            @Override
            protected void succeeded() {
                if (!getValue().isEmpty()) {
                    if (nc.hasEdits()) {
                        nc.end();
                        undoManager.addEdit(nc);
                    }
                    dialogService.notify(Localization.lang("Finished automatically setting external links."));
                } else {
                    dialogService.notify(Localization.lang("Finished automatically setting external links.") + " " + Localization.lang("No files found."));
                }
            }
        };

        dialogService.showProgressDialog(
                Localization.lang("Automatically setting file links"),
                Localization.lang("Searching for files"),
                linkFilesTask);
        taskExecutor.execute(linkFilesTask);
    }
}
