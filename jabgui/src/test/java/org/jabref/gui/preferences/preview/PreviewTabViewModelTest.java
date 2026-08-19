package org.jabref.gui.preferences.preview;

import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.Dragboard;

import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.StateManager;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.JavaFxThreadingUtil;
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class PreviewTabViewModelTest {

    private DialogService dialogService;
    private LayoutFormatterPreferences layoutFormatterPreferences;
    private StateManager stateManager;
    private JournalAbbreviationRepository abbreviationRepository;
    private TaskExecutor taskExecutor;
    private BibEntryTypesManager bibEntryTypesManager;

    private final String ID1 = "id1";
    private final String ID2 = "id2";
    private final String ID3 = "id3";
    private final String NAME1 = "name1";
    private final String NAME2 = "name2";
    private final String NAME3 = "name3";
    private final String TEXT1 = "<b>text1</b>";
    private final String TEXT2 = "<b>text2</b>";
    private final String TEXT3 = "<b>text3</b>";
    private final String TEST_FILEPATH = "test-filepath";
    private final String TEST_TITLE = "test-title";
    private final String TEST_SHORT_TITLE = "test-short-title";
    private final String TEST_SOURCE = "test-source";

    @BeforeEach
    void setUp() {
        dialogService = Mockito.mock(DialogService.class);
        layoutFormatterPreferences = Mockito.mock(LayoutFormatterPreferences.class);
        when(layoutFormatterPreferences.getNameFormatterPreferences()).thenReturn(NameFormatterPreferences.getDefault());
        stateManager = Mockito.mock(StateManager.class);
        when(stateManager.getLocalDragboard()).thenReturn(new CustomLocalDragboard());
        abbreviationRepository = Mockito.mock(JournalAbbreviationRepository.class);
        taskExecutor = Mockito.mock(TaskExecutor.class);
        bibEntryTypesManager = Mockito.mock(BibEntryTypesManager.class);
    }

    /*
    constructing a ListView to borrow its selection model
    If not init, tests throw IllegalStateException: Toolkit not initialized.
    Platform.startup throws IllegalStateException if the toolkit is already running in another test class
     */
    @BeforeAll
    static void initToolkit() throws InterruptedException {
        JavaFxThreadingUtil.initializeJavaFxToolkit();
    }

    private PreviewTabViewModel viewModelWith(PreviewPreferences previewPreferences) {
        return new PreviewTabViewModel(dialogService, previewPreferences, layoutFormatterPreferences,
                taskExecutor, stateManager, abbreviationRepository);
    }

    private MultipleSelectionModel<PreviewLayout> selectionModelWith(ObservableList<PreviewLayout> items) {
        return new ListView<>(items).getSelectionModel();
    }

    private PreviewTabViewModel setUpViewModel() {
        PreviewPreferences previewPreferences = new PreviewPreferences(
                List.of(), 0, List.of(), false, false, List.of(), false);
        PreviewTabViewModel viewModel = viewModelWith(previewPreferences);
        viewModel.setValues();
        return viewModel;
    }

    @Test
    void setValuesLoadsCustomizedStylesIntoCustomizedList() {
        PreviewPreferences previewPreferences = new PreviewPreferences(
                List.of(),
                0,
                // populate customizedListProperty
                List.of(
                        new CustomizedPreviewStyle(ID1, NAME1, TEXT1),
                        new CustomizedPreviewStyle(ID2, NAME2, TEXT2),
                        new CustomizedPreviewStyle(ID3, NAME3, TEXT3)),
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
        assertInstanceOf(TextBasedPreviewLayout.class, customizedListProperty.getValue().getFirst());
        TextBasedPreviewLayout customizedPreviewStyle = (TextBasedPreviewLayout) customizedListProperty.getValue().getFirst();
        assertEquals(ID1, customizedPreviewStyle.getId());
        customizedPreviewStyle = (TextBasedPreviewLayout) customizedListProperty.getValue().get(1);
        assertEquals(NAME2, customizedPreviewStyle.getName());
        customizedPreviewStyle = (TextBasedPreviewLayout) customizedListProperty.getValue().get(2);
        assertEquals(TEXT3, customizedPreviewStyle.getText());
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

        List<CustomizedPreviewStyle> stored = previewPreferences.getCustomizedPreviewStyles();
        assertEquals(2, stored.size());
        assertEquals("id1", stored.getFirst().id());
        assertEquals("name1", stored.getFirst().name());
        assertEquals("<b>text1</b>", stored.getFirst().text());
        assertEquals("id2", stored.get(1).id());
        assertEquals("name2", stored.get(1).name());
        assertEquals("<b>text2</b>", stored.get(1).text());
    }

    @Test
    void renameAndEditUnderSameId() {
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

        List<CustomizedPreviewStyle> stored = previewPreferences.getCustomizedPreviewStyles();
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
        PreviewTabViewModel viewModel = setUpViewModel();

        viewModel.storeSettings();

        assertEquals(1, viewModel.chosenListProperty().size());
        PreviewLayout defaultPreviewLayout = viewModel.chosenListProperty().getFirst();
        assertInstanceOf(TextBasedPreviewLayout.class, defaultPreviewLayout);
        assertEquals(TextBasedPreviewLayout.NAME, defaultPreviewLayout.getName());
    }

    @Test
    void addToChosenMovesStyleFromSourceListToChosenList() {
        // set up view model
        PreviewTabViewModel viewModel = setUpViewModel();

        // adds layout to CSL List
        CitationStyle citationStyle = new CitationStyle(TEST_FILEPATH, TEST_TITLE, TEST_SHORT_TITLE,
                false, false, false, TEST_SOURCE);
        CitationStylePreviewLayout cslLayout =
                new CitationStylePreviewLayout(citationStyle, bibEntryTypesManager);
        viewModel.cslListProperty().add(cslLayout);

        // simulate user 'selecting' CSL list
        MultipleSelectionModel<PreviewLayout> selectionModel = selectionModelWith(viewModel.cslListProperty());
        selectionModel.select(cslLayout);
        viewModel.availableSelectionModelProperty().setValue(selectionModel);

        // simulate moving layout to 'selected' chosen list
        viewModel.addToChosen(viewModel.cslListProperty());

        // movement validation
        assertFalse(viewModel.cslListProperty().contains(cslLayout));
        assertTrue(viewModel.chosenListProperty().contains(cslLayout));
        assertEquals(1, viewModel.chosenListProperty().size());

        // data validation
        PreviewLayout previewLayout = viewModel.chosenListProperty().getFirst();
        assertInstanceOf(CitationStylePreviewLayout.class, previewLayout);
        CitationStylePreviewLayout citationStylePreviewLayout = (CitationStylePreviewLayout) previewLayout;
        assertEquals(TEST_FILEPATH, citationStylePreviewLayout.citationStyle().getFilePath());
        assertEquals(TEST_TITLE, citationStylePreviewLayout.citationStyle().getTitle());
        assertEquals(TEST_SHORT_TITLE, citationStylePreviewLayout.citationStyle().getShortTitle());
        assertEquals(TEST_SOURCE, citationStylePreviewLayout.citationStyle().getSource());
    }

    @Test
    void removeFromChosenMoveTextBasedLayoutToCustomizedList() {
        // set up view model
        PreviewTabViewModel viewModel = setUpViewModel();

        // adds layout to chosen List
        TextBasedPreviewLayout textBasedPreviewLayout = TextBasedPreviewLayout.of(ID1, NAME1, TEXT1,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.chosenListProperty().add(textBasedPreviewLayout);

        // adds layout to CSL List
        CitationStyle citationStyle = new CitationStyle(TEST_FILEPATH, TEST_TITLE, TEST_SHORT_TITLE,
                false, false, false, TEST_SOURCE);
        CitationStylePreviewLayout cslLayout =
                new CitationStylePreviewLayout(citationStyle, bibEntryTypesManager);
        viewModel.chosenListProperty().add(cslLayout);

        // simulate user 'selecting' from Chosen list
        MultipleSelectionModel<PreviewLayout> selectionModel = selectionModelWith(viewModel.chosenListProperty());
        selectionModel.select(textBasedPreviewLayout);
        viewModel.chosenSelectionModelProperty().setValue(selectionModel);

        // simulate moving layout from 'selected' chosen list to csl/customized list
        viewModel.removeFromChosen();

        // simulate user 'selecting' CSL list
        selectionModel = selectionModelWith(viewModel.chosenListProperty());
        selectionModel.select(cslLayout);
        viewModel.chosenSelectionModelProperty().setValue(selectionModel);

        // simulate moving layout from 'selected' chosen list to csl/customized list
        viewModel.removeFromChosen();

        assertFalse(viewModel.chosenListProperty().contains(textBasedPreviewLayout));
        assertFalse(viewModel.chosenListProperty().contains(cslLayout));
        assertTrue(viewModel.customizedListProperty().contains(textBasedPreviewLayout));
        assertTrue(viewModel.cslListProperty().contains(cslLayout));
    }

    @Test
    void dragDroppedMovesLayoutFromCslToChosen() {
        // set up view model
        PreviewTabViewModel viewModel = setUpViewModel();

        // adds layout to CSL List
        CitationStyle citationStyle = new CitationStyle(TEST_FILEPATH, TEST_TITLE, TEST_SHORT_TITLE,
                false, false, false, TEST_SOURCE);
        CitationStylePreviewLayout cslLayout =
                new CitationStylePreviewLayout(citationStyle, bibEntryTypesManager);
        viewModel.cslListProperty().add(cslLayout);

        // simulate user 'selecting' Csl list
        MultipleSelectionModel<PreviewLayout> selectionModel = selectionModelWith(viewModel.cslListProperty());
        selectionModel.select(cslLayout);
        viewModel.availableSelectionModelProperty().setValue(selectionModel);

        // simulate drag and drop layout from csl list to 'selected' chosen list
        Dragboard dragboard = Mockito.mock(Dragboard.class);
        when(dragboard.hasContent(DragAndDropDataFormats.PREVIEWLAYOUTS)).thenReturn(true);
        viewModel.dragDetected(viewModel.cslListProperty(), viewModel.availableSelectionModelProperty(), List.of(cslLayout), dragboard);
        boolean success = viewModel.dragDropped(viewModel.chosenListProperty(), dragboard);

        assertTrue(success);
        assertFalse(viewModel.cslListProperty().contains(cslLayout));
        assertTrue(viewModel.chosenListProperty().contains(cslLayout));
    }

    @Test
    void dragDroppedIntoSameCustomizedListIsIgnored() {
        // set up view model
        PreviewTabViewModel viewModel = setUpViewModel();

        // adds layout to customized List
        TextBasedPreviewLayout textBasedPreviewLayout = TextBasedPreviewLayout.of(ID1, NAME1, TEXT1,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.customizedListProperty().add(textBasedPreviewLayout);

        // simulate user 'selecting' from Customized list
        MultipleSelectionModel<PreviewLayout> selectionModel = selectionModelWith(viewModel.customizedListProperty());
        selectionModel.select(textBasedPreviewLayout);
        viewModel.availableSelectionModelProperty().setValue(selectionModel);

        // simulate drag & drop layout from Customized list to itself. Prevents redundant action of add/drag to same list
        Dragboard dragboard = Mockito.mock(Dragboard.class);
        when(dragboard.hasContent(DragAndDropDataFormats.PREVIEWLAYOUTS)).thenReturn(true);
        viewModel.dragDetected(viewModel.customizedListProperty(), viewModel.availableSelectionModelProperty(), List.of(textBasedPreviewLayout), dragboard);
        boolean success = viewModel.dragDropped(viewModel.customizedListProperty(), dragboard);

        assertFalse(success);
        assertTrue(viewModel.customizedListProperty().contains(textBasedPreviewLayout));
        assertEquals(1, viewModel.customizedListProperty().size());
    }

    @Test
    void dragDroppedIntoSameCSLListIsIgnored() {
        // set up view model
        PreviewTabViewModel viewModel = setUpViewModel();

        // adds layout to CSL List
        CitationStyle citationStyle = new CitationStyle(TEST_FILEPATH, TEST_TITLE, TEST_SHORT_TITLE,
                false, false, false, TEST_SOURCE);
        CitationStylePreviewLayout cslLayout =
                new CitationStylePreviewLayout(citationStyle, bibEntryTypesManager);
        viewModel.cslListProperty().add(cslLayout);

        // simulate user 'selecting' CSL list
        MultipleSelectionModel<PreviewLayout> selectionModel = selectionModelWith(viewModel.cslListProperty());
        selectionModel.select(cslLayout);
        viewModel.availableSelectionModelProperty().setValue(selectionModel);

        // simulate drag & drop layout from csl list to itself. Prevents redundant action of add/drag to same list
        Dragboard dragboard = Mockito.mock(Dragboard.class);
        when(dragboard.hasContent(DragAndDropDataFormats.PREVIEWLAYOUTS)).thenReturn(true);
        viewModel.dragDetected(viewModel.cslListProperty(), viewModel.availableSelectionModelProperty(), List.of(cslLayout), dragboard);
        boolean success = viewModel.dragDropped(viewModel.cslListProperty(), dragboard);

        assertFalse(success);
        assertTrue(viewModel.cslListProperty().contains(cslLayout));
        assertEquals(1, viewModel.cslListProperty().size());
    }

    @Test
    void resetDefaultLayoutRestoresDefaultTemplateText() {
        PreviewTabViewModel viewModel = setUpViewModel();

        TextBasedPreviewLayout textBasedPreviewLayout = TextBasedPreviewLayout.of(ID1, NAME1, TEXT1,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.setPreviewLayout(textBasedPreviewLayout);
        viewModel.resetDefaultLayout();     // resets the given layout to default

        assertEquals(TextBasedPreviewLayout.DEFAULT.replace("__NEWLINE__", "\n"), textBasedPreviewLayout.getText());
        assertEquals(viewModel.selectedLayoutProperty().getValue(), textBasedPreviewLayout);
    }

    @Test
    void addCustomizedStyleCreatesAndSelectsNewDefaultStyle() {
        PreviewTabViewModel viewModel = setUpViewModel();
        assertEquals(0, viewModel.customizedListProperty().size());

        viewModel.addCustomizedStyle(); // Adds a newly generated custom style to customized list

        assertEquals(1, viewModel.customizedListProperty().size());
        PreviewLayout layout = viewModel.customizedListProperty().getLast();
        assertInstanceOf(TextBasedPreviewLayout.class, layout);
        assertEquals(TextBasedPreviewLayout.DEFAULT.replace("__NEWLINE__", "\n"), layout.getText());
        assertEquals(layout, viewModel.selectedLayoutProperty().getValue());
    }

    @Test
    void removeCustomizedStyleRemovesSelectedTextBasedStyle() {
        PreviewTabViewModel viewModel = setUpViewModel();

        // add new custom layout
        TextBasedPreviewLayout textBasedPreviewLayout = TextBasedPreviewLayout.of(ID1, NAME1, TEXT1,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.customizedListProperty().add(textBasedPreviewLayout);

        // select newly added layout
        MultipleSelectionModel<PreviewLayout> selectionModel = selectionModelWith(viewModel.customizedListProperty());
        selectionModel.select(textBasedPreviewLayout);
        viewModel.availableSelectionModelProperty().setValue(selectionModel);
        assertEquals(1, viewModel.customizedListProperty().size());

        viewModel.removeCustomizedStyle();

        assertEquals(0, viewModel.customizedListProperty().size());
        assertFalse(viewModel.customizedListProperty().contains(textBasedPreviewLayout));
    }

    @Test
    void removeCustomizedStyleIgnoresNonTextBasedSelection() {
        PreviewTabViewModel viewModel = setUpViewModel();

        // adds layout to CSL List
        CitationStyle citationStyle = new CitationStyle(TEST_FILEPATH, TEST_TITLE, TEST_SHORT_TITLE,
                false, false, false, TEST_SOURCE);
        CitationStylePreviewLayout cslLayout =
                new CitationStylePreviewLayout(citationStyle, bibEntryTypesManager);
        viewModel.cslListProperty().add(cslLayout);
        assertTrue(viewModel.cslListProperty().contains(cslLayout));

        // select csl layout
        MultipleSelectionModel<PreviewLayout> selectionModel = selectionModelWith(viewModel.cslListProperty());
        selectionModel.select(cslLayout);
        viewModel.availableSelectionModelProperty().setValue(selectionModel);

        viewModel.removeCustomizedStyle();  // not removed, style is not a custom layout (TextBasedPreviewLayout)

        assertTrue(viewModel.cslListProperty().contains(cslLayout));
        assertEquals(viewModel.availableSelectionModelProperty().getValue().getSelectedItem(), cslLayout);
    }

    @Test
    void renameSelectedStyleSuccess() {
        PreviewTabViewModel viewModel = setUpViewModel();

        // add new custom layout
        TextBasedPreviewLayout textBasedPreviewLayout = TextBasedPreviewLayout.of(ID1, NAME1, TEXT1,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.customizedListProperty().add(textBasedPreviewLayout);
        viewModel.setPreviewLayout(textBasedPreviewLayout);

        viewModel.renameSelectedStyle(NAME2);

        assertEquals(ID1, textBasedPreviewLayout.getId());
        assertEquals(NAME2, textBasedPreviewLayout.getDisplayName());
        assertEquals(TEXT1, textBasedPreviewLayout.getText());
    }

    @Test
    void renameSelectedStyleFailsOnDuplicateNameInCustomizedList() {
        PreviewTabViewModel viewModel = setUpViewModel();

        // add new custom layout
        TextBasedPreviewLayout existing = TextBasedPreviewLayout.of(ID1, NAME1, TEXT1,
                layoutFormatterPreferences, abbreviationRepository);
        TextBasedPreviewLayout textBasedPreviewLayout = TextBasedPreviewLayout.of(ID2, NAME2, TEXT2,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.customizedListProperty().addAll(existing, textBasedPreviewLayout);
        viewModel.setPreviewLayout(textBasedPreviewLayout);

        viewModel.renameSelectedStyle(NAME1);   // can't rename to existing name

        assertEquals(ID2, textBasedPreviewLayout.getId());
        assertEquals(NAME2, textBasedPreviewLayout.getDisplayName());
        assertEquals(TEXT2, textBasedPreviewLayout.getText());
    }

    @Test
    void renameSelectedStyleFailsOnDuplicateNameInChosenList() {
        PreviewTabViewModel viewModel = setUpViewModel();

        TextBasedPreviewLayout chosenLayout = TextBasedPreviewLayout.of(ID1, NAME1, TEXT1,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.chosenListProperty().add(chosenLayout);

        TextBasedPreviewLayout layoutBeingRenamed = TextBasedPreviewLayout.of(ID2, NAME2, TEXT2,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.customizedListProperty().add(layoutBeingRenamed);
        viewModel.setPreviewLayout(layoutBeingRenamed);

        viewModel.renameSelectedStyle(NAME1);

        assertEquals(ID2, layoutBeingRenamed.getId());
        assertEquals(NAME2, layoutBeingRenamed.getDisplayName());
        assertEquals(TEXT2, layoutBeingRenamed.getText());
    }

    @Test
    void renameSelectedStyleFailsOnDuplicateNameInCslList() {
        PreviewTabViewModel viewModel = setUpViewModel();

        // adds layout to CSL List
        CitationStyle citationStyle = new CitationStyle(TEST_FILEPATH, TEST_TITLE, TEST_SHORT_TITLE,
                false, false, false, TEST_SOURCE);
        CitationStylePreviewLayout cslLayout =
                new CitationStylePreviewLayout(citationStyle, bibEntryTypesManager);
        viewModel.cslListProperty().add(cslLayout);
        assertTrue(viewModel.cslListProperty().contains(cslLayout));

        TextBasedPreviewLayout layoutBeingRenamed = TextBasedPreviewLayout.of(ID2, NAME2, TEXT2,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.customizedListProperty().add(layoutBeingRenamed);
        viewModel.setPreviewLayout(layoutBeingRenamed);

        viewModel.renameSelectedStyle(TEST_TITLE);

        assertEquals(ID2, layoutBeingRenamed.getId());
        assertEquals(NAME2, layoutBeingRenamed.getDisplayName());
        assertEquals(TEXT2, layoutBeingRenamed.getText());
    }

    @Test
    void renameSelectedStyleFailsOnBlankLayoutNameAndTextFiledUnchanged() {
        PreviewTabViewModel viewModel = setUpViewModel();

        TextBasedPreviewLayout textBasedPreviewLayout = TextBasedPreviewLayout.of(ID1, NAME1, TEXT1,
                layoutFormatterPreferences, abbreviationRepository);
        viewModel.customizedListProperty().add(textBasedPreviewLayout);
        viewModel.setPreviewLayout(textBasedPreviewLayout);
        viewModel.styleNameProperty().set(NAME1);

        viewModel.renameSelectedStyle("   ");
        assertEquals(NAME1, textBasedPreviewLayout.getDisplayName());
        viewModel.renameSelectedStyle("");
        assertEquals(NAME1, viewModel.styleNameProperty().getValue());
        assertEquals(ID1, textBasedPreviewLayout.getId());
        assertEquals(NAME1, textBasedPreviewLayout.getDisplayName());
        assertEquals(TEXT1, textBasedPreviewLayout.getText());
    }
}
