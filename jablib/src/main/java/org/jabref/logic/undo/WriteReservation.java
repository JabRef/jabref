package org.jabref.logic.undo;

import org.jspecify.annotations.NullMarked;

/// A command's claim on a library while it applies changes it has not yet handed to the journal.
///
/// Obtained from [UndoManager#reserveWrites], and closed once the changes are on the stack. While
/// one is open the library's undo and redo decline, because taking a change back over the top of
/// writes that are not yet recorded produces a library state no undo step describes.
///
/// Closing is idempotent and may happen on any thread, so a task that can finish through more than
/// one path — success, failure, cancellation — may close on all of them.
@NullMarked
public interface WriteReservation extends AutoCloseable {

    /// Releases the claim. Never throws, so it is safe in a `finally` and in try-with-resources.
    @Override
    void close();
}
