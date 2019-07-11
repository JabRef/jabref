package org.jabref.model.entry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;

public class BibEntryType implements Comparable<BibEntryType> {

    private final EntryType type;
    private final Set<OrFields> requiredFields;
    private final Set<BibField> fields;

    public BibEntryType(EntryType type, Set<BibField> fields, Set<OrFields> requiredFields) {
        this.type = type;
        this.requiredFields = requiredFields;
        this.fields = fields;
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
        return getAllFields().stream()
                             .filter(field -> !isRequired(field))
                             .collect(Collectors.toSet());
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
        return requiredFields;
    }

    /**
     * Returns all defined fields.
     */
    public Set<BibField> getAllFields() {
        return Collections.unmodifiableSet(fields);
    }

    Set<BibField> getPrimaryOptionalFields() {
        return getOptionalFields().stream()
                                  .filter(field -> field.getPriority() == FieldPriority.IMPORTANT)
                                  .collect(Collectors.toSet());
    }

    Set<Field> getSecondaryOptionalFields() {
        return getOptionalFields().stream()
                                  .filter(field -> field.getPriority() == FieldPriority.DETAIL)
                                  .collect(Collectors.toSet());
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
        Set<Field> optionalFieldsAndAliases = new LinkedHashSet<>();
        for (Field field : getOptionalFields()) {
            optionalFieldsAndAliases.add(field);
            if (EntryConverter.FIELD_ALIASES_LTX_TO_TEX.containsKey(field)) {
                optionalFieldsAndAliases.add(EntryConverter.FIELD_ALIASES_LTX_TO_TEX.get(field));
            }
        }
        return optionalFieldsAndAliases;
    }

    @Override
    public int compareTo(BibEntryType o) {
        return this.getType().getName().compareTo(o.getType().getName());
    }
}
