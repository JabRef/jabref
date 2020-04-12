package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class DOIValidityCheckerTest {

    @Test
    void doiAcceptsValidInput() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.DOI, "10.1023/A:1022883727209"));
    }

    @Test
    void doiAcceptsValidInputWithNotOnlyNumbers() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.DOI, "10.17487/rfc1436"));
    }

    @Test
    void doiAcceptsValidInputNoMatterTheLengthOfTheDOIName() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.DOI, "10.1002/(SICI)1097-4571(199205)43:4<284::AID-ASI3>3.0.CO;2-0"));
    }

    @Test
    void doiDoesNotAcceptInvalidInput() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.DOI, "asdf"));
    }

}
