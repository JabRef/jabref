package net.sf.jabref.model.entry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
                .map(field -> field.split("/"))
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
}
