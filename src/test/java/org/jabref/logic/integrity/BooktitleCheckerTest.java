package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

public class BooktitleCheckerTest {

    @Test
    void booktitleAcceptsIfItDoesNotEndWithConferenceOn() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.BOOKTITLE, "2014 Fourth International Conference on Digital Information and Communication Technology and it's Applications (DICTAP)", StandardEntryType.Proceedings));
    }

    @Test
    void booktitleDoesNotAcceptsIfItEndsWithConferenceOn() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.BOOKTITLE, "Digital Information and Communication Technology and it's Applications (DICTAP), 2014 Fourth International Conference on", StandardEntryType.Proceedings));
    }

}
