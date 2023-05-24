package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * JabRef internal fields. These are not normal fields but mostly placeholders with special functions.
 */
public enum InternalField implements Field {
    /**
     * The BibTeX key (which is used at \cite{key} in LaTeX
     */
    KEY_FIELD("citationkey"),

    /**
     * field which indicates the entrytype
     *
     * Example: @misc{key}
     */
    TYPE_HEADER("entrytype"),

    /**
     * Used in old layout files
     */
    OBSOLETE_TYPE_HEADER("bibtextype"),

    /**
     * used in old versions of JabRef. Currently used for conversion only
     */
    MARKED_INTERNAL("__markedentry"),

    /**
     * Marker that the content is just a BibTeX string
     */
    BIBTEX_STRING("__string"),

    /**
     * virtual field to denote "all fields". Used in the metadata serialization for save actions.
     */
    INTERNAL_ALL_FIELD("all"),

    /**
     * virtual field to denote "all text fields". Used in the metadata serialization for save actions.
     */
    INTERNAL_ALL_TEXT_FIELDS_FIELD("all-text-fields"),

    /**
     * all field names starting with "Jabref-internal-" are not appearing in .bib files
     */
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
        if ("bibtexkey".equalsIgnoreCase(name)) {
            // For backwards compatibility
            return Optional.of(InternalField.KEY_FIELD);
        }

        return Arrays.stream(InternalField.values())
                     .filter(field -> field.getName().equalsIgnoreCase(name))
                     .findAny();
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
}
