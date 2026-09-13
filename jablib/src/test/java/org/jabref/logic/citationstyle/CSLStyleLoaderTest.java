package org.jabref.logic.citationstyle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
        // [utest->req~ux.citation-styles.lazy-source-loading~1]
    void loadsCitationStyleSourceOnDemand() {
        AtomicInteger sourceLoadCount = new AtomicInteger();
        CitationStyle style = new CitationStyle(
                "test.csl",
                "test",
                "in-text",
                "Test",
                "Test",
                false,
                false,
                false,
                false,
                () -> {
                    sourceLoadCount.incrementAndGet();
                    return "<style/>";
                },
                true);

        assertEquals(0, sourceLoadCount.get());
        assertEquals("<style/>", style.getSource());
        assertEquals("<style/>", style.getSource());
        assertEquals(1, sourceLoadCount.get());
    }

    @Test
    void distinguishesLazyCitationStylesByPathWithoutLoadingTheirSources() {
        AtomicInteger sourceLoadCount = new AtomicInteger();
        CitationStyle firstStyle = new CitationStyle(
                "first.csl",
                "first",
                "in-text",
                "First",
                "First",
                false,
                false,
                false,
                false,
                () -> {
                    sourceLoadCount.incrementAndGet();
                    return "<style id=\"first\"/>";
                },
                true);
        CitationStyle secondStyle = new CitationStyle(
                "second.csl",
                "second",
                "in-text",
                "Second",
                "Second",
                false,
                false,
                false,
                false,
                () -> {
                    sourceLoadCount.incrementAndGet();
                    return "<style id=\"second\"/>";
                },
                true);

        assertNotEquals(firstStyle, secondStyle);
        assertEquals(0, sourceLoadCount.get());
    }

    @Test
    void missingCatalogKeysAreDetectedFromFirstEntryStructure() {
        Map<String, Object> incompleteEntry = new HashMap<>(Map.of(
                "path", "ieee.csl",
                "title", "IEEE",
                "styleId", "id",
                "styleClass", "in-text",
                "shortTitle", "IEEE",
                "isNumeric", true,
                "hasBibliography", true));

        assertEquals(
                List.of("hasBibliographySortOrder", "usesHangingIndent"),
                CSLStyleLoader.findMissingCatalogKeys(incompleteEntry));
    }
}
