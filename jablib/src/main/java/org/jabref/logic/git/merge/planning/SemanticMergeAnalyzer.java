package org.jabref.logic.git.merge.planning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.merge.planning.util.AutoPlan;
import org.jabref.logic.git.merge.planning.util.ConflictRules;
import org.jabref.logic.git.merge.planning.util.EntryTriples;
import org.jabref.logic.git.model.MergeAnalysis;
import org.jabref.logic.git.model.MergePlan;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/// Single-pass, three-way semantic merge planner:
///   - For each citation key that changed on either side (base→local or base→remote), we first detect semantic conflicts (entry- and field-level).
///   - Only if no conflict is found for that key, we generate the auto-merge plan; (remote - base) applied on local, but ONLY on fields where local kept base.
public final class SemanticMergeAnalyzer {
    public static MergeAnalysis analyze(BibDatabaseContext base,
                                        BibDatabaseContext local,
                                        BibDatabaseContext remote) {
        // 1) union of all citationKeys that were changed (on either side)
        EntryTriples triples = EntryTriples.from(base, local, remote);

        Map<String, Map<Field, String>> fieldPatches = new LinkedHashMap<>();
        List<BibEntry> newEntries = new ArrayList<>();
        List<String> deletedEntryKeys = new ArrayList<>();
        List<ThreeWayEntryConflict> conflicts = new ArrayList<>();

        for (String key : triples.allKeys()) {
            BibEntry baseEntry = triples.baseMap.get(key);
            BibEntry localEntry = triples.localMap.get(key);
            BibEntry remoteEntry = triples.remoteMap.get(key);

            // A) determining semantic conflicts first
            Optional<ThreeWayEntryConflict> threeWayEntryConflictsOpt = ConflictRules.detectEntryConflict(baseEntry, localEntry, remoteEntry);
            if (threeWayEntryConflictsOpt.isPresent()) {
                conflicts.add(new ThreeWayEntryConflict(baseEntry, localEntry, remoteEntry));
                continue;
            }

            // B) no semantic conflicts，generate autoPlan
            AutoPlan.generateAutoPlanForKey(
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
}
