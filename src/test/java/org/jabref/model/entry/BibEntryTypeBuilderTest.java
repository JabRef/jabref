package org.jabref.model.entry;

import java.util.LinkedHashSet;
import java.util.List;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled("Test is useless ")
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
    void detailOptionalWorks() {
        BibEntryType bibEntryType = new BibEntryTypeBuilder()
                .withImportantFields(StandardField.AUTHOR)
                .withDetailFields(StandardField.NOTE)
                .build();
        assertEquals(new LinkedHashSet<>(List.of(StandardField.NOTE)), bibEntryType.getDetailOptionalFields());
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
