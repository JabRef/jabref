package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HTMLCharacterCheckerTest {

    private final HTMLCharacterChecker checker = new HTMLCharacterChecker();
    private final BibEntry entry = new BibEntry();

    @Test
    void testSettingNullThrowsNPE() {
        assertThrows(
                NullPointerException.class,
                () -> entry.setField(StandardField.AUTHOR, null)
        );
    }

    @Test
    void titleAcceptsNonHTMLEncodedCharacters() {
        entry.setField(StandardField.TITLE, "Not a single {HTML} character");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void monthAcceptsNonHTMLEncodedCharacters() {
        entry.setField(StandardField.MONTH, "#jan#");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void authorAcceptsNonHTMLEncodedCharacters() {
        entry.setField(StandardField.AUTHOR, "A. Einstein and I. Newton");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void urlAcceptsNonHTMLEncodedCharacters() {
        entry.setField(StandardField.URL, "http://www.thinkmind.org/index.php?view=article&amp;articleid=cloud_computing_2013_1_20_20130");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void authorDoesNotAcceptHTMLEncodedCharacters() {
        entry.setField(StandardField.AUTHOR, "Lenhard, J&#227;rg");
        assertEquals(List.of(new IntegrityMessage("HTML encoded character found", entry, StandardField.AUTHOR)), checker.check(entry));
    }

    @Test
    void journalDoesNotAcceptHTMLEncodedCharacters() {
        entry.setField(StandardField.JOURNAL, "&Auml;rling Str&ouml;m for &#8211; &#x2031;");
        assertEquals(List.of(new IntegrityMessage("HTML encoded character found", entry, StandardField.JOURNAL)), checker.check(entry));
    }

    static Stream<Arguments> entryWithVerabitmFieldsNotCausingMessages() {
        return Stream.of(
                Arguments.of(StandardField.FILE, "one &amp; another.pdf"),
                Arguments.of(StandardField.URL, "https://example.org?key=value&key2=value2")
        );
    }

    @ParameterizedTest
    @MethodSource
    void entryWithVerabitmFieldsNotCausingMessages(Field field, String value) {
        entry.setField(field, value);
        assertEquals(List.of(), checker.check(entry));
    }
}
