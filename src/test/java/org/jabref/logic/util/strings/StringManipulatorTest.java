package org.jabref.logic.util.strings;

import java.util.stream.Stream;

import org.jabref.model.util.ResultingStringState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringManipulatorTest {

    @Test
    public void testCapitalizePreservesNewlines() {
        int caretPosition = 5; // Position of the caret, between the two ll in the first hellO"
        String input = "hello\n\nhELLO";
        String expectedResult = "hello\n\nHello";
        ResultingStringState textOutput = StringManipulator.capitalize(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
    }

    @Test
    public void testUppercasePreservesSpace() {
        int caretPosition = 3; // Position of the caret, between the two ll in the first hello
        String input = "hello hello";
        String expectedResult = "helLO hello";
        ResultingStringState textOutput = StringManipulator.uppercase(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
    }

    @Test
    public void testUppercasePreservesNewlines() {
        int caretPosition = 3; // Position of the caret, between the two ll in the first hello
        String input = "hello\nhello";
        String expectedResult = "helLO\nhello";
        ResultingStringState textOutput = StringManipulator.uppercase(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
    }

    @Test
    public void testUppercasePreservesTab() {
        int caretPosition = 3; // Position of the caret, between the two ll in the first hello
        String input = "hello\thello";
        String expectedResult = "helLO\thello";
        ResultingStringState textOutput = StringManipulator.uppercase(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
    }

    @Test
    public void testUppercasePreservesDoubleSpace() {
        int caretPosition = 5; // Position of the caret, at the first space
        String input = "hello  hello";
        String expectedResult = "hello  HELLO";
        ResultingStringState textOutput = StringManipulator.uppercase(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
    }

    @Test
    public void testUppercaseIgnoresTrailingWhitespace() {
        int caretPosition = 5; // First space
        String input = "hello  ";
        String expectedResult = "hello  ";
        ResultingStringState textOutput = StringManipulator.uppercase(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
        // Expected caret position is right after the last space, which is index 7
        assertEquals(7, textOutput.caretPosition);
    }

    @Test
    public void testKillWordTrimsTrailingWhitespace() {
        int caretPosition = 5; // First space
        String input = "hello  ";
        String expectedResult = "hello";
        ResultingStringState textOutput = StringManipulator.killWord(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
        assertEquals(caretPosition, textOutput.caretPosition);
    }

    @Test
    public void testBackwardsKillWordTrimsPreceedingWhitespace() {
        int caretPosition = 1; // Second space
        String input = "  hello";
        // One space should be preserved since we are deleting everything preceding the second space.
        String expectedResult = " hello";
        ResultingStringState textOutput = StringManipulator.backwardKillWord(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
        // The caret should have been moved to the start.
        assertEquals(0, textOutput.caretPosition);
    }

    @Test
    public void testUppercasePreservesMixedSpaceNewLineTab() {
        int caretPosition = 5; // Position of the caret, after first hello
        String input = "hello \n\thello";
        String expectedResult = "hello \n\tHELLO";
        ResultingStringState textOutput = StringManipulator.uppercase(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
    }

    @Test
    public void testLowercaseEditsTheNextWord() {
        int caretPosition = 5; // Position of the caret, right at the space
        String input = "hello HELLO";
        String expectedResult = "hello hello";
        ResultingStringState textOutput = StringManipulator.lowercase(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
    }

    @Test
    public void testKillWordRemovesFromPositionUpToNextWord() {
        int caretPosition = 3; // Position of the caret, between the two "ll in the first hello"
        String input = "hello hello";
        String expectedResult = "hel hello";
        ResultingStringState textOutput = StringManipulator.killWord(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
    }

    @Test
    public void testKillWordRemovesNextWordIfPositionIsInSpace() {
        int caretPosition = 5; // Position of the caret, after the first hello"
        String input = "hello person";
        String expectedResult = "hello";
        ResultingStringState textOutput = StringManipulator.killWord(caretPosition, input);
        assertEquals(expectedResult, textOutput.text);
    }

    @Test
    public void testKillPreviousWord() {
        int caretPosition = 8;
        int expectedPosition = 6;
        String input = "hello person";
        String expectedResult = "hello rson";
        ResultingStringState result = StringManipulator.backwardKillWord(caretPosition, input);
        assertEquals(expectedResult, result.text);
        assertEquals(expectedPosition, result.caretPosition);
    }

    @ParameterizedTest
    @MethodSource("wordBoundaryTestData")
    void testGetNextWordBoundary(String text, int caretPosition, int expectedPosition, StringManipulator.Direction direction) {
        int result = StringManipulator.getNextWordBoundary(caretPosition, text, direction);
        assertEquals(expectedPosition, result);
    }

    private static Stream<Arguments> wordBoundaryTestData() {
        return Stream.of(
                Arguments.of("hello person", 3, 0, StringManipulator.Direction.PREVIOUS),
                Arguments.of("hello person", 12, 6, StringManipulator.Direction.PREVIOUS),
                Arguments.of("hello person", 0, 0, StringManipulator.Direction.PREVIOUS),
                Arguments.of("hello person", 0, 5, StringManipulator.Direction.NEXT),
                Arguments.of("hello person", 5, 12, StringManipulator.Direction.NEXT),
                Arguments.of("hello person", 12, 12, StringManipulator.Direction.NEXT));
    }
}
