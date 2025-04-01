package org.jabref.model.biblog;

import java.util.Optional;

/**
 * Represents a warning message parsed from a BibTeX .blg file.
 * e.g. [SeverityType] -- [Message] in [EntryKey]
 */
public record BibWarning(
        SeverityType severityType,
        String message,
        String fieldName,
        String entryKey) {
    public Optional<String> getFieldName() {
        return Optional.ofNullable(fieldName);
    }
}
