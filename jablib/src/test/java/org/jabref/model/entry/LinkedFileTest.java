package org.jabref.model.entry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedFileTest {

    @Test
    void isOnlineLinkRecognizesVariousSchemes() {
        assertTrue(LinkedFile.isOnlineLink("ftp://example.org/resource.pdf"));
        assertTrue(LinkedFile.isOnlineLink("http://example.org"));
        assertTrue(LinkedFile.isOnlineLink("https://example.org"));
        assertTrue(LinkedFile.isOnlineLink("www.example.org/somepath"));

        assertFalse(LinkedFile.isOnlineLink("C:\\Users\\me\\file.pdf"));
        assertFalse(LinkedFile.isOnlineLink(""));
        assertFalse(LinkedFile.isOnlineLink(null));
    }
}