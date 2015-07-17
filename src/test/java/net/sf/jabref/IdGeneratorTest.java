package net.sf.jabref;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

public class IdGeneratorTest {

    @Test
    public void testCreateNeutralId() {

        HashSet<String> set = new HashSet<String>();
        for (int i = 0; i < 10000; i++) {
            String string = IdGenerator.next();
            Assert.assertFalse(set.contains(string));
            set.add(string);
        }

    }

}