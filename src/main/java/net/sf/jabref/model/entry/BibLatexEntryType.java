package net.sf.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class for all BibLaTex entry types.
 */
public abstract class BibLatexEntryType implements EntryType {

    private final List<String> requiredFields;
    private final List<String> optionalFields;


    public BibLatexEntryType() {
        requiredFields = new ArrayList<>();
        optionalFields = new ArrayList<>();
    }

    @Override
    public List<String> getOptionalFields() {
        return Collections.unmodifiableList(optionalFields);
    }

    @Override
    public List<String> getRequiredFields() {
        return Collections.unmodifiableList(requiredFields);
    }

    void addAllOptional(String... fieldNames) {
        optionalFields.addAll(Arrays.asList(fieldNames));
    }

    void addAllRequired(String... fieldNames) {
        requiredFields.addAll(Arrays.asList(fieldNames));
    }

    @Override
    public List<String> getPrimaryOptionalFields() {
        return getOptionalFields();
    }

    @Override
    public List<String> getSecondaryOptionalFields() {
        List<String> myOptionalFields = getOptionalFields();

        if (myOptionalFields == null) {
            return Collections.emptyList();
        }

        return myOptionalFields.stream().filter(field -> !isPrimary(field)).collect(Collectors.toList());
    }

    private boolean isPrimary(String field) {
        List<String> primaryFields = getPrimaryOptionalFields();

        if (primaryFields == null) {
            return false;
        }
        return primaryFields.contains(field);
    }

    @Override
    public int compareTo(EntryType o) {
        return getName().compareTo(o.getName());
    }
}
