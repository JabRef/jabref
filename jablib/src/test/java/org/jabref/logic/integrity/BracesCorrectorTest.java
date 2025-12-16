package org.jabref.logic.integrity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ResourceLock("Localization.lang")
class BracesCorrectorTest {

    @Test
    void inputIsNull() {
        assertNull(BracesCorrector.apply(null));
    }

    @Test
    void inputIsEmpty() {
        assertEquals("", BracesCorrector.apply(""));
    }

    @Test
    void inputWithoutBraces() {
        assertEquals("banana", BracesCorrector.apply("banana"));
    }

    @Test
    void inputAlreadyCorrect() {
        assertEquals("{banana}", BracesCorrector.apply("{banana}"));
    }

    @Test
    void inputMissingClosing() {
        assertEquals("{banana}", BracesCorrector.apply("{banana"));
    }

    @Test
    void inputMissingOpening() {
        assertEquals("{banana}", BracesCorrector.apply("banana}"));
    }

    @Test
    void inputWithMaskedBraces() {
        assertEquals("\\\\\\{banana", BracesCorrector.apply("\\\\\\{banana"));
    }

    @Test
    void inputWithMixedBraces() {
        assertEquals("{b{anana\\\\\\}}}", BracesCorrector.apply("{b{anana\\\\\\}"));
    }

    @Test
    void inputUnbalanced() {
        assertEquals("{{ban}ana}", BracesCorrector.apply("ban}ana}"));
    }
}
