package org.jabref.model.entry.field;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.strings.StringUtil;

public class UnknownField implements Field {
    private String name;
    private final Set<FieldProperty> properties;
    private final String displayName;

    public UnknownField(String name) {
        this(name, StringUtil.capitalizeFirst(name));
    }

    public UnknownField(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    public UnknownField(String name, FieldProperty first, FieldProperty... rest) {
        this(name, StringUtil.capitalizeFirst(name), first, rest);
    }

    public UnknownField(String name, String displayName, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.of(first, rest);
    }

    public static UnknownField fromDisplayName(String displayName) {
        return new UnknownField(displayName.toLowerCase(Locale.ROOT), displayName);
    }

    @Override
    public Set<FieldProperty> getProperties() {
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
    public String getDisplayName() {
        return displayName;
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
        if (!(o instanceof Field)) {
            return false;
        }
        Field other = (Field) o;
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
