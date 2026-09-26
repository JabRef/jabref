package org.jabref.gui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionsUtilTest {
    @Test
    void returnExceptionMessage() {
        RuntimeException exc = new RuntimeException("Error message");
        String res = ExceptionsUtil.generateExceptionMessage(exc);
        assertEquals("Error message", res);
    }
}
