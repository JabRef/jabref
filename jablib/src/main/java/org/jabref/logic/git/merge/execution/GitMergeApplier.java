package org.jabref.logic.git.merge.execution;

import java.util.List;

import org.jabref.logic.git.model.MergePlan;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class GitMergeApplier {
    /// Apply (remote - base) patches safely in the current BibDatabaseContext, plus safe new/deleted entries.
    public static void applyAutoPlan(BibDatabaseContext bibDatabaseContext, MergePlan plan) {
        for (BibEntry entry : plan.newEntries()) {
            bibDatabaseContext.getDatabase().insertEntry(new BibEntry(entry));
        }
        plan.fieldPatches().forEach((key, patch) ->
                bibDatabaseContext.getDatabase().getEntryByCitationKey(key).ifPresent(entry -> {
                    patch.forEach((field, newValue) -> {
                        if (newValue == null) {
                            entry.clearField(field);
                        } else {
                            entry.setField(field, newValue);
                        }
                    });
                })
        );
        for (String key : plan.deletedEntryKeys()) {
            bibDatabaseContext.getDatabase().getEntryByCitationKey(key).ifPresent(entry ->
                    bibDatabaseContext.getDatabase().removeEntry(entry)
            );
        }
    }

    /// Apply user-resolved entries into the current BibDatabaseContext: replace or insert by citation key.
    /// (Aligned with `MergeEntriesAction`’s “edit the in-memory library first” philosophy.)
    public static void applyResolved(BibDatabaseContext bibDatabaseContext, List<BibEntry> resolved) {
        for (BibEntry merged : resolved) {
            merged.getCitationKey().ifPresent(key -> {
                bibDatabaseContext.getDatabase().getEntryByCitationKey(key).ifPresentOrElse(existing -> {
                    existing.setType(merged.getType());
                    existing.getFields().forEach(field -> {
                        if (merged.getField(field).isEmpty()) {
                            existing.clearField(field);
                        }
                    });
                    merged.getFields().forEach(field -> merged.getField(field).ifPresent(value -> existing.setField(field, value)));
                }, () -> bibDatabaseContext.getDatabase().insertEntry(new BibEntry(merged)));
            });
        }
    }
}
