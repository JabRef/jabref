package org.jabref.logic.layout;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class LayoutHelperTest {

    @Test
    public void backslashDoesNotTriggerException() {
        StringReader stringReader = new StringReader("\\");
        LayoutFormatterPreferences layoutFormatterPreferences = mock(LayoutFormatterPreferences.class);
        LayoutHelper layoutHelper = new LayoutHelper(stringReader, layoutFormatterPreferences);
        assertThrows(IOException.class, () -> layoutHelper.getLayoutFromText());
    }

    @Test
    public void unbalancedBeginEndIsParsed() throws Exception {
        StringReader stringReader = new StringReader("\\begin{doi}, DOI: \\doi");
        LayoutFormatterPreferences layoutFormatterPreferences = mock(LayoutFormatterPreferences.class);
        LayoutHelper layoutHelper = new LayoutHelper(stringReader, layoutFormatterPreferences);
        Layout layout = layoutHelper.getLayoutFromText();
        assertNotNull(layout);
    }

    @Test
    public void minimalExampleWithDoiGetsParsed() throws Exception {
        StringReader stringReader = new StringReader("\\begin{doi}, DOI: \\doi\\end{doi}");
        LayoutFormatterPreferences layoutFormatterPreferences = mock(LayoutFormatterPreferences.class);
        LayoutHelper layoutHelper = new LayoutHelper(stringReader, layoutFormatterPreferences);
        Layout layout = layoutHelper.getLayoutFromText();
        assertNotNull(layout);
    }
}
