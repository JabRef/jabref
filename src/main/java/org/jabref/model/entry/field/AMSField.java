package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum AMSField implements Field {

    FJOURNAL("fjournal");

    private final String name;
    private final String displayName;
    private final Set<FieldProperty> properties;

    AMSField(String name) {
        this.name = name;
        this.displayName = null;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    AMSField(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    AMSField(String name, String displayName, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.of(first, rest);
    }

    AMSField(String name, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.displayName = null;
        this.properties = EnumSet.of(first, rest);
    }

    public static <T> Optional<AMSField> fromName(T type, String name) {
        return Arrays.stream(AMSField.values())
                     .filter(field -> field.getName().equalsIgnoreCase(name))
                     .findAny();
    }

    @Override
    public Set<FieldProperty> getProperties() {
        return Collections.unmodifiableSet(properties);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isStandardField() {
        return false;
    }

    @Override
    public String getDisplayName() {
        if (displayName == null) {
            return Field.super.getDisplayName();
        } else {
            return displayName;
        }
    }
}
