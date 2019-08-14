package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.entry.BibEntry;

import com.google.common.collect.Multimap;

public class TexBibEntriesResolverResult {

    private final TexParserResult texParserResult;
    private final Set<String> newEntryKeys;
    private final Set<BibEntry> newEntries;

    public TexBibEntriesResolverResult(TexParserResult texParserResult) {
        this.texParserResult = texParserResult;
        this.newEntryKeys = new HashSet<>();
        this.newEntries = new HashSet<>();
    }

    public TexParserResult getTexParserResult() {
        return texParserResult;
    }

    public Set<String> getNewEntryKeys() {
        return newEntryKeys;
    }

    public Set<BibEntry> getNewEntries() {
        return newEntries;
    }

    public void addEntry(BibEntry entry) {
        newEntries.add(entry);
        entry.getCiteKeyOptional().ifPresent(entryKey -> {
            newEntryKeys.add(entryKey);
            newEntries.add(entry);
        });
    }

    /**
     * Return the BIB files multimap from the TexParserResult object.
     */
    public Multimap<Path, Path> getBibFiles() {
        return texParserResult.getBibFiles();
    }

    /**
     * Return the citations multimap from the TexParserResult object.
     */
    public Multimap<String, Citation> getCitations() {
        return texParserResult.getCitations();
    }

    @Override
    public String toString() {
        return String.format("TexBibEntriesResolverResult{texParserResult=%s, newEntryKeys=%s, newEntries=%s}",
                this.texParserResult,
                this.newEntryKeys,
                this.newEntries);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        TexBibEntriesResolverResult that = (TexBibEntriesResolverResult) obj;

        return Objects.equals(texParserResult, that.texParserResult)
                && Objects.equals(newEntryKeys, that.newEntryKeys)
                && Objects.equals(newEntries, that.newEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texParserResult, newEntryKeys, newEntries);
    }
}
