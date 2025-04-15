package org.jabref.logic.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StandardFileTypeTest {
    @Test
    void recognizeBlgFileType() {
        FileType detected = StandardFileType.fromExtensions("blg");
        assertEquals(StandardFileType.BLG, detected);
    }

    @Test
    void blgFileTypeProperties() {
        assertEquals("BibTeX log file", StandardFileType.BLG.getName());
        assertEquals(List.of("blg"), StandardFileType.BLG.getExtensions());
    }
}
