package org.jabref.gui.preview;

import java.util.List;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewPreferencesTest {

    private final LayoutFormatterPreferences layoutFormatterPreferences = LayoutFormatterPreferences.getDefault();
    private final JournalAbbreviationRepository abbreviationRepository = new JournalAbbreviationRepository();
    private final BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();

    @Test
    void getDefaultHasNoStyles() {
        PreviewPreferences defaults = PreviewPreferences.getDefault();

        assertTrue(defaults.getLayoutCycle().isEmpty());
        assertTrue(defaults.getCustomPreviewLayouts().isEmpty());
    }

    @Test
    void getDefaultWithStylesSeedsExactlyOneCustomLayout() {
        PreviewPreferences defaults = PreviewPreferences.getDefaultWithStyles(layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertEquals(1, defaults.getCustomPreviewLayouts().size());
    }

    @Test
    void getDefaultWithStylesSeedsTwoLayoutCycleEntries() {
        PreviewPreferences defaults = PreviewPreferences.getDefaultWithStyles(layoutFormatterPreferences, abbreviationRepository, entryTypesManager);

        assertEquals(2, defaults.getLayoutCycle().size());
    }

    @Test
    void getDefaultWithStylesCustomLayoutIsSameInstanceInBothLists() {
        PreviewPreferences defaults = PreviewPreferences.getDefaultWithStyles(layoutFormatterPreferences, abbreviationRepository, entryTypesManager);
        TextBasedPreviewLayout customLayout = defaults.getCustomPreviewLayouts().getFirst();

        assertTrue(defaults.getLayoutCycle().contains(customLayout));
    }

    @Test
    void setCustomPreviewLayoutsReplacesExistingList() {
        PreviewPreferences preferences = PreviewPreferences.getDefault();
        TextBasedPreviewLayout layout = new TextBasedPreviewLayout("My Style", "some text", layoutFormatterPreferences, abbreviationRepository);
        preferences.setCustomPreviewLayouts(List.of(layout));

        assertEquals(1, preferences.getCustomPreviewLayouts().size());
        assertSame(layout, preferences.getCustomPreviewLayouts().getFirst());
    }

    @Test
    void getSelectedPreviewLayoutFallsBackToFirstCustomLayoutTextWhenCycleIsEmpty() {
        PreviewPreferences preferences = PreviewPreferences.getDefault();
        TextBasedPreviewLayout layout = new TextBasedPreviewLayout("My Style", "some text", layoutFormatterPreferences, abbreviationRepository);
        preferences.setCustomPreviewLayouts(List.of(layout));
        PreviewLayout selected = preferences.getSelectedPreviewLayout();

        assertEquals("some text", selected.getText());
    }
}
