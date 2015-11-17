package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

import java.util.List;

public interface EntryType extends Comparable<EntryType> {
    String getName();

    boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database);

    List<String> getOptionalFields();

    List<String> getRequiredFields();

    boolean isRequired(String field);

    boolean isOptional(String field);

    EntryTypes getEntryType();

    boolean isVisibleAtNewEntryDialog();

    List<String> getRequiredFieldsForCustomization();

    /**
     * TODO: remove
     */
    List<String> getPrimaryOptionalFields();


    /**
     * TODO: remove
     */
    List<String> getSecondaryOptionalFields();
}
