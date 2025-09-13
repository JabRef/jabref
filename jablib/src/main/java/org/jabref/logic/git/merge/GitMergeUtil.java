package org.jabref.logic.git.merge;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class GitMergeUtil {
    /**
     * Replace conflicting entries in the remote context with user-resolved versions.
     *
     * @param remote          the original remote BibDatabaseContext
     * @param resolvedEntries list of entries that the user has manually resolved via GUI
     * @return a new BibDatabaseContext with resolved entries replacing original ones
     */
    public static BibDatabaseContext replaceEntries(BibDatabaseContext remote, List<BibEntry> resolvedEntries) {
        // 1. make a copy of the remote database
        BibDatabase newDatabase = new BibDatabase();
        // 2. build a map of resolved entries by citation key (assuming all resolved entries have keys)
        Map<String, BibEntry> resolvedMap = resolvedEntries.stream()
                                                           .filter(entry -> entry.getCitationKey().isPresent())
                                                           .collect(Collectors.toMap(
                                                                   entry -> entry.getCitationKey().get(),
                                                                   Function.identity()));

        // 3. Iterate original remote entries
        for (BibEntry entry : remote.getDatabase().getEntries()) {
            String citationKey = entry.getCitationKey().orElse(null);

            if (citationKey != null && resolvedMap.containsKey(citationKey)) {
                // Skip: this entry will be replaced
                continue;
            }

            // Clone the entry and add it to new DB
            newDatabase.insertEntry(new BibEntry(entry));
        }

        // 4. Insert all resolved entries (cloned for safety)
        for (BibEntry resolved : resolvedEntries) {
            newDatabase.insertEntry(new BibEntry(resolved));
        }

        // 5. Construct a new BibDatabaseContext with this new database and same metadata
        return new BibDatabaseContext(newDatabase, remote.getMetaData());
    }
}
