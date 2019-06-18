package org.jabref.logic.texparser;

import java.util.Optional;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.texparser.TexBibEntriesResolverResult;
import org.jabref.model.texparser.TexParserResult;

public class TexBibEntriesResolver {

    private final BibDatabase masterDatabase;

    public TexBibEntriesResolver(BibDatabase masterDatabase) {
        this.masterDatabase = masterDatabase;
    }

    /**
     * Look for BibTeX entries within the reference database for all keys inside of the TEX files.
     * Insert these data in the list of new entries.
     */
    public TexBibEntriesResolverResult resolveKeys(TexParserResult texParserResult) {
        TexBibEntriesResolverResult result = new TexBibEntriesResolverResult(texParserResult);
        Set<String> keySet = result.getCitationsKeySet();

        for (String key : keySet) {
            if (!result.checkEntryNewDatabase(key)) {
                Optional<BibEntry> entry = masterDatabase.getEntryByKey(key);

                if (entry.isPresent()) {
                    result.insertEntry(entry.get());
                    resolveCrossReferences(result, entry.get());
                } else {
                    result.addUnresolvedKey(key);
                }
            }
        }

        return result;
    }

    /**
     * Find cross references for inserting into the list of new entries.
     */
    private void resolveCrossReferences(TexBibEntriesResolverResult result, BibEntry entry) {
        entry.getField(FieldName.CROSSREF).ifPresent(crossRef -> {
            if (!result.checkEntryNewDatabase(crossRef)) {
                Optional<BibEntry> refEntry = masterDatabase.getEntryByKey(crossRef);

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
