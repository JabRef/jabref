package org.jabref.logic.externalParser;

import org.junit.Test;

/**
 * Used to test the whole logic of the external parser anystyle.
 * Therefore any possible outgoing when the user is working with the parser is going to be
 * tested in this class.
 */

public class externalParserTest {

    /**
     * Tests the base functionality of the parser is working by taking some example
     * strings and parses them with the external parser and checks if the corresponding fields
     * are filled in correctly.
     */
    @Test
    public void singleTextResourceParseTest() {

    }

    /**
     * Takes a string which has some obvious parts in the text where have to be almost 100%
     * allocated to the correct field of the corresponding entry.
     */
    @Test
    public void correctParseWithObviousContentTest() {

    }

    /**
     * Tests the teach functionality of the parser by testing the appropriate function of the parser which
     * should remove that error from it in the further tries of parsing the exact same text.
     */
    @Test
    public void teachTheParserToRecognizeFormatTest() {

    }

    /**
     * Tests if the parser recognizes garbage text and reacts accordingly.
     */
    @Test
    public void parseGarbageTextTest() {

    }

    /**
     * If there is no text available for the parser then nothing should happen.
     */
    @Test
    public void parseEmptyTextTest() {

    }

    /**
     * The parser should skip / ignore characters which are not readable like symbols or asian signs for example.
     */
    @Test
    public void parseInvalidCharacters() {

    }

}
