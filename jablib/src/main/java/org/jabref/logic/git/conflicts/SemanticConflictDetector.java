package org.jabref.logic.git.conflicts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.git.merge.MergePlan;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/// Detects semantic merge conflicts between base, local, and remote.
///
/// Strategy:
/// Instead of computing full diffs from base to local/remote, we simulate a Git-style merge
/// by applying the diff between base and remote onto local (`result := local + remoteDiff`).
///
/// Caveats:
/// - Only entries with the same citation key are considered matching.
/// - Entries without citation keys are currently ignored.
///   - TODO: Improve handling of such entries.
///     See: `BibDatabaseDiffTest#compareOfTwoEntriesWithSameContentAndMixedLineEndingsReportsNoDifferences`
/// - Changing a citation key is not supported and is treated as deletion + addition.
public class SemanticConflictDetector {
    public static List<ThreeWayEntryConflict> detectConflicts(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        // 1. get diffs between base and remote
        List<BibEntryDiff> remoteDiffs = BibDatabaseDiff.compare(base, remote).getEntryDifferences();

        // 2. map citation key to entry for local/remote diffs
        Map<String, BibEntry> baseEntries = getCitationKeyToEntryMap(base);
        Map<String, BibEntry> localEntries = getCitationKeyToEntryMap(local);

        List<ThreeWayEntryConflict> conflicts = new ArrayList<>();

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

            // Case 1: if the entry exists in all 3 versions
            if (baseEntry != null && localEntry != null && remoteEntry != null) {
                if (hasConflictingFields(baseEntry, localEntry, remoteEntry)) {
                    conflicts.add(new ThreeWayEntryConflict(baseEntry, localEntry, remoteEntry));
                }
            // Case 2: base missing, but local + remote both added same citation key with different content
            } else if (baseEntry == null && localEntry != null && remoteEntry != null) {
                if (!Objects.equals(localEntry, remoteEntry)) {
                    conflicts.add(new ThreeWayEntryConflict(null, localEntry, remoteEntry));
                }
            // Case 3: one side deleted, other side modified
            } else if (baseEntry != null) {
                if (localEntry != null && remoteEntry == null && !Objects.equals(baseEntry, localEntry)) {
                    conflicts.add(new ThreeWayEntryConflict(baseEntry, localEntry, null));
                }
                if (localEntry == null && remoteEntry != null && !Objects.equals(baseEntry, remoteEntry)) {
                    conflicts.add(new ThreeWayEntryConflict(baseEntry, null, remoteEntry));
                }
            }
        }
        return conflicts;
    }

    private static Map<String, BibEntry> getCitationKeyToEntryMap(BibDatabaseContext context) {
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
        return Stream.of(base, local, remote)
                     .flatMap(entry -> entry.getFields().stream())
                     .distinct()
                     .anyMatch(field -> {
                         String baseVal = base.getField(field).orElse(null);
                         String localVal = local.getField(field).orElse(null);
                         String remoteVal = remote.getField(field).orElse(null);

                         boolean localChanged = !Objects.equals(baseVal, localVal);
                         boolean remoteChanged = !Objects.equals(baseVal, remoteVal);

                         return localChanged && remoteChanged && !Objects.equals(localVal, remoteVal);
                     });
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
        Map<String, BibEntry> baseMap = getCitationKeyToEntryMap(base);
        Map<String, BibEntry> remoteMap = getCitationKeyToEntryMap(remote);

        Map<String, Map<Field, String>> fieldPatches = new LinkedHashMap<>();
        List<BibEntry> newEntries = new ArrayList<>();

        for (Map.Entry<String, BibEntry> remoteEntryPair : remoteMap.entrySet()) {
            String key = remoteEntryPair.getKey();
            BibEntry remoteEntry = remoteEntryPair.getValue();
            BibEntry baseEntry = baseMap.get(key);

            if (baseEntry == null) {
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
     *
     * @param base base version
     * @param remote remote version
     * @return A map from field to new value
     */
    private static Map<Field, String> computeFieldPatch(BibEntry base, BibEntry remote) {
        Map<Field, String> patch = new LinkedHashMap<>();

        Stream.concat(base.getFields().stream(), remote.getFields().stream())
              .distinct()
              .forEach(field -> {
                  String baseValue = base.getField(field).orElse(null);
                  String remoteValue = remote.getField(field).orElse(null);

                  if (!Objects.equals(baseValue, remoteValue)) {
                      patch.put(field, remoteValue);
                  }
              });

        return patch;
    }
}
