package org.jabref.logic.openoffice.style;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BstStyleTest {

    @Test
    void internalNameStripsBstExtensionIeee() throws IOException {
        BstStyle style = BstStyle.createInternal(BstStyle.INTERNAL_IEEETRAN_PATH);
        assertEquals("IEEEtran", style.getName());
    }

    @Test
    void internalNameStripsBstExtensionAbbrv() throws IOException {
        BstStyle style = BstStyle.createInternal(BstStyle.INTERNAL_ABBRV_PATH);
        assertEquals("abbrv", style.getName());
    }

    @Test
    void externalLowercaseExtensionIsStripped(@TempDir Path tempDir) throws IOException {
        Path stylePath = tempDir.resolve("apa.bst");
        Files.writeString(stylePath, "READ");
        BstStyle style = BstStyle.loadExternal(stylePath);
        assertEquals("apa", style.getName());
    }

    @Test
    void externalUppercaseExtensionIsStripped(@TempDir Path tempDir) throws IOException {
        Path stylePath = tempDir.resolve("FOO.BST");
        Files.writeString(stylePath, "READ");
        BstStyle style = BstStyle.loadExternal(stylePath);
        assertEquals("FOO", style.getName());
    }

    @Test
    void externalNoExtensionIsUnchanged(@TempDir Path tempDir) throws IOException {
        Path stylePath = tempDir.resolve("customstyle");
        Files.writeString(stylePath, "READ");
        BstStyle style = BstStyle.loadExternal(stylePath);
        assertEquals("customstyle", style.getName());
    }

    @Test
    void hasSortCommandIsStoredOnStyle() throws IOException {
        BstStyle ieeeStyle = BstStyle.createInternal(BstStyle.INTERNAL_IEEETRAN_PATH);
        BstStyle abbrvStyle = BstStyle.createInternal(BstStyle.INTERNAL_ABBRV_PATH);

        assertFalse(ieeeStyle.hasSortCommand());
        assertTrue(abbrvStyle.hasSortCommand());
    }

    @Test
    void invalidExternalBstIsRejected(@TempDir Path tempDir) throws IOException {
        Path stylePath = tempDir.resolve("invalid.bst");
        Files.writeString(stylePath, "}");

        assertThrows(IOException.class, () -> BstStyle.loadExternal(stylePath));
    }
}
