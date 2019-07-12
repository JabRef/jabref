package org.jabref.model.texparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.google.common.collect.Multimap;

public class TexBibEntriesResolverResult {

    private final TexParserResult texParserResult;
    private final List<String> unresolvedKeys;
    private final List<BibEntry> newEntries;
    private int crossRefsCount;

    public TexBibEntriesResolverResult(TexParserResult texParserResult) {
        this.texParserResult = texParserResult;
        this.unresolvedKeys = new ArrayList<>();
        this.newEntries = new ArrayList<>();
        this.crossRefsCount = 0;
    }

    public TexParserResult getTexParserResult() {
        return texParserResult;
    }

    public List<String> getUnresolvedKeys() {
        return unresolvedKeys;
    }

    public List<BibEntry> getNewEntries() {
        return newEntries;
    }

    public int getCrossRefsCount() {
        return crossRefsCount;
    }

    /**
     * Return the citations multimap from the TexParserResult object.
     */
    public Multimap<String, Citation> getCitations() {
        return texParserResult.getCitations();
    }

    /**
     * Return a set of strings with the keys of the citations multimap from the TexParserResult object.
     */
    public Set<String> getCitationsKeySet() {
        return texParserResult.getCitationsKeySet();
    }

    /**
     * Add an unresolved key to the list.
     */
    public void addUnresolvedKey(String key) {
        unresolvedKeys.add(key);
    }

    /**
     * Check if an entry with the given key is present in the list of new entries.
     */
    public boolean checkEntryNewDatabase(String key) {
        return newEntries.stream().anyMatch(entry -> key.equals(entry.getCiteKeyOptional().orElse(null)));
    }

    /**
     * Add 1 to the cross references counter.
     */
    public void increaseCrossRefsCount() {
        crossRefsCount++;
    }

    /**
     * Insert into the list of new entries an entry with the given key.
     */
    public void insertEntry(BibDatabase masterDatabase, String key) {
        masterDatabase.getEntryByKey(key).ifPresent(this::insertEntry);
    }

    /**
     * Insert into the list of new entries the given entry.
     */
    public void insertEntry(BibEntry entry) {
        newEntries.add(entry);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]")
                .add("texParserResult = " + texParserResult)
                .add("unresolvedKeys = " + unresolvedKeys)
                .add("newEntries = " + newEntries)
                .add("crossRefsCount = " + crossRefsCount)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TexBibEntriesResolverResult that = (TexBibEntriesResolverResult) o;

        return Objects.equals(texParserResult, that.texParserResult)
                && Objects.equals(unresolvedKeys, that.unresolvedKeys)
                && Objects.equals(newEntries, that.newEntries)
                && Objects.equals(crossRefsCount, that.crossRefsCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texParserResult, unresolvedKeys, newEntries, crossRefsCount);
    }
}
