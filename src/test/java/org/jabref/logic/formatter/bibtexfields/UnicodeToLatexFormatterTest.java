package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnicodeToLatexFormatterTest {

    private UnicodeToLatexFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new UnicodeToLatexFormatter();
    }

    @Test
    void formatWithoutUnicodeCharactersReturnsSameString() {
        assertEquals("abc", formatter.format("abc"));
    }

    @Test
    void formatOfMacronAIsCorrect() {
        assertEquals("{\\={a}}", formatter.format("ā"));
    }

    @Test
    void formatMultipleUnicodeCharacters() {
        assertEquals("{{\\aa}}{\\\"{a}}{\\\"{o}}", formatter.format("\u00E5\u00E4\u00F6"));
    }

    @Test
    void testSanskrit() {
        assertEquals("Pu\\d{n}ya-pattana-vidy{\\={a}}-p{\\i{\\={}}}ṭh{\\={a}}dhi-kṛtaiḥ pr{\\={a}}-ka{{\\'{s}}}yaṃ n{\\i{\\={}}}taḥ", formatter.format("Pu\\d{n}ya-pattana-vidyā-pı̄ṭhādhi-kṛtaiḥ prā-kaśyaṃ nı̄taḥ"));
    }

    @Test
    void formatExample() {
        assertEquals("M{\\\"{o}}nch", formatter.format(formatter.getExampleInput()));
    }

}
