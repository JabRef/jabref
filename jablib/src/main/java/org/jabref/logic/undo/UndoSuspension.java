package org.jabref.logic.undo;

import org.jspecify.annotations.NullMarked;

/// Undo and redo, suspended for one library while a command applies changes it has not yet handed
/// to the journal.
///
/// Taking a change back over the top of writes that are not yet recorded produces a library state
/// no undo step describes, so [UndoManager#suspendUndo] opens one of these for as long as that
/// window is open, and the undo UI declines meanwhile. It is **not** a lock: two commands may write
/// to one library at the same time, and undo returns once the last of them has handed over.
///
/// Closing is idempotent and may happen on any thread, so a task that can finish through more than
/// one path — success, failure, cancellation — may close on all of them.
@NullMarked
public interface UndoSuspension extends AutoCloseable {

    /// Held by work that is already inside another suspension, where ending one early would reopen
    /// the window. Closing it does nothing.
    UndoSuspension NONE = () -> {
    };

    /// Ends the suspension. Never throws, so it is safe in a `finally` and in try-with-resources.
    @Override
    void close();
}
