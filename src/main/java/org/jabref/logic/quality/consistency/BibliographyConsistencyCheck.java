package org.jabref.logic.quality.consistency;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.EntryType;

public class BibliographyConsistencyCheck {

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
    public Result check(List<BibEntry> entries) {
        // collects fields existing in any entry, scoped by entry type
        Map<EntryType, Set<Field>> entryTypeToFieldsInAnyEntryMap = new HashMap<>();
        // collects fields existing in all entries, scoped by entry type
        Map<EntryType, Set<Field>> entryTypeToFieldsInAllEntriesMap = new HashMap<>();
        // collects entries of the same type
        Map<EntryType, Set<BibEntry>> entryTypeToEntriesMap = new HashMap<>();

        collectEntriesIntoMaps(entries, entryTypeToFieldsInAnyEntryMap, entryTypeToFieldsInAllEntriesMap, entryTypeToEntriesMap);

        Map<EntryType, EntryTypeResult> resultMap = new HashMap<>();

        entryTypeToFieldsInAnyEntryMap.forEach((entryType, fields) -> {
            Set<Field> commonFields = entryTypeToFieldsInAllEntriesMap.get(entryType);
            assert commonFields != null;
            Set<Field> uniqueFields = new HashSet<>(fields);
            uniqueFields.removeAll(commonFields);

            if (uniqueFields.isEmpty()) {
                return;
            }

            List<BibEntry> sortedEntries = entryTypeToEntriesMap
                    .get(entryType).stream()
                    .filter(entry -> !entry.getFields().equals(commonFields))
                    .sorted(getBibEntryComparator()).toList();
            resultMap.put(entryType, new EntryTypeResult(uniqueFields, sortedEntries));
        });

        return new Result(resultMap);
    }

    /**
     * Sorts entries by the number of fields and then by the field names.
     */
    private static Comparator<BibEntry> getBibEntryComparator() {
        return (e1, e2) -> {
            int sizeComparison = e1.getFields().size() - e2.getFields().size();
            if (sizeComparison != 0) {
                return sizeComparison;
            }
            Iterator<String> it1 = e1.getFields().stream().map(Field::getName).sorted().iterator();
            Iterator<String> it2 = e2.getFields().stream().map(Field::getName).sorted().iterator();
            while (it1.hasNext() && it2.hasNext()) {
                int fieldComparison = it1.next().compareTo(it2.next());
                if (fieldComparison != 0) {
                    return fieldComparison;
                }
            }
            assert !it1.hasNext() && !it2.hasNext();
            return 0;
        };
    }

    private static void collectEntriesIntoMaps(List<BibEntry> entries, Map<EntryType, Set<Field>> entryTypeToFieldsInAnyEntryMap, Map<EntryType, Set<Field>> entryTypeToFieldsInAllEntriesMap, Map<EntryType, Set<BibEntry>> entryTypeToEntriesMap) {
        entries.forEach(entry -> {
            EntryType entryType = entry.getType();

            Set<Field> fieldsInAnyEntry = entryTypeToFieldsInAnyEntryMap.computeIfAbsent(entryType, k -> new HashSet<>());
            fieldsInAnyEntry.addAll(entry.getFields());

            Set<Field> fieldsInAllEntries = entryTypeToFieldsInAllEntriesMap.computeIfAbsent(entryType, k -> new HashSet<>(entry.getFields()));
            fieldsInAllEntries.retainAll(entry.getFields());

            Set<BibEntry> entriesOfType = entryTypeToEntriesMap.computeIfAbsent(entryType, k -> new HashSet<>());
            entriesOfType.add(entry);
        });
    }
}
