package org.jabref.model.entry.field;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;

/// This class models a field that is not natively known to JabRef.
/// It might be a custom field added by the user.
/// Or it may originate from an importer if it cannot be mapped to one of the existing fields by its name and/or properties.
/// It is not called "CustomField", because there was the idea that StandardFields could be customized.
public class UnknownField implements Field {
    private String name;
    private final EnumSet<FieldProperty> properties;

    public UnknownField(String name) {
        this.name = name;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    public UnknownField(String name, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.properties = EnumSet.of(first, rest);
    }

    @Override
    public EnumSet<FieldProperty> getProperties() {
        return properties;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isStandardField() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Field other)) {
            return false;
        }
        return name.equalsIgnoreCase(other.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public String toString() {
        return "UnknownField{" +
                "name='" + name + '\'' +
                '}';
    }
}
