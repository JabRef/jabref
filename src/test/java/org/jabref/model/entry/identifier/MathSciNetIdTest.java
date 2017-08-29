package org.jabref.model.entry.identifier;

import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathSciNetIdTest {

    @Test
    public void parseRemovesNewLineCharacterAtEnd() throws Exception {
        Optional<MathSciNetId> id = MathSciNetId.parse("3014184\n");
        assertEquals(Optional.of(new MathSciNetId("3014184")), id);
    }
}
