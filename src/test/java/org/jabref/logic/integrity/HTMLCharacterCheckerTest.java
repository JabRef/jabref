package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class HTMLCharacterCheckerTest {

    @Test
    void titleAcceptsNonHTMLEncodedCharacters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.TITLE, "Not a single {HTML} character"));
    }

    @Test
    void monthAcceptsNonHTMLEncodedCharacters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.MONTH, "#jan#"));
    }

    @Test
    void authorAcceptsNonHTMLEncodedCharacters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.AUTHOR, "A. Einstein and I. Newton"));
    }

    @Test
    void urlAcceptsNonHTMLEncodedCharacters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.URL, "http://www.thinkmind.org/index.php?view=article&amp;articleid=cloud_computing_2013_1_20_20130"));
    }

    @Test
    void authorDoesNotAcceptHTMLEncodedCharacters() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.AUTHOR, "Lenhard, J&#227;rg"));
    }

    @Test
    void journalDoesNotAcceptHTMLEncodedCharacters() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.JOURNAL, "&Auml;rling Str&ouml;m for &#8211; &#x2031;"));
    }


}
