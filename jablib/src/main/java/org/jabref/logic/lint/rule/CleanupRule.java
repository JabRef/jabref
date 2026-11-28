package org.jabref.logic.lint.rule;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.CleanupJob;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Runs one of JabRef's [CleanupJob]s as a [Rule].
///
/// Everything the cleanup already decides is left to it: which fields it applies to, and what their
/// values become. It is run on a copy of the entry, since [#scan] must not change the entry, and
/// each [FieldChange] it reports becomes a [Finding]. The finding's fix sets just that one field, so
/// a cleanup over all fields still yields one repair per field. Idempotency, which [Rule] requires,
/// is up to the wrapped cleanup.
///
/// This is what lets JabFix report what a save has always done silently: the cleanups a save applies
/// are [CleanupJob]s, and as rules they can be named, switched off and reported like any other.
@NullMarked
public abstract class CleanupRule implements Rule {

    /// The cleanup this rule reports on.
    protected abstract CleanupJob cleanupJob();

    /// What to tell the user about a change the cleanup would make.
    protected abstract String message(FieldChange change);

    @Override
    public List<Finding> scan(BibEntry entry) {
        return cleanupJob().cleanup(new BibEntry(entry)).stream()
                           .map(change -> Finding.of(this, entry, change.field(), message(change), target -> apply(change, target)))
                           .toList();
    }

    /// A cleanup removes a field whose value it formats to nothing, reported as a `null` new value.
    private static Optional<FieldChange> apply(FieldChange change, BibEntry target) {
        return Optional.ofNullable(change.newValue())
                       .map(newValue -> target.setField(change.field(), newValue))
                       .orElseGet(() -> target.clearField(change.field()));
    }
}
