package org.jabref.logic.util.strings;

import org.jabref.model.util.ResultingEmacsState;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmacsStringManipulatorTest {

    @Test
    public void capitalizePreservesNewlines() {
        int pos = 5; // Position of the caret, between the two ll in the first hellO"
        String textInput = "hello\n\nhELLO";
        String whatTextShouldBe = "hello\n\nHello";
        ResultingEmacsState textOutput = EmacsStringManipulator.capitalize(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
    }

    @Test
    public void uppercasePreservesSpace() {
        int pos = 3; // Position of the caret, between the two ll in the first hello
        String textInput = "hello hello";
        String whatTextShouldBe = "helLO hello";
        ResultingEmacsState textOutput = EmacsStringManipulator.uppercase(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
    }

    @Test
    public void uppercasePreservesNewlines() {
        int pos = 3; // Position of the caret, between the two ll in the first hello
        String textInput = "hello\nhello";
        String whatTextShouldBe = "helLO\nhello";
        ResultingEmacsState textOutput = EmacsStringManipulator.uppercase(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
    }

    @Test
    public void uppercasePreservesTab() {
        int pos = 3; // Position of the caret, between the two ll in the first hello
        String textInput = "hello\thello";
        String whatTextShouldBe = "helLO\thello";
        ResultingEmacsState textOutput = EmacsStringManipulator.uppercase(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
    }

    @Test
    public void uppercasePreservesDoubleSpace() {
        int pos = 5; // Position of the caret, at the first space
        String textInput = "hello  hello";
        String whatTextShouldBe = "hello  HELLO";
        ResultingEmacsState textOutput = EmacsStringManipulator.uppercase(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
    }

    @Test
    public void uppercaseIgnoresTrailingWhitespace() {
        int pos = 5; // First space
        String textInput = "hello  ";
        String whatTextShouldBe = "hello  ";
        ResultingEmacsState textOutput = EmacsStringManipulator.uppercase(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
        // Expected caret position is right after the last space, which is index 7
        assertEquals(7, textOutput.caretPos);
    }

    @Test
    public void killWordTrimsTrailingWhitespace() {
        int pos = 5; // First space
        String textInput = "hello  ";
        String whatTextShouldBe = "hello";
        ResultingEmacsState textOutput = EmacsStringManipulator.killWord(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
        assertEquals(pos, textOutput.caretPos);
    }

    @Test
    public void backwardsKillWordTrimsPreceedingWhitespace() {
        int pos = 1; // Second space
        String textInput = "  hello";
        // One space should be preserved since we are deleting everything preceding the second space.
        String whatTextShouldBe = " hello";
        ResultingEmacsState textOutput = EmacsStringManipulator.backwardKillWord(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
        // The caret should have been moved to the start.
        assertEquals(0, textOutput.caretPos);
    }

    @Test
    public void uppercasePreservesMixedSpaceNewLineTab() {
        int pos = 5; // Position of the caret, after first hello
        String textInput = "hello \n\thello";
        String whatTextShouldBe = "hello \n\tHELLO";
        ResultingEmacsState textOutput = EmacsStringManipulator.uppercase(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
    }

    @Test
    public void lowercaseEditsTheNextWord() {
        int pos = 5; // Position of the caret, right at the space
        String textInput = "hello HELLO";
        String whatTextShouldBe = "hello hello";
        ResultingEmacsState textOutput = EmacsStringManipulator.lowercase(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
    }

    @Test
    public void killWordRemovesFromPositionUpToNextWord() {
        int pos = 3; // Position of the caret, between the two "ll in the first hello"
        String textInput = "hello hello";
        String whatTextShouldBe = "hel hello";
        ResultingEmacsState textOutput = EmacsStringManipulator.killWord(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
    }

    @Test
    public void killWordRemovesNextWordIfPositionIsInSpace() {
        int pos = 5; // Position of the caret, atfer the first hello"
        String textInput = "hello person";
        String whatTextShouldBe = "hello";
        ResultingEmacsState textOutput = EmacsStringManipulator.killWord(pos, textInput);
        assertEquals(whatTextShouldBe, textOutput.text);
    }

}
