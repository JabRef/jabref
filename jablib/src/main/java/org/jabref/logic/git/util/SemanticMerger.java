package org.jabref.logic.git.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemanticMerger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticMerger.class);
    /**
     * Applies remote's non-conflicting field changes to local entry, in-place.
     * Assumes conflict detection already run.
     */
    public static void patchEntryNonConflictingFields(BibEntry base, BibEntry local, BibEntry remote) {
        Set<Field> allFields = new HashSet<>();
        allFields.addAll(base.getFields());
        allFields.addAll(local.getFields());
        allFields.addAll(remote.getFields());

        for (Field field : allFields) {
            String baseVal = base.getField(field).orElse(null);
            String localVal = local.getField(field).orElse(null);
            String remoteVal = remote.getField(field).orElse(null);

            if (Objects.equals(baseVal, localVal) && !Objects.equals(baseVal, remoteVal)) {
                // Local untouched, remote changed -> apply remote
                if (remoteVal != null) {
                    local.setField(field, remoteVal);
                } else {
                    local.clearField(field);
                }
            } else if (!Objects.equals(baseVal, localVal) && !Objects.equals(baseVal, remoteVal) && !Objects.equals(localVal, remoteVal)) {
                // This should be conflict, but always assume it's already filtered before this class
                LOGGER.debug("Unexpected field-level conflict skipped: " + field.getName());
            }
            // else: either already applied or local wins
        }
    }

    /**
     * Applies remote diffs (based on base) onto local BibDatabaseContext.
     * - Adds new entries from remote if not present locally.
     * - Applies field-level patches on existing entries.
     * - Does NOT handle deletions.
     */
    public static void applyRemotePatchToDatabase(BibDatabaseContext base,
                                                  BibDatabaseContext local,
                                                  BibDatabaseContext remote) {
        Map<String, BibEntry> baseMap = SemanticConflictDetector.toEntryMap(base);
        Map<String, BibEntry> localMap = SemanticConflictDetector.toEntryMap(local);
        Map<String, BibEntry> remoteMap = SemanticConflictDetector.toEntryMap(remote);

        for (Map.Entry<String, BibEntry> entry : remoteMap.entrySet()) {
            String key = entry.getKey();
            BibEntry remoteEntry = entry.getValue();
            BibEntry baseEntry = baseMap.getOrDefault(key, new BibEntry());
            BibEntry localEntry = localMap.get(key);

            if (localEntry != null) {
                // Apply patch to existing entry
                patchEntryNonConflictingFields(baseEntry, localEntry, remoteEntry);
            } else if (baseEntry == null) {
                // New entry from remote (not in base or local) -> insert
                BibEntry newEntry = (BibEntry) remoteEntry.clone();
                local.getDatabase().insertEntry(newEntry);
            } else {
                // Entry was deleted in local → respect deletion (do nothing)
            }
        }
        // Optional: if localMap contains entries absent in remote+base -> do nothing (local additions)
    }
}
