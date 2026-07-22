package org.jabref.logic.openoffice.style;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BstStyleTest {

    @Test
    void internalNameStripsBstExtensionIeee() {
        BstStyle s = BstStyle.createInternal(BstStyle.INTERNAL_IEEETRAN_PATH);
        assertEquals("IEEEtran", s.getName());
    }

    @Test
    void internalNameStripsBstExtensionAbbrv() {
        BstStyle s = BstStyle.createInternal(BstStyle.INTERNAL_ABBRV_PATH);
        assertEquals("abbrv", s.getName());
    }

    @Test
    void externalLowercaseExtensionIsStripped() {
        BstStyle s = new BstStyle(Path.of("apa.bst"));
        assertEquals("apa", s.getName());
    }

    @Test
    void externalUppercaseExtensionIsStripped() {
        BstStyle s = new BstStyle(Path.of("FOO.BST"));
        assertEquals("FOO", s.getName());
    }

    @Test
    void externalNoExtensionIsUnchanged() {
        BstStyle s = new BstStyle(Path.of("customstyle"));
        assertEquals("customstyle", s.getName());
    }
}
