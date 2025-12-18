package org.jabref.gui.mergeentries;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class UpdateEntryFromWebSourcesAction extends SimpleCommand {
    private final StateManager stateManager;
    private final UndoManager undoManager;
    private final GuiPreferences preferences;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;

    public UpdateEntryFromWebSourcesAction(StateManager stateManager, UndoManager undoManager, GuiPreferences preferences, NotificationService notificationService, TaskExecutor taskExecutor, DialogService dialogService) {
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.preferences = preferences;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        if (stateManager.getSelectedEntries().size() != 1) {
            notificationService.notify(Localization.lang("Select exactly one entry to update from web sources"));
            return;
        }
        BibEntry entry = stateManager.getSelectedEntries().getFirst();

        MultiMergeEntriesView view = new MultiMergeEntriesView(preferences, taskExecutor);
        view.addSource(Localization.lang("Entry"), entry);

        SortedSet<EntryBasedFetcher> fetchers = WebFetchers.getEntryBasedFetchers(preferences.getImporterPreferences(), preferences.getImportFormatPreferences(), preferences.getFilePreferences(), stateManager.getActiveDatabase().get());

        for (EntryBasedFetcher fetcher : fetchers) {
            view.addSource(fetcher.getName(), () -> {
                try {
                    List<BibEntry> results = fetcher.performSearch(entry);
                    if (results.isEmpty()) {
                        return null;
                    }
                    return results.getFirst();
                } catch (FetcherException e) {
                    return null;
                }
            });
        }
        Optional<BibEntry> merged = dialogService.showCustomDialogAndWait(view);

        if (merged.isEmpty()) {
            notificationService.notify(Localization.lang("Canceled updating entry from web sources"));
            return;
        }

        BibEntry mergedEntry = merged.get();
        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Update entry from web sources"));

        Set<Field> jointFields = new TreeSet<>(Comparator.comparing(Field::getName));
        jointFields.addAll(mergedEntry.getFields());
        Set<Field> originalFields = new TreeSet<>(Comparator.comparing(Field::getName));
        originalFields.addAll(entry.getFields());

        boolean edited = false;

        for (Field field : jointFields) {
            Optional<String> originalString = entry.getField(field);
            Optional<String> mergedString = mergedEntry.getField(field);

            if (originalString.isEmpty() || !originalString.equals(mergedString)) {
                String newValue = mergedString.orElse("");
                entry.setField(field, newValue);
                compoundEdit.addEdit(new UndoableFieldChange(entry, field, originalString.orElse(null), newValue));
                edited = true;
            }
        }

        for (Field field : originalFields) {
            if (!jointFields.contains(field) && !FieldFactory.isInternalField(field)) {
                Optional<String> originalString = entry.getField(field);
                entry.clearField(field);
                compoundEdit.addEdit(new UndoableFieldChange(entry, field, originalString.orElse(null), null));
                edited = true;
            }
        }

        if (edited) {
            compoundEdit.end();
            undoManager.addEdit(compoundEdit);
            notificationService.notify(Localization.lang("Updated entry with info from web sources"));
        } else {
            notificationService.notify(Localization.lang("No information added"));
        }
    }
}
