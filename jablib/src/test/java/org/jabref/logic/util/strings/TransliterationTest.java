package org.jabref.logic.util.strings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransliterationTest {
    @ParameterizedTest(name = "string={0}, expected={1}")
    @CsvSource({
            "Hello, Hello",            // English
            "Grüße, Grusse",           // German
            "संस्कृतम्, Sanskrtam",        // Sanskrit
            "नमस्ते, Namaste",            // Hindi
            "Привет, Privet",          // Russian
            "Привіт, Privit",          // Ukrainian, though "Pryvit" is better for expected result.
            "你好, Ni Hao",             // Chinese
            "안녕하세요, Annyeonghaseyo", // Korean
            "مرحبا, Mrhba"             // Arabic
    })
    void transliterates(String string, String expected) {
        assertEquals(expected, Transliteration.transliterate(string, false));
    }

    @Test
    void removesSpaces(){
        assertEquals("DzabRef", Transliteration.transliterate("Джаб Реф", true));
    }

    @Test
    void keepsSpaces(){
        assertEquals("Dzab Ref", Transliteration.transliterate("Джаб Реф", false));
    }
}
