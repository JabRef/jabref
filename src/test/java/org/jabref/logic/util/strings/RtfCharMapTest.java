package org.jabref.logic.util.strings;

import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RtfCharMapTest {

    private RtfCharMap sut;

    @Before
    public void setup() {
        sut = new RtfCharMap();
    }

    @Test
    public void givenCorrectInput_whenGetMethod_thenReturnRespectiveValue() {
        String key = "`a";
        String value = "\\'e0";
        assertEquals(sut.get(key), value);
    }

    @Test
    public void givenIncorrectInput_whenGetMethod_thenReturnNull() {
        String key = "`b";
        assertTrue(sut.get(key) == null);
    }

}
