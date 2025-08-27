package org.jabref.logic.quality.consistency;

import java.util.ArrayList;
import java.util.Collection;
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
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;

import com.google.common.annotations.VisibleForTesting;

public class BibliographyConsistencyCheck {

    private static final Set<EntryType> BIBLATEX_TYPES = BiblatexEntryTypeDefinitions.ALL.stream()
            .map(BibEntryType::getType)
            .collect(Collectors.toSet());

    private static final Set<EntryType> BIBTEX_TYPES = BibtexEntryTypeDefinitions.ALL.stream()
            .map(BibEntryType::getType)
            .collect(Collectors.toSet());

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

    /**
     *
     * @param entries entries to analyze
     * @param differingFields fields that are present in some entries but not in all
     * @param fieldsInAllEntries fields that are present in all entries
     * @return a list of entries that present at least one of the differing fields
     */
    @VisibleForTesting
    List<BibEntry> filterEntriesWithFieldDifferences(Set<BibEntry> entries, Set<Field> differingFields, Set<Field> fieldsInAllEntries) {
        for (Field field : differingFields) {
            if (!fieldsInAllEntries.contains(field)) {
                return new ArrayList<>(entries);
            }
        }
        List<BibEntry> filteredEntries = new ArrayList<>();
        for (BibEntry entry : entries) {
            Set<Field> entryFields = filterExcludedFields(entry.getFields());
            boolean hasDifferingField = differingFields.stream()
                                                       .anyMatch(entryFields::contains);
            if (hasDifferingField) {
                filteredEntries.add(entry);
            }
        }
        return filteredEntries;
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
    public Result check(BibDatabaseContext bibContext, BiConsumer<Integer, Integer> entriesGroupingProgress) {
        // collects fields existing in any entry, scoped by entry type
        Map<EntryType, Set<Field>> entryTypeToFieldsInAnyEntryMap = new HashMap<>();
        // collects fields existing in all entries, scoped by entry type
        Map<EntryType, Set<Field>> entryTypeToFieldsInAllEntriesMap = new HashMap<>();
        // collects entries of the same type
        Map<EntryType, Set<BibEntry>> entryTypeToEntriesMap = new HashMap<>();

        collectEntriesIntoMaps(bibContext, entryTypeToFieldsInAnyEntryMap, entryTypeToFieldsInAllEntriesMap, entryTypeToEntriesMap);

        Map<EntryType, EntryTypeResult> resultMap = new LinkedHashMap<>();
        BibDatabaseMode mode = bibContext.getMode();
        List<BibEntryType> entryTypeDefinitions = (mode == BibDatabaseMode.BIBLATEX)
                ? BiblatexEntryTypeDefinitions.ALL
                : BibtexEntryTypeDefinitions.ALL;

        int counter = 0;
        for (Map.Entry<EntryType, Set<Field>> mapEntry : entryTypeToFieldsInAnyEntryMap.entrySet()) {
            entriesGroupingProgress.accept(counter++, entryTypeToFieldsInAnyEntryMap.size());
            EntryType entryType = mapEntry.getKey();
            Set<Field> fieldsInAnyEntry = mapEntry.getValue();
            Set<Field> fieldsInAllEntries = entryTypeToFieldsInAllEntriesMap.get(entryType);
            Set<Field> filteredFieldsInAnyEntry = filterExcludedFields(fieldsInAnyEntry);
            Set<Field> filteredFieldsInAllEntries = filterExcludedFields(fieldsInAllEntries);

            Set<Field> differingFields = new HashSet<>(filteredFieldsInAnyEntry);
            differingFields.removeAll(filteredFieldsInAllEntries);
            assert fieldsInAllEntries != null;

            differingFields.removeAll(fieldsInAllEntries);

            Optional<BibEntryType> typeDefOpt = entryTypeDefinitions.stream()
                                                                 .filter(def -> def.getType().equals(entryType))
                                                                 .findFirst();

            Set<Field> requiredFields = typeDefOpt.map(typeDef ->
                        typeDef.getRequiredFields().stream()
                               .flatMap(orFields -> orFields.getFields().stream())
                               .collect(Collectors.toSet())
                ).orElse(Set.of());

            // This is the "real" difference we are looking for: fields that are present in some entries but not in all
            for (Field req : requiredFields) {
                if (filteredFieldsInAnyEntry.contains(req) && !filteredFieldsInAllEntries.contains(req)) {
                    differingFields.add(req);
                }
            }

            Set<BibEntry> entries = entryTypeToEntriesMap.get(entryType);
            assert entries != null;
            if (entries == null || entries.size() <= 1 || differingFields.isEmpty()) {
                continue;
            }

            List<BibEntry> sortedEntries = filterEntriesWithFieldDifferences(entries, differingFields, fieldsInAllEntries);

            if (!sortedEntries.isEmpty()) {
                sortedEntries.sort(new FieldComparatorStack<>(List.of(
                        new BibEntryByCitationKeyComparator(),
                        new BibEntryByFieldsComparator()
                )));
                resultMap.put(entryType, new EntryTypeResult(differingFields, sortedEntries));
            }
        }

        return new Result(resultMap);
    }

    private static void collectEntriesIntoMaps(BibDatabaseContext bibContext, Map<EntryType, Set<Field>> entryTypeToFieldsInAnyEntryMap, Map<EntryType, Set<Field>> entryTypeToFieldsInAllEntriesMap, Map<EntryType, Set<BibEntry>> entryTypeToEntriesMap) {
        BibDatabaseMode mode = bibContext.getMode();
        List<BibEntry> entries = bibContext.getEntries();

        Set<EntryType> typeSet = switch (mode) {
            case BIBLATEX -> BIBLATEX_TYPES;
            case BIBTEX -> BIBTEX_TYPES;
        };

        for (BibEntry entry : entries) {
            if (typeSet.contains(entry.getType())) {
                EntryType entryType = entry.getType();

                Set<Field> filteredFields = filterExcludedFields(entry.getFields());

                entryTypeToFieldsInAnyEntryMap
                        .computeIfAbsent(entryType, _ -> new HashSet<>())
                        .addAll(filteredFields);
                if (entryTypeToFieldsInAllEntriesMap.containsKey(entryType)) {
                    entryTypeToFieldsInAllEntriesMap.get(entryType).retainAll(filteredFields);
                } else {
                    entryTypeToFieldsInAllEntriesMap.put(entryType, new HashSet<>(filteredFields));
                }

                entryTypeToEntriesMap
                        .computeIfAbsent(entryType, _ -> new java.util.LinkedHashSet<>())
                        .add(entry);
            }
        }
    }
}
