package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;
import java.util.SequencedSet;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.CitationFetcher;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;

public class CitationsRelationsTabViewModel {

    private final BibDatabaseContext databaseContext;
    private final GuiPreferences preferences;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;

    public CitationsRelationsTabViewModel(BibDatabaseContext databaseContext, GuiPreferences preferences, UndoManager undoManager, StateManager stateManager, DialogService dialogService, FileUpdateMonitor fileUpdateMonitor, TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.preferences = preferences;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
    }

    public void importEntries(List<CitationRelationItem> entriesToImport, CitationFetcher.SearchType searchType, BibEntry existingEntry) {
        List<BibEntry> entries = entriesToImport.stream()
                                                .map(CitationRelationItem::entry)
                                                // We need to have a clone of the entry, because we add the entry to the library (and keep it in the citation relation tab, too)
                                                .map(entry -> (BibEntry) entry.clone())
                                                .toList();

        ImportHandler importHandler = new ImportHandler(
                databaseContext,
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);
        CitationKeyGenerator generator = new CitationKeyGenerator(databaseContext, preferences.getCitationKeyPatternPreferences());
        boolean generateNewKeyOnImport = preferences.getImporterPreferences().generateNewKeyOnImportProperty().get();

        switch (searchType) {
            case CITES -> importCites(entries, existingEntry, importHandler, generator, generateNewKeyOnImport);
            case CITED_BY -> importCitedBy(entries, existingEntry, importHandler, generator, generateNewKeyOnImport);
        }
    }

    private void importCites(List<BibEntry> entries, BibEntry existingEntry, ImportHandler importHandler, CitationKeyGenerator generator, boolean generateNewKeyOnImport) {
        SequencedSet<String> citeKeys = existingEntry.getCites();

        for (BibEntry entryToCite : entries) {
            if (generateNewKeyOnImport || entryToCite.getCitationKey().isEmpty()) {
                String key = generator.generateKey(entryToCite);
                entryToCite.setCitationKey(key);
            }
            citeKeys.add(entryToCite.getCitationKey().get());
        }

        existingEntry.setCites(citeKeys);
        importHandler.importEntries(entries);
    }

    /**
     * "cited by" is the opposite of "cites", but not stored in field `CITED_BY`, but in the `CITES` field of the citing entry.
     * <p>
     * Therefore, some special handling is needed
     */
    private void importCitedBy(List<BibEntry> entries, BibEntry existingEntry, ImportHandler importHandler, CitationKeyGenerator generator, boolean generateNewKeyOnImport) {
        if (existingEntry.getCitationKey().isEmpty()) {
            if (!generateNewKeyOnImport) {
                dialogService.notify(Localization.lang("No citation key for %0", existingEntry.getAuthorTitleYear()));
                return;
            }
            existingEntry.setCitationKey(generator.generateKey(existingEntry));
        }
        String citationKey = existingEntry.getCitationKey().get();

        for (BibEntry citingEntry : entries) {
            SequencedSet<String> existingCites = citingEntry.getCites();
            existingCites.add(citationKey);
            citingEntry.setCites(existingCites);
        }

        importHandler.importEntries(entries);
    }
}
