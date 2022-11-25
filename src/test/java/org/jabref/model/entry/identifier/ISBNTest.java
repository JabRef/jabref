package org.jabref.model.entry.identifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ISBNTest {

    @Test
    public void testIsValidFormat10Correct() {
        assertTrue(new ISBN("0-123456-47-9").isValidFormat());
        assertTrue(new ISBN("0-9752298-0-X").isValidFormat());
    }

    @Test
    public void testIsValidFormat10Incorrect() {
        assertFalse(new ISBN("0-12B456-47-9").isValidFormat());
    }

    @Test
    public void testIsValidChecksum10Correct() {
        assertTrue(new ISBN("0-123456-47-9").isValidChecksum());
        assertTrue(new ISBN("0-9752298-0-X").isValidChecksum());
        assertTrue(new ISBN("0-9752298-0-x").isValidChecksum());
    }

    @Test
    public void testIsValidChecksum10Incorrect() {
        assertFalse(new ISBN("0-123456-47-8").isValidChecksum());
    }

    @Test
    public void testIsValidFormat13Correct() {
        assertTrue(new ISBN("978-1-56619-909-4").isValidFormat());
    }

    @Test
    public void testIsValidFormat13Incorrect() {
        assertFalse(new ISBN("978-1-56619-9O9-4 ").isValidFormat());
    }

    @Test
    public void testIsValidChecksum13Correct() {
        assertTrue(new ISBN("978-1-56619-909-4 ").isValidChecksum());
    }

    @Test
    public void testIsValidChecksum13Incorrect() {
        assertFalse(new ISBN("978-1-56619-909-5").isValidChecksum());
    }

    @Test
    public void testIsIsbn10Correct() {
        assertTrue(new ISBN("0-123456-47-9").isIsbn10());
        assertTrue(new ISBN("0-9752298-0-X").isIsbn10());
    }

    @Test
    public void testIsIsbn10Incorrect() {
        assertFalse(new ISBN("978-1-56619-909-4").isIsbn10());
    }

    @Test
    public void testIsIsbn13Correct() {
        assertTrue(new ISBN("978-1-56619-909-4").isIsbn13());
    }

    @Test
    public void testIsIsbn13Incorrect() {
        assertFalse(new ISBN("0-123456-47-9").isIsbn13());
    }
}
