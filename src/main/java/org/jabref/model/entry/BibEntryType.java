package org.jabref.model.entry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;

public class BibEntryType implements Comparable<BibEntryType> {

    private final EntryType type;
    private final LinkedHashSet<OrFields> requiredFields;
    private final LinkedHashSet<BibField> fields;

    public BibEntryType(EntryType type, Collection<BibField> fields, Collection<OrFields> requiredFields) {
        this.type = Objects.requireNonNull(type);
        this.requiredFields = new LinkedHashSet<>(requiredFields);
        this.fields = new LinkedHashSet<>(fields);
    }

    public EntryType getType() {
        return type;
    }

    /**
     * Returns all supported optional field names.
     *
     * @return a List of optional field name Strings
     */
    public Set<BibField> getOptionalFields() {
        return getAllBibFields().stream()
                             .filter(field -> !isRequired(field.field()))
                             .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isRequired(Field field) {
        return getRequiredFields().stream()
                                  .anyMatch(fields -> fields.contains(field));
    }

    /**
     * Returns all required field names.
     * If fields have an OR relationship the name includes both field names divided by /, e.g. author/editor.
     * If you need all required fields as sole entities use @see{getRequiredFieldsFlat} .
     *
     * @return a List of required field name Strings
     */
    public Set<OrFields> getRequiredFields() {
        return Collections.unmodifiableSet(requiredFields);
    }

    /**
     * Returns all defined fields.
     */
    public Set<BibField> getAllBibFields() {
        return Collections.unmodifiableSet(fields);
    }

    public Set<Field> getAllFields() {
        return fields.stream().map(BibField::field).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Field> getPrimaryOptionalFields() {
        return getOptionalFields().stream()
                                  .filter(field -> field.priority() == FieldPriority.IMPORTANT)
                                  .map(BibField::field)
                                  .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Field> getSecondaryOptionalFields() {
        return getOptionalFields().stream()
                                  .filter(field -> field.priority() == FieldPriority.DETAIL)
                                  .map(BibField::field)
                                  .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Field> getDeprecatedFields(BibDatabaseMode mode) {
        if (mode == BibDatabaseMode.BIBTEX) {
            return Collections.emptySet();
        }
        Set<Field> deprecatedFields = new LinkedHashSet<>(EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.keySet());

        // Only the optional fields which are mapped to another BibLaTeX name should be shown as "deprecated"
        deprecatedFields.retainAll(getOptionalFieldsAndAliases());

        // BibLaTeX aims for that field "date" is used
        // Thus, year + month is deprecated
        // However, year is used in the wild very often, so we do not mark that as default as deprecated
        deprecatedFields.add(StandardField.MONTH);

        return deprecatedFields;
    }

    public Set<Field> getSecondaryOptionalNotDeprecatedFields(BibDatabaseMode mode) {
        Set<Field> optionalFieldsNotPrimaryOrDeprecated = new LinkedHashSet<>(getSecondaryOptionalFields());
        optionalFieldsNotPrimaryOrDeprecated.removeAll(getDeprecatedFields(mode));
        return optionalFieldsNotPrimaryOrDeprecated;
    }

    /**
     * Get list of all optional fields of this entry and all fields being source for a BibTeX to BibLaTeX conversion.
     */
    private Set<Field> getOptionalFieldsAndAliases() {
        Set<Field> optionalFieldsAndAliases = new LinkedHashSet<>(getOptionalFields().size());
        for (BibField field : getOptionalFields()) {
            optionalFieldsAndAliases.add(field.field());
            if (EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.containsKey(field.field())) {
                optionalFieldsAndAliases.add(field.field());
            }
        }
        return optionalFieldsAndAliases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        BibEntryType that = (BibEntryType) o;
        return type.equals(that.type) &&
               Objects.equals(requiredFields, that.requiredFields) &&
               Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, requiredFields, fields);
    }

    @Override
    public String toString() {
        return "BibEntryType{" +
               "type=" + type +
               ", requiredFields=" + requiredFields +
               ", fields=" + fields +
               '}';
    }

    @Override
    public int compareTo(BibEntryType o) {
        return this.getType().getName().compareTo(o.getType().getName());
    }
}
