package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeEnDashesFormatterTest {

    private NormalizeEnDashesFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new NormalizeEnDashesFormatter();
    }

    @Test
    public void formatExample() {
        assertEquals("Winery -- A Modeling Tool for TOSCA-based Cloud Applications", formatter.format(formatter.getExampleInput()));
    }

    @Test
    public void formatExampleOfChangelog() {
        assertEquals("Example -- illustrative", formatter.format("Example - illustrative"));
    }

    @Test
    public void dashesWithinWordsAreKept() {
        assertEquals("Example-illustrative", formatter.format("Example-illustrative"));
    }

    @Test
    public void dashesPreceededByASpaceAreKept() {
        assertEquals("Example -illustrative", formatter.format("Example -illustrative"));
    }

    @Test
    public void dashesFollowedByASpaceAreKept() {
        assertEquals("Example- illustrative", formatter.format("Example- illustrative"));
    }

    @Test
    public void dashAtTheBeginningIsKept() {
        assertEquals("- illustrative", formatter.format("- illustrative"));
    }

    @Test
    public void dashAtTheEndIsKept() {
        assertEquals("Example-", formatter.format("Example-"));
    }

    @Test
    public void givenLocalizationLanguageSetToEnglish_whenGetNameMethod_thenNormalizeEnDashesIsReturned() {
        Localization.setLanguage(Language.English);
        assertEquals("Normalize en dashes", formatter.getName());
    }

    @Test
    public void givenLocalizationLanguageSetToEnglish_whenGetDescriptionMethod_thenNormalizesTheEnDashesIsReturned() {
        Localization.setLanguage(Language.English);
        assertEquals("Normalizes the en dashes.", formatter.getDescription());
    }

    @Test
    public void whenGetKeyMethod_thenNormalize_En_DashesReturned() {
        assertEquals("normalize_en_dashes", formatter.getKey());
    }

    @Test
    public void whenGetExampleInputMethod_thenWineryMsgReturned() {
        assertEquals("Winery - A Modeling Tool for TOSCA-based Cloud Applications", formatter.getExampleInput());
    }
}
