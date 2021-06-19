package org.jabref.gui.citationkeypattern;

import java.util.List;
import java.util.function.Consumer;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class GenerateCitationKeyAction extends SimpleCommand {

    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final StateManager stateManager;

    private List<BibEntry> entries;
    private boolean isCanceled;

    private final TaskExecutor taskExecutor;
    private final PreferencesService preferencesService;

    public GenerateCitationKeyAction(JabRefFrame frame, DialogService dialogService, StateManager stateManager, TaskExecutor taskExecutor, PreferencesService preferencesService) {
        this.frame = frame;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        entries = stateManager.getSelectedEntries();

        if (entries.isEmpty()) {
            dialogService.showWarningDialogAndWait(Localization.lang("Autogenerate citation keys"),
                    Localization.lang("First select the entries you want keys to be generated for."));
            return;
        }
        dialogService.notify(formatOutputMessage(Localization.lang("Generating citation key for"), entries.size()));

        checkOverwriteKeysChosen();

        if (!this.isCanceled) {
            BackgroundTask backgroundTask = this.generateKeysInBackground();
            backgroundTask.showToUser(true);
            backgroundTask.titleProperty().set(Localization.lang("Autogenerate citation keys"));
            backgroundTask.messageProperty().set(Localization.lang("%0/%1 entries", 0, entries.size()));

            backgroundTask.executeWith(this.taskExecutor);
        }
    }

    public static boolean confirmOverwriteKeys(DialogService dialogService, PreferencesService preferencesService) {
        if (preferencesService.getCitationKeyPatternPreferences().shouldWarnBeforeOverwriteCiteKey()) {
            return dialogService.showConfirmationDialogWithOptOutAndWait(
                    Localization.lang("Overwrite keys"),
                    Localization.lang("One or more keys will be overwritten. Continue?"),
                    Localization.lang("Overwrite keys"),
                    Localization.lang("Cancel"),
                    Localization.lang("Do not ask again"),
                    optOut -> preferencesService.storeCitationKeyPatternPreferences(
                            preferencesService.getCitationKeyPatternPreferences().withWarnBeforeOverwriteCiteKey(!optOut)));
        } else {
            // Always overwrite keys by default
            return true;
        }
    }

    private void checkOverwriteKeysChosen() {
        // We don't want to generate keys for entries which already have one thus remove the entries
        if (this.preferencesService.getCitationKeyPatternPreferences().shouldAvoidOverwriteCiteKey()) {
            entries.removeIf(BibEntry::hasCitationKey);
            // if we're going to override some citation keys warn the user about it
        } else if (entries.parallelStream().anyMatch(BibEntry::hasCitationKey)) {
            boolean overwriteKeys = confirmOverwriteKeys(dialogService, this.preferencesService);

            // The user doesn't want to override citation keys
            if (!overwriteKeys) {
                isCanceled = true;
            }
        }
    }

    private BackgroundTask generateKeysInBackground() {
        return new BackgroundTask<Void>() {

            private NamedCompound compound;

            @Override
            protected Void call() {
                    if (isCanceled) {
                        return null;
                    }
                    DefaultTaskExecutor.runInJavaFXThread(() -> {
                        updateProgress(0, entries.size());
                        messageProperty().set(Localization.lang("%0/%1 entries", 0, entries.size()));
                    });
                    stateManager.getActiveDatabase().ifPresent(databaseContext -> {
                        // generate the new citation keys for each entry
                        compound = new NamedCompound(Localization.lang("Autogenerate citation keys"));
                        CitationKeyGenerator keyGenerator =
                                new CitationKeyGenerator(databaseContext, preferencesService.getCitationKeyPatternPreferences());
                        int entriesDone = 0;
                        for (BibEntry entry : entries) {
                            keyGenerator.generateAndSetKey(entry)
                                        .ifPresent(fieldChange -> compound.addEdit(new UndoableKeyChange(fieldChange)));
                            entriesDone++;
                            int finalEntriesDone = entriesDone;
                            DefaultTaskExecutor.runInJavaFXThread(() -> {
                                updateProgress(finalEntriesDone, entries.size());
                                messageProperty().set(Localization.lang("%0/%1 entries", finalEntriesDone, entries.size()));
                            });
                        }
                        compound.end();
                    });
                    return null;
            }

            @Override
            public BackgroundTask<Void> onSuccess(Consumer<Void> onSuccess) {
                // register the undo event only if new citation keys were generated
                if (compound.hasEdits()) {
                    frame.getUndoManager().addEdit(compound);
                }

                frame.getCurrentLibraryTab().markBaseChanged();
                dialogService.notify(formatOutputMessage(Localization.lang("Generated citation key for"), entries.size()));
                return super.onSuccess(onSuccess);
            }
        };

    }

    private String formatOutputMessage(String start, int count) {
        return String.format("%s %d %s.", start, count,
                (count > 1 ? Localization.lang("entries") : Localization.lang("entry")));
    }
}
