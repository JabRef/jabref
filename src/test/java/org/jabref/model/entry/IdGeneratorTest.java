package org.jabref.model.entry;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class IdGeneratorTest {

    @Test
    void createNeutralId() {
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            String string = IdGenerator.next();
            assertFalse(set.contains(string));
            set.add(string);
        }
    }
}
