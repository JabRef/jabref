package org.jabref.model.entry.field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardFieldTest {

    @Test
    void fieldsConsideredEqualIfSame() {
        assertEquals(StandardField.TITLE, StandardField.TITLE);
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"DAY", "DAYFILED", "CITATIONCOUNT"})
    void numericFieldsAreNumeric(StandardField field) {
        assertTrue(field.isNumeric());
    }
}
