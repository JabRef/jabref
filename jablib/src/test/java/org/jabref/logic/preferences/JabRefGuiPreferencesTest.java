package org.jabref.logic.preferences;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Localization.lang")
@ResourceLock("Preferences")
class JabRefGuiPreferencesTest {
    private static final String LEGACY_OO_STYLE_KEY = "ooBibliographyStyleFile";

    private final Map<String, Optional<String>> savedPreferences = new HashMap<>();

    @BeforeEach
    void saveOpenOfficeStylePreferences() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        for (String key : List.of(JabRefCliPreferences.OO_CURRENT_STYLE, JabRefCliPreferences.OO_CSL_BIBLIOGRAPHY_BODY_FORMAT, LEGACY_OO_STYLE_KEY)) {
            savedPreferences.put(key, preferences.hasKey(key) ? Optional.of(preferences.get(key, "")) : Optional.empty());
            preferences.remove(key);
        }
    }

    @AfterEach
    void restoreOpenOfficeStylePreferences() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        savedPreferences.forEach((key, value) -> value.ifPresentOrElse(
                stored -> preferences.put(key, stored),
                () -> preferences.remove(key)));
    }

    @ParameterizedTest
    @MethodSource("openOfficeStyles")
    void currentOpenOfficeStyleSurvivesPreferencesReload(OOStyle selectedStyle) {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        JournalAbbreviationRepository abbreviationRepository = mock(JournalAbbreviationRepository.class);
        OpenOfficePreferences openOfficePreferences = preferences.getOpenOfficePreferences(abbreviationRepository);

        openOfficePreferences.setCurrentStyle(selectedStyle);

        assertEquals(selectedStyle.getPath(), preferences.get(JabRefCliPreferences.OO_CURRENT_STYLE, ""));
        assertEquals(selectedStyle, new JabRefCliPreferences().getOpenOfficePreferences(abbreviationRepository).getCurrentStyle());
    }

    static Stream<OOStyle> openOfficeStyles() throws IOException {
        return Stream.of(
                CSLStyleUtils.createCitationStyleFromFile("apa.csl").orElseThrow(),
                new JStyle(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, LayoutFormatterPreferences.getDefault(), mock(JournalAbbreviationRepository.class)),
                BstStyle.createInternal("/resource/openoffice/IEEEtran.bst"));
    }

    @Test
    void ignoresLegacyJStylePreferenceWithoutMigratingIt() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.put(LEGACY_OO_STYLE_KEY, JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH);

        OpenOfficePreferences openOfficePreferences = preferences.getOpenOfficePreferences(mock(JournalAbbreviationRepository.class));

        assertEquals(CSLStyleLoader.getDefaultStyle(), openOfficePreferences.getCurrentStyle());
        assertEquals(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, preferences.get(LEGACY_OO_STYLE_KEY, ""));
        assertEquals("", preferences.get(JabRefCliPreferences.OO_CURRENT_STYLE, ""));
    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of(List.of("A", "B", "C", "D"), "A;B;C;D"),
                Arguments.of(List.of("A", "B", "C", ""), "A;B;C;"),
                Arguments.of(List.of("a;b", "C:\\Tools\\app.exe"), "a\\;b;C:\\\\Tools\\\\app.exe"),
                Arguments.of(List.of("a\\", "b"), "a\\\\;b")
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void convertStringToList(List<String> sampleList, String sampleString) {
        assertEquals(sampleList, JabRefCliPreferences.convertStringToList(sampleString));
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void convertListToString(List<String> sampleList, String sampleString) {
        assertEquals(sampleString, JabRefCliPreferences.convertListToString(sampleList));
    }

    @Test
    void convertStringToListPreservesUnescapedBackslashes() {
        assertEquals(List.of("C:\\Tools\\app.exe"), JabRefCliPreferences.convertStringToList("C:\\Tools\\app.exe"));
    }
}
