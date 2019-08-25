package org.jabref.model.entry;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;

public class BibEntryType implements Comparable<BibEntryType> {

    private final EntryType type;
    private final SortedSet<OrFields> requiredFields;
    private final SortedSet<BibField> fields;

    public BibEntryType(EntryType type, Collection<BibField> fields, Collection<OrFields> requiredFields) {
        this.type = Objects.requireNonNull(type);
        this.requiredFields = new TreeSet<>(requiredFields);
        this.fields = new TreeSet<>(fields);
    }

    public EntryType getType() {
        return type;
    }

    /**
     * Returns all supported optional field names.
     *
     * @return a List of optional field name Strings
     */
    public SortedSet<BibField> getOptionalFields() {
        return getAllFields().stream()
                             .filter(field -> !isRequired(field.getField()))
                             .collect(Collectors.toCollection(TreeSet::new));
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
    public SortedSet<OrFields> getRequiredFields() {
        return Collections.unmodifiableSortedSet(requiredFields);
    }

    /**
     * Returns all defined fields.
     */
    public SortedSet<BibField> getAllFields() {
        return Collections.unmodifiableSortedSet(fields);
    }

    public SortedSet<BibField> getPrimaryOptionalFields() {
        return getOptionalFields().stream()
                                  .filter(field -> field.getPriority() == FieldPriority.IMPORTANT)
                                  .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedSet<Field> getSecondaryOptionalFields() {
        return getOptionalFields().stream()
                                  .filter(field -> field.getPriority() == FieldPriority.DETAIL)
                                  .map(BibField::getField)
                                  .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Field::getName))));
    }

    public SortedSet<Field> getDeprecatedFields() {
        SortedSet<Field> deprecatedFields = new TreeSet<>(Comparator.comparing(Field::getName));
        deprecatedFields.addAll(EntryConverter.FIELD_ALIASES_TEX_TO_LTX.keySet());
        deprecatedFields.add(StandardField.YEAR);
        deprecatedFields.add(StandardField.MONTH);

        deprecatedFields.retainAll(getOptionalFieldsAndAliases());

        return deprecatedFields;
    }

    public SortedSet<Field> getSecondaryOptionalNotDeprecatedFields() {
        SortedSet<Field> optionalFieldsNotPrimaryOrDeprecated = new TreeSet<>(Comparator.comparing(Field::getName));
        optionalFieldsNotPrimaryOrDeprecated.addAll(getSecondaryOptionalFields());
        optionalFieldsNotPrimaryOrDeprecated.removeAll(getDeprecatedFields());
        return optionalFieldsNotPrimaryOrDeprecated;
    }

    /**
     * Get list of all optional fields of this entry and their aliases.
     */
    private SortedSet<Field> getOptionalFieldsAndAliases() {
        SortedSet<Field> optionalFieldsAndAliases = new TreeSet<>(Comparator.comparing(Field::getName));
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BibEntryType that = (BibEntryType) o;
        return type.equals(that.type) &&
                requiredFields.equals(that.requiredFields) &&
                fields.equals(that.fields);
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
