package org.jabref.logic.texparser;

import java.util.Optional;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.texparser.TexParserResult;

class CrossReferences {

    private CrossReferences() throws IllegalStateException {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Look for an equivalent BibTeX entry within the reference database for all keys inside of the TEX files.
     */
    static void resolveKeys(TexParserResult result) {
        BibDatabase masterDatabase = result.getMasterDatabase();
        Set<String> keySet = result.getUniqueKeys().keySet();

        for (String key : keySet) {
            if (!result.getGeneratedBibDatabase().getEntryByKey(key).isPresent()) {
                Optional<BibEntry> entry = masterDatabase.getEntryByKey(key);

                if (entry.isPresent()) {
                    insertEntry(result, entry.get());
                    resolveCrossReferences(result, masterDatabase, entry.get());
                } else {
                    result.getUnresolvedKeys().add(key);
                }
            }
        }

        // Copy database definitions
        if (result.getGeneratedBibDatabase().hasEntries()) {
            result.getGeneratedBibDatabase().copyPreamble(masterDatabase);
            result.insertStrings(masterDatabase.getUsedStrings(result.getGeneratedBibDatabase().getEntries()));
        }
    }

    private static void resolveCrossReferences(TexParserResult result, BibDatabase masterDatabase, BibEntry entry) {
        entry.getField(FieldName.CROSSREF).ifPresent(crossRef -> {
            if (!result.getGeneratedBibDatabase().getEntryByKey(crossRef).isPresent()) {
                Optional<BibEntry> refEntry = masterDatabase.getEntryByKey(crossRef);

                if (refEntry.isPresent()) {
                    result.getGeneratedBibDatabase().insertEntry((BibEntry) refEntry.get().clone());
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
    private static void insertEntry(TexParserResult result, BibEntry entry) {
        BibEntry clonedEntry = (BibEntry) entry.clone();
        result.getGeneratedBibDatabase().insertEntry(clonedEntry);
    }
}
