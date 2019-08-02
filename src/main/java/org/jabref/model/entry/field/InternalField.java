package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * JabRef internal fields
 */
public enum InternalField implements Field {
    INTERNAL_ALL_FIELD("all"),
    INTERNAL_ALL_TEXT_FIELDS_FIELD("all-text-fields"),
    MARKED_INTERNAL("__markedentry"),
    OWNER("owner"),
    TIMESTAMP("timestamp", FieldProperty.DATE),
    GROUPS("groups"),
    KEY_FIELD("bibtexkey"),
    TYPE_HEADER("entrytype"),
    OBSOLETE_TYPE_HEADER("bibtextype"),
    BIBTEX_STRING("__string"),
    INTERNAL_ID_FIELD("JabRef-internal-id");

    private final String name;
    private final Set<FieldProperty> properties;

    InternalField(String name) {
        this.name = name;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    InternalField(String name, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.properties = EnumSet.of(first, rest);
    }

    public static Optional<InternalField> fromName(String name) {
        return Arrays.stream(InternalField.values())
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
}
