package org.jabref.logic.layout;

import java.io.IOException;
import java.io.StringReader;

import org.jabref.logic.journals.JournalAbbreviationRepository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class LayoutHelperTest {

    private final LayoutFormatterPreferences layoutFormatterPreferences = mock(LayoutFormatterPreferences.class);
    private final JournalAbbreviationRepository abbreviationRepository = mock(JournalAbbreviationRepository.class);

    @Test
    public void backslashDoesNotTriggerException() {
        StringReader stringReader = new StringReader("\\");
        LayoutHelper layoutHelper = new LayoutHelper(stringReader, layoutFormatterPreferences, abbreviationRepository);
        assertThrows(IOException.class, layoutHelper::getLayoutFromText);
    }

    @Test
    public void unbalancedBeginEndIsParsed() throws Exception {
        StringReader stringReader = new StringReader("\\begin{doi}, DOI: \\doi");
        LayoutHelper layoutHelper = new LayoutHelper(stringReader, layoutFormatterPreferences, abbreviationRepository);
        Layout layout = layoutHelper.getLayoutFromText();
        assertNotNull(layout);
    }

    @Test
    public void minimalExampleWithDoiGetsParsed() throws Exception {
        StringReader stringReader = new StringReader("\\begin{doi}, DOI: \\doi\\end{doi}");
        LayoutHelper layoutHelper = new LayoutHelper(stringReader, layoutFormatterPreferences, abbreviationRepository);
        Layout layout = layoutHelper.getLayoutFromText();
        assertNotNull(layout);
    }
}
