package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.comparator.CrossRefEntryComparator;
import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.logic.bibtex.comparator.IdComparator;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import org.jspecify.annotations.NonNull;

public class BibDatabaseSaver {
    private final BibWriter bibWriter;
    private final SelfContainedSaveConfiguration saveConfiguration;
    private final List<FieldChange> saveActionsFieldChanges = new ArrayList<>();
    private final BibEntryTypesManager entryTypesManager;
    private final CliPreferences cliPreferences;

    public BibDatabaseSaver(BibWriter bibWriter, SelfContainedSaveConfiguration saveConfiguration, CliPreferences cliPreferences, BibEntryTypesManager entryTypesManager) {
        this.bibWriter = bibWriter;
        this.saveConfiguration = saveConfiguration;
        this.cliPreferences = cliPreferences;
        this.entryTypesManager = entryTypesManager;
    }

    public BibDatabaseSaver(@NonNull CliPreferences cliPreferences, @NonNull Writer writer, @NonNull BibDatabaseContext bibDatabaseContext) {
        this(new BibWriter(writer, bibDatabaseContext.getDatabase().getNewLineSeparator()),
                cliPreferences.getSelfContainedExportConfiguration(),
                cliPreferences,
                cliPreferences.getCustomEntryTypesRepository());
    }

    public void saveDatabase(@NonNull BibDatabaseContext bibDatabaseContext) throws IOException {
        List<BibEntry> entries = bibDatabaseContext.getDatabase().getEntries()
                                                   .stream()
                                                   .filter(entry -> !entry.isEmpty())
                                                   .toList();
        savePartOfDatabase(bibDatabaseContext, entries);
    }

    public void savePartOfDatabase(@NonNull BibDatabaseContext bibDatabaseContext, List<BibEntry> entries) throws IOException {
        List<BibEntry> sortedEntries = getSortedEntries(entries, saveConfiguration.getSelfContainedSaveOrder());

        JournalAbbreviationPreferences journalAbbreviationPreferences = cliPreferences.getJournalAbbreviationPreferences();
        SaveActionsWorker saveActionsWorker = new SaveActionsWorker(bibDatabaseContext, cliPreferences.getFilePreferences(), cliPreferences.getTimestampPreferences(), cliPreferences.getFieldPreferences(),
                journalAbbreviationPreferences.shouldUseFJournalField(), JournalAbbreviationLoader.loadRepository(journalAbbreviationPreferences));
        List<FieldChange> saveActionChanges = saveActionsWorker.applySaveActions(sortedEntries, bibDatabaseContext.getMetaData());
        saveActionsFieldChanges.addAll(saveActionChanges);

        CitationKeyPatternPreferences keyPatternPreferences = cliPreferences.getCitationKeyPatternPreferences();
        if (keyPatternPreferences.shouldGenerateCiteKeysBeforeSaving()) {
            List<FieldChange> keyChanges = generateCitationKeys(bibDatabaseContext, sortedEntries, keyPatternPreferences);
            saveActionsFieldChanges.addAll(keyChanges);
        }

        BibDatabaseWriter bibDatabaseWriter = new BibDatabaseWriter(bibWriter, saveConfiguration, cliPreferences.getFieldPreferences(), keyPatternPreferences, entryTypesManager);
        bibDatabaseWriter.writePartOfDatabase(bibDatabaseContext, sortedEntries);
    }

    private static List<Comparator<BibEntry>> getSaveComparators(SaveOrder saveOrder) {
        List<Comparator<BibEntry>> comparators = new ArrayList<>();

        // Take care, using CrossRefEntry-Comparator, that referred entries occur after referring
        // ones. This is a necessary requirement for BibTeX to be able to resolve referenced entries correctly.
        comparators.add(new CrossRefEntryComparator());

        if (saveOrder.getOrderType() == SaveOrder.OrderType.ORIGINAL) {
            // entries will be sorted based on their internal IDs
            comparators.add(new IdComparator());
        } else {
            // use configured sorting strategy
            List<FieldComparator> fieldComparators = saveOrder.getSortCriteria().stream()
                                                              .map(FieldComparator::new)
                                                              .toList();
            comparators.addAll(fieldComparators);
            comparators.add(new FieldComparator(InternalField.KEY_FIELD));
        }

        return comparators;
    }

    /// We have begun to use getSortedEntries() for both database save operations and non-database save operations. In a
    /// non-database save operation (such as the exportDatabase call), we do not wish to use the global preference of
    /// saving in standard order.
    public static List<BibEntry> getSortedEntries(@NonNull List<BibEntry> entriesToSort,
                                                  @NonNull SelfContainedSaveOrder saveOrder) {
        List<Comparator<BibEntry>> comparators = getSaveComparators(saveOrder);
        FieldComparatorStack<BibEntry> comparatorStack = new FieldComparatorStack<>(comparators);

        List<BibEntry> sorted = new ArrayList<>(entriesToSort);
        sorted.sort(comparatorStack);
        return sorted;
    }

    /// Generate keys for all entries that are lacking keys.
    private List<FieldChange> generateCitationKeys(BibDatabaseContext databaseContext, List<BibEntry> entries, CitationKeyPatternPreferences keyPatternPreferences) {
        List<FieldChange> changes = new ArrayList<>();
        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(databaseContext, keyPatternPreferences);
        for (BibEntry bes : entries) {
            Optional<String> oldKey = bes.getCitationKey();
            if (StringUtil.isBlank(oldKey)) {
                Optional<FieldChange> change = keyGenerator.generateAndSetKey(bes);
                change.ifPresent(changes::add);
            }
        }
        return changes;
    }

    public List<FieldChange> getSaveActionsFieldChanges() {
        return Collections.unmodifiableList(saveActionsFieldChanges);
    }
}
