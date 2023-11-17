package org.jabref.model.entry.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StandardFieldTest {

    @Test
    void fieldsConsideredEqualIfSame() {
        assertEquals(StandardField.TITLE, StandardField.TITLE);
    }
}
