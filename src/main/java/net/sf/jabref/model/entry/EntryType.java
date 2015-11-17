package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

import java.util.List;

public interface EntryType extends Comparable<BibtexEntryType> {
    String getName();

    boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database);

    List<String> getOptionalFields();

    List<String> getRequiredFields();

    boolean isRequired(String field);

    boolean isOptional(String field);

    boolean isVisibleAtNewEntryDialog();

    List<String> getRequiredFieldsForCustomization();
}
