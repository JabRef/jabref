package org.jabref.logic.citation.repository;

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

class BibEntryHashSetSerializer extends BasicDataType<LinkedHashSet<BibEntry>> {

    private final BasicDataType<BibEntry> bibEntryDataType;

    BibEntryHashSetSerializer(BibEntryTypesManager entryTypesManager, ImportFormatPreferences importFormatPreferences, FieldPreferences fieldPreferences) {
        this.bibEntryDataType = new BibEntrySerializer(entryTypesManager, importFormatPreferences, fieldPreferences);
    }

    BibEntryHashSetSerializer(BasicDataType<BibEntry> bibEntryDataType) {
        this.bibEntryDataType = bibEntryDataType;
    }

    @Override
    public int getMemory(LinkedHashSet<BibEntry> bibEntries) {
        // Memory size is the sum of all aggregated bibEntries' memory size plus 4 bytes.
        // Those 4 bytes are used to store the length of the collection itself.
        return bibEntries
                .stream()
                .map(this.bibEntryDataType::getMemory)
                .reduce(0, Integer::sum) + 4;
    }

    @Override
    public void write(WriteBuffer buff, LinkedHashSet<BibEntry> bibEntries) {
        buff.putInt(bibEntries.size());
        bibEntries.forEach(entry -> this.bibEntryDataType.write(buff, entry));
    }

    @Override
    public LinkedHashSet<BibEntry> read(ByteBuffer buff) {
        return IntStream.range(0, buff.getInt())
                        .mapToObj(it -> this.bibEntryDataType.read(buff))
                        .filter(entry -> !entry.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @SuppressWarnings("unchecked")
    public LinkedHashSet<BibEntry>[] createStorage(int size) {
        return (LinkedHashSet<BibEntry>[]) new LinkedHashSet[size];
    }

    @Override
    public boolean isMemoryEstimationAllowed() {
        return false;
    }
}
