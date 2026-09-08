package org.jabref.model.undo;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/// What applying a [BibChange] achieved.
///
/// Two things stop a change from being applied. A [ChangeSet] applies best-effort, aborting
/// midway would leave the library in a state that is neither the old nor the new one, and that no
/// later undo could describe, so it reports the elements that threw. And a change that describes
/// one modification refuses when the library no longer holds the state it recorded, rather than
/// writing over whatever is there now.
///
/// @param failures the changes that were not applied, in the order they were attempted
@NullMarked
public record ApplyResult(List<Failure> failures) {
    /// Everything asked for was applied.
    public static final ApplyResult SUCCESS = new ApplyResult(List.of());

    public ApplyResult {
        failures = List.copyOf(failures);
    }

    /// A change that was not applied, and why — the reason is for a log or a message, not for a
    /// caller to branch on.
    public record Failure(BibChange change, String reason) {
    }

    /// One change that was not applied.
    public static ApplyResult of(BibChange change, String reason) {
        return new ApplyResult(List.of(new Failure(change, reason)));
    }

    public boolean complete() {
        return failures.isEmpty();
    }
}
