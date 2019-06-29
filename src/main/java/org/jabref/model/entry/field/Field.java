package org.jabref.model.entry.field;

import java.util.Optional;

import org.jabref.model.strings.StringUtil;

public interface Field<SelfType extends Field<SelfType>> extends Comparable<SelfType> {
    /**
     * @return A version of the field name more suitable for display
     */
    default String getDisplayName() {
        return StringUtil.capitalizeFirst(getName());
    }

    String getName();

    default boolean isDeprecated() {
        return false;
    }

    default Optional<Field> getAlias() {
        return Optional.empty();
    }

    default int compareTo(Field o) {
        return getName().compareTo(o.getName());
    }
}
