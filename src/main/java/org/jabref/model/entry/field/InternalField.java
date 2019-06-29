package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Optional;

/**
 * JabRef internal fields
 */
public enum InternalField implements Field<InternalField> {
    INTERNAL_ALL_FIELD("all"),
    INTERNAL_ALL_TEXT_FIELDS_FIELD("all-text-fields"),
    SEARCH_INTERNAL("__search"),
    GROUPSEARCH_INTERNAL("__groupsearch"),
    MARKED_INTERNAL("__markedentry"),
    OWNER("owner"),
    TIMESTAMP("timestamp"), // Not the actual field name, but the default value
    GROUPS("groups"),
    KEY_FIELD("bibtexkey"),
    TYPE_HEADER("entrytype"),
    OBSOLETE_TYPE_HEADER("bibtextype"),
    BIBTEX_STRING("__string"),
    INTERNAL_ID_FIELD("JabRef-internal-id");

    private final String name;

    InternalField(String name) {
        this.name = name;
    }

    public static Optional<InternalField> fromName(String name) {
        return Arrays.stream(InternalField.values())
                     .filter(field -> field.getName().equalsIgnoreCase(name))
                     .findAny();
    }

    @Override
    public String getName() {
        return name;
    }
}
