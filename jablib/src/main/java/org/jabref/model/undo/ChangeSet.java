package org.jabref.model.undo;

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
/// Today it is read in one place, the warning [#apply] logs when part of a set fails, so it is a
/// diagnostic that is written to be shown. Naming it as the user would recognise the action —
/// from [org.jabref.gui.actions.Action#getText] where the step comes from a command, from a
/// localized string otherwise — is what keeps it usable the day something renders it, which is
/// what the postponed P5 does. A developer token here is a defect, not a shortcut.
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

    /// Applies every change, continuing past a failing one.
    ///
    /// Aborting midway would leave the library in a state that is neither the old nor the new
    /// one and that no subsequent undo could describe, so a partially applied set is preferred
    /// over a partially reverted one. Failures are logged rather than propagated because
    /// callers have no meaningful recovery.
    @Override
    public void apply() {
        for (BibChange change : changes) {
            try {
                change.apply();
            } catch (RuntimeException e) {
                LOGGER.warn("Could not apply {} as part of '{}'", change, name, e);
            }
        }
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }
}
