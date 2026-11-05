package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.comparator.BibEntryByCitationKeyComparator;
import org.jabref.logic.bibtex.comparator.BibEntryByFieldsComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.EntryType;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jspecify.annotations.NullMarked;

public class BibliographyConsistencyCheck {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    // TODO: This is BibTeX - See org.jabref.model.entry.EntryConverter for mapping to BibLaTeX
    private static final Set<Field> EXPLICITLY_EXCLUDED_FIELDS = Set.of(
            InternalField.KEY_FIELD, // Citation key
            StandardField.KEY,
            StandardField.COMMENT,
            StandardField.CROSSREF,
            StandardField.CITES,
            StandardField.FILE,
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

    /// Returns whether none of the given (alternative) required fields is set in the entry.
    ///
    /// Aliases are taken into account. For instance, `year` satisfies a required `date`.
    static boolean isMissing(OrFields requiredFields, BibEntry entry) {
        return requiredFields.getFields().stream()
                             .allMatch(field -> entry.getFieldOrAlias(field).isEmpty());
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
                      .filter(entry -> {
                          // This removes entries that have all differing fields set (could be confusing to the user)
                          boolean hasDifferingFields = !Collections.disjoint(filterExcludedFields(entry.getFields()), differingFields);
                          boolean hasMissingRequiredFields = requiredFields.stream()
                                                                           .anyMatch(orFields -> isMissing(orFields, entry));
                          return hasDifferingFields || hasMissingRequiredFields;
                      })
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

    /// Checks the consistency of the given entries by looking at the present and absent fields.
    ///
    /// Computation takes place grouped by each entryType.
    /// Computes the fields set in all entries. In case entries of the same type has more fields defined, it is output.
    ///
    /// This class **does** check if required fields are set for an entry type but **does not** check if the fields are valid for the entry type.
    ///
    /// That result can a) be retrieved by using the JabRef UI and b) by checking the CSV output of [BibliographyConsistencyCheckResultCsvWriter#writeFindings]
    ///
    /// @implNote This class does not implement [org.jabref.logic.integrity.DatabaseChecker], because it returns a list of [org.jabref.logic.integrity.IntegrityMessage], which are too fine-grained.
    public Result check(BibDatabaseContext bibContext, BibEntryTypesManager bibEntryTypesManager, BiConsumer<Integer, Integer> entriesGroupingProgress) {
        return checkEntries(bibContext.getEntries(), bibContext.getMode(), bibEntryTypesManager, entriesGroupingProgress);
    }

    /// Checks a selected set of entries without registering database listeners on them.
    public Result checkEntries(List<BibEntry> sourceEntries, BibDatabaseMode mode, BibEntryTypesManager bibEntryTypesManager,
                               BiConsumer<Integer, Integer> entriesGroupingProgress) {
        // collects fields existing in any entry, scoped by entry type
        Map<EntryType, Set<Field>> entryTypeToFieldsInAnyEntryMap = new HashMap<>();
        // collects fields existing in all entries, scoped by entry type
        Map<EntryType, Set<Field>> entryTypeToFieldsInAllEntriesMap = new HashMap<>();
        // collects entries of the same type
        Map<EntryType, Set<BibEntry>> entryTypeToEntriesMap = new HashMap<>();

        collectEntriesIntoMaps(sourceEntries, entryTypeToFieldsInAnyEntryMap, entryTypeToFieldsInAllEntriesMap, entryTypeToEntriesMap);

        List<BibEntryType> entryTypeDefinitions = bibEntryTypesManager.getAllTypes(mode).stream().toList();

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

    private static void collectEntriesIntoMaps(List<BibEntry> entries, Map<EntryType, Set<Field>> entryTypeToFieldsInAnyEntryMap, Map<EntryType, Set<Field>> entryTypeToFieldsInAllEntriesMap, Map<EntryType, Set<BibEntry>> entryTypeToEntriesMap) {
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

    /// Captures field presence and ordering without copying entry models or retaining raw field values.
    // [impl->adr~compact-consistency-snapshots~1]
    // [impl->req~ux.consistency.export-snapshot~1]
    public static Snapshot snapshot(List<BibEntry> entries, BibDatabaseMode mode, BibEntryTypesManager types, BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        Set<BibEntry> unique = new HashSet<>();
        Map<Field, Integer> indexes = new HashMap<>();
        Map<EntryType, TypeFields> typeFields = new HashMap<>();
        List<BibEntryType> definitions = List.copyOf(types.getAllTypes(mode));
        List<EntryCapture> captures = new ArrayList<>();
        for (BibEntry entry : entries) {
            checkCancelled(cancelled);
            if (!unique.add(entry)) {
                continue;
            }
            EntryType capturedType = entry.getType();
            Set<Field> capturedFields = Set.copyOf(entry.getFields());
            for (Field field : capturedFields) {
                indexes.computeIfAbsent(field, _ -> indexes.size());
            }
            TypeFields type = typeFields.computeIfAbsent(capturedType, entryType -> {
                List<Set<Field>> checkedRequired = definitions.stream()
                                                              .filter(definition -> definition.getType().equals(entryType))
                                                              .findFirst().stream().flatMap(definition -> definition.getRequiredFields().stream())
                                                              .map(group -> Set.copyOf(group.getFields())).toList();
                Optional<BibEntryType> enriched = types.enrich(entryType, mode);
                List<Set<Field>> reportedRequired = enriched.stream().flatMap(definition -> definition.getRequiredFields().stream())
                                                            .map(group -> Set.copyOf(group.getFields())).toList();
                Set<Field> optional = enriched.stream().flatMap(definition -> definition.getOptionalFields().stream())
                                              .map(BibField::field).collect(Collectors.toSet());
                Set<Field> required = new HashSet<>();
                checkedRequired.forEach(required::addAll);
                reportedRequired.forEach(required::addAll);
                required.forEach(field -> indexes.computeIfAbsent(field, _ -> indexes.size()));
                return new TypeFields(entryType.getName(), entryType.getDisplayName(), normalize(entryType.getDisplayName()), checkedRequired, reportedRequired, Set.copyOf(optional), Set.copyOf(required));
            });
            BitSet listed = new BitSet();
            BitSet direct = new BitSet();
            BitSet aliases = new BitSet();
            List<String> sortFields = new ArrayList<>(capturedFields.size());
            for (Field capturedField : capturedFields) {
                checkCancelled(cancelled);
                int fieldIndex = indexes.get(capturedField);
                listed.set(fieldIndex);
                sortFields.add(capturedField.getName());
                entry.getField(capturedField).ifPresent(_ -> direct.set(fieldIndex));
            }
            sortFields.sort((left, right) -> {
                checkCancelled(cancelled);
                return left.compareTo(right);
            });
            for (Field field : type.requiredFields()) {
                checkCancelled(cancelled);
                entry.getFieldOrAlias(field).ifPresent(_ -> aliases.set(indexes.get(field)));
            }
            Optional<String> key = entry.getCitationKey();
            String capturedKey = key.orElse("");
            captures.add(new EntryCapture(capturedType, capturedKey, normalize(capturedKey), key.isPresent(), listed, direct, aliases, entry, sortFields));
        }
        captures.sort((left, right) -> {
            checkCancelled(cancelled);
            int comparison = Boolean.compare(right.hasKey(), left.hasKey());
            if (comparison == 0 && left.hasKey()) {
                comparison = left.key().compareTo(right.key());
            }
            if (comparison == 0) {
                comparison = Integer.compare(left.sortFields().size(), right.sortFields().size());
            }
            for (int index = 0; comparison == 0 && index < left.sortFields().size(); index++) {
                checkCancelled(cancelled);
                comparison = left.sortFields().get(index).compareTo(right.sortFields().get(index));
            }
            return comparison;
        });
        checkCancelled(cancelled);
        List<Field> fields = new ArrayList<>(Collections.nCopies(indexes.size(), StandardField.TITLE));
        indexes.forEach((field, index) -> fields.set(index, field));
        Map<EntryType, TypePresence> presenceByType = new HashMap<>();
        typeFields.forEach((type, definition) -> {
            checkCancelled(cancelled);
            presenceByType.put(type, new TypePresence(definition, indexes));
        });
        BitSet eligible = new BitSet();
        filterExcludedFields(fields).forEach(field -> eligible.set(indexes.get(field)));
        List<EntryPresence> captured = new ArrayList<>(captures.size());
        for (EntryCapture capture : captures) {
            checkCancelled(cancelled);
            capture.listed().and(eligible);
            captured.add(new EntryPresence(presenceByType.get(capture.type()), capture.key(), capture.normalizedKey(), capture.hasKey(), capture.listed(), capture.direct(), capture.aliases(), capture.original()));
        }
        return new Snapshot(List.copyOf(fields), List.copyOf(captured));
    }

    private static String normalize(String text) {
        return WHITESPACE.matcher(text).replaceAll(" ");
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
            throw new CancellationException();
        }
    }

    @NullMarked
    private record TypeFields(String name, String displayName, String normalizedDisplayName, List<Set<Field>> checkedRequired, List<Set<Field>> reportedRequired, Set<Field> optional, Set<Field> requiredFields) {
    }

    @NullMarked
    private record EntryCapture(EntryType type, String key, String normalizedKey, boolean hasKey, BitSet listed, BitSet direct, BitSet aliases, BibEntry original, List<String> sortFields) {
    }

    @NullMarked
    private record EntryPresence(TypePresence type, String key, String normalizedKey, boolean hasKey, BitSet listed, BitSet direct, BitSet aliases, BibEntry original) {
    }

    @NullMarked
    private static final class TypePresence {
        private final TypeFields fields;
        private final List<BitSet> checkedRequired;
        private final List<BitSet> reportedRequired;
        private final BitSet reportedRequiredFields = new BitSet();
        private final BitSet optional = new BitSet();

        private TypePresence(TypeFields fields, Map<Field, Integer> indexes) {
            this.fields = fields;
            this.checkedRequired = TypePresence.groups(fields.checkedRequired(), indexes);
            this.reportedRequired = TypePresence.groups(fields.reportedRequired(), indexes);
            this.reportedRequired.forEach(this.reportedRequiredFields::or);
            fields.optional().forEach(field -> {
                if (indexes.containsKey(field)) {
                    this.optional.set(indexes.get(field));
                }
            });
        }

        private static List<BitSet> groups(List<Set<Field>> groups, Map<Field, Integer> indexes) {
            return groups.stream().map(group -> {
                BitSet presence = new BitSet();
                group.forEach(field -> presence.set(indexes.get(field)));
                return presence;
            }).toList();
        }
    }

    /// Reusable immutable presence data for checks of cited-entry subsets.
    @NullMarked
    public static final class Snapshot {
        private final List<Field> fields;
        private final List<EntryPresence> entries;

        private Snapshot(List<Field> fields, List<EntryPresence> entries) {
            this.fields = fields;
            this.entries = entries;
        }

        public Report check(Set<String> keys, BooleanSupplier cancelled) {
            checkCancelled(cancelled);
            boolean all = keys.contains("*");
            Map<TypePresence, List<EntryPresence>> selectedByType = new HashMap<>();
            for (EntryPresence entry : entries) {
                checkCancelled(cancelled);
                if (all || (entry.hasKey() && keys.contains(entry.key()))) {
                    selectedByType.computeIfAbsent(entry.type(), _ -> new ArrayList<>()).add(entry);
                }
            }
            List<TypePresence> types = new ArrayList<>(selectedByType.keySet());
            types.sort((left, right) -> {
                checkCancelled(cancelled);
                return left.fields.name().compareTo(right.fields.name());
            });
            List<EntryPresence> rows = new ArrayList<>();
            Map<String, BitSet> fieldsByType = new LinkedHashMap<>();
            BitSet allReported = new BitSet();
            for (TypePresence type : types) {
                checkCancelled(cancelled);
                List<EntryPresence> selected = selectedByType.get(type);
                BitSet union = new BitSet();
                BitSet intersection = (BitSet) selected.getFirst().listed().clone();
                for (EntryPresence entry : selected) {
                    checkCancelled(cancelled);
                    union.or(entry.listed());
                    intersection.and(entry.listed());
                }
                union.andNot(intersection);
                BitSet reported = (BitSet) union.clone();
                int firstRow = rows.size();
                for (EntryPresence entry : selected) {
                    checkCancelled(cancelled);
                    boolean missing = type.checkedRequired.stream().anyMatch(group -> !group.intersects(entry.aliases()));
                    if (entry.listed().intersects(union) || missing) {
                        rows.add(entry);
                        for (BitSet group : type.reportedRequired) {
                            checkCancelled(cancelled);
                            if (!group.intersects(entry.aliases())) {
                                reported.or(group);
                            }
                        }
                    }
                }
                if (rows.size() > firstRow) {
                    fieldsByType.computeIfAbsent(type.fields.displayName(), _ -> new BitSet()).or(reported);
                    allReported.or(reported);
                }
            }
            List<Integer> columns = new ArrayList<>();
            for (int index = allReported.nextSetBit(0); index >= 0; index = allReported.nextSetBit(index + 1)) {
                checkCancelled(cancelled);
                columns.add(index);
            }
            columns.sort((left, right) -> {
                checkCancelled(cancelled);
                return fields.get(left).getName().compareTo(fields.get(right).getName());
            });
            checkCancelled(cancelled);
            return new Report(fields, List.copyOf(columns), List.copyOf(rows), fieldsByType, cancelled);
        }
    }

    /// Report cells are derived on demand, without retaining a row-by-column matrix.
    @NullMarked
    public static final class Report {
        private final List<Field> fields;
        private final List<Integer> columns;
        private final List<EntryPresence> rows;
        private final Map<String, List<Field>> fieldsByType;
        private final List<String> headers;
        private final List<ConsistencyMessage> messages;

        private Report(List<Field> allFields, List<Integer> columns, List<EntryPresence> rows,
                       Map<String, BitSet> reportedByType, BooleanSupplier cancelled) {
            this.columns = columns;
            this.rows = rows;
            fields = columns.stream().map(allFields::get).toList();
            Map<String, List<Field>> grouped = new LinkedHashMap<>();
            reportedByType.forEach((name, reported) -> {
                checkCancelled(cancelled);
                grouped.put(name, columns.stream().filter(reported::get).map(allFields::get).toList());
            });
            fieldsByType = Collections.unmodifiableMap(grouped);
            List<String> names = new ArrayList<>();
            names.add("entry type");
            names.add("citation key");
            for (Field field : fields) {
                checkCancelled(cancelled);
                names.add(FieldTextMapper.getDisplayName(field));
            }
            headers = List.copyOf(names);
            messages = Collections.unmodifiableList(new AbstractList<>() {
                @Override
                public ConsistencyMessage get(int index) {
                    EntryPresence row = rows.get(index);
                    // [impl->req~ux.consistency.entry-navigation~1]
                    return new ConsistencyMessage(new Cells(row, true), row.original());
                }

                @Override
                public int size() {
                    return rows.size();
                }
            });
        }

        public List<Field> fields() {
            return fields;
        }

        public Map<String, List<Field>> fieldsByType() {
            return fieldsByType;
        }

        public List<ConsistencyMessage> messages() {
            return messages;
        }

        public void writeCsv(Writer writer) throws IOException {
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            printer.printRecord(headers);
            for (EntryPresence row : rows) {
                checkCancelled(() -> false);
                printer.printRecord(new Cells(row, false));
            }
        }

        public void writeTxt(Writer writer) throws IOException {
            if (rows.isEmpty()) {
                return;
            }
            int[] widths = headers.stream().mapToInt(String::length).toArray();
            for (EntryPresence row : rows) {
                checkCancelled(() -> false);
                widths[0] = Math.max(widths[0], row.type().fields.displayName().length());
                widths[1] = Math.max(widths[1], row.key().length());
            }
            writeTxtRow(writer, headers, widths);
            List<String> separator = new ArrayList<>(widths.length);
            for (int width : widths) {
                separator.add("-".repeat(width));
            }
            writeTxtRow(writer, separator, widths);
            for (EntryPresence row : rows) {
                checkCancelled(() -> false);
                writeTxtRow(writer, new Cells(row, false), widths);
            }
        }

        private void writeTxtRow(Writer writer, List<String> row, int[] widths) throws IOException {
            StringJoiner output = new StringJoiner(" | ", "| ", " |\n");
            for (int index = 0; index < row.size(); index++) {
                output.add(("%-" + widths[index] + "s").formatted(row.get(index)));
            }
            writer.write(output.toString());
        }

        @NullMarked
        private final class Cells extends AbstractList<String> implements RandomAccess {
            private final EntryPresence entry;
            private final boolean normalize;

            private Cells(EntryPresence entry, boolean normalize) {
                this.entry = entry;
                this.normalize = normalize;
            }

            @Override
            public String get(int index) {
                if (index == 0) {
                    return normalize ? entry.type().fields.normalizedDisplayName() : entry.type().fields.displayName();
                }
                if (index == 1) {
                    return normalize ? entry.normalizedKey() : entry.key();
                }
                int field = columns.get(index - 2);
                if (entry.type().reportedRequiredFields.get(field)) {
                    return entry.aliases().get(field) ? "x" : "-";
                }
                if (!entry.direct().get(field)) {
                    return "-";
                }
                return entry.type().optional.get(field) ? "o" : "?";
            }

            @Override
            public int size() {
                return columns.size() + 2;
            }
        }
    }
}
