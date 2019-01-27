package org.jabref.logic.integrity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BracesCorrectorTest {

    @Test
    public void inputIsNull() {
        assertNull(BracesCorrector.apply(null));
    }

    @Test
    public void inputIsEmpty() {
        assertEquals("", BracesCorrector.apply(""));
    }

    @Test
    public void inputWithoutBraces() {
        assertEquals("banana", BracesCorrector.apply("banana"));
    }

    @Test
    public void inputMissingClosing() {
        assertEquals("{banana}", BracesCorrector.apply("{banana"));
    }

    @Test
    public void inputMissingOpening() {
        assertEquals("{banana}", BracesCorrector.apply("banana}"));
    }

    @Test
    public void inputWithMaskedBraces() {
        assertEquals("\\\\\\{banana", BracesCorrector.apply("\\\\\\{banana"));
    }

    @Test
    public void inputWithMixedBraces() {
        assertEquals("{b{anana\\\\\\}}}", BracesCorrector.apply("{b{anana\\\\\\}"));
    }
}
