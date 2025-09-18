package org.jabref.logic.quality.consistency;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.types.EntryType;

import org.jooq.lambda.Unchecked;

/**
 * Outputs the findings as CSV.
 * <p>
 * Following symbols are used (as default):
 *
 * <ul>
 *     <li><code>x</code> - required field is present</li>
 *     <li><code>o</code> - optional field is present</li>
 *     <li><code>?</code> - unknown field is present</li>
 *     <li><code>-</code> - field is absent</li>
 * </ul>
 * <p>
 * Note that this classification is based on JabRef's definition and might not match the publisher's definition.
 *
 * @implNote We could have implemented a <code>PaperConsistencyCheckResultFormatter</code>, but that would have been too much effort.
 */
public abstract class BibliographyConsistencyCheckResultWriter implements Closeable {

    protected static final String REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "x";
    protected static final String OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "o";
    protected static final String UNKNOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "?";
    protected static final String UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "-";

    protected final BibliographyConsistencyCheck.Result result;
    protected final Writer writer;
    protected final boolean isPorcelain;
    protected final BibEntryTypesManager entryTypesManager;
    protected final BibDatabaseMode bibDatabaseMode;
    protected final List<String> columnNames;
    protected final int columnCount;

    private final List<Field> allReportedFields;

    public BibliographyConsistencyCheckResultWriter(BibliographyConsistencyCheck.Result result, Writer writer, boolean isPorcelain) {
        this(result, writer, isPorcelain, new BibEntryTypesManager(), BibDatabaseMode.BIBTEX);
    }

    public BibliographyConsistencyCheckResultWriter(BibliographyConsistencyCheck.Result result, Writer writer, boolean isPorcelain, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        this.result = result;
        this.writer = writer;
        this.isPorcelain = isPorcelain;
        this.entryTypesManager = entryTypesManager;
        this.bibDatabaseMode = bibDatabaseMode;
        this.allReportedFields = result.entryTypeToResultMap().values().stream()
                                       .flatMap(entryTypeResult -> entryTypeResult.fields().stream())
                                       .sorted(Comparator.comparing(Field::getName))
                                       .distinct()
                                       .toList();
        this.columnNames = getColumnNames();
        this.columnCount = columnNames.size();
    }

    public void writeFindings() throws IOException {
        result.entryTypeToResultMap().entrySet().stream()
              .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
              .forEach(Unchecked.consumer(this::writeMapEntry));
    }

    private List<String> getColumnNames() {
        List<String> results = new ArrayList<>(columnCount + 2);
        results.add("entry type");
        results.add("citation key");
        allReportedFields.forEach(field -> results.add(FieldTextMapper.getDisplayName(field)));
        return results;
    }

    protected List<String> getFindingsAsList(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) {
        List<String> results = new ArrayList<>(columnCount + 2);
        results.add(entryType);
        results.add(bibEntry.getCitationKey().orElse(""));
        allReportedFields.forEach(field -> results.add(
                bibEntry.getField(field).map(value -> {
                    if (requiredFields.contains(field)) {
                        return REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY;
                    } else if (optionalFields.contains(field)) {
                        return OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY;
                    } else {
                        return UNKNOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY;
                    }
                }).orElse(UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY)));
        return results;
    }

    protected void writeMapEntry(Map.Entry<EntryType, BibliographyConsistencyCheck.EntryTypeResult> mapEntry) {
        String entryType = mapEntry.getKey().getDisplayName();

        Optional<BibEntryType> bibEntryType = this.entryTypesManager.enrich(mapEntry.getKey(), bibDatabaseMode);
        Set<Field> requiredFields = bibEntryType
                .map(BibEntryType::getRequiredFields)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(orFields -> orFields.getFields().stream())
                .collect(Collectors.toSet());
        Set<Field> optionalFields = bibEntryType
                .map(BibEntryType::getOptionalFields)
                .stream()
                .flatMap(Collection::stream)
                .map(BibField::field)
                .collect(Collectors.toSet());

        BibliographyConsistencyCheck.EntryTypeResult entries = mapEntry.getValue();
        SequencedCollection<BibEntry> bibEntries = entries.sortedEntries();

        bibEntries.forEach(Unchecked.consumer(bibEntry -> writeBibEntry(bibEntry, entryType, requiredFields, optionalFields)));
    }

    protected abstract void writeBibEntry(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) throws IOException;
}
