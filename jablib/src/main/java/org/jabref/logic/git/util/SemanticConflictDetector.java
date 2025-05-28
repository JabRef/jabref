package org.jabref.logic.git.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class SemanticConflictDetector {
    public static List<BibEntryDiff> detectConflicts(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        // 1. get diffs between base and local
        // List<BibEntryDiff> localDiffs = BibDatabaseDiff.compare(base, local).getEntryDifferences();
        // 2. get diffs between base and remote
        List<BibEntryDiff> remoteDiffs = BibDatabaseDiff.compare(base, remote).getEntryDifferences();
        // 3. map citation key to entry for local/remote diffs
        Map<String, BibEntry> baseEntries = toEntryMap(base);
        Map<String, BibEntryDiff> localDiffMap = toDiffMap(localDiffs);
        Map<String, BibEntryDiff> remoteDiffMap = toDiffMap(remoteDiffs);

        // result := local + remoteDiff
        // and then create merge commit having result as file content and local and remotebranch as parent

        List<BibEntryDiff> conflicts = new ArrayList<>();

        // 4. look for entries modified in both local and remote
        for (String citationKey : localDiffMap.keySet()) {
            // ignore only local modified
            if (!remoteDiffMap.containsKey(citationKey)) {
                continue;
            }
            BibEntryDiff localDiff = localDiffMap.get(citationKey);
            BibEntryDiff remoteDiff = remoteDiffMap.get(citationKey);

            // get versions of this entry in base/local/remote;
            BibEntry baseEntry = baseEntries.get(citationKey);
            BibEntry localEntry = localDiff.newEntry();
            BibEntry remoteEntry = remoteDiff.newEntry();

            if (baseEntry != null && localEntry != null && remoteEntry != null) {
                // check if there are any field conflicts
                if (hasConflictingFields(baseEntry, localEntry, remoteEntry)) {
                    conflicts.add(new BibEntryDiff(localEntry, remoteEntry));
                }
            }
        }

        return conflicts;
    }

    private static Map<String, BibEntryDiff> toDiffMap(List<BibEntryDiff> diffs) {
        return diffs.stream()
                    .filter(diff -> diff.newEntry().getCitationKey().isPresent())
                    .collect(Collectors.toMap(
                            diff -> diff.newEntry().getCitationKey().get(),
                            Function.identity()));
    }

    public static Map<String, BibEntry> toEntryMap(BibDatabaseContext ctx) {
        return ctx.getDatabase().getEntries().stream()
                  .filter(entry -> entry.getCitationKey().isPresent())
                  .collect(Collectors.toMap(
                          entry -> entry.getCitationKey().get(),
                          Function.identity()));
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
}
