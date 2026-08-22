package org.jabref.gui.preview;

import java.util.List;

import javafx.collections.ObservableList;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.preview.CitationStylePreviewLayout;
import org.jabref.logic.preview.CustomizedPreviewStyle;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PreviewPreferencesTest {
    private BibEntryTypesManager bibEntryTypesManager;
    private final String ID1 = "id1";
    private final String NAME1 = "name1";
    private final String TEXT1 = "<b>text1</b>";
    private final String TEST_FILEPATH = "test-filepath";
    private final String TEST_TITLE = "test-title";
    private final String TEST_SHORT_TITLE = "test-short-title";
    private final String TEST_SOURCE = "test-source";

    @BeforeEach
    void setUp() {
        bibEntryTypesManager = Mockito.mock(BibEntryTypesManager.class);
    }

    @Test
    void getDefaultReturnOneDefaultCustomizedStyle() {
        PreviewPreferences preferences = PreviewPreferences.getDefault();
        ObservableList<CustomizedPreviewStyle> customizedPreviewStyles = preferences.getCustomizedPreviewStyles();
        assertEquals(1, customizedPreviewStyles.size());
        assertEquals(TextBasedPreviewLayout.DEFAULT, customizedPreviewStyles.getFirst().text());
        assertEquals(TextBasedPreviewLayout.NAME, customizedPreviewStyles.getFirst().name());
    }

    @Test
    void getSelectedReturnFirstCustomizedStyleWhenEmptyLayoutCycle() {
        PreviewPreferences preferences = new PreviewPreferences(
                List.of(),
                0,
                List.of(new CustomizedPreviewStyle(ID1, NAME1, TEXT1)),
                false,
                false,
                List.of(),
                false);
        PreviewLayout previewLayout = preferences.getSelectedPreviewLayout();
        assertInstanceOf(TextBasedPreviewLayout.class, previewLayout);
        ObservableList<CustomizedPreviewStyle> customizedPreviewStyles = preferences.getCustomizedPreviewStyles();
        assertEquals(1, customizedPreviewStyles.size());
        assertEquals(ID1, customizedPreviewStyles.getFirst().id());
        assertEquals(NAME1, customizedPreviewStyles.getFirst().name());
        assertEquals(TEXT1, customizedPreviewStyles.getFirst().text());
    }

    @Test
    void getSelectedReturnFirstPreviewWhenLayoutCyclePopulated() {
        PreviewPreferences preferences = new PreviewPreferences(
                List.of(new CitationStylePreviewLayout(
                        new CitationStyle(TEST_FILEPATH,
                                TEST_TITLE,
                                TEST_SHORT_TITLE,
                                false,
                                false,
                                false,
                                TEST_SOURCE),
                        bibEntryTypesManager)
                ),
                0,
                List.of(),
                false,
                false,
                List.of(),
                false);
        PreviewLayout previewLayout = preferences.getLayoutCycle().getFirst();
        assertInstanceOf(CitationStylePreviewLayout.class, previewLayout);
        CitationStyle citationStyle = ((CitationStylePreviewLayout) previewLayout).citationStyle();
        assertEquals(1, preferences.getLayoutCycle().size());
        assertEquals(TEST_FILEPATH, citationStyle.getFilePath());
        assertEquals(TEST_TITLE, citationStyle.getTitle());
        assertEquals(TEST_SHORT_TITLE, citationStyle.getShortTitle());
        assertEquals(TEST_SOURCE, citationStyle.getSource());
    }
}
