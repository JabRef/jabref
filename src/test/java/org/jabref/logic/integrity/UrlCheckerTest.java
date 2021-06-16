package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class UrlCheckerTest {

    private final UrlChecker checker = new UrlChecker();

    @Test
    void urlFieldAcceptsHttpAddress() {
        assertEquals(Optional.empty(), checker.checkValue("http://www.google.com"));
    }

    @Test
    void urlFieldAcceptsFullLocalPath() {
        assertEquals(Optional.empty(), checker.checkValue("file://c:/asdf/asdf"));
    }

    @Test
    void urlFieldAcceptsFullPathHttpAddress() {
        assertEquals(Optional.empty(), checker.checkValue("http://scikit-learn.org/stable/modules/ensemble.html#random-forests"));
    }

    @Test
    void urlFieldDoesNotAcceptHttpAddressWithoutTheHttp() {
        assertNotEquals(Optional.empty(), checker.checkValue("www.google.com"));
    }

    @Test
    void urlFieldDoesNotAcceptPartialHttpAddress() {
        assertNotEquals(Optional.empty(), checker.checkValue("google.com"));
    }

    @Test
    void urlFieldDoesNotAcceptPartialLocalPath() {
        assertNotEquals(Optional.empty(), checker.checkValue("c:/asdf/asdf"));
    }
}
