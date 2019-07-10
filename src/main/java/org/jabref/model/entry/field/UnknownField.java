package org.jabref.model.entry.field;

import java.util.EnumSet;
import java.util.Set;

import org.jabref.model.entry.FieldProperty;

public class UnknownField implements Field<UnknownField> {
    private final String name;

    public UnknownField(String name) {
        this.name = name;
    }

    @Override
    public Set<FieldProperty> getProperties() {
        return EnumSet.noneOf(FieldProperty.class);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isStandardField() {
        return false;
    }
}
