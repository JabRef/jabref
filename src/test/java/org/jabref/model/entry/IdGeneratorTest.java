package org.jabref.model.entry;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

public class IdGeneratorTest {

    @Test
    public void testCreateNeutralId() {

        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            String string = IdGenerator.next();
            Assert.assertFalse(set.contains(string));
            set.add(string);
        }

    }

}
