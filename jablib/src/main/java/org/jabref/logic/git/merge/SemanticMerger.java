package org.jabref.logic.git.merge;

import java.util.Map;
import java.util.Optional;

import org.jabref.logic.git.conflicts.SemanticConflictDetector;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemanticMerger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticMerger.class);

    /**
     * Implementation-only merge logic: applies changes from remote (relative to base) to local.
     * does not check for "modifications" or "conflicts"
     * all decisions should be handled in advance by the {@link SemanticConflictDetector}
     */
    public static void applyMergePlan(BibDatabaseContext localCopy, MergePlan plan) {
        applyPatchToDatabase(localCopy, plan.fieldPatches());

        for (BibEntry newEntry : plan.newEntries()) {
            BibEntry clone = new BibEntry(newEntry);

            clone.getCitationKey().ifPresent(citationKey ->
                    localCopy.getDatabase().getEntryByCitationKey(citationKey).ifPresent(existing -> {
                        localCopy.getDatabase().removeEntry(existing);
                        LOGGER.debug("Removed existing entry '{}' before re-inserting", citationKey);
                    })
            );

            localCopy.getDatabase().insertEntry(clone);
            LOGGER.debug("Inserted (or replaced) entry '{}', fields={}, marked as changed",
                    clone.getCitationKey().orElse("?"),
                    clone.getFieldMap());
        }
    }

    public static void applyPatchToDatabase(BibDatabaseContext localCopy, Map<String, Map<Field, String>> patchMap) {
        for (Map.Entry<String, Map<Field, String>> entry : patchMap.entrySet()) {
            String key = entry.getKey();
            Map<Field, String> fieldPatch = entry.getValue();
            Optional<BibEntry> localEntryOpt = localCopy.getDatabase().getEntryByCitationKey(key);

            if (localEntryOpt.isEmpty()) {
                LOGGER.warn("Skip patch: local does not contain entry '{}'", key);
                continue;
            }

            BibEntry localEntry = localEntryOpt.get();
            applyFieldPatchToEntry(localEntry, fieldPatch);
        }
    }

    public static void applyFieldPatchToEntry(BibEntry localEntry, Map<Field, String> patch) {
        for (Map.Entry<Field, String> diff : patch.entrySet()) {
            Field field = diff.getKey();
            String newValue = diff.getValue();
            String oldValue = localEntry.getField(field).orElse(null);

            if (newValue == null) {
                localEntry.clearField(field);
                LOGGER.debug("Cleared field '{}' (was '{}')", field.getName(), oldValue);
            } else {
                localEntry.setField(field, newValue);
                LOGGER.debug("Set field '{}' to '{}', replacing '{}'", field.getName(), newValue, oldValue);
            }
        }
    }
}
