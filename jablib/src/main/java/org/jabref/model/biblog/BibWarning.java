package org.jabref.model.biblog;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

/// Represents a warning message parsed from a BibTeX .blg file.
/// e.g. [SeverityType] -- [Message] in [EntryKey]
public record BibWarning(
        SeverityType severityType,
        String message,
        @Nullable String fieldName,
        String entryKey) {
    public Optional<String> getFieldName() {
        return Optional.ofNullable(fieldName);
    }
}
