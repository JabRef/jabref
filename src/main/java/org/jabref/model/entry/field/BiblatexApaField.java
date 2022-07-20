package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

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
    private final Set<FieldProperty> properties;

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
            return Optional.empty();
        }
        return Arrays.stream(BiblatexApaField.values())
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

    @Override
    public String getDisplayName() {
        if (displayName == null) {
            return Field.super.getDisplayName();
        } else {
            return displayName;
        }
    }

}
