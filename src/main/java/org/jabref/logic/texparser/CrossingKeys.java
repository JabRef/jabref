package org.jabref.logic.texparser;

import java.util.Optional;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.texparser.CrossingKeysResult;
import org.jabref.model.texparser.TexParserResult;

public class CrossingKeys {

    private final CrossingKeysResult result;

    public CrossingKeys(TexParserResult texParserResult, BibDatabase masterDatabase) {
        this.result = new CrossingKeysResult(texParserResult, masterDatabase);
    }

    public CrossingKeysResult getResult() {
        return result;
    }

    /**
     * Look for an equivalent BibTeX entry within the reference database for all keys inside of the TEX files.
     */
    public CrossingKeysResult resolveKeys() {
        Set<String> keySet = result.getCitationsKeySet();

        for (String key : keySet) {
            if (!result.checkEntryNewDatabase(key)) {
                Optional<BibEntry> entry = result.getEntryMasterDatabase(key);

                if (entry.isPresent()) {
                    result.insertEntry(entry.get());
                    resolveCrossReferences(entry.get());
                } else {
                    result.addUnresolvedKey(key);
                }
            }
        }

        return result;
    }

    /**
     * Find cross references for inserting into the new database.
     */
    private void resolveCrossReferences(BibEntry entry) {
        entry.getField(FieldName.CROSSREF).ifPresent(crossRef -> {
            if (!result.checkEntryNewDatabase(crossRef)) {
                Optional<BibEntry> refEntry = result.getEntryMasterDatabase(crossRef);

                if (refEntry.isPresent()) {
                    result.insertEntry(refEntry.get());
                    result.increaseCrossRefsCount();
                } else {
                    result.addUnresolvedKey(crossRef);
                }
            }
        });
    }
}
