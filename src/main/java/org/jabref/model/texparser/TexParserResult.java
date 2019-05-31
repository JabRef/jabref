package org.jabref.model.texparser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

public class TexParserResult {

    private final BibDatabase masterDatabase;
    private final Map<String, List<Citation>> uniqueKeys = new HashMap<>();
    private final List<String> unresolvedKeys = new ArrayList<>();
    private final BibDatabase texDatabase = new BibDatabase();
    private int insertedStrings = 0;

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
        return texDatabase.getEntryCount();
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
}
