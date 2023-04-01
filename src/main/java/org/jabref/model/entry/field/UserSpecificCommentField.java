package org.jabref.model.entry.field;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class UserSpecificCommentField implements Field {
    private final String name;
    private final Set<FieldProperty> properties;

    public UserSpecificCommentField(String username) {
        this.name = "comment-" + username;
        this.properties = EnumSet.of(FieldProperty.COMMENT);
    }

    @Override
    public Set<FieldProperty> getProperties() {
        return properties;
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
        return "UnknownCommentField{" +
                "name='" + name + '\'' +
                '}';
    }
}
