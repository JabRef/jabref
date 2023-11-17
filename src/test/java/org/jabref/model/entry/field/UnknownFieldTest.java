package org.jabref.model.entry.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnknownFieldTest {

    @Test
    void fieldsConsideredEqualIfSameName() {
        assertEquals(new UnknownField("title"), new UnknownField("title"));
    }

    @Test
    void fieldsConsideredEqualINameDifferByCapitalization() {
        assertEquals(new UnknownField("tiTle"), new UnknownField("Title"));
    }

    @Test
    void displayNameConstructor() {
        UnknownField cAsED = UnknownField.fromDisplayName("cAsEd");
        assertEquals(new UnknownField("cased", "cAsEd"), cAsED);
    }
}
