package org.jabref.logic.shared.notifications;

/// Payload of a `jabrefLiveUpdate` notification.
///
/// If `field` is `null`, the change content did not fit into the payload (or was not safe to
/// send) and receivers pull from the database instead.
///
/// @param version the entry's version after the change, as assigned by the database
public record FieldChange(
        String sourceProcessorId,
        String bibEntryId,
        String field,
        String oldValue,
        String newValue,
        int version) {}
