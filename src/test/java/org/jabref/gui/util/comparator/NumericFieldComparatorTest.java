package org.jabref.gui.util.comparator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumericFieldComparatorTest {

    private final NumericFieldComparator comparator = new NumericFieldComparator();

    @Test
    public void compareTwoNumericInputs() {
        assertEquals(2, comparator.compare("4", "2"));
    }

    @Test
    public void compareTwoNullInputs() {
        assertEquals(0, comparator.compare(null, null));
    }

    @Test
    public void compareTwoInputsWithFirstNull() {
        assertEquals(-1, comparator.compare(null, "2"));
    }

    @Test
    public void compareTwoInputsWithSecondNull() {
        assertEquals(1, comparator.compare("4", null));
    }

    @Test
    public void compareTwoNotNumericInputs() {
        assertEquals(-32, comparator.compare("HELLO", "hello"));
    }

    @Test
    public void compareStringWithInteger() {
        assertEquals(-1, comparator.compare("hi", "2"));
    }

    @Test
    public void compareIntegerWithString() {
        assertEquals(1, comparator.compare("4", "hi"));
    }

    @Test
    public void compareNegativeInteger() {
        assertEquals(1, comparator.compare("-4", "-5"));
    }

    @Test
    public void compareWithMinusString() {
        assertEquals(-1, comparator.compare("-", "-5"));
    }

    @Test
    public void compareWithPlusString() {
        assertEquals(-1, comparator.compare("+", "-5"));
    }

    @Test
    public void compareWordWithMinus() {
        assertEquals(-1, comparator.compare("-abc", "-5"));
    }
}
