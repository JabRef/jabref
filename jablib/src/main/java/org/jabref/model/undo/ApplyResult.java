package org.jabref.model.undo;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/// What applying a [BibChange] achieved.
///
/// A change that describes one modification either performs it or throws, so it reports
/// [#SUCCESS] and nothing else. A [ChangeSet] is the one implementation that can come back with
/// less than it promised: it applies best-effort, because aborting midway would leave the
/// library in a state that is neither the old nor the new one and that no later undo could
/// describe. Saying so here is what keeps it a substitutable [BibChange] — it reports the
/// success it delivered rather than the one its siblings guarantee.
///
/// @param failures the changes that could not be applied, in the order they were attempted
@NullMarked
public record ApplyResult(List<Failure> failures) {

    /// Everything asked for was applied.
    public static final ApplyResult SUCCESS = new ApplyResult(List.of());

    public ApplyResult {
        failures = List.copyOf(failures);
    }

    /// A change that could not be applied, and what stopped it.
    public record Failure(BibChange change, RuntimeException cause) {
    }

    public boolean isComplete() {
        return failures.isEmpty();
    }
}
