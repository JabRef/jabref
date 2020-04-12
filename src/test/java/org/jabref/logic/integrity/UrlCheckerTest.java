package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;

public class UrlCheckerTest {

    @Test
    void urlFieldAcceptsHttpAddress() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.URL, "http://www.google.com"));
    }

    @Test
    void urlFieldAcceptsFullLocalPath() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.URL, "file://c:/asdf/asdf"));
    }

    @Test
    void urlFieldAcceptsFullPathHttpAddress() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.URL, "http://scikit-learn.org/stable/modules/ensemble.html#random-forests"));
    }

    @Test
    void urlFieldDoesNotAcceptHttpAddressWithoutTheHttp() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.URL, "www.google.com"));
    }

    @Test
    void urlFieldDoesNotAcceptPartialHttpAddress() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.URL, "google.com"));
    }

    @Test
    void urlFieldDoesNotAcceptPartialLocalPath() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.URL, "c:/asdf/asdf"));
    }
}
