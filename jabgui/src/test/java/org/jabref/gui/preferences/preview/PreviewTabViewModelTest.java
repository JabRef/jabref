package org.jabref.gui.preferences.preview;

import java.util.List;

import javafx.beans.property.ListProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.preview.CitationStylePreviewLayout;
import org.jabref.logic.preview.CustomizedPreviewStyle;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PreviewTabViewModelTest {

    private DialogService dialogService;
    private LayoutFormatterPreferences layoutFormatterPreferences;
    private StateManager stateManager;
    private JournalAbbreviationRepository abbreviationRepository;
    private TaskExecutor taskExecutor;
    private BibEntryTypesManager bibEntryTypesManager;

    @BeforeEach
    void setUp() {
        dialogService = Mockito.mock(DialogService.class);
        layoutFormatterPreferences = Mockito.mock(LayoutFormatterPreferences.class);
        Mockito.when(layoutFormatterPreferences.getNameFormatterPreferences()).thenReturn(NameFormatterPreferences.getDefault());
        stateManager = Mockito.mock(StateManager.class);
        Mockito.when(stateManager.getLocalDragboard()).thenReturn(new CustomLocalDragboard());
        abbreviationRepository = Mockito.mock(JournalAbbreviationRepository.class);
        taskExecutor = Mockito.mock(TaskExecutor.class);
        bibEntryTypesManager = Mockito.mock(BibEntryTypesManager.class);
    }

    private PreviewTabViewModel viewModelWith(PreviewPreferences previewPreferences) {
        return new PreviewTabViewModel(dialogService, previewPreferences, layoutFormatterPreferences,
                taskExecutor, stateManager, abbreviationRepository);
    }

    @Test
    void setValuesLoadsCustomizedStylesIntoCustomizedList() {
        PreviewPreferences previewPreferences = new PreviewPreferences(
                List.of(),
                0,
                // populate customizedListProperty
                List.of(
                        new CustomizedPreviewStyle("id0", "name0", "<b>text0</b>"),
                        new CustomizedPreviewStyle("id1", "name1", "<b>text1</b>"),
                        new CustomizedPreviewStyle("id2", "name2", "<b>text2</b>")),
                false,
                false,
                List.of(),
                false
        );
        PreviewTabViewModel viewModel = viewModelWith(previewPreferences);
        viewModel.setValues();

        // check contents of customizedListProperty
        ListProperty<PreviewLayout> customizedListProperty = viewModel.customizedListProperty();
        assertEquals(3, viewModel.customizedListProperty().size());
        assertInstanceOf(TextBasedPreviewLayout.class, customizedListProperty.getValue().get(0));
        TextBasedPreviewLayout customizedPreviewStyle = (TextBasedPreviewLayout) customizedListProperty.getValue().get(0);
        assertEquals("id0", customizedPreviewStyle.getId());
        customizedPreviewStyle = (TextBasedPreviewLayout) customizedListProperty.getValue().get(1);
        assertEquals("name1", customizedPreviewStyle.getName());
        customizedPreviewStyle = (TextBasedPreviewLayout) customizedListProperty.getValue().get(2);
        assertEquals("<b>text2</b>", customizedPreviewStyle.getText());
    }

    @Test
    void setValuesLoadsChosenStylesIntoChosenList() {
        PreviewPreferences previewPreferences = new PreviewPreferences(
                // populate chosenListProperty
                List.of(new CitationStylePreviewLayout(
                        new CitationStyle("testfilepath",
                                "test-title",
                                "test-short-title",
                                false,
                                false,
                                false,
                                "test-source"),
                        bibEntryTypesManager)
                ),
                0,
                List.of(),
                false,
                false,
                List.of(),
                false
        );
        PreviewTabViewModel viewModel = viewModelWith(previewPreferences);
        viewModel.setValues();

        ListProperty<PreviewLayout> chosenListProperty = viewModel.chosenListProperty();
        assertEquals(1, chosenListProperty.size());
        assertInstanceOf(CitationStylePreviewLayout.class, chosenListProperty.getValue().getFirst());
        CitationStylePreviewLayout citationStylePreviewLayout1 = (CitationStylePreviewLayout) chosenListProperty.getValue().getFirst();
        CitationStyle citationStyle1 = citationStylePreviewLayout1.citationStyle();
        assertEquals("testfilepath", citationStyle1.getPath());
        assertEquals("test-title", citationStyle1.getTitle());
        assertEquals("test-short-title", citationStyle1.getShortTitle());
        assertEquals("test-source", citationStyle1.getSource());
    }

    @Test
    void storeSettingsPersistsAllCustomizedStyles() {
        PreviewPreferences previewPreferences = new PreviewPreferences(
                List.of(), 0, List.of(), false, false, List.of(), false);
        PreviewTabViewModel viewModel = viewModelWith(previewPreferences);
        viewModel.setValues();

        TextBasedPreviewLayout style1 = TextBasedPreviewLayout.of("id1", "name1", "<b>text1</b>",
                layoutFormatterPreferences, abbreviationRepository);
        TextBasedPreviewLayout style2 = TextBasedPreviewLayout.of("id2", "name2", "<b>text2</b>",
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.customizedListProperty().add(style1);
        viewModel.chosenListProperty().add(style2);

        viewModel.storeSettings();

        List<CustomizedPreviewStyle> stored = previewPreferences.getCustomizedPreviewLayouts();
        assertEquals(2, stored.size());
        assertEquals("id1", stored.getFirst().id());
        assertEquals("name1", stored.getFirst().name());
        assertEquals("<b>text1</b>", stored.getFirst().text());
        assertEquals("id2", stored.get(1).id());
        assertEquals("name2", stored.get(1).name());
        assertEquals("<b>text2</b>", stored.get(1).text());
    }

    @Test
    void testRenameAndEditUnderSameId() {
        PreviewPreferences previewPreferences = new PreviewPreferences(
                List.of(),
                0,
                List.of(new CustomizedPreviewStyle("id1", "name", "<b>originalText</b>")),
                false,
                false,
                List.of(),
                false
        );
        PreviewTabViewModel viewModel = viewModelWith(previewPreferences);
        viewModel.setValues();
        assertInstanceOf(TextBasedPreviewLayout.class, viewModel.customizedListProperty().getFirst());
        TextBasedPreviewLayout style = (TextBasedPreviewLayout) viewModel.customizedListProperty().getFirst();

        style.setName("renamed");
        style.setText("<b>editedText</b>");

        viewModel.storeSettings();

        List<CustomizedPreviewStyle> stored = previewPreferences.getCustomizedPreviewLayouts();
        assertEquals(1, stored.size());
        assertEquals("id1", stored.getFirst().id());
        assertEquals("renamed", stored.getFirst().name());
        assertEquals("<b>editedText</b>", stored.getFirst().text());
    }

    @Test
    void storeSettingsEmptyChosenListDefaultsToExistingCustomizedStyle() {
        PreviewPreferences previewPreferences = new PreviewPreferences(
                List.of(),
                0,
                List.of(new CustomizedPreviewStyle("id1", "existingName", "<b>text1</b>")),
                false,
                false,
                List.of(),
                false
        );
        PreviewTabViewModel viewModel = viewModelWith(previewPreferences);
        viewModel.setValues();

        TextBasedPreviewLayout style = TextBasedPreviewLayout.of("id1", "existingName", "<b>text</b>",
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.customizedListProperty().add(style);
        // chosenListProperty deliberately left empty, code defaults to looking at customized list

        viewModel.storeSettings();

        ListProperty<PreviewLayout> chosenListProperty = viewModel.chosenListProperty();
        assertEquals(1, chosenListProperty.size());
        assertInstanceOf(TextBasedPreviewLayout.class, viewModel.chosenListProperty().getFirst());
        TextBasedPreviewLayout customLayout = (TextBasedPreviewLayout) chosenListProperty.getFirst();

        assertEquals("id1", customLayout.getId());
        assertEquals("existingName", customLayout.getName());
        assertEquals("<b>text1</b>", customLayout.getText());
    }

    @Test
    void storeSettingsEmptyListsCreatesDefault() {
        //        PreviewPreferences previewPreferences = new PreviewPreferences(
        //                List.of(), 0, List.of(), false, false, List.of(), false);
        //        PreviewTabViewModel viewModel = viewModelWith(previewPreferences);
        //        viewModel.setValues();
        //
        //        viewModel.storeSettings();
        //
        //        assertEquals(1, viewModel.chosenListProperty().size());
        //        PreviewLayout fallback = viewModel.chosenListProperty().getFirst();
        //        assertTrue(fallback instanceof TextBasedPreviewLayout);
        //        assertEquals(TextBasedPreviewLayout.NAME, ((TextBasedPreviewLayout) fallback).getName());

        PreviewPreferences previewPreferences = new PreviewPreferences(
                List.of(),
                0,
                List.of(),
                false,
                false,
                List.of(),
                false
        );
        PreviewTabViewModel viewModel = viewModelWith(previewPreferences);
        viewModel.setValues();

        viewModel.storeSettings();

        assertEquals(1, viewModel.chosenListProperty().size());
        PreviewLayout defaultPreviewLayout = viewModel.chosenListProperty().getFirst();
        assertInstanceOf(TextBasedPreviewLayout.class, defaultPreviewLayout);
        assertEquals(TextBasedPreviewLayout.NAME, defaultPreviewLayout.getName());
    }
}
