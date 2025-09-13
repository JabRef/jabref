package org.jabref.model.entry.field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnknownFieldTest {
    @Test
    void fieldsConsideredEqualIfSameName() {
        assertEquals(new UnknownField("title"), new UnknownField("title"));
    }

    @Test
    void fieldsConsideredEqualINameDifferByCapitalization() {
        assertEquals(new UnknownField("tiTle"), new UnknownField("Title"));
    }
}
