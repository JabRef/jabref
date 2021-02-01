package org.jabref.model.entry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
                             .filter(field -> !isRequired(field.getField()))
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
        return fields.stream().map(BibField::getField).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Field> getPrimaryOptionalFields() {
        return getOptionalFields().stream()
                                  .filter(field -> field.getPriority() == FieldPriority.IMPORTANT)
                                  .map(BibField::getField)
                                  .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Field> getSecondaryOptionalFields() {
        return getOptionalFields().stream()
                                  .filter(field -> field.getPriority() == FieldPriority.DETAIL)
                                  .map(BibField::getField)
                                  .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Field> getDeprecatedFields() {
        Set<Field> deprecatedFields = new LinkedHashSet<>(EntryConverter.FIELD_ALIASES_TEX_TO_LTX.keySet());
        deprecatedFields.add(StandardField.YEAR);
        deprecatedFields.add(StandardField.MONTH);

        deprecatedFields.retainAll(getOptionalFieldsAndAliases());

        return deprecatedFields;
    }

    public Set<Field> getSecondaryOptionalNotDeprecatedFields() {
        Set<Field> optionalFieldsNotPrimaryOrDeprecated = new LinkedHashSet<>(getSecondaryOptionalFields());
        optionalFieldsNotPrimaryOrDeprecated.removeAll(getDeprecatedFields());
        return optionalFieldsNotPrimaryOrDeprecated;
    }

    /**
     * Get list of all optional fields of this entry and their aliases.
     */
    private Set<Field> getOptionalFieldsAndAliases() {
        Set<Field> optionalFieldsAndAliases = new LinkedHashSet<>(getOptionalFields().size());
        for (BibField field : getOptionalFields()) {
            optionalFieldsAndAliases.add(field.getField());
            if (EntryConverter.FIELD_ALIASES_LTX_TO_TEX.containsKey(field.getField())) {
                optionalFieldsAndAliases.add(EntryConverter.FIELD_ALIASES_LTX_TO_TEX.get(field.getField()));
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
