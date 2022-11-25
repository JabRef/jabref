package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.entry.types.BiblatexSoftwareEntryType;

public enum BiblatexSoftwareField implements Field {

    HALID("hal_id"),
    HALVERSION("hal_version"),
    INTRODUCEDIN("introducedin"),
    LICENSE("license"),
    RELATEDTYPE("relatedtype"),
    RELATEDSTRING("relatedstring"),
    REPOSITORY("repository"),
    SWHID("swhid");

    private final String name;
    private final String displayName;
    private final Set<FieldProperty> properties;

    BiblatexSoftwareField(String name) {
        this.name = name;
        this.displayName = null;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    BiblatexSoftwareField(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    BiblatexSoftwareField(String name, String displayName, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.of(first, rest);
    }

    BiblatexSoftwareField(String name, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.displayName = null;
        this.properties = EnumSet.of(first, rest);
    }

    public static <T> Optional<BiblatexSoftwareField> fromName(T type, String name) {
        if (!(type instanceof BiblatexSoftwareEntryType)) {
            return Optional.empty();
        }
        return Arrays.stream(BiblatexSoftwareField.values())
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
