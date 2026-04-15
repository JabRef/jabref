package org.jabref.gui.mergeentries;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;

public class UpdateWithBibliographicInformationByWebFetchers extends SimpleCommand {

    private final DialogService dialogService;
    private final GuiPreferences guiPreferences;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;

    public UpdateWithBibliographicInformationByWebFetchers(DialogService dialogService,
                                                           GuiPreferences preferences,
                                                           StateManager stateManager,
                                                           TaskExecutor taskExecutor,
                                                           UndoManager undoManager) {
        this.dialogService = dialogService;
        this.guiPreferences = preferences;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(1, stateManager));
    }

    @Override
    public void execute() {
        assert stateManager.getActiveDatabase().isPresent();

        BibEntry originalEntry = stateManager.getSelectedEntries().getFirst();

        MultiMergeEntriesView mergedEntriesView = new MultiMergeEntriesView(guiPreferences, taskExecutor);
        mergedEntriesView.addSource(Localization.lang("Original Entry"), () -> originalEntry);

        Set<EntryBasedFetcher> webFetchers = WebFetchers.getEntryBasedFetchers(
                guiPreferences.getImporterPreferences(),
                guiPreferences.getImportFormatPreferences(),
                guiPreferences.getFilePreferences(),
                stateManager.getActiveDatabase().get()
        );

        for (EntryBasedFetcher webFetcher : webFetchers) {
            mergedEntriesView.addSource(webFetcher.getName(), () -> {
                try {
                    return webFetcher.performSearch(originalEntry).stream().findFirst().orElse(null);
                } catch (FetcherException e) {
                    return null;
                }
            });
        }

        Optional<BibEntry> mergedEntry = dialogService.showCustomDialogAndWait(mergedEntriesView);

        if (mergedEntry.isPresent()) {
            NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Merge entry with information"));

            // Updated the original entry with the new fields
            Set<Field> jointFields = new TreeSet<>(Comparator.comparing(Field::getName));
            jointFields.addAll(mergedEntry.get().getFields());
            Set<Field> originalFields = new TreeSet<>(Comparator.comparing(Field::getName));
            originalFields.addAll(originalEntry.getFields());
            boolean edited = false;

            // entry type
            EntryType oldType = originalEntry.getType();
            EntryType newType = mergedEntry.get().getType();

            if (!oldType.equals(newType)) {
                originalEntry.setType(newType);
                compoundEdit.addEdit(new UndoableChangeType(originalEntry, oldType, newType));
                edited = true;
            }

            // fields
            for (Field field : jointFields) {
                Optional<String> originalString = originalEntry.getField(field);
                Optional<String> mergedString = mergedEntry.get().getField(field);
                if (originalString.isEmpty() || !originalString.equals(mergedString)) {
                    originalEntry.setField(field, mergedString.get()); // mergedString always present
                    compoundEdit.addEdit(new UndoableFieldChange(originalEntry, field, originalString.orElse(null),
                            mergedString.get()));
                    edited = true;
                }
            }

            // Remove fields which are not in the merged entry, unless they are internal fields
            for (Field field : originalFields) {
                if (!jointFields.contains(field) && !FieldFactory.isInternalField(field)) {
                    Optional<String> originalString = originalEntry.getField(field);
                    originalEntry.clearField(field);
                    compoundEdit.addEdit(new UndoableFieldChange(originalEntry, field, originalString.get(), null)); // originalString always present
                    edited = true;
                }
            }

            if (edited) {
                compoundEdit.end();
                undoManager.addEdit(compoundEdit);
                dialogService.notify(Localization.lang("Updated entry with merged information from multiple sources"));
            } else {
                dialogService.notify(Localization.lang("No information added"));
            }
        } else {
            dialogService.notify(Localization.lang("Canceled merging entries"));
        }
    }
}
