package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveHyphenatedNewlinesFormatterTest {
    private RemoveHyphenatedNewlinesFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveHyphenatedNewlinesFormatter();
    }

    @Test
    public void removeHyphensBeforeNewlines() {
        assertEquals("water", formatter.format("wa-\nter"));
        assertEquals("water", formatter.format("wa-\r\nter"));
        assertEquals("water", formatter.format("wa-\rter"));
    }

    @Test
    public void removeHyphensBeforePlatformSpecificNewlines() {
        String newLine = String.format("%n");
        assertEquals("water", formatter.format("wa-" + newLine + "ter"));
    }

    @Test
    public void givenLocalizationLanguageSetToEnglish_whenGetNameMethod_thenRemoveHyphenatedLineBreaksIsReturned(){
        Localization.setLanguage(Language.English);
        assertEquals("Remove hyphenated line breaks", formatter.getName());
    }

    @Test
    public void givenLocalizationLanguageSetToEnglish_whenGetDescriptionMethod_thenRemovesAllMsgIsReturned(){
        Localization.setLanguage(Language.English);
        assertEquals("Removes all hyphenated line breaks in the field content.", formatter.getDescription());
    }

    @Test
    public void whenGetKeyMethod_thenRemove_Hyphenated_NewlinesReturned(){
        assertEquals("remove_hyphenated_newlines", formatter.getKey());
    }

    @Test
    public void whenGetExampleInputMethod_thenHyphenatedGimmeShelterReturned(){
        assertEquals("Gimme shel-\nter", formatter.getExampleInput());
    }

}
