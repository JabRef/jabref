package org.jabref.logic.crawler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jabref.logic.database.DuplicateCheck;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

public class AutomaticDuplicateRemover {
    private final BibEntryTypesManager bibEntryTypesManager;

    public AutomaticDuplicateRemover(BibEntryTypesManager bibEntryTypesManager) {
        this.bibEntryTypesManager = bibEntryTypesManager;
    }

    public void removeDuplicates(BibDatabaseContext databaseContext) {
        DuplicateCheck duplicateCheck = new DuplicateCheck(bibEntryTypesManager);
        BibDatabase database = databaseContext.getDatabase();
        List<BibEntry> entries = database.getEntries();
        List<BibEntry> entriesToRemove = new ArrayList<>();
        Set<BibEntry> handledEntries = new HashSet<>();

        for (int i = 0; i < entries.size(); i++) {
            BibEntry entry1 = entries.get(i);
            if (handledEntries.contains(entry1)) {
                continue;
            }

            for (int j = i + 1; j < entries.size(); j++) {
                BibEntry entry2 = entries.get(j);
                if (handledEntries.contains(entry2)) {
                    continue;
                }

                if (duplicateCheck.isDuplicate(entry1, entry2, databaseContext.getMode())) {
                    entry1.mergeWith(entry2);
                    entriesToRemove.add(entry2);
                    handledEntries.add(entry2);
                }
            }
            handledEntries.add(entry1);
        }

        for (BibEntry entry : entriesToRemove) {
            database.removeEntry(entry);
        }
    }
}
