package org.jabref.model.entry.field;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class UserSpecificCommentField implements Field {
    private static final Set<FieldProperty> PROPERTIES = EnumSet.of(FieldProperty.COMMENT, FieldProperty.MULTILINE_TEXT, FieldProperty.VERBATIM);
    private final String name;

    public UserSpecificCommentField(String username) {
        this.name = "comment-" + username;
    }

    @Override
    public Set<FieldProperty> getProperties() {
        return PROPERTIES;
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
        if (!(o instanceof Field other)) {
            return false;
        }
        return name.equals(other.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "UnknownCommentField{" +
                "name='" + name + '\'' +
                '}';
    }
}
