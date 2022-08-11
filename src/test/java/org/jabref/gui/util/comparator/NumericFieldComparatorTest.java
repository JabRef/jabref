package org.jabref.gui.util.comparator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void compareNumericSignalWithoutNumberWithLenghtBiggerThanOne() {
        assertEquals(2, comparator.compare("- ", "+ "));
    }

    @Test
    void compareNumericSignalAfterNumber() {
        assertEquals(-2, comparator.compare("5- ", "7+ "));
    }

    // CT1
    @Test
    public void numberIsNull(){ 
        assertFalse(comparator.isNumberMethodPublic(null)); 
    }

    // CT2
    @Test
    public void numberIsEmpty(){ 
        assertFalse(comparator.isNumberMethodPublic("")); 
    }

    // CT3
    @Test
    public void numberIsMinusChar(){ 
        assertFalse(comparator.isNumberMethodPublic("-")); 
    }

    // CT4
    @Test
    public void numberIsPlusChar(){ 
        assertFalse(comparator.isNumberMethodPublic("+")); 
    }

    // CT5
    @Test
    public void numberIsAOneCharNumber(){ 
        assertTrue(comparator.isNumberMethodPublic("1")); 
    }

    // CT6
    @Test
    public void numberIsAOneCharLetter(){ 
        assertFalse(comparator.isNumberMethodPublic("a")); 
    }

    // CT7
    @Test
    public void numberBeginsWithMinusCharAndIsNumber(){ 
        assertTrue(comparator.isNumberMethodPublic("-1")); 
    }

    // CT8
    @Test
    public void numberBeginsWithPlusCharAndIsNumber(){ 
        assertTrue(comparator.isNumberMethodPublic("-1")); 
    }

    // CT9
    @Test
    public void numberBeginsWithMinusCharAndHasDigit(){ 
        assertFalse(comparator.isNumberMethodPublic("-12a3")); 
    }

    // CT10
    @Test
    public void numberBeginsWithPlusCharAndHasDigit(){ 
        assertFalse(comparator.isNumberMethodPublic("+12a3")); 
    }

    // CT11
    @Test
    public void numberHasDigit(){ 
        assertFalse(comparator.isNumberMethodPublic("12a3"));
    }

    // CT12
    @Test
    public void numberIsNumber(){ 
        assertTrue(comparator.isNumberMethodPublic("11")); 
    }

}
