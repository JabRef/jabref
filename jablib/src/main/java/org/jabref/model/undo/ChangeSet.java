package org.jabref.model.undo;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A group of changes that the user performed as one action, and that undo therefore has to
/// reverse as one action.
///
/// A `ChangeSet` is itself a [BibChange], so sets may nest — a command delegating to a helper
/// does not have to flatten anything. Only the outermost set is pushed onto the undo stack, so
/// one user action stays one undo step regardless of nesting.
///
/// `name` is the only text in the change model, and it exists at this granularity because that
/// is the granularity the user acts in: "Merge entries", not "change field author of entry X".
///
/// It is shown to the user: the notification after an undo names the step, and the warning
/// [#apply] logs when part of a set fails carries it too. So it is named as the user would
/// recognise the action — from [org.jabref.gui.actions.Action#getText] where the step comes from
/// a command, from a localized string otherwise. A developer token here is a defect.
@NullMarked
public record ChangeSet(String name, List<BibChange> changes) implements BibChange {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeSet.class);

    public ChangeSet {
        changes = List.copyOf(changes);
    }

    /// Reverses the order *and* inverts each element — both are required for a group to undo
    /// correctly, because later changes may depend on earlier ones.
    @Override
    public ChangeSet inverted() {
        return new ChangeSet(name, changes.reversed().stream().map(BibChange::inverted).toList());
    }

    /// Applies every change, continuing past a failing one, and reports what did not make it.
    ///
    /// Aborting midway would leave the library in a state that is neither the old nor the new
    /// one and that no subsequent undo could describe, so a partially applied set is preferred
    /// over a partially reverted one. Failures are not propagated, because a caller half-way
    /// through a set has no meaningful recovery; they are returned, because a caller that
    /// believes the whole set was applied has been told something untrue.
    ///
    /// Two things come back as failures: an element that threw, and an element that refused
    /// because the library no longer holds what it recorded. Failures of nested sets travel up as
    /// they are, so what comes back names the changes that failed rather than the sets that
    /// contained them.
    @Override
    public ApplyResult apply() {
        List<ApplyResult.Failure> failures = new ArrayList<>();
        for (BibChange change : changes) {
            try {
                failures.addAll(change.apply().failures());
            } catch (RuntimeException e) {
                LOGGER.warn("Could not apply {} as part of '{}'", change, name, e);
                failures.add(new ApplyResult.Failure(change, e.toString()));
            }
        }
        return failures.isEmpty() ? ApplyResult.SUCCESS : new ApplyResult(failures);
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }
}
