package org.jabref.model.entry.field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardFieldTest {

    @Test
    void fieldsConsideredEqualIfSame() {
        assertEquals(StandardField.TITLE, StandardField.TITLE);
    }

    @Test
    void numericFieldsHaveNumericProperty() {
        assertTrue(StandardField.CITATIONCOUNT.isNumeric());
        assertTrue(StandardField.DAY.isNumeric());
        assertTrue(StandardField.DAYFILED.isNumeric());
    }

    @Test
    void nonNumericFieldsDoNotHaveNumericProperty() {
        assertFalse(StandardField.CHAPTER.isNumeric());
        assertFalse(StandardField.PART.isNumeric());
    }
}
