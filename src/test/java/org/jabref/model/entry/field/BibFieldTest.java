package org.jabref.model.entry.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class BibFieldTest {

    @Test
    void bibFieldsConsideredEqualIfUnderlyingFieldIsEqual() {
        assertEquals(
            new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT),
            new BibField(StandardField.AUTHOR, FieldPriority.DETAIL)
        );
    }

    @Test
    void bibFieldsConsideredNotEqualIfUnderlyingFieldNotEqual() {
        assertNotEquals(
            new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT),
            new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)
        );
    }
}
