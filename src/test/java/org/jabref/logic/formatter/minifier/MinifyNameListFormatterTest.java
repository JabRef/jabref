package org.jabref.logic.formatter.minifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class MinifyNameListFormatterTest {

    private static final MinifyNameListFormatter formatter = new MinifyNameListFormatter();

    @Test
    public void minifyAuthorNames() {
        expectCorrect("Simon Harrer", "Simon Harrer");
        expectCorrect("Simon Harrer and others", "Simon Harrer and others");
        expectCorrect("Simon Harrer and Jörg Lenhard", "Simon Harrer and Jörg Lenhard");
        expectCorrect("Simon Harrer and Jörg Lenhard and Guido Wirtz", "Simon Harrer and others");
        expectCorrect("Simon Harrer and Jörg Lenhard and Guido Wirtz and others", "Simon Harrer and others");
    }

    @Test
    public void formatExample() {
        expectCorrect(formatter.getExampleInput(), "Stefan Kolb and others");
    }

    private void expectCorrect(String input, String expected) {
        assertEquals(expected, formatter.format(input));
    }
}
