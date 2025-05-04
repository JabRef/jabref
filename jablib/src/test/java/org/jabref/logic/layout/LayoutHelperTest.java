package org.jabref.logic.layout;

import java.io.IOException;
import java.io.Reader;

import org.jabref.logic.journals.JournalAbbreviationRepository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class LayoutHelperTest {

    private final LayoutFormatterPreferences layoutFormatterPreferences = mock(LayoutFormatterPreferences.class);
    private final JournalAbbreviationRepository abbreviationRepository = mock(JournalAbbreviationRepository.class);

    @Test
    void backslashDoesNotTriggerException() {
        Reader reader = Reader.of("\\");
        LayoutHelper layoutHelper = new LayoutHelper(reader, layoutFormatterPreferences, abbreviationRepository);
        assertThrows(IOException.class, layoutHelper::getLayoutFromText);
    }

    @Test
    void unbalancedBeginEndIsParsed() throws IOException {
        Reader reader = Reader.of("\\begin{doi}, DOI: \\doi");
        LayoutHelper layoutHelper = new LayoutHelper(reader, layoutFormatterPreferences, abbreviationRepository);
        Layout layout = layoutHelper.getLayoutFromText();
        assertNotNull(layout);
    }

    @Test
    void minimalExampleWithDoiGetsParsed() throws IOException {
        Reader reader = Reader.of("\\begin{doi}, DOI: \\doi\\end{doi}");
        LayoutHelper layoutHelper = new LayoutHelper(reader, layoutFormatterPreferences, abbreviationRepository);
        Layout layout = layoutHelper.getLayoutFromText();
        assertNotNull(layout);
    }
}
