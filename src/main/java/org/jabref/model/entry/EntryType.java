package org.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
    List<String> getOptionalFields();

    /**
     * Returns all required field names.
     * If fields have an OR relationship the name includes both field names divided by /, e.g. author/editor.
     * If you need all required fields as sole entities use @see{getRequiredFieldsFlat} .
     *
     * @return a List of required field name Strings
     */
    List<String> getRequiredFields();

    /**
     * Returns all required field names.
     * No OR relationships are captured here.
     *
     * @return a List of required field name Strings
     */
    default List<String> getRequiredFieldsFlat() {
        List<String> requiredFlat = getRequiredFields().stream()
                .map(field -> field.split(FieldName.FIELD_SEPARATOR))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());

        return Collections.unmodifiableList(requiredFlat);
    }

    /**
     * Returns all defined (required & optional) fields.
     * No OR relationships are captured here.
     *
     * @return a List of all defined field name Strings
     */
    default List<String> getAllFields() {
        List<String> allFields = Stream.concat(getRequiredFieldsFlat().stream(), getOptionalFields().stream())
                .collect(Collectors.toList());

        return Collections.unmodifiableList(allFields);
    }

    /**
     * TODO: move inside GUI
     */
    List<String> getPrimaryOptionalFields();

    /**
     * TODO: move inside GUI
     */
    List<String> getSecondaryOptionalFields();

    default List<String> getDeprecatedFields() {
        Set<String> deprecatedFields = new HashSet<>(EntryConverter.FIELD_ALIASES_TEX_TO_LTX.keySet());
        deprecatedFields.add(FieldName.YEAR);
        deprecatedFields.add(FieldName.MONTH);

        deprecatedFields.retainAll(getOptionalFieldsAndAliases());

        return new ArrayList<>(deprecatedFields);
    }

    default List<String> getSecondaryOptionalNotDeprecatedFields() {
        List<String> optionalFieldsNotPrimaryOrDeprecated = new ArrayList<>(getSecondaryOptionalFields());
        optionalFieldsNotPrimaryOrDeprecated.removeAll(getDeprecatedFields());
        return optionalFieldsNotPrimaryOrDeprecated;
    }

    /**
     * Get list of all optional fields of this entry and their aliases.
     */
    default Set<String> getOptionalFieldsAndAliases() {
        Set<String> optionalFieldsAndAliases = new HashSet<>();
        for (String field : getOptionalFields()) {
            optionalFieldsAndAliases.add(field);
            if (EntryConverter.FIELD_ALIASES_LTX_TO_TEX.containsKey(field)) {
                optionalFieldsAndAliases.add(EntryConverter.FIELD_ALIASES_LTX_TO_TEX.get(field));
            }
        }
        return optionalFieldsAndAliases;
    }
}
