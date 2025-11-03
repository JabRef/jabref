package org.jabref.logic.util.strings;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuotedStringTokenizerTest {

    @Test
    void simpleTokenizationWorks() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer("a,b,c", ",", '\\');
        assertTrue(tokenizer.hasMoreTokens());
        assertEquals("a", tokenizer.nextToken());
        assertEquals("b", tokenizer.nextToken());
        assertEquals("c", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    void ignoresQuotedDelimiters() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer("a\\,b,c", ",", '\\');
        assertEquals("a\\,b", tokenizer.nextToken());
        assertEquals("c", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    void consecutiveDelimitersAreSkipped() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer("a,,b", ",", '\\');
        assertEquals("a", tokenizer.nextToken());
        assertEquals("", tokenizer.nextToken());
        assertEquals("b", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    void handlesEmptyInput() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer("", ",", '\\');
        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    void handlesOnlyDelimiters() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(",,,,", ",", '\\');
        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    void handlesQuotedCharacterAtEnd() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer("a,b\\", ",", '\\');
        assertEquals("a", tokenizer.nextToken());
        assertEquals("b\\", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    void handlesQuotedDelimiterAtEndOfToken() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer("a,b\\,c,d", ",", '\\');
        assertEquals("a", tokenizer.nextToken());
        assertEquals("b\\,c", tokenizer.nextToken());
        assertEquals("d", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    void returnsEmptyTokenWhenDelimiterAtEnd() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer("a,b,", ",", '\\');
        assertEquals("a", tokenizer.nextToken());
        assertEquals("b", tokenizer.nextToken());
        assertEquals("", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    void handlesNoDelimitersPresent() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer("abc", ",", '\\');
        assertEquals("abc", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    void handlesQuotedDelimiterImmediatelyAfterQuote() {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer("\\,a,b", ",", '\\');
        assertEquals("\\,a", tokenizer.nextToken());
        assertEquals("b", tokenizer.nextToken());
        assertFalse(tokenizer.hasMoreTokens());
    }
}
