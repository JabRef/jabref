package org.jabref.logic.git.util;

import java.util.Map;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemanticMerger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticMerger.class);

    /**
     * Implementation-only merge logic: applies changes from remote (relative to base) to local.
     * does not check for "modifications" or "conflicts" — all decisions should be handled in advance by the SemanticConflictDetector
     */
    public static void applyMergePlan(BibDatabaseContext local, MergePlan plan) {
        applyPatchToDatabase(local, plan.fieldPatches());

        for (BibEntry newEntry : plan.newEntries()) {
            BibEntry clone = (BibEntry) newEntry.clone();
            local.getDatabase().insertEntry(clone);
            LOGGER.debug("Inserted new entry '{}'", newEntry.getCitationKey().orElse("?"));
        }
    }

    public static void applyPatchToDatabase(BibDatabaseContext local, Map<String, Map<Field, String>> patchMap) {
        for (Map.Entry<String, Map<Field, String>> entry : patchMap.entrySet()) {
            String key = entry.getKey();
            Map<Field, String> fieldPatch = entry.getValue();
            Optional<BibEntry> maybeLocalEntry = local.getDatabase().getEntryByCitationKey(key);

            if (maybeLocalEntry.isEmpty()) {
                LOGGER.warn("Skip patch: local does not contain entry '{}'", key);
                continue;
            }

            BibEntry localEntry = maybeLocalEntry.get();
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
