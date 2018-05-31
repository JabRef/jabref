package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Interface for all EntryTypes.
 */
public interface EntryType extends Comparable<EntryType> {
    /**
     * Returns the tag name of the entry type.
     *
     * @return tag name of the entry type.
     */
    String getName();

    /**
     * Returns all supported optional field names.
     *
     * @return a List of optional field name Strings
     */
    Set<String> getOptionalFields();

    /**
     * Returns all required field names.
     * If fields have an OR relationship the name includes both field names divided by /, e.g. author/editor.
     * If you need all required fields as sole entities use @see{getRequiredFieldsFlat} .
     *
     * @return a List of required field name Strings
     */
    Set<String> getRequiredFields();

    /**
     * Returns all required field names.
     * No OR relationships are captured here.
     *
     * @return a List of required field name Strings
     */
    default Set<String> getRequiredFieldsFlat() {
        List<String> requiredFlat = getRequiredFields().stream()
                .map(field -> field.split(FieldName.FIELD_SEPARATOR))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());

        return Collections.unmodifiableSet(new LinkedHashSet<>(requiredFlat));
    }

    /**
     * Returns all defined (required & optional) fields.
     * No OR relationships are captured here.
     *
     * @return a List of all defined field name Strings
     */
    default Set<String> getAllFields() {
        List<String> allFields = Stream.concat(getRequiredFieldsFlat().stream(), getOptionalFields().stream())
                .collect(Collectors.toList());

        return Collections.unmodifiableSet(new LinkedHashSet<>(allFields));
    }

    /**
     * TODO: move inside GUI
     */
    Set<String> getPrimaryOptionalFields();

    /**
     * TODO: move inside GUI
     */
    Set<String> getSecondaryOptionalFields();

    default Set<String> getDeprecatedFields() {
        Set<String> deprecatedFields = new LinkedHashSet<>(EntryConverter.FIELD_ALIASES_TEX_TO_LTX.keySet());
        deprecatedFields.add(FieldName.YEAR);
        deprecatedFields.add(FieldName.MONTH);

        deprecatedFields.retainAll(getOptionalFieldsAndAliases());

        return deprecatedFields;
    }

    default Set<String> getSecondaryOptionalNotDeprecatedFields() {
        Set<String> optionalFieldsNotPrimaryOrDeprecated = new LinkedHashSet<>(getSecondaryOptionalFields());
        optionalFieldsNotPrimaryOrDeprecated.removeAll(getDeprecatedFields());
        return optionalFieldsNotPrimaryOrDeprecated;
    }

    /**
     * Get list of all optional fields of this entry and their aliases.
     */
    default Set<String> getOptionalFieldsAndAliases() {
        Set<String> optionalFieldsAndAliases = new LinkedHashSet<>();
        for (String field : getOptionalFields()) {
            optionalFieldsAndAliases.add(field);
            if (EntryConverter.FIELD_ALIASES_LTX_TO_TEX.containsKey(field)) {
                optionalFieldsAndAliases.add(EntryConverter.FIELD_ALIASES_LTX_TO_TEX.get(field));
            }
        }
        return optionalFieldsAndAliases;
    }
}
