package org.jabref.logic.citationstyle;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CSLStyleLoaderTest {
    @Test
    void getDefault() {
        assertNotNull(CSLStyleLoader.getDefaultStyle());
    }

    @Test
    void discoverInternalCitationStylesNotNull() {
        List<CitationStyle> styleList = CSLStyleLoader.getInternalStyles();
        assertNotNull(styleList);
        assertFalse(styleList.isEmpty());
    }
}
