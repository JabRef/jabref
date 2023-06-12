package org.jabref.model.entry.field;

import java.util.Optional;
import java.util.Set;

import org.jabref.model.strings.StringUtil;

public interface Field {

    /**
     * properties contains mappings to tell the EntryEditor to add a specific function to this field,
     * for instance a dropdown for selecting the month for the month field.
     *
     * Note that this set needs to be mutable. This is required, because we allow standard fields to be modifiable via the UI.
     */
    Set<FieldProperty> getProperties();

    /**
     * @return A version of the field name more suitable for display
     */
    default String getDisplayName() {
        return StringUtil.capitalizeFirst(getName());
    }

    String getName();

    boolean isStandardField();

    default boolean isDeprecated() {
        return false;
    }

    default Optional<Field> getAlias() {
        return Optional.empty();
    }

    default boolean isNumeric() {
        return getProperties().contains(FieldProperty.NUMERIC);
    }

    default boolean isMultiLineDefined() {
        return getProperties().contains(FieldProperty.MULTILINE_TEXT);
    }
}
