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
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.types.EntryType;

import org.jooq.lambda.Unchecked;

/// Outputs the findings as CSV.
///
/// Following symbols are used (as default):
///
///
/// - `x` - required field is present
/// - `o` - optional field is present
/// - `?` - unknown field is present
/// - `-` - field is absent
///
///
/// Note that this classification is based on JabRef's definition and might not match the publisher's definition.
///
/// @implNote We could have implemented a `PaperConsistencyCheckResultFormatter`, but that would have been too much effort.
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

    /// The fields reported for any entry type. Used as columns of tabular output.
    protected final List<Field> allReportedFields;

    private final Map<EntryType, EntryTypeReport> entryTypeToReport;

    /// The information on one entry type needed to report its entries.
    ///
    /// @param displayName      the display name of the entry type
    /// @param differingFields  the fields set in some, but not all entries of the entry type, sorted by name
    /// @param reportedFields   the differing fields plus the required fields missing in at least one entry, sorted by name
    /// @param requiredOrFields the required fields of the entry type (each [OrFields] is satisfied by any of its fields)
    /// @param requiredFields   all fields contained in `requiredOrFields`
    /// @param optionalFields   the optional fields of the entry type
    /// @param sortedEntries    the entries to report
    protected record EntryTypeReport(
            String displayName,
            List<Field> differingFields,
            List<Field> reportedFields,
            List<OrFields> requiredOrFields,
            Set<Field> requiredFields,
            Set<Field> optionalFields,
            SequencedCollection<BibEntry> sortedEntries) {

        /// Returns the required fields not satisfied by the given entry, sorted by name
        public List<OrFields> getMissingRequiredFields(BibEntry bibEntry) {
            return BibliographyConsistencyCheckResultWriter.getMissingRequiredFields(requiredOrFields, bibEntry);
        }
    }

    public BibliographyConsistencyCheckResultWriter(BibliographyConsistencyCheck.Result result, Writer writer, boolean isPorcelain) {
        this(result, writer, isPorcelain, new BibEntryTypesManager(), BibDatabaseMode.BIBTEX);
    }

    public BibliographyConsistencyCheckResultWriter(BibliographyConsistencyCheck.Result result, Writer writer, boolean isPorcelain, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        this.result = result;
        this.writer = writer;
        this.isPorcelain = isPorcelain;
        this.entryTypesManager = entryTypesManager;
        this.bibDatabaseMode = bibDatabaseMode;
        this.entryTypeToReport = result.entryTypeToResultMap().entrySet().stream()
                                       .collect(Collectors.toMap(
                                               Map.Entry::getKey,
                                               mapEntry -> createEntryTypeReport(mapEntry.getKey(), mapEntry.getValue())));
        this.allReportedFields = entryTypeToReport.values().stream()
                                                  .flatMap(entryTypeReport -> entryTypeReport.reportedFields().stream())
                                                  .sorted(Comparator.comparing(Field::getName))
                                                  .distinct()
                                                  .toList();
        this.columnNames = getColumnNames();
        this.columnCount = columnNames.size();
    }

    public void writeFindings() throws IOException {
        entryTypeToReport.entrySet().stream()
                         .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
                         .forEach(entry -> writeEntryTypeReport(entry.getValue()));
    }

    private List<String> getColumnNames() {
        List<String> results = new ArrayList<>(columnCount + 2);
        results.add("entry type");
        results.add("citation key");
        allReportedFields.forEach(field -> results.add(FieldTextMapper.getDisplayName(field)));
        return results;
    }

    protected List<String> getFindingsAsList(BibEntry bibEntry, EntryTypeReport entryTypeReport) {
        Set<Field> requiredFields = entryTypeReport.requiredFields();
        Set<Field> optionalFields = entryTypeReport.optionalFields();
        List<String> results = new ArrayList<>(columnCount + 2);
        results.add(entryTypeReport.displayName());
        results.add(bibEntry.getCitationKey().orElse(""));
        allReportedFields.forEach(field -> {
            if (requiredFields.contains(field)) {
                // Aliases satisfy required fields, consistent with [BibliographyConsistencyCheck#isMissing]
                results.add(bibEntry.getFieldOrAlias(field)
                                    .map(_ -> REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY)
                                    .orElse(UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY));
            } else {
                results.add(bibEntry.getField(field)
                                    .map(_ -> optionalFields.contains(field) ? OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY : UNKNOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY)
                                    .orElse(UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY));
            }
        });
        return results;
    }

    private EntryTypeReport createEntryTypeReport(EntryType entryType, BibliographyConsistencyCheck.EntryTypeResult entryTypeResult) {
        Optional<BibEntryType> bibEntryType = this.entryTypesManager.enrich(entryType, bibDatabaseMode);
        List<OrFields> requiredOrFields = bibEntryType
                .map(BibEntryType::getRequiredFields)
                .stream()
                .flatMap(Collection::stream)
                .sorted()
                .toList();
        Set<Field> requiredFields = requiredOrFields.stream()
                                                    .flatMap(orFields -> orFields.getFields().stream())
                                                    .collect(Collectors.toSet());
        Set<Field> optionalFields = bibEntryType
                .map(BibEntryType::getOptionalFields)
                .stream()
                .flatMap(Collection::stream)
                .map(BibField::field)
                .collect(Collectors.toSet());

        List<Field> differingFields = entryTypeResult.fields().stream()
                                                     .sorted(Comparator.comparing(Field::getName))
                                                     .toList();

        // Required fields missing in an entry are reported, too. Otherwise, an entry flagged only because of them would show no finding.
        Stream<Field> missingRequiredFields = entryTypeResult.sortedEntries().stream()
                                                             .flatMap(bibEntry -> getMissingRequiredFields(requiredOrFields, bibEntry).stream())
                                                             .flatMap(orFields -> orFields.getFields().stream());
        List<Field> reportedFields = Stream.concat(differingFields.stream(), missingRequiredFields)
                                           .distinct()
                                           .sorted(Comparator.comparing(Field::getName))
                                           .toList();

        return new EntryTypeReport(entryType.getDisplayName(), differingFields, reportedFields, requiredOrFields, requiredFields, optionalFields, entryTypeResult.sortedEntries());
    }

    private static List<OrFields> getMissingRequiredFields(List<OrFields> requiredOrFields, BibEntry bibEntry) {
        return requiredOrFields.stream()
                               .filter(orFields -> BibliographyConsistencyCheck.isMissing(orFields, bibEntry))
                               .toList();
    }

    protected void writeEntryTypeReport(EntryTypeReport entryTypeReport) {
        entryTypeReport.sortedEntries().forEach(Unchecked.consumer(bibEntry -> writeBibEntry(bibEntry, entryTypeReport)));
    }

    protected abstract void writeBibEntry(BibEntry bibEntry, EntryTypeReport entryTypeReport) throws IOException;
}
