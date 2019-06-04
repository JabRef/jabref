package org.jabref.logic.texparser;

import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.texparser.TexParserResult;

public class CrossReferences {

    private CrossReferences() throws IllegalStateException {
        throw new IllegalStateException("Utility class");
    }

    protected static void resolve(BibDatabase database, TexParserResult result, BibEntry entry) {
        entry.getField(FieldName.CROSSREF).ifPresent(crossRef -> {
            if (!result.getGeneratedBibDatabase().getEntryByKey(crossRef).isPresent()) {
                Optional<BibEntry> refEntry = database.getEntryByKey(crossRef);

                if (refEntry.isPresent()) {
                    result.getGeneratedBibDatabase().insertEntry((BibEntry) refEntry.get().clone());
                    result.increaseCrossRefEntriesCounter();
                } else {
                    result.getUnresolvedKeys().add(crossRef);
                }
            }
        });
    }
}
