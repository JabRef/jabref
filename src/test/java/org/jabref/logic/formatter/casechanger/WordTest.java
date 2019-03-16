package org.jabref.logic.formatter.casechanger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class WordTest {

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Test
    public void givenShorterProtectedCharBoolArray_whenMakeWord_thenThrowIllegalArgumentException() {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("the chars and the protectedChars array must be of same length");
        char[] arr = {'a', 'b', 'c'};
        boolean[] arr2 = {false};
        Word sut = new Word(arr, arr2);
    }
}

