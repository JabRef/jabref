package org.jabref.gui.mergeentries;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.undo.ChangeRecorder;
import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.logic.bibtex.comparator.plausibility.PlausibilityComparatorFactory;
import org.jabref.model.FieldChange;
import org.jabref.model.change.UndoableChangeType;
import org.jabref.model.change.UndoableFieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Helper class for merging bibliography entries with undo support.
/// Source entry data is merged into the library entry, with longer field values preferred
/// and obsolete fields removed.
public final class MergeEntriesHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeEntriesHelper.class);

    private MergeEntriesHelper() {
    }

    /// Merges two BibEntry objects with undo support.
    ///
    /// @param entryFromFetcher The entry containing new information (source, from the fetcher)
    /// @param entryFromLibrary The entry to be updated (target, from the library)
    /// @param changeRecorder   Compound edit to collect undo information
    /// @param keywordSeparator Separator character used for union-merging the groups field
    public static boolean mergeEntries(BibEntry entryFromFetcher, BibEntry entryFromLibrary, ChangeRecorder changeRecorder, char keywordSeparator) {
        LOGGER.debug("Entry from fetcher: {}", entryFromFetcher);
        LOGGER.debug("Entry from library: {}", entryFromLibrary);

        boolean typeChanged = mergeEntryType(entryFromFetcher, entryFromLibrary, changeRecorder);
        boolean fieldsChanged = mergeFields(entryFromFetcher, entryFromLibrary, changeRecorder, keywordSeparator);
        boolean fieldsRemoved = removeFieldsNotPresentInFetcher(entryFromFetcher, entryFromLibrary, changeRecorder);
        boolean citationKeyChanged = mergeCitationKey(entryFromFetcher, entryFromLibrary, changeRecorder);

        return typeChanged || fieldsChanged || fieldsRemoved || citationKeyChanged;
    }

    /// Adopts the fetcher-provided citation key (e.g. an INSPIRE texkey) onto the library entry,
    /// but only if the library entry doesn't already have one — an existing, possibly user-chosen
    /// key is never overwritten by this merge.
    private static boolean mergeCitationKey(BibEntry entryFromFetcher, BibEntry entryFromLibrary, ChangeRecorder changeRecorder) {
        if (entryFromLibrary.getCitationKey().isPresent()) {
            return false;
        }

        return entryFromFetcher.getCitationKey()
                               .filter(key -> !key.isBlank())
                               .map(key -> {
                                   LOGGER.debug("Adopting citation key from fetcher: {}", key);
                                   Optional<FieldChange> change = entryFromLibrary.setCitationKey(key);
                                   change.ifPresent(fieldChange -> changeRecorder.record(new UndoableFieldChange(fieldChange)));
                                   return true;
                               })
                               .orElse(false);
    }

    private static boolean mergeEntryType(BibEntry entryFromFetcher, BibEntry entryFromLibrary, ChangeRecorder changeRecorder) {
        EntryType fetcherType = entryFromFetcher.getType();
        EntryType libraryType = entryFromLibrary.getType();

        if (!libraryType.equals(fetcherType)) {
            LOGGER.debug("Updating type {} -> {}", libraryType, fetcherType);
            entryFromLibrary.setType(fetcherType);
            changeRecorder.record(new UndoableChangeType(entryFromLibrary, libraryType, fetcherType));
            return true;
        }
        return false;
    }

    private static boolean mergeFields(BibEntry entryFromFetcher, BibEntry entryFromLibrary, ChangeRecorder changeRecorder, char keywordSeparator) {
        Set<Field> allFields = new LinkedHashSet<>();
        allFields.addAll(entryFromFetcher.getFields());
        allFields.addAll(entryFromLibrary.getFields());

        boolean anyFieldsChanged = false;

        for (Field field : allFields) {
            Optional<String> fetcherValue = entryFromFetcher.getField(field);
            Optional<String> libraryValue = entryFromLibrary.getField(field);

            if (field == StandardField.GROUPS && fetcherValue.isPresent()) {
                // Always union-merge groups so no source group is ever lost
                String merged = KeywordList.merge(libraryValue.orElse(""), fetcherValue.get(), keywordSeparator)
                                           .getAsString(keywordSeparator);
                if (!merged.equals(libraryValue.orElse(""))) {
                    LOGGER.debug("Union-merging groups: {} + {} -> {}", libraryValue.orElse(""), fetcherValue.get(), merged);
                    entryFromLibrary.setField(field, merged);
                    changeRecorder.record(new UndoableFieldChange(entryFromLibrary, field, libraryValue.orElse(null), merged));
                    anyFieldsChanged = true;
                }
            } else if (fetcherValue.isPresent() && shouldUpdateField(field, fetcherValue.get(), libraryValue)) {
                LOGGER.debug("Updating field {}: {} -> {}", field, libraryValue.orElse(null), fetcherValue.get());
                entryFromLibrary.setField(field, fetcherValue.get());
                changeRecorder.record(new UndoableFieldChange(entryFromLibrary, field, libraryValue.orElse(null), fetcherValue.get()));
                anyFieldsChanged = true;
            }
        }
        return anyFieldsChanged;
    }

    private static boolean removeFieldsNotPresentInFetcher(BibEntry entryFromFetcher, BibEntry entryFromLibrary, ChangeRecorder changeRecorder) {
        Set<Field> obsoleteFields = new LinkedHashSet<>(entryFromLibrary.getFields());
        obsoleteFields.removeAll(entryFromFetcher.getFields());

        boolean anyFieldsRemoved = false;

        for (Field field : obsoleteFields) {
            if (FieldFactory.isInternalField(field) || field == StandardField.GROUPS) {
                continue;
            }

            Optional<String> value = entryFromLibrary.getField(field);
            if (value.isPresent()) {
                LOGGER.debug("Removing obsolete field {} with value {}", field, value.get());
                entryFromLibrary.clearField(field);
                changeRecorder.record(new UndoableFieldChange(entryFromLibrary, field, value.get(), null));
                anyFieldsRemoved = true;
            }
        }
        return anyFieldsRemoved;
    }

    private static boolean shouldUpdateField(Field field, String fetcherValue, Optional<String> libraryValue) {
        if (libraryValue.isEmpty()) {
            return true;
        }

        return PlausibilityComparatorFactory.INSTANCE.getPlausibilityComparator(field)
                                                     .map(comparator -> comparator.compare(fetcherValue, libraryValue.get()))
                                                     .filter(result -> result == ComparisonResult.LEFT_BETTER)
                                                     .map(_ -> true)
                                                     .orElse(false);
    }
}
