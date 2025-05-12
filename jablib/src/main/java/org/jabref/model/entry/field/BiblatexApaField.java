package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;

import org.jabref.model.entry.types.BiblatexApaEntryType;

public enum BiblatexApaField implements Field {

    AMENDMENT("amendment"),
    ARTICLE("article"),
    CITATION("citation"),
    CITATION_CITEORG("citation_citeorg"),
    CITATION_CITEDATE("citation_citedate", FieldProperty.DATE),
    CITATION_CITEINFO("citation_citeinfo"),
    SECTION("section", FieldProperty.NUMERIC),
    SOURCE("source");

    private final String name;
    private final String displayName;
    private final EnumSet<FieldProperty> properties;

    BiblatexApaField(String name) {
        this.name = name;
        this.displayName = null;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    BiblatexApaField(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    BiblatexApaField(String name, String displayName, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.of(first, rest);
    }

    BiblatexApaField(String name, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.displayName = null;
        this.properties = EnumSet.of(first, rest);
    }

    public static <T> Optional<BiblatexApaField> fromName(T type, String name) {
        if (!(type instanceof BiblatexApaEntryType)) {
            // Also returns nothing if no type is given.
            // Reason: The field should also be recognized in the presence of a BiblatexApa entry type.
            return Optional.empty();
        }
        return Arrays.stream(BiblatexApaField.values())
                     .filter(field -> field.getName().equalsIgnoreCase(name))
                     .findAny();
    }

    @Override
    public EnumSet<FieldProperty> getProperties() {
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
    public String getDisplayName() {
        if (displayName == null) {
            return Field.super.getDisplayName();
        } else {
            return displayName;
        }
    }
}
