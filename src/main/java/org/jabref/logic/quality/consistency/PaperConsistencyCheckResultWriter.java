package org.jabref.logic.quality.consistency;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
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
import org.jabref.model.entry.types.EntryType;

import org.jooq.lambda.Unchecked;

public abstract class PaperConsistencyCheckResultWriter implements Closeable {

    protected final PaperConsistencyCheck.Result result;
    protected final Path path;
    protected final BibEntryTypesManager entryTypesManager;
    protected final BibDatabaseMode bibDatabaseMode;
    protected List<Field> allFields;

    public PaperConsistencyCheckResultWriter(PaperConsistencyCheck.Result result, Path path) {
        this(result, path, new BibEntryTypesManager(), BibDatabaseMode.BIBTEX);
    }

    public PaperConsistencyCheckResultWriter(PaperConsistencyCheck.Result result, Path path, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        this.result = result;
        this.path = path;
        this.entryTypesManager = entryTypesManager;
        this.bibDatabaseMode = bibDatabaseMode;
        this.allFields = result.entryTypeToResultMap().values().stream()
                               .flatMap(entryTypeResult -> entryTypeResult.fields().stream())
                               .sorted(Comparator.comparing(Field::getName))
                               .distinct()
                               .toList();
    }

    public void writeFindings() throws IOException {
        result.entryTypeToResultMap().entrySet().stream()
              .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
              .forEach(Unchecked.consumer(mapEntry -> {
                  writeMapEntry(mapEntry);
              }));
    }

    protected void writeMapEntry(Map.Entry<EntryType, PaperConsistencyCheck.EntryTypeResult> mapEntry) {
        String entryType = mapEntry.getKey().getDisplayName();

        Optional<BibEntryType> bibEntryType = this.entryTypesManager.enrich(mapEntry.getKey(), bibDatabaseMode);
        Set<Field> requiredFields = bibEntryType
                .map(BibEntryType::getRequiredFields)
                .stream()
                .flatMap(orFieldsCollection -> orFieldsCollection.stream())
                .flatMap(orFields -> orFields.getFields().stream())
                .collect(Collectors.toSet());
        Set<Field> optionalFields = bibEntryType
                .map(BibEntryType::getOptionalFields)
                .stream()
                .flatMap(bibFieldSet -> bibFieldSet.stream())
                .map(BibField::field)
                .collect(Collectors.toSet());

        PaperConsistencyCheck.EntryTypeResult entries = mapEntry.getValue();
        SequencedCollection<BibEntry> bibEntries = entries.sortedEntries();

        bibEntries.forEach(Unchecked.consumer(bibEntry -> {
            writeBibEntry(bibEntry, entryType, requiredFields, optionalFields);
        }));
    }

    protected abstract void writeBibEntry(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) throws IOException;
}
