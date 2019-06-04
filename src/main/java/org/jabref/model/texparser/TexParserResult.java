package org.jabref.model.texparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

public class TexParserResult {

    private final BibDatabase masterDatabase;
    private final Map<String, List<Citation>> uniqueKeys = new HashMap<>();
    private final List<String> unresolvedKeys = new ArrayList<>();
    private final BibDatabase texDatabase = new BibDatabase();
    private int insertedStrings = 0;
    private int nestedFilesCount = 0;
    private int crossRefEntriesCount = 0;

    public TexParserResult(BibDatabase masterDatabase, int insertedStrings, int nestedFilesCount, int crossRefEntriesCount) {
        this.masterDatabase = masterDatabase;
        this.insertedStrings = insertedStrings;
        this.nestedFilesCount = nestedFilesCount;
        this.crossRefEntriesCount = crossRefEntriesCount;
    }

    public TexParserResult(BibDatabase masterDatabase) {
        this.masterDatabase = masterDatabase;
    }

    public BibDatabase getMasterDatabase() {
        return masterDatabase;
    }

    public Map<String, List<Citation>> getUniqueKeys() {
        return uniqueKeys;
    }

    public int getFoundKeysInTex() {
        return uniqueKeys.size();
    }

    public int getCitationsCountByKey(String key) {
        return uniqueKeys.get(key).size();
    }

    public List<String> getUnresolvedKeys() {
        return unresolvedKeys;
    }

    public int getUnresolvedKeysCount() {
        return unresolvedKeys.size();
    }

    public BibDatabase getGeneratedBibDatabase() {
        return texDatabase;
    }

    public int getResolvedKeysCount() {
        return texDatabase.getEntryCount() - crossRefEntriesCount;
    }

    public int getInsertedStrings() {
        return insertedStrings;
    }

    public void insertStrings(Collection<BibtexString> usedStrings) {
        for (BibtexString string : usedStrings) {
            texDatabase.addString(string);
            insertedStrings++;
        }
    }

    public int getNestedFilesCount() {
        return nestedFilesCount;
    }

    public void increaseNestedFilesCounter() {
        nestedFilesCount++;
    }

    public int getCrossRefEntriesCount() {
        return crossRefEntriesCount;
    }

    public void increaseCrossRefEntriesCounter() {
        crossRefEntriesCount++;
    }

    @Override
    public String toString() {
        return String.format("%nTexParserResult{%n  masterDatabase=%s,%n  uniqueKeys=%s,%n  unresolvedKeys=%s,%n  texDatabase=%s,%n  insertedStrings=%s,%n  nestedFilesCount=%s,%n  crossRefEntriesCount=%s%n}%n",
                masterDatabase,
                uniqueKeys,
                unresolvedKeys,
                texDatabase,
                insertedStrings,
                nestedFilesCount,
                crossRefEntriesCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TexParserResult result = (TexParserResult) o;

        return masterDatabase.equals(result.masterDatabase)
                && uniqueKeys.equals(result.uniqueKeys)
                && unresolvedKeys.equals(result.unresolvedKeys)
                && (new HashSet<>(texDatabase.getEntries())).equals(new HashSet<>(result.texDatabase.getEntries()))
                && insertedStrings == result.insertedStrings
                && nestedFilesCount == result.nestedFilesCount
                && crossRefEntriesCount == result.crossRefEntriesCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterDatabase, uniqueKeys, unresolvedKeys, texDatabase, insertedStrings, nestedFilesCount, crossRefEntriesCount);
    }
}
