package org.jabref.model.entry;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

public class IdGeneratorTest {

    @Test
    public void testCreateNeutralId() {
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            String string = IdGenerator.next();
            assertFalse(set.contains(string));
            set.add(string);
        }
    }
}
