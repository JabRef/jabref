package org.jabref.logic.bibtex.comparator.plausibility;

import java.util.Optional;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;

public enum PlausibilityComparatorFactory {

    // Single instance ensured by Java compiler
    INSTANCE;

    public Optional<FieldValuePlausibilityComparator> getPlausibilityComparator(Field field) {
        // Similar code as [org.jabref.gui.fieldeditors.FieldEditors.getForField]
        if (field.getProperties().contains(FieldProperty.PERSON_NAMES)) {
            return Optional.of(new PersonNamesPlausibilityComparator());
        }
        if (field.getProperties().contains(FieldProperty.YEAR)) {
            return Optional.of(new YearFieldValuePlausibilityComparator());
        }
        if (InternalField.TYPE_HEADER == field) {
            return Optional.of(new EntryTypePlausibilityComparator());
        }
        return Optional.empty();
    }
}
