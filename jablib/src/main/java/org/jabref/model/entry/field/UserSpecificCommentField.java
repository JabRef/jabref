package org.jabref.model.entry.field;

import java.util.EnumSet;
import java.util.Objects;

public class UserSpecificCommentField implements Field {
    private static final EnumSet<FieldProperty> PROPERTIES = EnumSet.of(FieldProperty.MULTILINE_TEXT, FieldProperty.MARKDOWN);
    private final String name;

    public UserSpecificCommentField(String username) {
        this.name = "comment-" + username;
    }

    @Override
    public EnumSet<FieldProperty> getProperties() {
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
