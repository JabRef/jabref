package org.jabref.logic.shared.notifications;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Payload of a `jabrefLiveUpdate` notification.
///
/// If `field` is `null`, the change content did not fit into the payload (or was not safe to
/// send) and receivers pull from the database instead.
///
/// @param version the entry's version after the change, as assigned by the database
@NullMarked
public record FieldChange(
        String sourceProcessorId,
        @Nullable String bibEntryId,
        @Nullable String field,
        @Nullable String oldValue,
        @Nullable String newValue,
        int version) {
}
