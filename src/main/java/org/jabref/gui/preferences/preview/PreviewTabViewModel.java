package org.jabref.gui.preferences.preview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PreviewPreferences;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is Preferences -> Entry Preview tab model
 * <p>
 *     {@link PreviewTab} is the controller of Entry Preview tab
 * </p>
 * @see PreviewTab
 * */
public class PreviewTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewTabViewModel.class);

    private final ListProperty<PreviewLayout> availableListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<MultipleSelectionModel<PreviewLayout>> availableSelectionModelProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());
    private final ListProperty<PreviewLayout> chosenListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<MultipleSelectionModel<PreviewLayout>> chosenSelectionModelProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());
    private final BooleanProperty showAsExtraTab = new SimpleBooleanProperty(false);
    private final BooleanProperty selectedIsEditableProperty = new SimpleBooleanProperty(false);
    private final ObjectProperty<PreviewLayout> layoutProperty = new SimpleObjectProperty<>();
    /*
    * A local variable to store the preview text in \n format instead of _NEWLINE_
    * */
    private final StringProperty sourceTextProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final PreviewPreferences initialPreviewPreferences;
    private final TaskExecutor taskExecutor;

    private final Validator chosenListValidator;

    private final CustomLocalDragboard localDragboard;
    private ListProperty<PreviewLayout> dragSourceList = null;
    private ObjectProperty<MultipleSelectionModel<PreviewLayout>> dragSourceSelectionModel = null;
    private final List<String> restartWarning = new ArrayList<>();

    private final FilteredList<PreviewLayout> filteredPreviews = new FilteredList<>(this.availableListProperty());

    public PreviewTabViewModel(DialogService dialogService, PreferencesService preferences, TaskExecutor taskExecutor, StateManager stateManager) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.localDragboard = stateManager.getLocalDragboard();
        initialPreviewPreferences = preferences.getPreviewPreferences();

        chosenListValidator = new FunctionBasedValidator<>(
                chosenListProperty,
                input -> !chosenListProperty.getValue().isEmpty(),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("Entry preview"),
                        Localization.lang("Selected"),
                        Localization.lang("Selected Layouts can not be empty")
                        )
                )
        );

    }

    public BooleanProperty showAsExtraTabProperty() {
        return showAsExtraTab;
    }

    @Override
    public void setValues() {
        showAsExtraTab.set(initialPreviewPreferences.showPreviewAsExtraTab());
        chosenListProperty().getValue().clear();
        chosenListProperty.getValue().addAll(initialPreviewPreferences.getPreviewCycle());

        availableListProperty.clear();
        if (chosenListProperty.stream().noneMatch(layout -> layout instanceof TextBasedPreviewLayout)) {
            availableListProperty.getValue().add(initialPreviewPreferences.getTextBasedPreviewLayout());
        }

        BackgroundTask.wrap(CitationStyle::discoverCitationStyles)
                      .onSuccess(styles -> styles.stream()
                                                 .map(CitationStylePreviewLayout::new)
                                                 .filter(style -> chosenListProperty.getValue().filtered(item ->
                                                         item.getName().equals(style.getName())).isEmpty())
                                                 .sorted(Comparator.comparing(PreviewLayout::getName))
                                                 .forEach(availableListProperty::add))
                      .onFailure(ex -> {
                          LOGGER.error("Something went wrong while adding the discovered CitationStyles to the list. ", ex);
                          dialogService.showErrorDialogAndWait(Localization.lang("Error adding discovered CitationStyles"), ex);
                      })
                      .executeWith(taskExecutor);
    }

    public void setPreviewLayout(PreviewLayout selectedLayout) {
        if (selectedLayout == null) {
            selectedIsEditableProperty.setValue(false);
            return;
        }

        try {
            layoutProperty.setValue(selectedLayout);
        } catch (StringIndexOutOfBoundsException exception) {
            LOGGER.warn("Parsing error.", exception);
            dialogService.showErrorDialogAndWait(Localization.lang("Parsing error"), Localization.lang("Parsing error") + ": " + Localization.lang("illegal backslash expression"), exception);
        }

        if (selectedLayout instanceof TextBasedPreviewLayout) {
            sourceTextProperty.setValue(((TextBasedPreviewLayout) selectedLayout).getText().replace("__NEWLINE__", "\n"));
            selectedIsEditableProperty.setValue(true);
        } else {
            sourceTextProperty.setValue(((CitationStylePreviewLayout) selectedLayout).getSource());
            selectedIsEditableProperty.setValue(false);
        }
    }

    public void refreshPreview() {
        setPreviewLayout(chosenSelectionModelProperty.getValue().getSelectedItem());
    }

    private PreviewLayout findLayoutByName(String name) {
        return availableListProperty.getValue().stream().filter(layout -> layout.getName().equals(name))
                                    .findAny()
                                    .orElse(chosenListProperty.getValue().stream().filter(layout -> layout.getName().equals(name))
                                                              .findAny()
                                                              .orElse(null));
    }

    /**
     * Getter method for warning list.
     * @return list of string with warning information.
     */
    @Override
    public List<String> getRestartWarnings() {
        return restartWarning;
    }

    /**
     * Store the changes of preference-preview settings.
     */
    @Override
    public void storeSettings() {
        PreviewPreferences previewPreferences = preferences.getPreviewPreferences();

        if (chosenListProperty.isEmpty()) {
            chosenListProperty.add(previewPreferences.getTextBasedPreviewLayout());
        }

        PreviewLayout previewStyle = findLayoutByName(TextBasedPreviewLayout.NAME);
        if (previewStyle == null) {
            previewStyle = previewPreferences.getTextBasedPreviewLayout();
        }
        if (showAsExtraTab.getValue() != initialPreviewPreferences.showPreviewAsExtraTab()) {
            restartWarning.add(Localization.lang("Show preview as a tab in entry editor"));
        }

        PreviewPreferences newPreviewPreferences = preferences.getPreviewPreferences()
                                                              .getBuilder()
                                                              .withPreviewCycle(chosenListProperty)
                                                              .withShowAsExtraTab(showAsExtraTab.getValue())
                                                              .withPreviewStyle(((TextBasedPreviewLayout) previewStyle).getText())
                                                              .build();

        if (!chosenSelectionModelProperty.getValue().getSelectedItems().isEmpty()) {
            newPreviewPreferences = newPreviewPreferences.getBuilder().withPreviewCyclePosition(
                    chosenListProperty.getValue().indexOf(
                            chosenSelectionModelProperty.getValue().getSelectedItems().get(0))).build();
        }

        preferences.storePreviewPreferences(newPreviewPreferences);

        for (LibraryTab libraryTab : JabRefGUI.getMainFrame().getLibraryTabs()) {
            // TODO: Find a better way to update preview
            libraryTab.closeBottomPane();
            // basePanel.getPreviewPanel().updateLayout(preferences.getPreviewPreferences());
        }
    }

    public ValidationStatus chosenListValidationStatus() {
        return chosenListValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        ValidationStatus validationStatus = chosenListValidationStatus();
        if (!validationStatus.isValid()) {
            if (validationStatus.getHighestMessage().isPresent()) {
                validationStatus.getHighestMessage().ifPresent(message ->
                        dialogService.showErrorDialogAndWait(message.getMessage()));
            }
            return false;
        }
        return true;
    }

    public void addToChosen() {
        List<PreviewLayout> selected = new ArrayList<>(availableSelectionModelProperty.getValue().getSelectedItems());
        availableSelectionModelProperty.getValue().clearSelection();
        availableListProperty.removeAll(selected);
        chosenListProperty.addAll(selected);
    }

    public void removeFromChosen() {
        List<PreviewLayout> selected = new ArrayList<>(chosenSelectionModelProperty.getValue().getSelectedItems());
        chosenSelectionModelProperty.getValue().clearSelection();
        chosenListProperty.removeAll(selected);
        availableListProperty.addAll(selected);
        availableListProperty.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
    }

    public void selectedInChosenUp() {
        if (chosenSelectionModelProperty.getValue().isEmpty()) {
            return;
        }

        List<Integer> selected = new ArrayList<>(chosenSelectionModelProperty.getValue().getSelectedIndices());
        List<Integer> newIndices = new ArrayList<>();
        chosenSelectionModelProperty.getValue().clearSelection();

        for (int oldIndex : selected) {
            boolean alreadyTaken = newIndices.contains(oldIndex - 1);
            int newIndex = ((oldIndex > 0) && !alreadyTaken) ? oldIndex - 1 : oldIndex;
            chosenListProperty.add(newIndex, chosenListProperty.remove(oldIndex));
            newIndices.add(newIndex);
        }

        newIndices.forEach(index -> chosenSelectionModelProperty.getValue().select(index));
        chosenSelectionModelProperty.getValue().select(newIndices.get(0));
        refreshPreview();
    }

    public void selectedInChosenDown() {
        if (chosenSelectionModelProperty.getValue().isEmpty()) {
            return;
        }

        List<Integer> selected = new ArrayList<>(chosenSelectionModelProperty.getValue().getSelectedIndices());
        List<Integer> newIndices = new ArrayList<>();
        chosenSelectionModelProperty.getValue().clearSelection();

        for (int i = selected.size() - 1; i >= 0; i--) {
            int oldIndex = selected.get(i);
            boolean alreadyTaken = newIndices.contains(oldIndex + 1);
            int newIndex = ((oldIndex < (chosenListProperty.size() - 1)) && !alreadyTaken) ? oldIndex + 1 : oldIndex;
            chosenListProperty.add(newIndex, chosenListProperty.remove(oldIndex));
            newIndices.add(newIndex);
        }

        newIndices.forEach(index -> chosenSelectionModelProperty.getValue().select(index));
        chosenSelectionModelProperty.getValue().select(newIndices.get(0));
        refreshPreview();
    }

    public void resetDefaultLayout() {
        PreviewLayout defaultLayout = findLayoutByName("Preview");
        if (defaultLayout instanceof TextBasedPreviewLayout) {
            ((TextBasedPreviewLayout) defaultLayout).setText(preferences.getPreviewPreferences().getDefaultPreviewStyle());
        }
        refreshPreview();
    }

    /**
     * XML-Syntax-Highlighting for RichTextFX-Codearea created by (c) Carlos Martins (github:
     * <a href="https://github.com/cmartins">@cemartins</a>)
     * <p>
     * License: <a href="https://github.com/FXMisc/RichTextFX/blob/master/LICENSE">BSD-2-Clause</a>
     * <p>
     * See also
     * <a href="https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/README.md#xml-editor">https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/README.md#xml-editor</a>
     *
     * @param text to parse and highlight
     * @return highlighted span for codeArea
     */
    public StyleSpans<Collection<String>> computeHighlighting(String text) {

        final Pattern XML_TAG = Pattern.compile("(?<ELEMENT>(</?\\h*)(\\w+)([^<>]*)(\\h*/?>))"
                + "|(?<COMMENT><!--[^<>]+-->)");
        final Pattern ATTRIBUTES = Pattern.compile("(\\w+\\h*)(=)(\\h*\"[^\"]+\")");

        final int GROUP_OPEN_BRACKET = 2;
        final int GROUP_ELEMENT_NAME = 3;
        final int GROUP_ATTRIBUTES_SECTION = 4;
        final int GROUP_CLOSE_BRACKET = 5;
        final int GROUP_ATTRIBUTE_NAME = 1;
        final int GROUP_EQUAL_SYMBOL = 2;
        final int GROUP_ATTRIBUTE_VALUE = 3;

        Matcher matcher = XML_TAG.matcher(text);
        int lastKeywordEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKeywordEnd);
            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
            } else {
                if (matcher.group("ELEMENT") != null) {
                    String attributesText = matcher.group(GROUP_ATTRIBUTES_SECTION);

                    spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_OPEN_BRACKET) - matcher.start(GROUP_OPEN_BRACKET));
                    spansBuilder.add(Collections.singleton("anytag"), matcher.end(GROUP_ELEMENT_NAME) - matcher.end(GROUP_OPEN_BRACKET));

                    if (!attributesText.isEmpty()) {

                        lastKeywordEnd = 0;

                        Matcher attributesMatcher = ATTRIBUTES.matcher(attributesText);
                        while (attributesMatcher.find()) {
                            spansBuilder.add(Collections.emptyList(), attributesMatcher.start() - lastKeywordEnd);
                            spansBuilder.add(Collections.singleton("attribute"), attributesMatcher.end(GROUP_ATTRIBUTE_NAME) - attributesMatcher.start(GROUP_ATTRIBUTE_NAME));
                            spansBuilder.add(Collections.singleton("tagmark"), attributesMatcher.end(GROUP_EQUAL_SYMBOL) - attributesMatcher.end(GROUP_ATTRIBUTE_NAME));
                            spansBuilder.add(Collections.singleton("avalue"), attributesMatcher.end(GROUP_ATTRIBUTE_VALUE) - attributesMatcher.end(GROUP_EQUAL_SYMBOL));
                            lastKeywordEnd = attributesMatcher.end();
                        }
                        if (attributesText.length() > lastKeywordEnd) {
                            spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKeywordEnd);
                        }
                    }

                    lastKeywordEnd = matcher.end(GROUP_ATTRIBUTES_SECTION);

                    spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_CLOSE_BRACKET) - lastKeywordEnd);
                }
            }
            lastKeywordEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKeywordEnd);
        return spansBuilder.create();
    }

    public void dragOver(DragEvent event) {
        if (event.getDragboard().hasContent(DragAndDropDataFormats.PREVIEWLAYOUTS)) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
    }

    public void dragDetected(ListProperty<PreviewLayout> sourceList, ObjectProperty<MultipleSelectionModel<PreviewLayout>> sourceSelectionModel, List<PreviewLayout> selectedLayouts, Dragboard dragboard) {
        ClipboardContent content = new ClipboardContent();
        content.put(DragAndDropDataFormats.PREVIEWLAYOUTS, "");
        dragboard.setContent(content);
        localDragboard.putPreviewLayouts(selectedLayouts);
        dragSourceList = sourceList;
        dragSourceSelectionModel = sourceSelectionModel;
    }

    /**
     * This is called, when the user drops some PreviewLayouts either in the availableListView or in the empty space of chosenListView
     *
     * @param targetList either availableListView or chosenListView
     */

    public boolean dragDropped(ListProperty<PreviewLayout> targetList, Dragboard dragboard) {
        boolean success = false;

        if (dragboard.hasContent(DragAndDropDataFormats.PREVIEWLAYOUTS)) {
            List<PreviewLayout> draggedLayouts = localDragboard.getPreviewLayouts();
            if (!draggedLayouts.isEmpty()) {
                dragSourceSelectionModel.getValue().clearSelection();
                dragSourceList.getValue().removeAll(draggedLayouts);
                targetList.getValue().addAll(draggedLayouts);
                success = true;

                if (targetList == availableListProperty) {
                    targetList.getValue().sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
                }
            }
        }

        return success;
    }

    /**
     * This is called, when the user drops some PreviewLayouts on another cell in chosenListView to sort them
     *
     * @param targetLayout the Layout, the user drops a layout on
     */

    public boolean dragDroppedInChosenCell(PreviewLayout targetLayout, Dragboard dragboard) {
        boolean success = false;

        if (dragboard.hasContent(DragAndDropDataFormats.PREVIEWLAYOUTS)) {
            List<PreviewLayout> draggedSelectedLayouts = new ArrayList<>(localDragboard.getPreviewLayouts());
            if (!draggedSelectedLayouts.isEmpty()) {
                chosenSelectionModelProperty.getValue().clearSelection();
                int targetId = chosenListProperty.getValue().indexOf(targetLayout);

                // see https://stackoverflow.com/questions/28603224/sort-tableview-with-drag-and-drop-rows
                int onSelectedDelta = 0;
                while (draggedSelectedLayouts.contains(targetLayout)) {
                    onSelectedDelta = 1;
                    targetId--;
                    if (targetId < 0) {
                        targetId = 0;
                        targetLayout = null;
                        break;
                    }
                    targetLayout = chosenListProperty.getValue().get(targetId);
                }
                dragSourceSelectionModel.getValue().clearSelection();
                dragSourceList.getValue().removeAll(draggedSelectedLayouts);

                if (targetLayout != null) {
                    targetId = chosenListProperty.getValue().indexOf(targetLayout) + onSelectedDelta;
                } else if (targetId != 0) {
                    targetId = chosenListProperty.getValue().size();
                }

                chosenListProperty.getValue().addAll(targetId, draggedSelectedLayouts);

                draggedSelectedLayouts.forEach(layout -> chosenSelectionModelProperty.getValue().select(layout));

                success = true;
            }
        }

        return success;
    }

    public FilteredList<PreviewLayout> getFilteredPreviews() {
        return this.filteredPreviews;
    }

    public ListProperty<PreviewLayout> availableListProperty() {
        return availableListProperty;
    }

    public ObjectProperty<MultipleSelectionModel<PreviewLayout>> availableSelectionModelProperty() {
        return availableSelectionModelProperty;
    }

    public ListProperty<PreviewLayout> chosenListProperty() {
        return chosenListProperty;
    }

    public ObjectProperty<MultipleSelectionModel<PreviewLayout>> chosenSelectionModelProperty() {
        return chosenSelectionModelProperty;
    }

    public BooleanProperty selectedIsEditableProperty() {
        return selectedIsEditableProperty;
    }

    public ObjectProperty<PreviewLayout> layoutProperty() {
        return layoutProperty;
    }

    public StringProperty sourceTextProperty() {
        return sourceTextProperty;
    }

    public void setFilterPredicate(String searchTerm) {
        this.filteredPreviews.setPredicate(preview -> searchTerm.isEmpty() || preview.containsCaseIndependent(searchTerm));
    }
}
