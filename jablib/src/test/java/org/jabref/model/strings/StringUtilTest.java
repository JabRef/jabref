package org.jabref.logic.util.strings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.junit.platform.JavaSource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilTest {

    @Test
    void StringUtilClassIsSmall() throws IOException {
        Path path = JavaSource.getJavaFileForClass(StringUtil.class);
        int lineCount = Files.readAllLines(path, StandardCharsets.UTF_8).size();

        assertTrue(lineCount <= 830, "StringUtil increased in size to " + lineCount + ". "
                + "We try to keep this class as small as possible. "
                + "Thus think twice if you add something to StringUtil.");
    }

    @Test
    void booleanToBinaryString() {
        assertEquals("0", StringUtil.booleanToBinaryString(false));
        assertEquals("1", StringUtil.booleanToBinaryString(true));
    }

    @Test
    void quoteSimple() {
        assertEquals("a::", StringUtil.quote("a:", "", ':'));
    }

    @Test
    void quoteNullQuotation() {
        assertEquals("a::", StringUtil.quote("a:", null, ':'));
    }

    @Test
    void quoteNullString() {
        assertEquals("", StringUtil.quote(null, ";", ':'));
    }

    @Test
    void quoteQuotationCharacter() {
        assertEquals("a:::;", StringUtil.quote("a:;", ";", ':'));
    }

    @Test
    void quoteMoreComplicated() {
        assertEquals("a::b:%c:;", StringUtil.quote("a:b%c;", "%;", ':'));
    }

    @Test
    void unifyLineBreaks() {
        // Mac < v9
        String result = StringUtil.unifyLineBreaks("\r", "newline");
        assertEquals("newline", result);
        // Windows
        result = StringUtil.unifyLineBreaks("\r\n", "newline");
        assertEquals("newline", result);
        // Unix
        result = StringUtil.unifyLineBreaks("\n", "newline");
        assertEquals("newline", result);
    }

    @Test
    void getCorrectFileName() {
        assertEquals("aa.bib", StringUtil.getCorrectFileName("aa", "bib"));
        assertEquals(".login.bib", StringUtil.getCorrectFileName(".login", "bib"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "bib"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a.bib", "BIB"));
        assertEquals("a.bib", StringUtil.getCorrectFileName("a", "bib"));
        assertEquals("a.bb", StringUtil.getCorrectFileName("a.bb", "bib"));
        assertEquals("", StringUtil.getCorrectFileName(null, "bib"));
    }

    @Test
    void quoteForHTML() {
        assertEquals("&#33;", StringUtil.quoteForHTML("!"));
        assertEquals("&#33;&#33;&#33;", StringUtil.quoteForHTML("!!!"));
    }

    @Test
    void removeBracesAroundCapitals() {
        assertEquals("ABC", StringUtil.removeBracesAroundCapitals("{ABC}"));
        assertEquals("ABC", StringUtil.removeBracesAroundCapitals("{{ABC}}"));
        assertEquals("{abc}", StringUtil.removeBracesAroundCapitals("{abc}"));
        assertEquals("ABCDEF", StringUtil.removeBracesAroundCapitals("{ABC}{DEF}"));
    }

    @Test
    void putBracesAroundCapitals() {
        assertEquals("{ABC}", StringUtil.putBracesAroundCapitals("ABC"));
        assertEquals("{ABC}", StringUtil.putBracesAroundCapitals("{ABC}"));
        assertEquals("abc", StringUtil.putBracesAroundCapitals("abc"));
        assertEquals("#ABC#", StringUtil.putBracesAroundCapitals("#ABC#"));
        assertEquals("{ABC} def {EFG}", StringUtil.putBracesAroundCapitals("ABC def EFG"));
    }

    @Test
    void shaveString() {
        assertEquals("", StringUtil.shaveString(null));
        assertEquals("", StringUtil.shaveString(""));
        assertEquals("aaa", StringUtil.shaveString("   aaa\t\t\n\r"));
        assertEquals("a", StringUtil.shaveString("  {a}    "));
        assertEquals("a", StringUtil.shaveString("  \"a\"    "));
        assertEquals("{a}", StringUtil.shaveString("  {{a}}    "));
        assertEquals("{a}", StringUtil.shaveString("  \"{a}\"    "));
        assertEquals("\"{a\"}", StringUtil.shaveString("  \"{a\"}    "));
    }

    @Test
    void join() {
        String[] s = {"ab", "cd", "ed"};
        assertEquals("ab\\cd\\ed", StringUtil.join(s, "\\", 0, s.length));
        assertEquals("cd\\ed", StringUtil.join(s, "\\", 1, s.length));
        assertEquals("ed", StringUtil.join(s, "\\", 2, s.length));
        assertEquals("", StringUtil.join(s, "\\", 3, s.length));
        assertEquals("", StringUtil.join(new String[]{}, "\\", 0, 0));
    }

    @Test
    void stripBrackets() {
        assertEquals("foo", StringUtil.stripBrackets("[foo]"));
        assertEquals("[foo]", StringUtil.stripBrackets("[[foo]]"));
        assertEquals("", StringUtil.stripBrackets(""));
        assertEquals("[foo", StringUtil.stripBrackets("[foo"));
        assertEquals("]", StringUtil.stripBrackets("]"));
        assertEquals("", StringUtil.stripBrackets("[]"));
        assertEquals("f[]f", StringUtil.stripBrackets("f[]f"));
        assertNull(StringUtil.stripBrackets(null));
    }

    @Test
    void getPart() {
        // Get word between braces
        assertEquals("{makes}", StringUtil.getPart("Practice {makes} perfect", 8, false));
        // When the string is empty and start Index equal zero
        assertEquals("", StringUtil.getPart("", 0, false));
        // When the word are in between close curly bracket
        assertEquals("", StringUtil.getPart("A closed mouth catches no }flies}", 25, false));
        // Get the word from the end of the sentence
        assertEquals("bite", StringUtil.getPart("Barking dogs seldom bite", 19, true));
    }

    @Test
    void wrap() {
        String newline = "newline";
        assertEquals("aaaaa" + newline + "\tbbbbb" + newline + "\tccccc",
                StringUtil.wrap("aaaaa bbbbb ccccc", 5, newline));
        assertEquals("aaaaa bbbbb" + newline + "\tccccc", StringUtil.wrap("aaaaa bbbbb ccccc", 8, newline));
        assertEquals("aaaaa bbbbb" + newline + "\tccccc", StringUtil.wrap("aaaaa bbbbb ccccc", 11, newline));
        assertEquals("aaaaa bbbbb ccccc", StringUtil.wrap("aaaaa bbbbb ccccc", 12, newline));
        assertEquals("aaaaa" + newline + "\t" + newline + "\tbbbbb" + newline + "\t" + newline + "\tccccc",
                StringUtil.wrap("aaaaa\nbbbbb\nccccc", 12, newline));
        assertEquals(
                "aaaaa" + newline + "\t" + newline + "\t" + newline + "\tbbbbb" + newline + "\t" + newline + "\tccccc",
                StringUtil.wrap("aaaaa\n\nbbbbb\nccccc", 12, newline));
        assertEquals("aaaaa" + newline + "\t" + newline + "\tbbbbb" + newline + "\t" + newline + "\tccccc",
                StringUtil.wrap("aaaaa\r\nbbbbb\r\nccccc", 12, newline));
    }

    @Test
    void decodeStringDoubleArray() {
        assertArrayEquals(new String[][]{{"a", "b"}, {"c", "d"}}, StringUtil.decodeStringDoubleArray("a:b;c:d"));
        assertArrayEquals(new String[][]{{"a", ""}, {"c", "d"}}, StringUtil.decodeStringDoubleArray("a:;c:d"));
        assertArrayEquals(new String[][]{{"a", ":b"}, {"c;", "d"}}, StringUtil.decodeStringDoubleArray("a:\\:b;c\\;:d"));
    }

    @Test
    void isInCurlyBrackets() {
        assertFalse(StringUtil.isInCurlyBrackets(""));
        assertFalse(StringUtil.isInCurlyBrackets(null));
        assertTrue(StringUtil.isInCurlyBrackets("{}"));
        assertTrue(StringUtil.isInCurlyBrackets("{a}"));
        assertFalse(StringUtil.isInCurlyBrackets("{"));
        assertFalse(StringUtil.isInCurlyBrackets("}"));
        assertFalse(StringUtil.isInCurlyBrackets("a{}"));
        assertFalse(StringUtil.isInCurlyBrackets("{}a"));
        assertFalse(StringUtil.isInCurlyBrackets("{a}a"));
        assertTrue(StringUtil.isInCurlyBrackets("{{a}}"));
    }
}