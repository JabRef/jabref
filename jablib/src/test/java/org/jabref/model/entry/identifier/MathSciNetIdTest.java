package org.jabref.model.entry.identifier;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MathSciNetIdTest {

    @Test
    void parseRemovesNewLineCharacterAtEnd() throws Exception {
        Optional<MathSciNetId> id = MathSciNetId.parse("3014184\n");
        assertEquals(Optional.of(new MathSciNetId("3014184")), id);
    }
}
