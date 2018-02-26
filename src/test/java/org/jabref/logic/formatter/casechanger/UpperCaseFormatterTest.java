package org.jabref.logic.formatter.casechanger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class UpperCaseFormatterTest {

    private UpperCaseFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new UpperCaseFormatter();
    }

    @Test
    public void test() {
        assertEquals("LOWER", formatter.format("LOWER"));
        assertEquals("UPPER", formatter.format("upper"));
        assertEquals("UPPER", formatter.format("UPPER"));
        assertEquals("UPPER {lower}", formatter.format("upper {lower}"));
        assertEquals("UPPER {l}OWER", formatter.format("upper {l}ower"));
    }

    @Test
    public void formatExample() {
        assertEquals("KDE {Amarok}", formatter.format(formatter.getExampleInput()));
    }
}
