package org.jabref.model.entry;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BibEntryTypeBuilderTest {

    @Test
    void fieldAlreadySeenSameCategory() {
        assertThrows(IllegalArgumentException.class, () ->
        new BibEntryTypeBuilder()
                .withImportantFields(StandardField.AUTHOR)
                .withImportantFields(StandardField.AUTHOR)
                .build());
    }

    @Test
    void fieldAlreadySeenDifferentCategories() {
        assertThrows(IllegalArgumentException.class, () ->
        new BibEntryTypeBuilder()
                .withRequiredFields(StandardField.AUTHOR)
                .withImportantFields(StandardField.AUTHOR)
                .build());
    }
}
