package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract base class for all BibTex entry types.
 */
public abstract class BibtexEntryType implements EntryType {

    private final Set<String> requiredFields;
    private final Set<String> optionalFields;


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
    public Set<String> getOptionalFields() {
        return Collections.unmodifiableSet(optionalFields);
    }

    @Override
    public Set<String> getRequiredFields() {
        return Collections.unmodifiableSet(requiredFields);
    }

    @Override
    public int compareTo(EntryType o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public Set<String> getPrimaryOptionalFields() {
        return getOptionalFields();
    }

    @Override
    public Set<String> getSecondaryOptionalFields() {
        return getOptionalFields().stream().filter(field -> !isPrimary(field)).collect(Collectors.toSet());
    }

    private boolean isPrimary(String field) {
        return getPrimaryOptionalFields().contains(field);
    }
}
