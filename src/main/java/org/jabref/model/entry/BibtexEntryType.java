package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.field.Field;

/**
 * Abstract base class for all BibTex entry types.
 */
public abstract class BibtexEntryType implements EntryType {

    private final Set<Field> requiredFields;
    private final Set<Field> optionalFields;


    public BibtexEntryType() {
        requiredFields = new LinkedHashSet<>();
        optionalFields = new LinkedHashSet<>();
    }

    void addAllOptional(String... fieldNames) {
        optionalFields.addAll(Arrays.asList(fieldNames));
    }

    void addAllRequired(String... fieldNames) {
        requiredFields.addAll(Arrays.asList(fieldNames));
    }

    @Override
    public Set<Field> getOptionalFields() {
        return Collections.unmodifiableSet(optionalFields);
    }

    @Override
    public Set<Field> getRequiredFields() {
        return Collections.unmodifiableSet(requiredFields);
    }

    @Override
    public int compareTo(EntryType o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public Set<Field> getPrimaryOptionalFields() {
        return getOptionalFields();
    }

    @Override
    public Set<Field> getSecondaryOptionalFields() {
        return getOptionalFields().stream().filter(field -> !isPrimary(field)).collect(Collectors.toSet());
    }

    private boolean isPrimary(Field field) {
        return getPrimaryOptionalFields().contains(field);
    }

    @Override
    public String toString() {
        return getName();
    }
}
