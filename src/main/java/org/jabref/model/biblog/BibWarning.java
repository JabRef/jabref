package org.jabref.model.biblog;

import java.util.Objects;
import java.util.Optional;

/**
 *
 */
public class BibWarning {
    //  [SeverityType] -- [Message] in [EntryKey]
    private final SeverityType severityType;
    private final String message;
    private final String fieldName;
    private final String entryKey;

    public BibWarning(SeverityType severityType, String message, String fieldName, String entryKey) {
        this.severityType = severityType;
        this.message = message;
        this.fieldName = fieldName;
        this.entryKey = entryKey;
    }

    public String getEntryKey() {
        return entryKey;
    }

    public Optional<String> getFieldName() {
        return Optional.ofNullable(fieldName);
    }

    public String getMessage() {
        return message;
    }

    public SeverityType getSeverityType() {
        return severityType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BibWarning that)) {
            return false;
        }
        return severityType == that.severityType &&
                Objects.equals(message, that.message) &&
                Objects.equals(fieldName, that.fieldName) &&
                Objects.equals(entryKey, that.entryKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(severityType, message, fieldName, entryKey);
    }
}
