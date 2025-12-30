package org.jabref.logic.quality.consistency;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.comparator.BibEntryByCitationKeyComparator;
import org.jabref.logic.bibtex.comparator.BibEntryByFieldsComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.EntryType;

import com.google.common.annotations.VisibleForTesting;

public class BibliographyConsistencyCheck {

    private static final Set<Field> EXPLICITLY_EXCLUDED_FIELDS = Set.of(
            InternalField.KEY_FIELD, // Citation key
            StandardField.KEY,
            StandardField.COMMENT,
            StandardField.CROSSREF,
            StandardField.CITES,
            StandardField.PDF,
            StandardField.REVIEW,
            StandardField.SORTKEY,
            StandardField.SORTNAME,
            StandardField.TYPE,
            StandardField.XREF,

            // JabRef-specific
            StandardField.GROUPS,
            StandardField.OWNER,
            StandardField.CITATIONCOUNT,
            StandardField.TIMESTAMP,
            StandardField.CREATIONDATE,
            StandardField.MODIFICATIONDATE
    );

    private static Set<Field> filterExcludedFields(Collection<Field> fields) {
        return fields.stream()
                     .filter(field -> !EXPLICITLY_EXCLUDED_FIELDS.contains(field))
                     .filter(field -> !StandardField.AUTOMATIC_FIELDS.contains(field))
                     .filter(field -> !(field instanceof SpecialField))
                     .filter(field -> !(field instanceof UserSpecificCommentField))
                     .collect(Collectors.toSet());
    }

    /// Filters the given entries to those that violate consistency:
    ///
    /// - Fields not set (but set in other entries of the same type)
    /// - Required fields not set
    ///
    /// Additionally, the entries are sorted
    @VisibleForTesting
    List<BibEntry> filterAndSortEntriesWithFieldDifferences(Set<BibEntry> entries, Set<Field> differingFields, Set<OrFields> requiredFields) {
        return entries.stream()
                      .filter(entry ->
                              // Handles violation: this entry is inconsistent because it sets a field
                              // that not every other entry of its type sets (differing field)
                              !Collections.disjoint(filterExcludedFields(entry.getFields()), differingFields)
                                      // Handles violation: this entry is inconsistent because it does not set fields
                                      // for its type that specify required fields
                                      || (requiredFields.stream()
                                                        .map(OrFields::getFields)
                                                        .anyMatch(subfields ->
                                                                Collections.disjoint(subfields, entry.getFields())
                                                        )
                              )
                      )
                      .sorted(new FieldComparatorStack<>(List.of(
                              new BibEntryByCitationKeyComparator(),
                              new BibEntryByFieldsComparator()
                      )))
                      .toList();
    }

    public record Result(Map<EntryType, EntryTypeResult> entryTypeToResultMap) {
    }

    public record EntryTypeResult(Collection<Field> fields, SequencedCollection<BibEntry> sortedEntries) {
    }

    /**
     * Checks the consistency of the given entries by looking at the present and absent fields.
     * <p>
     * Computation takes place grouped by each entryType.
     * Computes the fields set in all entries. In case entries of the same type has more fields defined, it is output.
     * <p>
     * This class <em>does not</em> check whether all required fields are present or if the fields are valid for the entry type.
     * That result can a) be retrieved by using the JabRef UI and b) by checking the CSV output of {@link BibliographyConsistencyCheckResultCsvWriter#writeFindings}
     *
     * @implNote This class does not implement {@link org.jabref.logic.integrity.DatabaseChecker}, because it returns a list of {@link org.jabref.logic.integrity.IntegrityMessage}, which are too fine-grained.
     */
    public Result check(BibDatabaseContext bibContext, BibEntryTypesManager bibEntryTypesManager, BiConsumer<Integer, Integer> entriesGroupingProgress) {
        // collects fields existing in any entry, scoped by entry type
        Map<EntryType, Set<Field>> entryTypeToFieldsInAnyEntryMap = new HashMap<>();
        // collects fields existing in all entries, scoped by entry type
        Map<EntryType, Set<Field>> entryTypeToFieldsInAllEntriesMap = new HashMap<>();
        // collects entries of the same type
        Map<EntryType, Set<BibEntry>> entryTypeToEntriesMap = new HashMap<>();

        collectEntriesIntoMaps(bibContext, entryTypeToFieldsInAnyEntryMap, entryTypeToFieldsInAllEntriesMap, entryTypeToEntriesMap);

        List<BibEntryType> entryTypeDefinitions = bibEntryTypesManager.getAllTypes(bibContext.getMode()).stream().toList();

        // Use LinkedHashMap to preserve the order of Bib(tex|latex)EntryTypeDefinitions.ALL
        Map<EntryType, EntryTypeResult> resultMap = new LinkedHashMap<>();

        int counter = 0;
        for (Map.Entry<EntryType, Set<Field>> mapEntry : entryTypeToFieldsInAnyEntryMap.entrySet()) {
            entriesGroupingProgress.accept(counter++, entryTypeToFieldsInAnyEntryMap.size());
            EntryType entryType = mapEntry.getKey();
            Set<Field> fieldsInAnyEntry = mapEntry.getValue();
            Set<Field> fieldsInAllEntries = entryTypeToFieldsInAllEntriesMap.get(entryType);
            Set<Field> filteredFieldsInAnyEntry = filterExcludedFields(fieldsInAnyEntry);

            Set<Field> differingFields = new HashSet<>(filteredFieldsInAnyEntry);
            differingFields.removeAll(fieldsInAllEntries);
            assert fieldsInAllEntries != null;

            Optional<BibEntryType> typeDefOpt = entryTypeDefinitions.stream()
                                                                    .filter(def -> def.getType().equals(entryType))
                                                                    .findFirst();

            Set<OrFields> requiredFields = typeDefOpt.map(typeDef ->
                    new HashSet<>(typeDef.getRequiredFields())
            ).orElse(new HashSet<>());

            Set<BibEntry> entries = entryTypeToEntriesMap.get(entryType);
            assert entries != null;

            List<BibEntry> sortedEntries = filterAndSortEntriesWithFieldDifferences(entries, differingFields, requiredFields);
            if (!sortedEntries.isEmpty()) {
                resultMap.put(entryType, new EntryTypeResult(differingFields, sortedEntries));
            }
        }

        return new Result(resultMap);
    }

    private static void collectEntriesIntoMaps(BibDatabaseContext bibContext, Map<EntryType, Set<Field>> entryTypeToFieldsInAnyEntryMap, Map<EntryType, Set<Field>> entryTypeToFieldsInAllEntriesMap, Map<EntryType, Set<BibEntry>> entryTypeToEntriesMap) {
        BibDatabaseMode mode = bibContext.getMode();
        List<BibEntry> entries = bibContext.getEntries();

        for (BibEntry entry : entries) {
            EntryType entryType = entry.getType();

            Set<Field> filteredFields = filterExcludedFields(entry.getFields());

            entryTypeToFieldsInAllEntriesMap
                    .computeIfAbsent(entryType, _ -> new HashSet<>(filteredFields))
                    .retainAll(filteredFields);

            entryTypeToFieldsInAnyEntryMap
                    .computeIfAbsent(entryType, _ -> new HashSet<>())
                    .addAll(filteredFields);

            entryTypeToEntriesMap
                    .computeIfAbsent(entryType, _ -> new HashSet<>())
                    .add(entry);
        }
    }
}
