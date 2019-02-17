
package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveNewlinesFormatterTest {

    private RemoveNewlinesFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveNewlinesFormatter();
    }

    @Test
    public void removeCarriageReturnLineFeed() {
        assertEquals("rn linebreak", formatter.format("rn\r\nlinebreak"));
    }

    @Test
    public void removeCarriageReturn() {
        assertEquals("r linebreak", formatter.format("r\rlinebreak"));
    }

    @Test
    public void removeLineFeed() {
        assertEquals("n linebreak", formatter.format("n\nlinebreak"));
    }

    @Test
    public void removePlatformSpecificNewLine() {
        String newLine = String.format("%n");
        assertEquals("linebreak on current platform", formatter.format("linebreak on" + newLine + "current platform"));
    }

    @Test
    public void givenLocalizationLanguageSetToEnglish_whenGetNameMethod_thenRemoveLineBreaksIsReturned(){
        Localization.setLanguage(Language.English);
        assertEquals("Remove line breaks", formatter.getName());
    }

    @Test
    public void givenLocalizationLanguageSetToEnglish_whenGetDescriptionMethod_thenRemovesAllLineBreaksInMsgIsReturned(){
        Localization.setLanguage(Language.English);
        assertEquals("Removes all line breaks in the field content.", formatter.getDescription());
    }

    @Test
    public void whenGetKeyMethod_thenRemove_NewlinesReturned(){
        assertEquals("remove_newlines", formatter.getKey());
    }

    @Test
    public void whenGetExampleInputMethod_thenInCDMAWithLineReturnsReturned(){
        assertEquals("In \n CDMA", formatter.getExampleInput());
    }
}
