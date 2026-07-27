package org.jabref.logic.openoffice.style;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BstStyleTest {

    @Test
    void internalNameStripsBstExtensionIeee() {
        BstStyle style = BstStyle.createInternal(BstStyle.INTERNAL_IEEETRAN_PATH);
        assertEquals("IEEEtran", style.getName());
    }

    @Test
    void internalNameStripsBstExtensionAbbrv() {
        BstStyle style = BstStyle.createInternal(BstStyle.INTERNAL_ABBRV_PATH);
        assertEquals("abbrv", style.getName());
    }

    @Test
    void externalLowercaseExtensionIsStripped() {
        BstStyle style = new BstStyle(Path.of("apa.bst"));
        assertEquals("apa", style.getName());
    }

    @Test
    void externalUppercaseExtensionIsStripped() {
        BstStyle style = new BstStyle(Path.of("FOO.BST"));
        assertEquals("FOO", style.getName());
    }

    @Test
    void externalNoExtensionIsUnchanged() {
        BstStyle style = new BstStyle(Path.of("customstyle"));
        assertEquals("customstyle", style.getName());
    }
}
