package org.jabref.logic.formatter.casechanger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class LowerCaseFormatterTest {

    private static final LowerCaseFormatter formatter = new LowerCaseFormatter();

    @Test
    public void test() {
        assertEquals("lower", formatter.format("LOWER"));
        assertEquals("lower {UPPER}", formatter.format("LOWER {UPPER}"));
        assertEquals("lower {U}pper", formatter.format("LOWER {U}PPER"));
    }

    @Test
    public void formatExample() {
        assertEquals("kde {Amarok}", formatter.format(formatter.getExampleInput()));
    }
}
