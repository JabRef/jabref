package org.jabref.logic.preview;

import java.util.List;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PreviewLayoutTest {

    private final LayoutFormatterPreferences layoutFormatterPreferences = LayoutFormatterPreferences.getDefault();
    private final JournalAbbreviationRepository abbreviationRepository = new JournalAbbreviationRepository();
    private final BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();

    @Test
    void ofFindsCustomLayoutByName() {
        TextBasedPreviewLayout myStyle = new TextBasedPreviewLayout("My Style", "some text", layoutFormatterPreferences, abbreviationRepository);
        List<TextBasedPreviewLayout> customLayouts = List.of(myStyle);
        PreviewLayout result = PreviewLayout.of("My Style", customLayouts, List.of(), layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertSame(myStyle, result);
    }

    @Test
    void ofReturnsCorrectLayoutAmongMultipleCustomStyles() {
        TextBasedPreviewLayout first = new TextBasedPreviewLayout("First", "some text", layoutFormatterPreferences, abbreviationRepository);
        TextBasedPreviewLayout second = new TextBasedPreviewLayout("Second", "some text", layoutFormatterPreferences, abbreviationRepository);
        List<TextBasedPreviewLayout> customLayouts = List.of(first, second);
        PreviewLayout result = PreviewLayout.of("Second", customLayouts, List.of(), layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertSame(second, result);
    }

    @Test
    void ofReturnsNullForUnknownStyle() {
        TextBasedPreviewLayout myStyle = new TextBasedPreviewLayout("My Style", "some text", layoutFormatterPreferences, abbreviationRepository);
        List<TextBasedPreviewLayout> customLayouts = List.of(myStyle);
        PreviewLayout result = PreviewLayout.of("Nonexistent Style", customLayouts, List.of(), layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertNull(result);
    }

    @Test
    void ofReturnsNullWhenNoCustomLayoutsExist() {
        PreviewLayout result = PreviewLayout.of("Anything", List.of(), List.of(), layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertNull(result);
    }
}
