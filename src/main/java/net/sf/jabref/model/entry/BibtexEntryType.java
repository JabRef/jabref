package net.sf.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class for all BibTex entry types.
 */
public abstract class BibtexEntryType implements EntryType {

    private final List<String> requiredFields;
    private final List<String> optionalFields;


    public BibtexEntryType() {
        requiredFields = new ArrayList<>();
        optionalFields = new ArrayList<>();
    }

    void addAllOptional(String... fieldNames) {
        optionalFields.addAll(Arrays.asList(fieldNames));
    }

    void addAllRequired(String... fieldNames) {
        requiredFields.addAll(Arrays.asList(fieldNames));
    }

    @Override
    public List<String> getOptionalFields() {
        return Collections.unmodifiableList(optionalFields);
    }

    @Override
    public List<String> getRequiredFields() {
        return Collections.unmodifiableList(requiredFields);
    }

    @Override
    public int compareTo(EntryType o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public List<String> getPrimaryOptionalFields() {
        return getOptionalFields();
    }

    @Override
    public List<String> getSecondaryOptionalFields() {
        return getOptionalFields().stream().filter(field -> !isPrimary(field)).collect(Collectors.toList());
    }

    private boolean isPrimary(String field) {
        return getPrimaryOptionalFields().contains(field);
    }
}
