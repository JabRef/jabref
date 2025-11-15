package org.jabref.logic.citation.repository;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.comparator.BibEntryCompare;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BibEntrySerializer extends BasicDataType<BibEntry> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibEntrySerializer.class);

    private final BibEntryTypesManager entryTypesManager;
    private final ImportFormatPreferences importFormatPreferences;
    private final FieldPreferences fieldPreferences;

    public BibEntrySerializer(BibEntryTypesManager entryTypesManager, ImportFormatPreferences importFormatPreferences, FieldPreferences fieldPreferences) {
        this.entryTypesManager = entryTypesManager;
        this.importFormatPreferences = importFormatPreferences;
        this.fieldPreferences = fieldPreferences;
    }

    private String toString(BibEntry entry) {
        // BibEntry is not Java serializable. Thus, we need to do the serialization manually
        // At reading of the clipboard in JabRef, we parse the plain string in all cases, so we don't need to flag we put BibEntries here
        // Furthermore, storing a string also enables other applications to work wih the data
        BibEntryWriter writer = new BibEntryWriter(new FieldWriter(fieldPreferences), entryTypesManager);
        try {
            return writer.write(List.of(entry), BibDatabaseMode.BIBTEX);
        } catch (IOException e) {
            LOGGER.error("Could not write entry", e);
            return entry.toString();
        }
    }

    private Optional<BibEntry> fromString(String serializedString) {
        try {
            return BibtexParser.singleFromString(serializedString, importFormatPreferences);
        } catch (ParseException e) {
            LOGGER.error("An error occurred while parsing from relation MV store.", e);
            return Optional.empty();
        }
    }

    @Override
    public int getMemory(BibEntry obj) {
        return toString(obj).getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public void write(WriteBuffer buff, BibEntry bibEntry) {
        byte[] asBytes = toString(bibEntry).getBytes(StandardCharsets.UTF_8);
        buff.putInt(asBytes.length);
        buff.put(asBytes);
    }

    @Override
    public BibEntry read(ByteBuffer buff) {
        int serializedEntrySize = buff.getInt();
        byte[] serializedEntry = new byte[serializedEntrySize];
        buff.get(serializedEntry);
        return fromString(new String(serializedEntry, StandardCharsets.UTF_8)).orElse(new BibEntry());
    }

    @Override
    public int compare(@NonNull BibEntry a, @NonNull BibEntry b) {
        return switch (BibEntryCompare.compareEntries(a, b)) {
            case SUBSET ->
                    -1;
            case SUPERSET ->
                    1;
            case EQUAL ->
                    0;
            default ->
                    Objects.compare(a.hashCode(), b.hashCode(), Integer::compare);
        };
    }

    @Override
    public BibEntry[] createStorage(int size) {
        return new BibEntry[size];
    }

    @Override
    public boolean isMemoryEstimationAllowed() {
        return false;
    }
}
