package org.jabref.logic.bibtex.comparator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class GitBibEntryDiff {
    private final BibEntry baseEntry;
    private final BibEntry localEntry;
    private final BibEntry remoteEntry;
    private final Map<Field, FieldChange> fieldChanges;

    public GitBibEntryDiff(BibEntry baseEntry, BibEntry localEntry, BibEntry remoteEntry) {
        this.baseEntry = baseEntry;
        this.localEntry = localEntry;
        this.remoteEntry = remoteEntry;
        this.fieldChanges = calculateFieldChanges();
    }

    private Map<Field, FieldChange> calculateFieldChanges() {
        Map<Field, FieldChange> changes = new HashMap<>();

        Set<Field> allFields = Stream.of(
                                             baseEntry != null ? baseEntry.getFields() : Set.of(),
                                             localEntry != null ? localEntry.getFields() : Set.of(),
                                             remoteEntry != null ? remoteEntry.getFields() : Set.of())
                                     .flatMap(Set::stream)
                                     .map(field -> (Field) field)
                                     .collect(Collectors.toSet());

        for (Field field : allFields) {
            String baseValue = baseEntry != null ? baseEntry.getField(field).orElse(null) : null;
            String localValue = localEntry != null ? localEntry.getField(field).orElse(null) : null;
            String remoteValue = remoteEntry != null ? remoteEntry.getField(field).orElse(null) : null;

            if (baseValue == null && localValue == null && remoteValue == null) {
                continue;
            }

            if (!Objects.equals(baseValue, localValue) || !Objects.equals(baseValue, remoteValue)) {
                changes.put(field, new FieldChange(field, baseValue, localValue, remoteValue));
            }
        }

        return changes;
    }

    public boolean hasConflicts() {
        return fieldChanges.values().stream().anyMatch(FieldChange::hasConflict);
    }

    public BibEntry baseEntry() {
        return baseEntry;
    }

    public BibEntry localEntry() {
        return localEntry;
    }

    public BibEntry remoteEntry() {
        return remoteEntry;
    }

    public Map<Field, FieldChange> getFieldChanges() {
        return fieldChanges;
    }

    public static class FieldChange {
        private final Field field;
        private final String baseValue;
        private final String localValue;
        private final String remoteValue;

        public FieldChange(Field field, String baseValue, String localValue, String remoteValue) {
            this.field = field;
            this.baseValue = baseValue;
            this.localValue = localValue;
            this.remoteValue = remoteValue;
        }

        public boolean hasConflict() {
            if (Objects.equals(localValue, remoteValue)) {
                return false;
            }

            return !(Objects.equals(baseValue, localValue) || Objects.equals(baseValue, remoteValue));
        }

        public String getResolvedValue() {
            if (!hasConflict()) {
                if (Objects.equals(localValue, remoteValue)) {
                    return localValue;
                }
                if (Objects.equals(baseValue, localValue)) {
                    return remoteValue;
                }
                return localValue;
            }
            return null;
        }

        public Field getField() {
            return field;
        }

        public String getBaseValue() {
            return baseValue;
        }

        public String getLocalValue() {
            return localValue;
        }

        public String getRemoteValue() {
            return remoteValue;
        }
    }
}
