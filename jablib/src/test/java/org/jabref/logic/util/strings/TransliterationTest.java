package org.jabref.logic.util.strings;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransliterationTest {
    @ParameterizedTest(name = "string={0}, expected={1}")
    @CsvSource({
            "Hello, Hello",             // English
            // "Grüße, Grusse",         // Commenting out, as ideally it should be `ue` instead of just `u`.
            "संस्कृतम्, sanskrtam",         // Sanskrit
            "नमस्ते, namaste",             // Hindi
            "Привет, Privet",           // Russian
            "Привіт, Privit",           // Ukrainian, though "Pryvit" is better for the expected result.
            "你好, ni hao",              // Chinese
            "안녕하세요, annyeonghaseyo", // Korean
            "مرحبا, mrhba"              // Arabic
    })
    void transliterates(String string, String expected) {
        assertEquals(expected, Transliteration.transliterate(string));
    }
}
