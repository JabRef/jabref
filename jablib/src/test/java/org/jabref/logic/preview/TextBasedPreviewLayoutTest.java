package org.jabref.logic.preview;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TextBasedPreviewLayoutTest {

    private final LayoutFormatterPreferences layoutFormatterPreferences = Mockito.mock(LayoutFormatterPreferences.class);
    private final JournalAbbreviationRepository abbreviationRepository = Mockito.mock(JournalAbbreviationRepository.class);
    private final String TEST_ID = "test-id";
    private final String NAME1 = "name1";
    private final String NAME2 = "name2";
    private final String TEXT1 = "<b>text1</b>";
    private final String TEXT2 = "<b>text2</b>";

    @Test
    void compareTextBasedPreviewLayoutForUniqueIDs() {
        CustomizedPreviewStyle style1 = new CustomizedPreviewStyle(NAME1, TEXT1);
        CustomizedPreviewStyle style2 = new CustomizedPreviewStyle(NAME2, TEXT2);
        TextBasedPreviewLayout first = TextBasedPreviewLayout.of(style1.id(), style1.name(), style1.text(), layoutFormatterPreferences, abbreviationRepository);
        TextBasedPreviewLayout second = TextBasedPreviewLayout.of(style2.id(), style2.name(), style2.text(), layoutFormatterPreferences, abbreviationRepository);
        assertNotEquals(first.getId(), second.getId());
        assertEquals(NAME1, first.getName());
        assertEquals(NAME2, second.getName());
        assertEquals(TEXT1, first.getText());
        assertEquals(TEXT2, second.getText());
    }

    @Test
    void ofFactoryAssignArgId() {
        TextBasedPreviewLayout layout = TextBasedPreviewLayout.of(TEST_ID, NAME1, TEXT1,
                layoutFormatterPreferences, abbreviationRepository);
        assertEquals(TEST_ID, layout.getId());
        assertEquals(NAME1, layout.getName());
        assertEquals(TEXT1, layout.getText());
    }

    @Test
    void layoutConstructorReturnsNonNullOnGetId() {
        // layout constructor doesn't set id, but TextBasedPreviewLayout.getId() shouldn't return null
        // this unit test checks for that scenario
        Layout rawLayout = Mockito.mock(Layout.class);
        Mockito.when(rawLayout.getText()).thenReturn("<b>text</b>");
        TextBasedPreviewLayout layout = new TextBasedPreviewLayout(rawLayout);
        assertNotNull(layout.getId());
    }
}
