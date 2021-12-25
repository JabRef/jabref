package org.jabref.logic.integrity;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UTF8CheckerTest {

    private final BibEntry entry = new BibEntry();

    /**
     * fieldAcceptsUTF8 to check UTF8Checker's result set
     * when the entry is encoded in UTF-8 (should be empty)
     */
    @Test
    void fieldAcceptsUTF8() {
        UTF8Checker checker = new UTF8Checker(StandardCharsets.UTF_8);
        entry.setField(StandardField.TITLE, "Only ascii characters!'@12");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    /**
     * fieldDoesNotAcceptUmlauts to check UTF8Checker's result set
     * when the entry is encoded in Non-Utf-8 charset and the Library
     * environment is Non UTF-8.
     * Finally we need to reset the environment charset.
     * @throws UnsupportedEncodingException initial a String in charset GBK
     * Demo: new String(StringDemo.getBytes(), "GBK");
     */
    @Test
    void fieldDoesNotAcceptUmlauts() throws UnsupportedEncodingException {
        UTF8Checker checker = new UTF8Checker(Charset.forName("GBK"));
        String NonUTF8 = new String("你好，这条语句使用GBK字符集".getBytes(), "GBK");
        entry.setField(StandardField.MONTH, NonUTF8);
        assertEquals(List.of(new IntegrityMessage("Non-UTF-8 encoded field found", entry, StandardField.MONTH)), checker.check(entry));
    }

    /**
     * To check the UTF8Checker.UTF8EncodingChecker
     * in NonUTF8 char array (should return false)
     *
     * @throws UnsupportedEncodingException initial a String in charset GBK
     * Demo: new String(StringDemo.getBytes(), "GBK");
     */
    @Test
    void NonUTF8EncodingCheckerTest() throws UnsupportedEncodingException {
        String NonUTF8 = new String("你好，这条语句使用GBK字符集".getBytes(), "GBK");
            assertFalse(UTF8Checker.UTF8EncodingChecker(NonUTF8.getBytes("GBK")));

    }

    /**
     * To check the UTF8Checker.UTF8EncodingChecker
     * in UTF-8 char array (should return true)
     */
    @Test
    void UTF8EncodingCheckerTest() {
        String UTF8Demo = new String("你好，这条语句使用GBK字符集".getBytes(), StandardCharsets.UTF_8);
            assertTrue(UTF8Checker.UTF8EncodingChecker(UTF8Demo.getBytes(StandardCharsets.UTF_8)));
    }
}
