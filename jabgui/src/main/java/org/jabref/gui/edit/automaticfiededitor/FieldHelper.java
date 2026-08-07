package org.jabref.gui.edit.automaticfiededitor;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class FieldHelper {

    public static Set<Field> getSetFieldsOnly(List<BibEntry> selectedEntries, Collection<Field> allFields) {
        Set<Field> allowedFields = FieldFactory.getAllFieldsWithOutInternal();

        return allFields.stream()
                        .filter(allowedFields::contains)
                        .filter(field -> selectedEntries.stream()
                                                        .anyMatch(entry -> entry.getField(field)
                                                                                .filter(Predicate.not(String::isBlank))
                                                                                .isPresent()))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
