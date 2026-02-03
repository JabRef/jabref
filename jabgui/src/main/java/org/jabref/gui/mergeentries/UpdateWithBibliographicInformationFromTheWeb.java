package org.jabref.gui.mergeentries;

import java.util.SortedSet;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;

public class UpdateWithBibliographicInformationFromTheWeb extends SimpleCommand {

    private final DialogService dialogService;
    private final GuiPreferences guiPreferences;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;

    public UpdateWithBibliographicInformationFromTheWeb(DialogService dialogService,
                                                        GuiPreferences preferences,
                                                        StateManager stateManager,
                                                        TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.guiPreferences = preferences;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;

        this.executable.bind(ActionHelper.needsEntriesSelected(1, stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        BibEntry originalEntry = stateManager.getSelectedEntries().getFirst();
        SortedSet<EntryBasedFetcher> webFetchers = WebFetchers.getEntryBasedFetchers(
                guiPreferences.getImporterPreferences(),
                guiPreferences.getImportFormatPreferences(),
                guiPreferences.getFilePreferences(),
                stateManager.getActiveDatabase().get()
        );
        MultiMergeEntriesView mergedEntriesView = new MultiMergeEntriesView(guiPreferences, taskExecutor);

        mergedEntriesView.addSource(Localization.lang("Original Entry"), () -> originalEntry);

        for (EntryBasedFetcher webFetcher : webFetchers) {
            mergedEntriesView.addSource(webFetcher.getName(), () -> {
                try {
                    return webFetcher.performSearch(originalEntry).stream().findFirst().orElse(null);
                } catch (FetcherException e) {
                    return null;
                }
            });
        }

        dialogService.showCustomDialogAndWait(mergedEntriesView);
    }
}
