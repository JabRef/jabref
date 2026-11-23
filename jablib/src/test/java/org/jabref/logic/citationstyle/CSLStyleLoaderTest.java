package org.jabref.logic.citationstyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.collections.FXCollections;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated("CSLStyleLoader maintains a shared static style registry")
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

    @Nested
    @NullMarked
    class ExternalStyleRegistration {

        private final OpenOfficePreferences openOfficePreferences = mock(OpenOfficePreferences.class);
        private final AtomicInteger sourceLoadCount = new AtomicInteger();
        private List<CitationStyle> originalExternalStyles;
        private CSLStyleLoader loader;

        @BeforeEach
        void setup() {
            originalExternalStyles = CSLStyleLoader.getStyles().stream()
                                                   .filter(style -> !style.isInternalStyle())
                                                   .toList();
            when(openOfficePreferences.getExternalCslStyles()).thenReturn(FXCollections.observableArrayList());
            loader = new CSLStyleLoader(openOfficePreferences);
            verify(openOfficePreferences, never()).setExternalCslStyles(anyList());
            clearInvocations(openOfficePreferences);
        }

        @AfterEach
        void restoreExternalStyles() {
            OpenOfficePreferences restoringPreferences = mock(OpenOfficePreferences.class);
            when(restoringPreferences.getExternalCslStyles()).thenReturn(FXCollections.observableArrayList());
            CSLStyleLoader restoringLoader = new CSLStyleLoader(restoringPreferences);
            originalExternalStyles.forEach(restoringLoader::addExternalStyle);
        }

        @Test
        void addExternalStyleAppendsAndPersistsEachAcceptedStyleOnce() {
            CitationStyle firstStyle = createExternalStyle("first-external.csl", "First external CSL style", "First external CSL");
            CitationStyle secondStyle = createExternalStyle("second-external.csl", "Second external CSL style", "Second external CSL");

            assertTrue(loader.addExternalStyle(firstStyle));
            assertTrue(loader.addExternalStyle(secondStyle));

            List<CitationStyle> externalStyles = CSLStyleLoader.getStyles().stream()
                                                               .filter(style -> !style.isInternalStyle())
                                                               .toList();
            assertEquals(List.of(firstStyle, secondStyle), externalStyles);
            verify(openOfficePreferences).setExternalCslStyles(List.of(firstStyle.getPath()));
            verify(openOfficePreferences).setExternalCslStyles(List.of(firstStyle.getPath(), secondStyle.getPath()));
            verifyNoMoreInteractions(openOfficePreferences);
            assertEquals(0, sourceLoadCount.get());
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void addExternalStyleRejectsBundledTitleOrShortTitle(boolean matchingShortTitle) {
            CitationStyle bundledStyle = CSLStyleLoader.getInternalStyles().stream()
                                                       .filter(style -> !style.getShortTitle().isBlank())
                                                       .findFirst()
                                                       .orElseThrow();
            CitationStyle duplicate = createExternalStyle(
                    "bundled-duplicate.csl",
                    matchingShortTitle ? "External CSL style with bundled short title" : bundledStyle.getTitle(),
                    matchingShortTitle ? bundledStyle.getShortTitle() : "External CSL abbreviation");

            assertRejectedStyle(duplicate);
        }

        @ParameterizedTest
        @CsvSource({
                "External CSL style, Different external CSL abbreviation",
                "Different external CSL style, External CSL abbreviation"
        })
        void addExternalStyleRejectsExternalTitleOrShortTitle(String title, String shortTitle) {
            CitationStyle existingStyle = createExternalStyle("existing-external.csl", "External CSL style", "External CSL abbreviation");
            assertTrue(loader.addExternalStyle(existingStyle));
            clearInvocations(openOfficePreferences);
            CitationStyle duplicate = createExternalStyle("duplicate-external.csl", title, shortTitle);

            assertRejectedStyle(duplicate);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " \t"})
        void addExternalStyleAcceptsDistinctTitlesWithBlankShortTitles(String shortTitle) {
            CitationStyle firstStyle = createExternalStyle("first-blank.csl", "First external CSL style", shortTitle);
            CitationStyle secondStyle = createExternalStyle("second-blank.csl", "Second external CSL style", shortTitle);

            assertTrue(loader.addExternalStyle(firstStyle));
            assertTrue(loader.addExternalStyle(secondStyle));

            verify(openOfficePreferences).setExternalCslStyles(List.of(firstStyle.getPath()));
            verify(openOfficePreferences).setExternalCslStyles(List.of(firstStyle.getPath(), secondStyle.getPath()));
            verifyNoMoreInteractions(openOfficePreferences);
        }

        @Test
        void addExternalStyleRejectsInternalStyleWithDistinctMetadata() {
            CitationStyle internalStyle = new CitationStyle(
                    "unregistered-internal.csl",
                    "unregistered-internal",
                    "in-text",
                    "Unregistered internal CSL style",
                    "Unregistered internal CSL",
                    false,
                    false,
                    false,
                    false,
                    "<style/>",
                    true);

            assertRejectedStyle(internalStyle);
        }

        @Test
        void removeStyleDoesNotPersistAbsentExternalStyle() {
            CitationStyle absentStyle = createExternalStyle("absent-external.csl", "Absent external CSL style", "Absent external CSL");
            List<CitationStyle> originalStyles = CSLStyleLoader.getStyles();

            assertFalse(loader.removeStyle(absentStyle));

            assertEquals(originalStyles, CSLStyleLoader.getStyles());
            verify(openOfficePreferences, never()).setExternalCslStyles(anyList());
        }

        @Test
        void removeStyleDoesNotPersistInternalStyle() {
            List<CitationStyle> originalStyles = CSLStyleLoader.getStyles();

            assertFalse(loader.removeStyle(CSLStyleLoader.getInternalStyles().getFirst()));

            assertEquals(originalStyles, CSLStyleLoader.getStyles());
            verify(openOfficePreferences, never()).setExternalCslStyles(anyList());
        }

        @Test
        void removeStylePersistsSuccessfulRemovalOnce() {
            CitationStyle removedStyle = createExternalStyle("removed-external.csl", "Removed external CSL style", "Removed external CSL");
            CitationStyle remainingStyle = createExternalStyle("remaining-external.csl", "Remaining external CSL style", "Remaining external CSL");
            assertTrue(loader.addExternalStyle(removedStyle));
            assertTrue(loader.addExternalStyle(remainingStyle));
            clearInvocations(openOfficePreferences);

            assertTrue(loader.removeStyle(removedStyle));

            List<CitationStyle> externalStyles = CSLStyleLoader.getStyles().stream()
                                                               .filter(style -> !style.isInternalStyle())
                                                               .toList();
            assertEquals(List.of(remainingStyle), externalStyles);
            verify(openOfficePreferences).setExternalCslStyles(List.of(remainingStyle.getPath()));
            verifyNoMoreInteractions(openOfficePreferences);
        }

        @Test
        void getStylesReturnsUnmodifiableSnapshot() {
            List<CitationStyle> styles = CSLStyleLoader.getStyles();
            CitationStyle externalStyle = createExternalStyle("unmodifiable-external.csl", "External CSL style", "External CSL abbreviation");

            assertThrows(UnsupportedOperationException.class, () -> styles.add(externalStyle));
        }

        @Test
        void getStylesReturnsDetachedSnapshotsWithBundledStylesBeforeExternalStyles() {
            List<CitationStyle> beforeAddition = CSLStyleLoader.getStyles();
            List<CitationStyle> bundledStyles = CSLStyleLoader.getInternalStyles();
            CitationStyle externalStyle = createExternalStyle("snapshot-external.csl", "External CSL style", "External CSL abbreviation");
            assertTrue(loader.addExternalStyle(externalStyle));
            List<CitationStyle> afterAddition = CSLStyleLoader.getStyles();
            List<CitationStyle> expectedStyles = new ArrayList<>(bundledStyles);
            expectedStyles.add(externalStyle);
            assertTrue(loader.removeStyle(externalStyle));

            assertEquals(bundledStyles, beforeAddition);
            assertEquals(expectedStyles, afterAddition);
            assertEquals(bundledStyles, CSLStyleLoader.getStyles());
            assertEquals(0, sourceLoadCount.get());
        }

        private void assertRejectedStyle(CitationStyle style) {
            List<CitationStyle> originalStyles = CSLStyleLoader.getStyles();

            assertFalse(loader.addExternalStyle(style));

            assertEquals(originalStyles, CSLStyleLoader.getStyles());
            verify(openOfficePreferences, never()).setExternalCslStyles(anyList());
            assertEquals(0, sourceLoadCount.get());
        }

        private CitationStyle createExternalStyle(String path, String title, String shortTitle) {
            return new CitationStyle(
                    path,
                    path,
                    "in-text",
                    title,
                    shortTitle,
                    false,
                    false,
                    false,
                    false,
                    () -> {
                        sourceLoadCount.incrementAndGet();
                        return "<style/>";
                    },
                    false);
        }
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
