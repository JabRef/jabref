package org.jabref.model.entry.identifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ISBNTest {

    @ParameterizedTest
    @CsvSource(
            textBlock = """
                    0-123456-47-9
                    0-9752298-0-X
                    """
    )
    void isValidFormat10Correct(String isbn) {
        assertTrue(new ISBN(isbn).isValidFormat());
    }

    @Test
    void isValidFormat10Incorrect() {
        assertFalse(new ISBN("0-12B456-47-9").isValidFormat());
    }

    @ParameterizedTest
    @CsvSource(
            textBlock = """
                    0-123456-47-9
                    0-9752298-0-X
                    0-9752298-0-x
                    """
    )
    void isValidChecksum10Correct(String isbn) {
        assertTrue(new ISBN(isbn).isValidChecksum());
    }

    @Test
    void isValidChecksum10Incorrect() {
        assertFalse(new ISBN("0-123456-47-8").isValidChecksum());
    }

    @Test
    void isValidFormat13Correct() {
        assertTrue(new ISBN("978-1-56619-909-4").isValidFormat());
    }

    @Test
    void isValidFormat13Incorrect() {
        assertFalse(new ISBN("978-1-56619-9O9-4 ").isValidFormat());
    }

    @Test
    void isValidChecksum13Correct() {
        assertTrue(new ISBN("978-1-56619-909-4 ").isValidChecksum());
    }

    @Test
    void isValidChecksum13Incorrect() {
        assertFalse(new ISBN("978-1-56619-909-5").isValidChecksum());
    }

    @ParameterizedTest
    @CsvSource(
            textBlock = """
                    0-123456-47-9
                    0-9752298-0-X
                    """
    )
    void isIsbn10Correct(String isbn) {
        assertTrue(new ISBN(isbn).isIsbn10());
    }

    @Test
    void isIsbn10Incorrect() {
        assertFalse(new ISBN("978-1-56619-909-4").isIsbn10());
    }

    @Test
    void isIsbn13Correct() {
        assertTrue(new ISBN("978-1-56619-909-4").isIsbn13());
    }

    @Test
    void isIsbn13Incorrect() {
        assertFalse(new ISBN("0-123456-47-9").isIsbn13());
    }
}
