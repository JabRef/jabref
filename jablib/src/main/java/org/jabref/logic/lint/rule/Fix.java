package org.jabref.logic.lint.rule;

import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// The repair belonging to a [Finding].
///
/// Applied only when JabFix is actually formatting; `--check` collects findings and never calls
/// this. A fix must repair exactly what its finding reported and nothing else, so that switching a
/// rule off removes precisely that change from the output.
///
/// The change is handed back so that whoever applied it can undo it -- this is what a save reports
/// to JabRef's undo manager.
@NullMarked
@FunctionalInterface
public interface Fix {

    /// @param entry the entry the finding was reported on
    /// @return what the repair changed, empty when the entry already carried that value or the repair
    ///         changed no field
    Optional<FieldChange> applyTo(BibEntry entry);
}
