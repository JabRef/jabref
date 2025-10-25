package org.jabref.logic.git.merge.planning.util;

import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public final class AutoPlan {
    private static final FieldPatchComputer PATCHER = new FieldPatchComputer();

    /// Generate the safe auto-merge plan for a single citation key.
    /// Precondition: this key has passed the semantic conflict gate.
    ///
    /// Rules:
    ///  - base == null:
    ///      B0) only local added -> NO-OP (remote − base is empty)
    ///      B1) both added & no overlapping-field disagreement -> add union entry
    ///      B2) only remote added -> add remote entry
    ///  - base != null:
    ///      B3) remote deleted -> accept deletion if local kept base OR local also deleted
    ///      B4) otherwise field-level patch: (remote − base) applied where local == base
    ///
    /// Notes:
    ///  - We mutate the supplied collections (fieldPatches/newEntries/deletedEntryKeys).
    ///  - This method is side-effect free for all other state.
    public static void generateAutoPlanForKey(String key,
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
                Map<Field, String> patch = FieldPatchComputer.compute(null, localEntry, remoteEntry);
                if (!patch.isEmpty()) {
                    fieldPatches.put(key, patch);
                }
                return;
            }
            // B2: only remote added -> add remote entry
            if (localEntry == null && remoteEntry != null) {
                newEntries.add(remoteEntry);
            }
            return;
        }

        // base exists
        // B3: remote deletion -> accept if local kept base OR local also deleted
        if (remoteEntry == null) {
            boolean localDeleted = (localEntry == null);
            boolean localKeptBase = !localDeleted && baseEntry.getFieldMap().equals(localEntry.getFieldMap());
            if (localDeleted || localKeptBase) {
                deletedEntryKeys.add(key);
            }
            return;
        }

        // B4: field-level patch (remote − base), only where local == base
        Map<Field, String> patch = FieldPatchComputer.compute(baseEntry, localEntry, remoteEntry);
        if (!patch.isEmpty()) {
            fieldPatches.put(key, patch);
        }
    }
}
