package org.jabref.logic.git.merge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.model.MergePlan;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/// Single-pass, three-way semantic merge planner:
///   - For each citation key that changed on either side (base→local or base→remote), we first detect semantic conflicts (entry- and field-level).
///   - Only if no conflict is found for that key, we generate the auto-merge plan; (remote - base) applied on local, but ONLY on fields where local kept base.
public final class SemanticMergeAnalyzer {
    private SemanticMergeAnalyzer() { }

    public static MergeAnalysis analyze(BibDatabaseContext base,
                                        BibDatabaseContext local,
                                        BibDatabaseContext remote) {
        // 1) union of all citationKeys that were changed (on either side)
        EntryTriples triples = EntryTriples.from(base, local, remote);

        // 2) get diffs between base, local and remote
        BibDatabaseDiff localDiff = BibDatabaseDiff.compare(base, local);
        BibDatabaseDiff remoteDiff = BibDatabaseDiff.compare(base, remote);

        Map<String, BibEntryDiff> localDiffMap = indexByCitationKey(localDiff.getEntryDifferences());
        Map<String, BibEntryDiff> remoteDiffMap = indexByCitationKey(remoteDiff.getEntryDifferences());

        Set<String> allKeys = new LinkedHashSet<>(localDiffMap.keySet());
        allKeys.addAll(remoteDiffMap.keySet());

        Map<String, Map<Field, String>> fieldPatches = new LinkedHashMap<>();
        List<BibEntry> newEntries = new ArrayList<>();
        List<String> deletedEntryKeys = new ArrayList<>();
        List<ThreeWayEntryConflict> conflicts = new ArrayList<>();

        for (String key : allKeys) {
            BibEntry baseEntry = triples.baseMap.get(key);
            BibEntry localEntry = resolveEntry(key, localDiffMap.get(key), triples.localMap);
            BibEntry remoteEntry = resolveEntry(key, remoteDiffMap.get(key), triples.remoteMap);

            // A) determining semantic conflicts first
            if (detectEntryConflict(baseEntry, localEntry, remoteEntry).isPresent()) {
                conflicts.add(new ThreeWayEntryConflict(baseEntry, localEntry, remoteEntry));
                continue;
            }

            // B) no semantic conflicts，generate autoPlan
            generateAutoPlanForKey(
                    key,
                    baseEntry,
                    localEntry,
                    remoteEntry,
                    fieldPatches,
                    newEntries,
                    deletedEntryKeys
            );
        }

        return new MergeAnalysis(new MergePlan(fieldPatches, newEntries, deletedEntryKeys), conflicts);
    }

    // ----------------------------
    // A) Semantic conflict detection
    // ----------------------------

    /**
     * Detect entry-level conflicts among base, local, and remote versions of an entry.
     * <p>
     *
     * @param base the entry in the common ancestor
     * @param local the entry in the local version
     * @param remote the entry in the remote version
     * @return optional conflict (if detected)
     */
    private static Optional<ThreeWayEntryConflict> detectEntryConflict(BibEntry base,
                                                                       BibEntry local,
                                                                       BibEntry remote) {
        // Case 1: Both local and remote added same citation key -> compare their fields
        if (base == null && local != null && remote != null) {
            if (hasConflictingFields(new BibEntry(), local, remote)) {
                return Optional.of(new ThreeWayEntryConflict(null, local, remote));
            } else {
                return Optional.empty();
            }
        }

        // Case 2: base exists, one side deleted, other modified -> conflict
        if (base != null) {
            boolean localDeleted = local == null;
            boolean remoteDeleted = remote == null;

            boolean localChanged = !localDeleted && !base.getFieldMap().equals(local.getFieldMap());
            boolean remoteChanged = !remoteDeleted && !base.getFieldMap().equals(remote.getFieldMap());

            if ((localChanged && remoteDeleted) || (remoteChanged && localDeleted)) {
                return Optional.of(new ThreeWayEntryConflict(base, local, remote));
            }
        }

        // Case 3: base exists, both sides modified the entry -> check field-level diff
        if (base != null && local != null && remote != null) {
            boolean localChanged = !base.getFieldMap().equals(local.getFieldMap());
            boolean remoteChanged = !base.getFieldMap().equals(remote.getFieldMap());

            if (localChanged && remoteChanged && hasConflictingFields(base, local, remote)) {
                return Optional.of(new ThreeWayEntryConflict(base, local, remote));
            }
        }

        return Optional.empty();
    }

    private static boolean hasConflictingFields(BibEntry base, BibEntry local, BibEntry remote) {
        if (entryTypeChangedDifferently(base, local, remote)) {
            return true;
        }

        Set<Field> allFields = Stream.of(base, local, remote)
                                     .flatMap(entry -> entry.getFields().stream())
                                     .collect(Collectors.toSet());

        for (Field field : allFields) {
            String baseVal = base.getField(field).orElse(null);
            String localVal = local.getField(field).orElse(null);
            String remoteVal = remote.getField(field).orElse(null);

            // Case 1: Both local and remote modified the same field from base, and the values differ
            if (modifiedOnBothSidesWithDisagreement(baseVal, localVal, remoteVal)) {
                return true;
            }

            // Case 2: One side deleted the field, the other side modified it
            if (oneSideDeletedOneSideModified(baseVal, localVal, remoteVal)) {
                return true;
            }

            // Case 3: Both sides added the field with different values
            if (addedOnBothSidesWithDisagreement(baseVal, localVal, remoteVal)) {
                return true;
            }
        }

        return false;
    }

    private static boolean entryTypeChangedDifferently(BibEntry base, BibEntry local, BibEntry remote) {
        if (base == null || local == null || remote == null) {
            return false;
        }

        boolean localChanged = !base.getType().equals(local.getType());
        boolean remoteChanged = !base.getType().equals(remote.getType());
        boolean changedToDifferentTypes = !local.getType().equals(remote.getType());

        return localChanged && remoteChanged && changedToDifferentTypes;
    }

    private static boolean modifiedOnBothSidesWithDisagreement(String baseVal, String localVal, String remoteVal) {
        return notEqual(baseVal, localVal) && notEqual(baseVal, remoteVal) && notEqual(localVal, remoteVal);
    }

    private static boolean oneSideDeletedOneSideModified(String baseVal, String localVal, String remoteVal) {
        if (localVal == null && remoteVal == null) {
            return false;
        }

        return (baseVal != null)
                && ((localVal == null && notEqual(baseVal, remoteVal))
                || (remoteVal == null && notEqual(baseVal, localVal)));
    }

    private static boolean addedOnBothSidesWithDisagreement(String baseVal, String localVal, String remoteVal) {
        return baseVal == null && localVal != null && remoteVal != null && notEqual(localVal, remoteVal);
    }

    private static boolean notEqual(String a, String b) {
        return !Objects.equals(a, b);
    }

    /// Compares base and remote and constructs a patch at the field level. null == the field is deleted.
    ///
    /// - Apply remote change when local kept base value (including deletions: null);
    /// - If both sides changed to the same value, no patch needed;
    /// - Fallback: if a divergence is still observed, do not override local; skip this field,
    ///
    /// @param base base version
    /// @param local local version
    /// @param remote remote version
    /// @return A map from field to new value
    private static Map<Field, String> computeFieldPatch(BibEntry base, BibEntry local, BibEntry remote) {
        Map<Field, String> patch = new LinkedHashMap<>();

        if (remote == null) {
            return patch;
        }

        Stream.concat(base.getFields().stream(), remote.getFields().stream())
              .distinct()
              .filter(field -> !isMetaField(field))
              .forEach(field -> {
                  String baseValue = base.getField(field).orElse(null);
                  String remoteValue = remote.getField(field).orElse(null);
                  String localValue = local == null ? null : local.getField(field).orElse(null);

                  if (Objects.equals(baseValue, remoteValue)) {
                      return;
                  }
                  if (Objects.equals(localValue, baseValue)) {
                      patch.put(field, remoteValue);
                      return;
                  }
                  if (Objects.equals(localValue, remoteValue)) {
                      return;
                  }
              });
        return patch;
    }

    private static boolean isMetaField(Field f) {
        String n = f.getName();
        return n.startsWith("_") || "_jabref_shared".equalsIgnoreCase(n);
    }

    private static boolean hasOverlappingFieldDisagreement(BibEntry left, BibEntry right) {
        Set<Field> overlap = new LinkedHashSet<>(left.getFields());
        overlap.retainAll(right.getFields());
        overlap.removeIf(SemanticMergeAnalyzer::isMetaField);
        for (Field overlappingField : overlap) {
            String leftFieldValue = left.getField(overlappingField).orElse(null);
            String rightFieldValue = right.getField(overlappingField).orElse(null);
            if (!leftFieldValue.equals(rightFieldValue)) {
                return true;
            }
        }
        return false;
    }

    private static BibEntry unionEntry(BibEntry localAdded, BibEntry remoteAdded) {
        BibEntry merged = new BibEntry(localAdded);
        for (Field f : remoteAdded.getFields()) {
            if (isMetaField(f)) {
                continue;
            }
            if (merged.getField(f).isEmpty()) {
                merged.setField(f, remoteAdded.getField(f).get());
            }
        }
        return merged;
    }

    // ---------------------------------------- B) Auto-merge plan (safe-only generation) ----------------------------------------

    /// Generate the safe auto-merge plan for a single citation key.
    /// Precondition: this key has passed the semantic conflict gate.
    ///
    /// Rules:
    ///  - base == null:
    ///      B0) only local added -> NO-OP (remote − base is empty)
    ///      B1) both added & no overlapping-field disagreement -> add union entry
    ///      B2) only remote added -> add remote entry
    ///  - base != null:
    ///      B3) remote deleted -> accept deletion ONLY if local kept base
    ///      B4) otherwise field-level patch: (remote − base) applied where local == base
    ///
    /// Notes:
    ///  - We mutate the supplied collections (fieldPatches/newEntries/deletedEntryKeys).
    ///  - This method is side-effect free for all other state.
    private static void generateAutoPlanForKey(String key,
                                               BibEntry baseEntry,
                                               BibEntry localEntry,
                                               BibEntry remoteEntry,
                                               Map<String, Map<Field, String>> fieldPatches,
                                               List<BibEntry> newEntries,
                                               List<String> deletedEntryKeys) {

        // base == null cases:
        if (baseEntry == null) {
            // B0: only local added -> no-op (keep local as-is)
            if (localEntry != null && remoteEntry == null) {
                return;
            }
            // B1: both added the same key -> safe union if no overlapping-field disagreement
            if (localEntry != null && remoteEntry != null) {
                if (!hasOverlappingFieldDisagreement(localEntry, remoteEntry)) {
                    newEntries.add(unionEntry(localEntry, remoteEntry));
                }
                return;
            }
            // B2: only remote added -> add remote entry
            if (localEntry == null && remoteEntry != null) {
                newEntries.add(remoteEntry); // consider cloning if you want stronger immutability guarantees
            }
            return;
        }

        // base exists
        // B3: remote deletion -> accept if local kept base (E08)
        if (remoteEntry == null) {
            boolean localDeleted = (localEntry == null);
            boolean localKeptBase = !localDeleted && baseEntry.getFieldMap().equals(localEntry.getFieldMap());
            if (localKeptBase) {
                deletedEntryKeys.add(key);
            }
            return;
        }

        // B4: field-level patch (remote − base), only where local == base
        Map<Field, String> patch = computeFieldPatch(baseEntry, localEntry, remoteEntry);
        if (!patch.isEmpty()) {
            fieldPatches.put(key, patch);
        }
    }

    // ---------------------------- Utilities ----------------------------

    /**
     * Converts a List of BibEntryDiff into a Map where the key is the citation key,
     * and the value is the corresponding BibEntryDiff.
     * <p>
     * Notes:
     * - Only entries with a citation key are included (entries without a key cannot be uniquely identified during computeMergePlan).
     * - Entries that represent additions (base == null) or deletions (new == null) are also included.
     * - If multiple BibEntryDiffs share the same citation key (rare), the latter one will overwrite the former.
     * <p>
     *
     * @param entryDiffs A list of entry diffs produced by BibDatabaseDiff
     * @return A map from citation key to corresponding BibEntryDiff
     */
    private static Map<String, BibEntryDiff> indexByCitationKey(List<BibEntryDiff> entryDiffs) {
        Map<String, BibEntryDiff> result = new LinkedHashMap<>();

        for (BibEntryDiff diff : entryDiffs) {
            Optional<String> citationKey = Optional.ofNullable(diff.newEntry())
                                                   .flatMap(BibEntry::getCitationKey)
                                                   .or(() -> Optional.ofNullable(diff.originalEntry())
                                                                     .flatMap(BibEntry::getCitationKey));
            citationKey.ifPresent(key -> result.put(key, diff));
        }

        return result;
    }

    private static BibEntry resolveEntry(String key, BibEntryDiff diff, Map<String, BibEntry> fullMap) {
        if (diff == null) {
            return fullMap.get(key);
        }
        return diff.newEntry(); // new=null -> delete；base=null -> new entry
    }

    static final class EntryTriples {
        final Map<String, BibEntry> baseMap;
        final Map<String, BibEntry> localMap;
        final Map<String, BibEntry> remoteMap;

        private EntryTriples(Map<String, BibEntry> baseMap,
                             Map<String, BibEntry> localMap,
                             Map<String, BibEntry> remoteMap) {
            this.baseMap = baseMap;
            this.localMap = localMap;
            this.remoteMap = remoteMap;
        }

        static EntryTriples from(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
            return new EntryTriples(
                    getCitationKeyToEntryMap(base),
                    getCitationKeyToEntryMap(local),
                    getCitationKeyToEntryMap(remote)
            );
        }
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
}
