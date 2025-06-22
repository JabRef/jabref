package org.jabref.logic.git.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class SemanticConflictDetector {
    /**
     * result := local + remoteDiff
     * and then create merge commit having result as file content and local and remote branch as parent
     */
    public static List<BibEntryDiff> detectConflicts(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        // 1. get diffs between base and remote
        List<BibEntryDiff> remoteDiffs = BibDatabaseDiff.compare(base, remote).getEntryDifferences();
        if (remoteDiffs == null) {
            return List.of();
        }
        // 2. map citation key to entry for local/remote diffs
        Map<String, BibEntry> baseEntries = toEntryMap(base);
        Map<String, BibEntry> localEntries = toEntryMap(local);

        List<BibEntryDiff> conflicts = new ArrayList<>();

        // 3. look for entries modified in both local and remote
        for (BibEntryDiff remoteDiff : remoteDiffs) {
            Optional<String> keyOpt = remoteDiff.newEntry().getCitationKey();
            if (keyOpt.isEmpty()) {
                continue;
            }

            String citationKey = keyOpt.get();
            BibEntry baseEntry = baseEntries.get(citationKey);
            BibEntry localEntry = localEntries.get(citationKey);
            BibEntry remoteEntry = remoteDiff.newEntry();

            // if the entry exists in all 3 versions
            if (baseEntry != null && localEntry != null && remoteEntry != null) {
                if (hasConflictingFields(baseEntry, localEntry, remoteEntry)) {
                    conflicts.add(new BibEntryDiff(localEntry, remoteEntry));
                }
            }
        }
        return conflicts;
    }

    public static Map<String, BibEntry> toEntryMap(BibDatabaseContext context) {
        return context.getDatabase().getEntries().stream()
                      .filter(entry -> entry.getCitationKey().isPresent())
                      .collect(Collectors.toMap(
                              entry -> entry.getCitationKey().get(),
                              Function.identity(),
                              (existing, replacement) -> replacement,
                              LinkedHashMap::new
                      ));
    }

    private static boolean hasConflictingFields(BibEntry base, BibEntry local, BibEntry remote) {
        // Go through union of all fields
        Set<Field> fields = new HashSet<>();
        fields.addAll(base.getFields());
        fields.addAll(local.getFields());
        fields.addAll(remote.getFields());

        for (Field field : fields) {
            String baseVal = base.getField(field).orElse(null);
            String localVal = local.getField(field).orElse(null);
            String remoteVal = remote.getField(field).orElse(null);

            boolean localChanged = !Objects.equals(baseVal, localVal);
            boolean remoteChanged = !Objects.equals(baseVal, remoteVal);

            if (localChanged && remoteChanged && !Objects.equals(localVal, remoteVal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares base and remote, finds all semantic-level changes (new entries, updated fields), and builds a patch plan.
     * This plan is meant to be applied to local during merge:
     * result = local + (remote − base)
     *
     * @param base The base version of the database.
     * @param remote The remote version to be merged.
     * @return A {@link MergePlan} describing how to update the local copy with remote changes.
     */
    public static MergePlan extractMergePlan(BibDatabaseContext base, BibDatabaseContext remote) {
        Map<String, BibEntry> baseMap = toEntryMap(base);
        Map<String, BibEntry> remoteMap = toEntryMap(remote);

        Map<String, Map<Field, String>> fieldPatches = new LinkedHashMap<>();
        List<BibEntry> newEntries = new ArrayList<>();

        for (Map.Entry<String, BibEntry> remoteEntryPair : remoteMap.entrySet()) {
            String key = remoteEntryPair.getKey();
            BibEntry remoteEntry = remoteEntryPair.getValue();
            BibEntry baseEntry = baseMap.get(key);

            if (baseEntry == null) {
                // New entry (not in base)
                newEntries.add(remoteEntry);
            } else {
                Map<Field, String> patch = computeFieldPatch(baseEntry, remoteEntry);
                if (!patch.isEmpty()) {
                    fieldPatches.put(key, patch);
                }
            }
        }

        return new MergePlan(fieldPatches, newEntries);
    }

    /**
     * Compares base and remote and constructs a patch at the field level. null == the field is deleted.
     */
    private static Map<Field, String> computeFieldPatch(BibEntry base, BibEntry remote) {
        Map<Field, String> patch = new LinkedHashMap<>();

        Set<Field> allFields = new LinkedHashSet<>();
        allFields.addAll(base.getFields());
        allFields.addAll(remote.getFields());

        for (Field field : allFields) {
            String baseValue = base.getField(field).orElse(null);
            String remoteValue = remote.getField(field).orElse(null);

            if (!Objects.equals(baseValue, remoteValue)) {
                patch.put(field, remoteValue);
            }
        }

        return patch;
    }
}
