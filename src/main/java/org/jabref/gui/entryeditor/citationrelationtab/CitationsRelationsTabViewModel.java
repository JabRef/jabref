package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.CitationFetcher;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class CitationsRelationsTabViewModel {

    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;

    public CitationsRelationsTabViewModel(BibDatabaseContext databaseContext, PreferencesService preferencesService, UndoManager undoManager, StateManager stateManager, DialogService dialogService, FileUpdateMonitor fileUpdateMonitor, TaskExecutor taskExecutor) {
        this.databaseContext = databaseContext;
        this.preferencesService = preferencesService;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
    }

    public void importEntries(List<CitationRelationItem> entriesToImport, CitationFetcher.SearchType searchType, BibEntry existingEntry) {
        List<BibEntry> entries = entriesToImport.stream().map(CitationRelationItem::entry).toList();

        ImportHandler importHandler = new ImportHandler(
                databaseContext,
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);

        switch (searchType) {
            case CITES -> importCites(entries, existingEntry, importHandler);
            case CITED_BY -> importCitedBy(entries, existingEntry, importHandler);
        }
    }

    private void importCites(List<BibEntry> entries, BibEntry existingEntry, ImportHandler importHandler) {
        CitationKeyPatternPreferences citationKeyPatternPreferences = preferencesService.getCitationKeyPatternPreferences();
        CitationKeyGenerator generator = new CitationKeyGenerator(databaseContext, citationKeyPatternPreferences);
        boolean generateNewKeyOnImport = preferencesService.getImporterPreferences().generateNewKeyOnImportProperty().get();

        List<String> citeKeys = getExistingEntriesFromCiteField(existingEntry);
        citeKeys.removeIf(String::isEmpty);
        for (BibEntry entryToCite : entries) {
            if (generateNewKeyOnImport || entryToCite.getCitationKey().isEmpty()) {
                String key = generator.generateKey(entryToCite);
                entryToCite.setCitationKey(key);
                addToKeyToList(citeKeys, key);
            } else {
                addToKeyToList(citeKeys, entryToCite.getCitationKey().get());
            }
        }
        existingEntry.setField(StandardField.CITES, toCommaSeparatedString(citeKeys));
        importHandler.importEntries(entries);
    }

    private void importCitedBy(List<BibEntry> entries, BibEntry existingEntry, ImportHandler importHandler) {
        CitationKeyPatternPreferences citationKeyPatternPreferences = preferencesService.getCitationKeyPatternPreferences();
        CitationKeyGenerator generator = new CitationKeyGenerator(databaseContext, citationKeyPatternPreferences);
        boolean generateNewKeyOnImport = preferencesService.getImporterPreferences().generateNewKeyOnImportProperty().get();

        for (BibEntry entryThatCitesOurExistingEntry : entries) {
            List<String> existingCites = getExistingEntriesFromCiteField(entryThatCitesOurExistingEntry);
            existingCites.removeIf(String::isEmpty);
            String key;
            if (generateNewKeyOnImport || entryThatCitesOurExistingEntry.getCitationKey().isEmpty()) {
                key = generator.generateKey(entryThatCitesOurExistingEntry);
                entryThatCitesOurExistingEntry.setCitationKey(key);
            } else {
                key = existingEntry.getCitationKey().get();
            }
            addToKeyToList(existingCites, key);
            entryThatCitesOurExistingEntry.setField(StandardField.CITES, toCommaSeparatedString(existingCites));
        }

        importHandler.importEntries(entries);
    }

    private void addToKeyToList(List<String> list, String key) {
        if (!list.contains(key)) {
            list.add(key);
        }
    }

    private List<String> getExistingEntriesFromCiteField(BibEntry entry) {
        return Arrays.stream(entry.getField(StandardField.CITES).orElse("").split(",")).collect(Collectors.toList());
    }

    private String toCommaSeparatedString(List<String> citeentries) {
        return String.join(",", citeentries);
    }
}
