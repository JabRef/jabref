package org.jabref.logic.shared.notifications;

public record FieldChange(
        String sourceProcessorId,
        String bibEntryId,
        String field,
        String oldValue,
        String newValue) {}
