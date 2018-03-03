package org.jabref.model.auxparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

public class AuxParserResult {

    private final BibDatabase masterDatabase;
    private final Set<String> uniqueKeys = new HashSet<>();
    private final List<String> unresolvedKeys = new ArrayList<>();

    private final BibDatabase auxDatabase = new BibDatabase();
    private int nestedAuxCount;
    private int crossRefEntriesCount;
    private int insertedStrings;

    public AuxParserResult(BibDatabase masterDatabase) {
        this.masterDatabase = masterDatabase;
    }

    public BibDatabase getGeneratedBibDatabase() {
        return auxDatabase;
    }

    public List<String> getUnresolvedKeys() {
        return unresolvedKeys;
    }

    public int getFoundKeysInAux() {
        return uniqueKeys.size();
    }

    public int getResolvedKeysCount() {
        return auxDatabase.getEntryCount() - crossRefEntriesCount;
    }

    public int getUnresolvedKeysCount() {
        return unresolvedKeys.size();
    }

    /**
     * Query the number of extra entries pulled in due to crossrefs from other entries.
     *
     * @return The number of additional entries pulled in due to crossref
     */
    public int getCrossRefEntriesCount() {
        return crossRefEntriesCount;
    }

    public void increaseCrossRefEntriesCounter() {
        crossRefEntriesCount++;
    }

    public void increaseNestedAuxFilesCounter() {
        nestedAuxCount++;
    }

    public void insertStrings(Collection<BibtexString> usedStrings) {
        for (BibtexString string : usedStrings) {
            auxDatabase.addString(string);
            insertedStrings++;
        }
    }

    public BibDatabase getMasterDatabase() {
        return masterDatabase;
    }

    public int getNestedAuxCount() {
        return nestedAuxCount;
    }

    public int getInsertedStrings() {
        return insertedStrings;
    }

    public Set<String> getUniqueKeys() {
        return uniqueKeys;
    }
}
