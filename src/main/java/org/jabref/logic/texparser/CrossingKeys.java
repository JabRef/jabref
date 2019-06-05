package org.jabref.logic.texparser;

import java.util.Optional;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.texparser.CrossingKeysResult;
import org.jabref.model.texparser.TexParserResult;

class CrossingKeys {

    private final CrossingKeysResult result;

    public CrossingKeys(TexParserResult texParserResult, BibDatabase masterDatabase) {
        this.result = new CrossingKeysResult(texParserResult, masterDatabase);
    }

    /**
     * Look for an equivalent BibTeX entry within the reference database for all keys inside of the TEX files.
     */
    public CrossingKeysResult resolveKeys() {
        Set<String> keySet = result.getParserResult().getCitations().keySet();

        for (String key : keySet) {
            if (!result.getNewDatabase().getEntryByKey(key).isPresent()) {
                Optional<BibEntry> entry = result.getMasterDatabase().getEntryByKey(key);

                if (entry.isPresent()) {
                    insertEntry(entry.get());
                    resolveCrossReferences(entry.get());
                } else {
                    result.getUnresolvedKeys().add(key);
                }
            }
        }

        // Copy database definitions.
        if (result.getNewDatabase().hasEntries()) {
            result.getNewDatabase().copyPreamble(result.getMasterDatabase());
            result.insertStrings(result.getMasterDatabase().getUsedStrings(result.getNewDatabase().getEntries()));
        }

        return result;
    }

    /**
     * Find cross references for inserting into the new database.
     */
    private void resolveCrossReferences(BibEntry entry) {
        entry.getField(FieldName.CROSSREF).ifPresent(crossRef -> {
            if (!result.getNewDatabase().getEntryByKey(crossRef).isPresent()) {
                Optional<BibEntry> refEntry = result.getMasterDatabase().getEntryByKey(crossRef);

                if (refEntry.isPresent()) {
                    insertEntry(refEntry.get());
                    result.increaseCrossRefEntriesCounter();
                } else {
                    result.getUnresolvedKeys().add(crossRef);
                }
            }
        });
    }

    /**
     * Insert into the database a clone of the given entry. The cloned entry has a new unique ID.
     */
    private void insertEntry(BibEntry entry) {
        BibEntry clonedEntry = (BibEntry) entry.clone();
        result.getNewDatabase().insertEntry(clonedEntry);
    }
}
