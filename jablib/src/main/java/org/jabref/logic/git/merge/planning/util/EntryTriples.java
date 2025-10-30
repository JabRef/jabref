package org.jabref.logic.git.merge.planning.util;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public final class EntryTriples {
    public final Map<String, BibEntry> baseMap;
    public final Map<String, BibEntry> localMap;
    public final Map<String, BibEntry> remoteMap;

    private EntryTriples(Map<String, BibEntry> baseMap,
                         Map<String, BibEntry> localMap,
                         Map<String, BibEntry> remoteMap) {
        this.baseMap = baseMap;
        this.localMap = localMap;
        this.remoteMap = remoteMap;
    }

    public static EntryTriples from(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        return new EntryTriples(
                getCitationKeyToEntryMap(base),
                getCitationKeyToEntryMap(local),
                getCitationKeyToEntryMap(remote)
        );
    }

    public static Map<String, BibEntry> getCitationKeyToEntryMap(BibDatabaseContext context) {
        return context.getDatabase().getEntries().stream()
                      .filter(entry -> entry.getCitationKey().isPresent())
                      .collect(Collectors.toMap(
                              entry -> entry.getCitationKey().get(),
                              Function.identity(),
                              (existing, replacement) -> replacement,
                              LinkedHashMap::new
                      ));
    }

    // TODO(merge/analyze): Entry key source & the role of BibDatabaseDiff
    //  Previous approach:
    //  1) Run BibDatabaseDiff twice (base→local, base→remote).
    //     BibDatabaseDiff localDiff  = BibDatabaseDiff.compare(base, local);
    //     BibDatabaseDiff remoteDiff = BibDatabaseDiff.compare(base, remote);
    //  2) Build citationKey→BibEntryDiff maps and use the union of their key sets as the worklist.
    //     Map<String, BibEntryDiff> localDiffMap  = indexByCitationKey(localDiff.getEntryDifferences());
    //     Map<String, BibEntryDiff> remoteDiffMap = indexByCitationKey(remoteDiff.getEntryDifferences());
    //     Set<String> allKeys = union(localDiffMap.keySet(), remoteDiffMap.keySet());
    //  3) For each key, resolve base/local/remote via the diff maps, then detect conflicts and build the auto plan.
    //  Problem: Using only diff-derived keys misses pure deletions → E05/E08 fail.
    //  - E05: base has 'a', local=∅, remote=∅ → compare(base, ∅) yields no entryDiff, so 'a' never enters the worklist.
    //  - E08: base has 'a', local==base, remote=∅ → remote diff empty; 'a' is dropped.
    //  Root cause: when the “new” DB is empty, BibDatabaseDiff returns no entryDiff.
    //  However, should we keep BibDatabaseDiff for meta/preamble/bibstrings, logging, and potential optimizations,

    public Set<String> allKeys() {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(baseMap.keySet());
        all.addAll(localMap.keySet());
        all.addAll(remoteMap.keySet());
        return all;
    }
}
