package org.jabref.gui.util.comparator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumericFieldComparatorTest {

    private final NumericFieldComparator comparator = new NumericFieldComparator();

    @Test
    void compareTwoNumericInputs() {
        assertEquals(2, comparator.compare("4", "2"));
    }

    @Test
    void compareTwoNullInputs() {
        assertEquals(0, comparator.compare(null, null));
    }

    @Test
    void compareTwoInputsWithFirstNull() {
        assertEquals(-1, comparator.compare(null, "2"));
    }

    @Test
    void compareTwoInputsWithSecondNull() {
        assertEquals(1, comparator.compare("4", null));
    }

    @Test
    void compareTwoNotNumericInputs() {
        assertEquals(-32, comparator.compare("HELLO", "hello"));
    }

    @Test
    void compareStringWithInteger() {
        assertEquals(-1, comparator.compare("hi", "2"));
    }

    @Test
    void compareIntegerWithString() {
        assertEquals(1, comparator.compare("4", "hi"));
    }

    @Test
    void compareNegativeInteger() {
        assertEquals(1, comparator.compare("-4", "-5"));
    }

    @Test
    void compareWithMinusString() {
        assertEquals(-1, comparator.compare("-", "-5"));
    }

    @Test
    void compareWithPlusString() {
        assertEquals(-1, comparator.compare("+", "-5"));
    }

    @Test
    void compareWordWithMinus() {
        assertEquals(-1, comparator.compare("-abc", "-5"));
    }

    @Test
    void compareNumericSignalWithoutNumberWithLenghtBiggerThanOne() {
        assertEquals(2, comparator.compare("- ", "+ "));
    }

    @Test
    void compareNumericSignalAfterNumber() {
        assertEquals(-2, comparator.compare("5- ", "7+ "));
    }
}
