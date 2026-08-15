package org.jabref.logic.preview;

import java.util.List;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class PreviewLayoutTest {

    private final LayoutFormatterPreferences layoutFormatterPreferences = Mockito.mock(LayoutFormatterPreferences.class);
    private final JournalAbbreviationRepository abbreviationRepository = Mockito.mock(JournalAbbreviationRepository.class);
    private final BibEntryTypesManager entryTypesManager = Mockito.mock(BibEntryTypesManager.class);
    private final String TEST_ID = "test-id";
    private final String NAME1 = "name1";
    private final String TEXT1 = "<b>text1</b>";

    @Test
    void ofFactoryCustomizedPreviewStyleReturnsTextBasedPreviewLayout() {
        CustomizedPreviewStyle customizedPreviewStyle = new CustomizedPreviewStyle(TEST_ID, NAME1, TEXT1);

        PreviewLayout previewLayout = PreviewLayout.of(TEST_ID, List.of(customizedPreviewStyle), List.of(),
                layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertInstanceOf(TextBasedPreviewLayout.class, previewLayout);
        assertEquals(TEST_ID, ((TextBasedPreviewLayout) previewLayout).getId());
        assertEquals(NAME1, previewLayout.getDisplayName());
        assertEquals(TEXT1, previewLayout.getText());
    }

    @Test
    void unknownIdentifierResolvesToNull() {
        // No customizedPreviewStyle/TextBasedPreviewLayout is being added to
        // List<CustomizedPreviewStyle> customizedPreviewLayouts and is empty.
        // Nothing found by the given layoutIdentifier and returns null
        PreviewLayout previewLayout = PreviewLayout.of("nothingIsFound", List.of(), List.of(),
                layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertNull(previewLayout);
    }
}
