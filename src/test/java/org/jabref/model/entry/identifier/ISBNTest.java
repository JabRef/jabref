package org.jabref.model.entry.identifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ISBNTest {

    @Test
    public void isValidFormat10Correct() {
        assertTrue(new ISBN("0-123456-47-9").isValidFormat());
        assertTrue(new ISBN("0-9752298-0-X").isValidFormat());
    }

    @Test
    public void isValidFormat10Incorrect() {
        assertFalse(new ISBN("0-12B456-47-9").isValidFormat());
    }

    @Test
    public void isValidChecksum10Correct() {
        assertTrue(new ISBN("0-123456-47-9").isValidChecksum());
        assertTrue(new ISBN("0-9752298-0-X").isValidChecksum());
        assertTrue(new ISBN("0-9752298-0-x").isValidChecksum());
    }

    @Test
    public void isValidChecksum10Incorrect() {
        assertFalse(new ISBN("0-123456-47-8").isValidChecksum());
    }

    @Test
    public void isValidFormat13Correct() {
        assertTrue(new ISBN("978-1-56619-909-4").isValidFormat());
    }

    @Test
    public void isValidFormat13Incorrect() {
        assertFalse(new ISBN("978-1-56619-9O9-4 ").isValidFormat());
    }

    @Test
    public void isValidChecksum13Correct() {
        assertTrue(new ISBN("978-1-56619-909-4 ").isValidChecksum());
    }

    @Test
    public void isValidChecksum13Incorrect() {
        assertFalse(new ISBN("978-1-56619-909-5").isValidChecksum());
    }

    @Test
    public void isIsbn10Correct() {
        assertTrue(new ISBN("0-123456-47-9").isIsbn10());
        assertTrue(new ISBN("0-9752298-0-X").isIsbn10());
    }

    @Test
    public void isIsbn10Incorrect() {
        assertFalse(new ISBN("978-1-56619-909-4").isIsbn10());
    }

    @Test
    public void isIsbn13Correct() {
        assertTrue(new ISBN("978-1-56619-909-4").isIsbn13());
    }

    @Test
    public void isIsbn13Incorrect() {
        assertFalse(new ISBN("0-123456-47-9").isIsbn13());
    }
}
