package org.jabref.gui.citationkeypattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableFieldChange;

public class GenerateCitationKeyAction extends SimpleCommand {

    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final StateManager stateManager;

    private List<BibEntry> entries;
    private boolean isCanceled;

    private final TaskExecutor taskExecutor;
    private final CliPreferences preferences;

    public GenerateCitationKeyAction(Supplier<LibraryTab> tabSupplier,
                                     DialogService dialogService,
                                     StateManager stateManager,
                                     TaskExecutor taskExecutor,
                                     CliPreferences preferences) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        entries = new ArrayList<>(stateManager.getSelectedEntries());
        stateManager.getActiveDatabase().ifPresent(this::generateKeysIn);
    }

    private void generateKeysIn(BibDatabaseContext databaseContext) {
        if (entries.isEmpty()) {
            dialogService.showWarningDialogAndWait(Localization.lang("Autogenerate citation keys"),
                    Localization.lang("First select the entries you want keys to be generated for."));
            return;
        }
        dialogService.notify(formatOutputMessage(Localization.lang("Generating citation key for"), entries.size()));

        checkOverwriteKeysChosen();

        if (!this.isCanceled) {
            BackgroundTask<Void> backgroundTask = this.generateKeysInBackground(databaseContext);
            backgroundTask.showToUser(true);
            backgroundTask.titleProperty().set(Localization.lang("Autogenerate citation keys"));
            backgroundTask.messageProperty().set(Localization.lang("%0/%1 entries", 0, entries.size()));

            backgroundTask.executeWith(this.taskExecutor);
        }
    }

    public static boolean confirmOverwriteKeys(DialogService dialogService, CliPreferences preferences) {
        if (preferences.getCitationKeyPatternPreferences().shouldWarnBeforeOverwriteCiteKey()) {
            return dialogService.showConfirmationDialogWithOptOutAndWait(
                    Localization.lang("Overwrite keys"),
                    Localization.lang("One or more keys will be overwritten. Continue?"),
                    Localization.lang("Overwrite keys"),
                    Localization.lang("Cancel"),
                    Localization.lang("Do not ask again"),
                    optOut -> preferences.getCitationKeyPatternPreferences().setWarnBeforeOverwriteCiteKey(!optOut));
        } else {
            // Always overwrite keys by default
            return true;
        }
    }

    private void checkOverwriteKeysChosen() {
        // We don't want to generate keys for entries which already have one thus remove the entries
        if (this.preferences.getCitationKeyPatternPreferences().shouldAvoidOverwriteCiteKey()) {
            entries.removeIf(BibEntry::hasCitationKey);
            // if we're going to override some citation keys warn the user about it
        } else if (entries.parallelStream().anyMatch(BibEntry::hasCitationKey)) {
            boolean overwriteKeys = confirmOverwriteKeys(dialogService, this.preferences);

            // The user doesn't want to override citation keys
            if (!overwriteKeys) {
                isCanceled = true;
            }
        }
    }

    private BackgroundTask<Void> generateKeysInBackground(BibDatabaseContext databaseContext) {
        // Taken here, while the library is certainly open: asking for a journal once it has closed
        // would create one nothing can reach.
        UndoManager undoManager = stateManager.getUndoManager(databaseContext);

        return new BackgroundTask<>() {
            private final CompoundEdit compound = new CompoundEdit(StandardActions.GENERATE_CITE_KEYS.getText());

            @Override
            public Void call() {
                if (isCanceled) {
                    return null;
                }
                UiTaskExecutor.runInJavaFXThread(() -> {
                    updateProgress(0, entries.size());
                    messageProperty().set(Localization.lang("%0/%1 entries", 0, entries.size()));
                });
                // generate the new citation keys for each entry
                CitationKeyGenerator keyGenerator =
                        new CitationKeyGenerator(databaseContext, preferences.getCitationKeyPatternPreferences());
                int entriesDone = 0;
                for (BibEntry entry : entries) {
                    String newKey = keyGenerator.generateKey(entry);
                    // Set the key on the FX thread, since BibEntry uses ObservableMap which fires FX listeners
                    Optional<FieldChange> fieldChange = UiTaskExecutor.runInJavaFXThread(() -> entry.setCitationKey(newKey));
                    if (fieldChange != null) {
                        fieldChange.ifPresent(change -> compound.addEdit(new UndoableFieldChange(change)));
                    }
                    entriesDone++;
                    int finalEntriesDone = entriesDone;
                    UiTaskExecutor.runInJavaFXThread(() -> {
                        updateProgress(finalEntriesDone, entries.size());
                        messageProperty().set(Localization.lang("%0/%1 entries", finalEntriesDone, entries.size()));
                    });
                }
                return null;
            }

            @Override
            public BackgroundTask<Void> onSuccess(Consumer<Void> onSuccess) {
                // register the undo event only if new citation keys were generated
                if (compound.hasEdits()) {
                    undoManager.addEdit(compound.toChangeSet());
                }

                tabSupplier.get().markBaseChanged();
                dialogService.notify(formatOutputMessage(Localization.lang("Generated citation key for"), entries.size()));
                return super.onSuccess(onSuccess);
            }
        };
    }

    private String formatOutputMessage(String start, int count) {
        return "%s %d %s.".formatted(start, count,
                count > 1 ? Localization.lang("entries") : Localization.lang("entry"));
    }
}
