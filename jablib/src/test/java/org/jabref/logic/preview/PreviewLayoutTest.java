package org.jabref.logic.preview;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        Optional<PreviewLayout> optionalPreviewLayout = PreviewLayout.of(TEST_ID, List.of(customizedPreviewStyle), List.of(),
                layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        PreviewLayout previewLayout = optionalPreviewLayout.orElseThrow();
        assertInstanceOf(TextBasedPreviewLayout.class, previewLayout);
        assertEquals(TEST_ID, ((TextBasedPreviewLayout) previewLayout).getId());
        assertEquals(NAME1, previewLayout.getDisplayName());
        assertEquals(TEXT1, previewLayout.getText());
    }

    @Test
    void ofFactoryUnknownIdentifierReturnsEmptyOptional() {
        Optional<PreviewLayout> result = PreviewLayout.of("dummyId", List.of(), List.of(),
                layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertTrue(result.isEmpty());
    }

    @Test
    void ofFactoryEmptyCustomizedListReturnsEmptyOptional() {
        Optional<PreviewLayout> result = PreviewLayout.of(TEST_ID, List.of(), List.of(),
                layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertTrue(result.isEmpty());
    }

    @Test
    void ofFactoryResolvesStyleByIdRegardlessOfDisplayName() {
        CustomizedPreviewStyle renamed = new CustomizedPreviewStyle(TEST_ID, "newName", TEXT1);

        PreviewLayout resolved = PreviewLayout.of(TEST_ID, List.of(renamed), List.of(),
                layoutFormatterPreferences, abbreviationRepository, entryTypesManager).orElseThrow();

        assertEquals("newName", resolved.getDisplayName());
    }
}
