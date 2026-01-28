package org.jabref.logic.citationstyle;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Execution(ExecutionMode.SAME_THREAD)
public class CSLStyleLoaderTest {

    @BeforeAll
    static void setup() {
        // Lifecycle of CSLStyleLoader is different; one needs to load the internal styles using a static method instead of creating an instance.
        CSLStyleLoader.loadInternalStyles();
    }

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
