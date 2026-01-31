package org.jabref.model.entry.identifier;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Partition tests for ISBN length/format validation.
 *
 * The input domain is divided into five partitions based on string length:
 *   P1: Too short (0-9 digits)
 *   P2: ISBN-10 length (exactly 10 digits)
 *   P3: Invalid middle length of 11 or 12 digits (11-12 digits)
 *   P4: ISBN-13 length (exactly 13 digits)
 *   P5: Too long (14+ digits)
 */
class ISBNLengthPartitionTest {

    @Nested
    class Partition1TooShort {

        @Test
        void emptyStringIsInvalid() {
            // Minimum boundary: empty string
            ISBN isbn = new ISBN("");
            assertFalse(isbn.isValidFormat());
        }

        @Test
        void threeDigitsIsInvalid() {
            // Representative value: 3 digits, too short
            ISBN isbn = new ISBN("123");
            assertFalse(isbn.isValidFormat());
        }

        @Test
        void nineDigitsIsInvalid() {
            // Boundary value: exactly 9 digits, this is the maximum length that still
            // falls in Partition 1, sitting right at the edge before crossing into
            // valid ISBN-10
            ISBN isbn = new ISBN("123456789");
            assertFalse(isbn.isValidFormat());
        }
    }

    @Nested
    class Partition2Isbn10Length {

        @Test
        void tenDigitsHasValidFormat() {
            // Representative value: exactly 10 digits and is a properly formatted ISBN-10
            ISBN isbn = new ISBN("0123456789");
            assertTrue(isbn.isValidFormat());
        }
    }

    @Nested
    class Partition3InvalidMiddleLength {

        @Test
        void elevenDigitsIsInvalid() {
            // Representative value for 11 digits: one more digit than ISBN-10
            ISBN isbn = new ISBN("01234567890");
            assertFalse(isbn.isValidFormat());
        }

        @Test
        void twelveDigitsIsInvalid() {
            // Representative value for 12 digits: one less digit than ISBN-13
            ISBN isbn = new ISBN("012345678901");
            assertFalse(isbn.isValidFormat());
        }
    }

    @Nested
    class Partition4Isbn13Length {

        @Test
        void thirteenDigitsHasValidFormat() {
            // Representative value: exactly 13 digits and is a properly formatted ISBN-13
            ISBN isbn = new ISBN("0123456789012");
            assertTrue(isbn.isValidFormat());
        }
    }

    @Nested
    class Partition5TooLong {

        @Test
        void fourteenDigitsIsInvalid() {
            // Boundary value: exactly 14 digits, the minimum length that exceeds
            // valid ISBN-13, sitting right at the partition boundary
            ISBN isbn = new ISBN("01234567890123");
            assertFalse(isbn.isValidFormat());
        }

        @Test
        void twentyDigitsIsInvalid() {
            // Representative value: at 20 digits, this is too long of an ISBN value
            ISBN isbn = new ISBN("01234567890123456789");
            assertFalse(isbn.isValidFormat());
        }
    }

    @Nested
    class BoundaryTransitionTests {

        // Tests the boundaries where partitions meet
        @ParameterizedTest
        @MethodSource("boundaryValues")
        void boundaryTransitions(String input, int length, boolean expectedValidFormat, String description) {
            ISBN isbn = new ISBN(input);
            assertEquals(expectedValidFormat, isbn.isValidFormat(), description + " (length=" + length + ")");
        }

        static Stream<Arguments> boundaryValues() {
            return Stream.of(
                    // Boundary: 9 -> 10 (P1 to P2)
                    Arguments.of("123456789", 9, false, "9 digits should be invalid"),
                    Arguments.of("0123456789", 10, true, "10 digits should be valid format"),

                    // Boundary: 10 -> 11 (P2 to P3)
                    Arguments.of("0123456789", 10, true, "10 digits should be valid format"),
                    Arguments.of("01234567890", 11, false, "11 digits should be invalid"),

                    // Boundary: 12 -> 13 (P3 to P4)
                    Arguments.of("012345678901", 12, false, "12 digits should be invalid"),
                    Arguments.of("0123456789012", 13, true, "13 digits should be valid format"),

                    // Boundary: 13 -> 14 (P4 to P5)
                    Arguments.of("0123456789012", 13, true, "13 digits should be valid format"),
                    Arguments.of("01234567890123", 14, false, "14 digits should be invalid")
            );
        }
    }
}
