package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface for all EntryTypes.
 */
public interface EntryType extends Comparable<EntryType> {
    String getName();

    List<String> getOptionalFields();

    List<String> getRequiredFields();

    default List<String> getRequiredFieldsFlat() {
        List<String> requiredFlat = getRequiredFields().stream()
                .map(field -> field.split("/"))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());

        return Collections.unmodifiableList(requiredFlat);
    }

    EntryTypes getEntryType();

    List<String> getRequiredFieldsForCustomization();

    /**
     * TODO: move inside GUI
     */
    List<String> getPrimaryOptionalFields();

    /**
     * TODO: move inside GUI
     */
    List<String> getSecondaryOptionalFields();
}
