package org.jabref.model.texparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class CrossingKeysResult {

    private final TexParserResult texParserResult;
    private final BibDatabase masterDatabase;
    private final List<String> unresolvedKeys;
    private final BibDatabase newDatabase;
    private int crossRefsCount;

    public CrossingKeysResult(TexParserResult texParserResult, BibDatabase masterDatabase) {
        this.texParserResult = texParserResult;
        this.masterDatabase = masterDatabase;
        this.unresolvedKeys = new ArrayList<>();
        this.newDatabase = new BibDatabase();
        this.crossRefsCount = 0;
    }

    public TexParserResult getTexParserResult() {
        return texParserResult;
    }

    public BibDatabase getMasterDatabase() {
        return masterDatabase;
    }

    public List<String> getUnresolvedKeys() {
        return unresolvedKeys;
    }

    public BibDatabase getNewDatabase() {
        return newDatabase;
    }

    public int getCrossRefsCount() {
        return crossRefsCount;
    }

    /**
     * Return the citations map from the TexParserResult object.
     */
    public Map<String, Set<Citation>> getCitations() {
        return texParserResult.getCitations();
    }

    /**
     * Return a set of strings with the keys of the citations map from the TexParserResult object.
     */
    public Set<String> getCitationsKeySet() {
        return texParserResult.getCitations().keySet();
    }

    /**
     * Return the master database in a set, for comparing two objects.
     */
    public Set<BibEntry> getMasterDatabaseSet() {
        return new HashSet<>(masterDatabase.getEntries());
    }

    /**
     * Get if an entry with the given key is present in the master database.
     */
    public Optional<BibEntry> getEntryMasterDatabase(String key) {
        return masterDatabase.getEntryByKey(key);
    }

    /**
     * Add an unresolved key to the list.
     */
    public void addUnresolvedKey(String key) {
        unresolvedKeys.add(key);
    }

    /**
     * Return the new database in a set, for comparing two objects.
     */
    public Set<BibEntry> getNewDatabaseSet() {
        return new HashSet<>(newDatabase.getEntries());
    }

    /**
     * Check if an entry with the given key is present in the new database.
     */
    public boolean checkEntryNewDatabase(String key) {
        return newDatabase.getEntryByKey(key).isPresent();
    }

    /**
     * Add 1 to cross references counter.
     */
    public void increaseCrossRefsCount() {
        crossRefsCount++;
    }

    /**
     * Insert into the database a clone of an entry with the given key. The cloned entry has a new unique ID.
     */
    public void insertEntry(String key) {
        insertEntry(masterDatabase.getEntryByKey(key).get());
    }

    /**
     * Insert into the database a clone of the given entry. The cloned entry has a new unique ID.
     */
    public void insertEntry(BibEntry entry) {
        BibEntry clonedEntry = (BibEntry) entry.clone();
        newDatabase.insertEntry(clonedEntry);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]")
                .add("texParserResult = " + texParserResult)
                .add("masterDatabase = " + masterDatabase)
                .add("unresolvedKeys = " + unresolvedKeys)
                .add("newDatabase = " + newDatabase)
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

        CrossingKeysResult that = (CrossingKeysResult) o;

        return Objects.equals(texParserResult, that.texParserResult)
                && Objects.equals(getMasterDatabaseSet(), that.getMasterDatabaseSet())
                && Objects.equals(masterDatabase, that.masterDatabase)
                && Objects.equals(unresolvedKeys, that.unresolvedKeys)
                && Objects.equals(getNewDatabaseSet(), that.getNewDatabaseSet())
                && Objects.equals(crossRefsCount, that.crossRefsCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texParserResult, masterDatabase, unresolvedKeys, newDatabase, crossRefsCount);
    }
}
