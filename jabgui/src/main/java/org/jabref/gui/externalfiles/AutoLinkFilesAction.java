package org.jabref.gui.externalfiles;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableFieldChange;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/// This Action may only be used in a menu or button.
/// Never in the entry editor. FileListEditor and EntryEditor have other ways to update the file links
public class AutoLinkFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final UiTaskExecutor taskExecutor;

    public AutoLinkFilesAction(DialogService dialogService, GuiPreferences preferences, StateManager stateManager, UiTaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(this.stateManager).and(needsEntriesSelected(stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(executable, "", Localization.lang("This operation requires one or more entries to be selected.")));
    }

    @Override
    public void execute() {
        final BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        final UndoManager undoManager = stateManager.getUndoManager(database);
        final List<BibEntry> entries = List.copyOf(stateManager.getSelectedEntries());

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(
                database,
                preferences.getExternalApplicationsPreferences(),
                preferences.getFilePreferences(),
                preferences.getAutoLinkPreferences());
        final CompoundEdit compound = new CompoundEdit(StandardActions.AUTO_LINK_FILES.getText());

        Task<AutoSetFileLinksUtil.LinkFilesResult> linkFilesTask = new Task<>() {
            final BiConsumer<List<LinkedFile>, BibEntry> onLinkedFilesUpdated = (newLinkedFiles, entry) -> {
                // lambda for gui actions that are relevant when setting the linked file entry when ui is opened
                String newVal = FileFieldWriter.getStringRepresentation(newLinkedFiles);
                String oldVal = entry.getField(StandardField.FILE).orElse(null);
                UndoableFieldChange fieldChange = new UndoableFieldChange(entry, StandardField.FILE, oldVal, newVal);
                compound.addEdit(fieldChange); // push to undo manager is in succeeded

                // Wait because there are several rounds in one auto-link operation
                // The later round depends on the updated bibEntry of the previous round
                UiTaskExecutor.runAndWaitInJavaFXThread(() -> entry.setFiles(newLinkedFiles));
            };

            @Override
            protected AutoSetFileLinksUtil.LinkFilesResult call() {
                return util.linkAssociatedFiles(entries, onLinkedFilesUpdated);
            }

            @Override
            protected void succeeded() {
                AutoSetFileLinksUtil.LinkFilesResult result = getValue();

                if (!result.getFileExceptions().isEmpty()) {
                    dialogService.showWarningDialogAndWait(
                            Localization.lang("Automatically set file links"),
                            Localization.lang("Problem finding files. See error log for details."));
                    return;
                }

                if (result.getChangedEntries().isEmpty()) {
                    dialogService.showWarningDialogAndWait(
                            Localization.lang("Automatically set file links"),
                            Localization.lang("Finished automatically setting external links.") + "\n"
                                    + Localization.lang("No files found."));
                    return;
                }

                if (compound.hasEdits()) {
                    undoManager.addEdit(compound.toChangeSet());
                }

                dialogService.notify("%s %s\n%s".formatted(
                        Localization.lang("Finished automatically setting external links."),
                        Localization.lang("Changed %0 entry(s).", result.getChangedEntries().size()),
                        Localization.lang("Affected entry(s): %0", result.getChangedEntries().stream()
                                                                         .map(BibEntry::getCitationKey)
                                                                         .flatMap(Optional::stream)
                                                                         .collect(Collectors.joining(", ")))));
            }
        };

        dialogService.showProgressDialog(
                Localization.lang("Automatically setting file links"),
                Localization.lang("Searching for files"),
                linkFilesTask);
        taskExecutor.execute(linkFilesTask);
    }
}
