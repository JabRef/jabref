package net.sf.jabref;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;

public class Bug1283 {

    @Test
    public void testBug1283() throws IOException {
        assertFalse(MonthUtil.getMonth("8,").isValid());
    }

}
