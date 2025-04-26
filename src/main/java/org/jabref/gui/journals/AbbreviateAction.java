package org.jabref.gui.journals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.journals.JournalAbbreviationRepositoryManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts journal full names to either iso or medline abbreviations for all selected entries.
 */
public class AbbreviateAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviateAction.class);

    private final StandardActions action;
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;
    private JournalAbbreviationRepository abbreviationRepository;
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;

    private AbbreviationType abbreviationType;

    public AbbreviateAction(StandardActions action,
                            Supplier<LibraryTab> tabSupplier,
                            DialogService dialogService,
                            StateManager stateManager,
                            JournalAbbreviationPreferences abbreviationPreferences,
                            TaskExecutor taskExecutor,
                            UndoManager undoManager) {
        this.action = action;
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.journalAbbreviationPreferences = abbreviationPreferences;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;

        switch (action) {
            case ABBREVIATE_DEFAULT ->
                    abbreviationType = AbbreviationType.DEFAULT;
            case ABBREVIATE_DOTLESS ->
                    abbreviationType = AbbreviationType.DOTLESS;
            case ABBREVIATE_SHORTEST_UNIQUE ->
                    abbreviationType = AbbreviationType.SHORTEST_UNIQUE;
            case ABBREVIATE_LTWA ->
                    abbreviationType = AbbreviationType.LTWA;
            default ->
                    LOGGER.debug("Unknown action: {}", action.name());
        }

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        if (action == StandardActions.UNABBREVIATE && !areAnyJournalSourcesEnabled()) {
            dialogService.notify(Localization.lang("Cannot unabbreviate: all journal lists are disabled."));
            return;
        }

        if ((action == StandardActions.ABBREVIATE_DEFAULT)
                || (action == StandardActions.ABBREVIATE_DOTLESS)
                || (action == StandardActions.ABBREVIATE_SHORTEST_UNIQUE)
                || (action == StandardActions.ABBREVIATE_LTWA)) {
            dialogService.notify(Localization.lang("Abbreviating..."));
            stateManager.getActiveDatabase().ifPresent(_ ->
                    BackgroundTask.wrap(() -> abbreviate(stateManager.getActiveDatabase().get(), stateManager.getSelectedEntries()))
                                  .onSuccess(dialogService::notify)
                                  .executeWith(taskExecutor));
        } else if (action == StandardActions.UNABBREVIATE) {
            dialogService.notify(Localization.lang("Unabbreviating..."));
            stateManager.getActiveDatabase().ifPresent(_ ->
                    BackgroundTask.wrap(() -> unabbreviate(stateManager.getActiveDatabase().get(), stateManager.getSelectedEntries()))
                                  .onSuccess(dialogService::notify)
                                  .executeWith(taskExecutor));
        } else {
            LOGGER.debug("Unknown action: {}", action.name());
        }
    }

    private String abbreviate(BibDatabaseContext databaseContext, List<BibEntry> entries) {
        // Use the repository manager to get a cached repository or build a new one if needed
        abbreviationRepository = JournalAbbreviationRepositoryManager.getInstance()
                                .getRepository(journalAbbreviationPreferences);
                                
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(
                abbreviationRepository,
                abbreviationType,
                journalAbbreviationPreferences.shouldUseFJournalField());

        NamedCompound ce = new NamedCompound(Localization.lang("Abbreviate journal names"));

        int count = entries.stream().mapToInt(entry ->
                (int) FieldFactory.getJournalNameFields().stream().filter(journalField ->
                        undoableAbbreviator.abbreviate(databaseContext.getDatabase(), entry, journalField, ce)).count()).sum();

        if (count == 0) {
            return Localization.lang("No journal names could be abbreviated.");
        }

        ce.end();
        undoManager.addEdit(ce);
        tabSupplier.get().markBaseChanged();
        return Localization.lang("Abbreviated %0 journal names.", String.valueOf(count));
    }

    /**
     * Unabbreviate journal names in entries, respecting the enabled/disabled state of sources.
     * Only unabbreviates entries from enabled sources.
     */
    private String unabbreviate(BibDatabaseContext databaseContext, List<BibEntry> entries) {
        List<BibEntry> filteredEntries = new ArrayList<>();
        
        JournalAbbreviationRepository freshRepository = JournalAbbreviationRepositoryManager.getInstance()
                                                      .getRepository(journalAbbreviationPreferences);
        
        Map<String, Boolean> sourceEnabledStates = new HashMap<>();
        String builtInId = JournalAbbreviationRepository.BUILTIN_LIST_ID;
        sourceEnabledStates.put(builtInId, journalAbbreviationPreferences.isSourceEnabled(builtInId));
        
        for (String listPath : journalAbbreviationPreferences.getExternalJournalLists()) {
            if (listPath != null && !listPath.isBlank()) {
                String fileName = Path.of(listPath).getFileName().toString();
                sourceEnabledStates.put(fileName, journalAbbreviationPreferences.isSourceEnabled(fileName));
            }
        }
        
        var allAbbreviationsWithSources = freshRepository.getAllAbbreviationsWithSources();
        Map<String, List<JournalAbbreviationRepository.AbbreviationWithSource>> textToSourceMap = new HashMap<>();
        
        for (var abbrWithSource : allAbbreviationsWithSources) {
            Abbreviation abbr = abbrWithSource.getAbbreviation();
            addToMap(textToSourceMap, abbr.getName(), abbrWithSource);
            addToMap(textToSourceMap, abbr.getAbbreviation(), abbrWithSource);
            addToMap(textToSourceMap, abbr.getDotlessAbbreviation(), abbrWithSource);
            addToMap(textToSourceMap, abbr.getShortestUniqueAbbreviation(), abbrWithSource);
        }
        
        for (BibEntry entry : entries) {
            boolean includeEntry = true;
            
            for (Field journalField : FieldFactory.getJournalNameFields()) {
                if (!entry.hasField(journalField)) {
                    continue;
                }
                
                String text = entry.getFieldLatexFree(journalField).orElse("");
                List<JournalAbbreviationRepository.AbbreviationWithSource> possibleSources = 
                    textToSourceMap.getOrDefault(text, List.of());
                
                if (!possibleSources.isEmpty()) {
                    boolean allSourcesDisabled = true;
                    for (var abbrWithSource : possibleSources) {
                        String source = abbrWithSource.getSource();
                        if (sourceEnabledStates.getOrDefault(source, true)) {
                            allSourcesDisabled = false;
                            break;
                        }
                    }
                    
                    if (allSourcesDisabled) {
                        includeEntry = false;
                        break;
                    }
                }
            }
            
            if (includeEntry) {
                filteredEntries.add(entry);
            }
        }
        
        UndoableUnabbreviator undoableAbbreviator = new UndoableUnabbreviator(freshRepository);
        NamedCompound ce = new NamedCompound(Localization.lang("Unabbreviate journal names"));
        int count = filteredEntries.stream().mapToInt(entry ->
                (int) FieldFactory.getJournalNameFields().stream().filter(journalField ->
                        undoableAbbreviator.unabbreviate(databaseContext.getDatabase(), entry, journalField, ce)).count()).sum();
        
        if (count == 0) {
            return Localization.lang("No journal names could be unabbreviated.");
        }

        ce.end();
        undoManager.addEdit(ce);
        tabSupplier.get().markBaseChanged();
        return Localization.lang("Unabbreviated %0 journal names.", String.valueOf(count));
    }

    /**
     * Helper method to add an abbreviation to the text-to-source map
     *
     * @param map The map to add to
     * @param text The text to use as key
     * @param abbrWithSource The abbreviation with source to add
     */
    private void addToMap(Map<String, List<JournalAbbreviationRepository.AbbreviationWithSource>> map, 
                          String text, 
                          JournalAbbreviationRepository.AbbreviationWithSource abbrWithSource) {
        if (text == null || text.isBlank()) {
            return;
        }
        
        map.computeIfAbsent(text, k -> new ArrayList<>()).add(abbrWithSource);
    }

    /**
     * Checks if any journal abbreviation source is enabled in the preferences.
     * This includes both the built-in list and any external journal lists.
     *
     * @return true if at least one source is enabled, false if all sources are disabled
     */
    private boolean areAnyJournalSourcesEnabled() {
        boolean anySourceEnabled = journalAbbreviationPreferences.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID);
        
        if (!anySourceEnabled) {
            for (String listPath : journalAbbreviationPreferences.getExternalJournalLists()) {
                if (listPath != null && !listPath.isBlank()) {
                    // Just check the filename since that's what's used as the source key
                    String fileName = Path.of(listPath).getFileName().toString();
                    if (journalAbbreviationPreferences.isSourceEnabled(fileName)) {
                        anySourceEnabled = true;
                        break;
                    }
                }
            }
        }
        
        return anySourceEnabled;
    }
}
