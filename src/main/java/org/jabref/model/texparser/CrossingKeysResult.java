package org.jabref.model.texparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

public class CrossingKeysResult {

    private final TexParserResult texParserResult;
    private final BibDatabase masterDatabase;
    private final List<String> unresolvedKeys;
    private final BibDatabase newDatabase;
    private int insertedStrings;
    private int crossRefEntriesCount;

    public CrossingKeysResult(TexParserResult texParserResult, BibDatabase masterDatabase, int insertedStrings, int crossRefEntriesCount) {
        this.texParserResult = texParserResult;
        this.masterDatabase = masterDatabase;
        this.unresolvedKeys = new ArrayList<>();
        this.newDatabase = new BibDatabase();
        this.insertedStrings = insertedStrings;
        this.crossRefEntriesCount = crossRefEntriesCount;
    }

    public CrossingKeysResult(TexParserResult texParserResult, BibDatabase masterDatabase) {
        this(texParserResult, masterDatabase, 0, 0);
    }

    public TexParserResult getParserResult() {
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

    public int getInsertedStrings() {
        return insertedStrings;
    }

    public int getResolvedKeysCount() {
        return newDatabase.getEntryCount() - crossRefEntriesCount;
    }

    public int getCrossRefEntriesCount() {
        return crossRefEntriesCount;
    }

    public void increaseCrossRefEntriesCounter() {
        crossRefEntriesCount++;
    }

    public void insertStrings(Collection<BibtexString> usedStrings) {
        for (BibtexString string : usedStrings) {
            newDatabase.addString(string);
            insertedStrings++;
        }
    }

    @Override
    public String toString() {
        return String.format("%nCrossReferencesResult{texParserResult=%s // masterDatabase=%s // unresolvedKeys=%s // newDatabase=%s // insertedStrings=%s // crossRefEntriesCount=%s}%n",
                texParserResult, masterDatabase, unresolvedKeys, newDatabase, insertedStrings, crossRefEntriesCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CrossingKeysResult result = (CrossingKeysResult) o;

        return texParserResult.equals(result.texParserResult)
                && masterDatabase.equals(result.masterDatabase)
                && unresolvedKeys.equals(result.unresolvedKeys)
                && (new HashSet<>(newDatabase.getEntries())).equals(new HashSet<>(result.newDatabase.getEntries()))
                && insertedStrings == result.insertedStrings
                && crossRefEntriesCount == result.crossRefEntriesCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterDatabase, unresolvedKeys, newDatabase, insertedStrings, crossRefEntriesCount);
    }
}
