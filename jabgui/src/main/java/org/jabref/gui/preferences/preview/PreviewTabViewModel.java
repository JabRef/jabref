package org.jabref.gui.preferences.preview;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
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
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.preview.BstPreviewLayout;
import org.jabref.logic.preview.CitationStylePreviewLayout;
import org.jabref.logic.preview.CustomizedPreviewStyle;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// This class is Preferences -> Entry Preview tab model
///
/// [PreviewTab] is the controller of Entry Preview tab
///
/// @see PreviewTab
public class PreviewTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewTabViewModel.class);
    private static final Pattern XML_TAG_PATTERN = Pattern.compile("(?<ELEMENT>(</?\\h*)(\\w+)([^<>]*)(\\h*/?>))"
            + "|(?<COMMENT><!--[^<>]+-->)");
    private static final Pattern XML_ATTRIBUTES_PATTERN = Pattern.compile("(\\w+\\h*)(=)(\\h*\"[^\"]+\")");

    private final BooleanProperty showAsExtraTabProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty showPreviewInEntryTableTooltip = new SimpleBooleanProperty(false);

    private final BooleanProperty shouldDownloadCovers = new SimpleBooleanProperty();

    private final ObjectProperty<PreviewLayout> lastRoutedLayoutProperty = new SimpleObjectProperty<>();
    private final ListProperty<PreviewLayout> cslListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<PreviewLayout> customizedListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<MultipleSelectionModel<PreviewLayout>> availableSelectionModelProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());
    private final FilteredList<PreviewLayout> filteredCslLayouts = new FilteredList<>(this.cslListProperty());
    private final FilteredList<PreviewLayout> filteredCustomizedLayouts = new FilteredList<>(this.customizedListProperty());
    private final ListProperty<PreviewLayout> chosenListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<MultipleSelectionModel<PreviewLayout>> chosenSelectionModelProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());

    private final ListProperty<Path> bstStylesPaths = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final BooleanProperty selectedIsEditableProperty = new SimpleBooleanProperty(false);
    private final ObjectProperty<PreviewLayout> selectedLayoutProperty = new SimpleObjectProperty<>();
    private final StringProperty sourceTextProperty = new SimpleStringProperty("");
    private final StringProperty styleNameProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final PreviewPreferences previewPreferences;
    private final LayoutFormatterPreferences layoutFormatterPreferences;
    private final TaskExecutor taskExecutor;

    private final Validator chosenListValidator;

    private final CustomLocalDragboard localDragboard;
    private ListProperty<PreviewLayout> dragSourceList = null;
    private ObjectProperty<MultipleSelectionModel<PreviewLayout>> dragSourceSelectionModel = null;

    public PreviewTabViewModel(DialogService dialogService,
                               PreviewPreferences previewPreferences,
                               LayoutFormatterPreferences layoutFormatterPreferences,
                               TaskExecutor taskExecutor,
                               StateManager stateManager,
                               JournalAbbreviationRepository abbreviationRepository) {
        this.dialogService = dialogService;
        this.previewPreferences = previewPreferences;
        this.layoutFormatterPreferences = layoutFormatterPreferences;
        this.taskExecutor = taskExecutor;
        this.localDragboard = stateManager.getLocalDragboard();
        this.abbreviationRepository = abbreviationRepository;

        sourceTextProperty.addListener((_, _, _) -> {
            if (selectedLayoutProperty.getValue() instanceof TextBasedPreviewLayout layout) {
                layout.setText(sourceTextProperty.getValue());
            }
        });

        chosenListValidator = new FunctionBasedValidator<>(
                chosenListProperty,
                _ -> !chosenListProperty.getValue().isEmpty(),
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                                Localization.lang("Entry preview"),
                                Localization.lang("Selected"),
                                Localization.lang("Selected Layouts can not be empty")
                        )
                )
        );
    }

    @Override
    public void setValues() {
        showAsExtraTabProperty.set(previewPreferences.shouldShowPreviewAsExtraTab());
        showPreviewInEntryTableTooltip.set(previewPreferences.shouldShowPreviewEntryTableTooltip());
        chosenListProperty().getValue().clear();
        chosenListProperty.getValue().addAll(previewPreferences.getLayoutCycle());

        cslListProperty.clear();
        customizedListProperty.clear();
        for (CustomizedPreviewStyle stored : previewPreferences.getCustomizedPreviewStyles()) {
            TextBasedPreviewLayout textBasedPreviewLayout = new TextBasedPreviewLayout(stored.id(), stored.name(), stored.text(), layoutFormatterPreferences, abbreviationRepository);
            if (chosenListProperty.stream().noneMatch(currLayout -> currLayout instanceof TextBasedPreviewLayout textLayout && textLayout.getId().equals(stored.id()))) {
                customizedListProperty.getValue().add(textBasedPreviewLayout);
            }
        }

        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);

        BackgroundTask.wrap(CSLStyleLoader::getStyles)
                      .onSuccess(styles -> styles.stream()
                                                 .map(style -> new CitationStylePreviewLayout(style, entryTypesManager))
                                                 .filter(style -> chosenListProperty.getValue().filtered(item ->
                                                         item.getName().equals(style.getName())).isEmpty())
                                                 .sorted(Comparator.comparing(PreviewLayout::getName))
                                                 .forEach(style -> {
                                                     cslListProperty.add(style);
                                                 }))
                      .onFailure(ex -> {
                          LOGGER.error("Something went wrong while adding the discovered CitationStyles to the list.", ex);
                          dialogService.showErrorDialogAndWait(Localization.lang("Error adding discovered CitationStyles"), ex);
                      })
                      .executeWith(taskExecutor);
        bstStylesPaths.clear();
        bstStylesPaths.addAll(previewPreferences.getBstPreviewLayoutPaths());
        bstStylesPaths.forEach(path -> {
            BstPreviewLayout layout = new BstPreviewLayout(path);
            cslListProperty.add(layout);
        });

        shouldDownloadCovers.setValue(previewPreferences.shouldDownloadCovers());
    }

    public void setPreviewLayout(PreviewLayout selectedLayout) {
        if (selectedLayout == null) {
            selectedIsEditableProperty.setValue(false);
            selectedLayoutProperty.setValue(null);
            styleNameProperty.setValue("");
            return;
        }

        try {
            selectedLayoutProperty.setValue(selectedLayout);
        } catch (StringIndexOutOfBoundsException exception) {
            LOGGER.warn("Parsing error.", exception);
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Parsing error"),
                    Localization.lang("Parsing error") + ": " + Localization.lang("illegal backslash expression"), exception);
        }

        styleNameProperty.setValue(selectedLayout.getDisplayName());
        boolean isEditingAllowed = selectedLayout instanceof TextBasedPreviewLayout;
        setContentForPreview(selectedLayout.getText(), isEditingAllowed);
    }

    private void setContentForPreview(String text, boolean editable) {
        sourceTextProperty.setValue(text);
        selectedIsEditableProperty.setValue(editable);
    }

    public void refreshPreview() {
        PreviewLayout current = selectedLayoutProperty.getValue();
        setPreviewLayout(null);
        setPreviewLayout(current);
    }

    /// Store the changes of preference-preview settings.
    @Override
    public void storeSettings() {
        if (chosenListProperty.isEmpty()) {
            PreviewLayout defaultLayout = customizedListProperty.stream()
                                                                .filter(TextBasedPreviewLayout.class::isInstance)
                                                                .findFirst()
                                                                .orElseGet(() -> TextBasedPreviewLayout.of(
                                                                        UUID.randomUUID().toString(),
                                                                        TextBasedPreviewLayout.NAME,
                                                                        TextBasedPreviewLayout.DEFAULT,
                                                                        layoutFormatterPreferences,
                                                                        abbreviationRepository));
            chosenListProperty.add(defaultLayout);
        }

        List<CustomizedPreviewStyle> toStore = java.util.stream.Stream.concat(
                                                           customizedListProperty.stream(),
                                                           chosenListProperty.stream().filter(TextBasedPreviewLayout.class::isInstance))
                                                                      .map(TextBasedPreviewLayout.class::cast)
                                                                      .distinct()
                                                                      .map(layout -> new CustomizedPreviewStyle(layout.getId(), layout.getDisplayName(), layout.getText()))
                                                                      .toList();
        previewPreferences.getCustomizedPreviewStyles().setAll(toStore);

        previewPreferences.getLayoutCycle().clear();
        previewPreferences.getLayoutCycle().addAll(chosenListProperty);
        previewPreferences.setShowPreviewAsExtraTab(showAsExtraTabProperty.getValue());
        previewPreferences.setShowPreviewEntryTableTooltip(showPreviewInEntryTableTooltip.getValue());
        previewPreferences.setBstPreviewLayoutPaths(bstStylesPaths);

        if (!chosenSelectionModelProperty.getValue().getSelectedItems().isEmpty()) {
            previewPreferences.setLayoutCyclePosition(chosenListProperty.getValue().indexOf(
                    chosenSelectionModelProperty.getValue().getSelectedItems().getFirst()));
        }

        previewPreferences.setShouldDownloadCovers(shouldDownloadCovers.getValue());
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

    public void addToChosen(ListProperty<PreviewLayout> sourceList) {
        // Adding style from 'available' to 'selected' must know where the source list is from
        List<PreviewLayout> selected = new ArrayList<>(availableSelectionModelProperty.getValue().getSelectedItems());
        availableSelectionModelProperty.getValue().clearSelection();
        sourceList.removeAll(selected);
        chosenListProperty.addAll(selected);
    }

    public void removeFromChosen() {
        List<PreviewLayout> selected = new ArrayList<>(chosenSelectionModelProperty.getValue().getSelectedItems());
        chosenSelectionModelProperty.getValue().clearSelection();
        chosenListProperty.removeAll(selected);
        for (PreviewLayout layout : selected) {
            ListProperty<PreviewLayout> destination = destinationFor(layout);
            if (!destination.getValue().contains(layout)) {
                destination.add(layout);
            }
            lastRoutedLayoutProperty.setValue(layout);
        }
        cslListProperty.getValue().sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
        customizedListProperty.getValue().sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
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
            int newIndex = (oldIndex > 0) && !alreadyTaken ? oldIndex - 1 : oldIndex;
            chosenListProperty.add(newIndex, chosenListProperty.remove(oldIndex));
            newIndices.add(newIndex);
        }

        newIndices.forEach(index -> chosenSelectionModelProperty.getValue().select(index));
        chosenSelectionModelProperty.getValue().select(newIndices.getFirst());
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
            int newIndex = (oldIndex < (chosenListProperty.size() - 1)) && !alreadyTaken ? oldIndex + 1 : oldIndex;
            chosenListProperty.add(newIndex, chosenListProperty.remove(oldIndex));
            newIndices.add(newIndex);
        }

        newIndices.forEach(index -> chosenSelectionModelProperty.getValue().select(index));
        chosenSelectionModelProperty.getValue().select(newIndices.getFirst());
        refreshPreview();
    }

    public void resetDefaultLayout() {
        PreviewLayout previewLayout = selectedLayoutProperty.getValue();
        if (previewLayout != null) {
            if (previewLayout instanceof TextBasedPreviewLayout layout) {
                layout.setText(TextBasedPreviewLayout.of(
                        TextBasedPreviewLayout.DEFAULT,
                        layoutFormatterPreferences,
                        abbreviationRepository).getText());
                refreshPreview();
            }
        }
    }

    /// XML-Syntax-Highlighting for RichTextFX-Codearea created by (c) Carlos Martins (github:
    /// <a href="https://github.com/cmartins">@cemartins</a>)
    ///
    /// License: <a href="https://github.com/FXMisc/RichTextFX/blob/master/LICENSE">BSD-2-Clause</a>
    ///
    /// See also
    /// <a href="https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/README.md#xml-editor">https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/README.md#xml-editor</a>
    ///
    /// @param text to parse and highlight
    /// @return highlighted span for codeArea
    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        final int GROUP_OPEN_BRACKET = 2;
        final int GROUP_ELEMENT_NAME = 3;
        final int GROUP_ATTRIBUTES_SECTION = 4;
        final int GROUP_CLOSE_BRACKET = 5;
        final int GROUP_ATTRIBUTE_NAME = 1;
        final int GROUP_EQUAL_SYMBOL = 2;
        final int GROUP_ATTRIBUTE_VALUE = 3;

        Matcher matcher = XML_TAG_PATTERN.matcher(text);
        int lastKeywordEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            spansBuilder.add(List.of(), matcher.start() - lastKeywordEnd);
            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Set.of("comment"), matcher.end() - matcher.start());
            } else {
                if (matcher.group("ELEMENT") != null) {
                    String attributesText = matcher.group(GROUP_ATTRIBUTES_SECTION);

                    spansBuilder.add(Set.of("tagmark"), matcher.end(GROUP_OPEN_BRACKET) - matcher.start(GROUP_OPEN_BRACKET));
                    spansBuilder.add(Set.of("anytag"), matcher.end(GROUP_ELEMENT_NAME) - matcher.end(GROUP_OPEN_BRACKET));

                    if (!attributesText.isEmpty()) {
                        lastKeywordEnd = 0;

                        Matcher attributesMatcher = XML_ATTRIBUTES_PATTERN.matcher(attributesText);
                        while (attributesMatcher.find()) {
                            spansBuilder.add(List.of(), attributesMatcher.start() - lastKeywordEnd);
                            spansBuilder.add(Set.of("attribute"), attributesMatcher.end(GROUP_ATTRIBUTE_NAME) - attributesMatcher.start(GROUP_ATTRIBUTE_NAME));
                            spansBuilder.add(Set.of("tagmark"), attributesMatcher.end(GROUP_EQUAL_SYMBOL) - attributesMatcher.end(GROUP_ATTRIBUTE_NAME));
                            spansBuilder.add(Set.of("avalue"), attributesMatcher.end(GROUP_ATTRIBUTE_VALUE) - attributesMatcher.end(GROUP_EQUAL_SYMBOL));
                            lastKeywordEnd = attributesMatcher.end();
                        }
                        if (attributesText.length() > lastKeywordEnd) {
                            spansBuilder.add(List.of(), attributesText.length() - lastKeywordEnd);
                        }
                    }

                    lastKeywordEnd = matcher.end(GROUP_ATTRIBUTES_SECTION);

                    spansBuilder.add(Set.of("tagmark"), matcher.end(GROUP_CLOSE_BRACKET) - lastKeywordEnd);
                }
            }
            lastKeywordEnd = matcher.end();
        }
        spansBuilder.add(List.of(), text.length() - lastKeywordEnd);
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

    /// This is called, when the user drops some PreviewLayouts either in the availableListView or in the empty space of chosenListView
    ///
    /// @param targetList either availableListView or chosenListView
    public boolean dragDropped(ListProperty<PreviewLayout> targetList, Dragboard dragboard) {
        boolean success = false;

        if (dragboard.hasContent(DragAndDropDataFormats.PREVIEWLAYOUTS)) {
            List<PreviewLayout> draggedLayouts = localDragboard.getPreviewLayouts();
            if (!draggedLayouts.isEmpty()) {
                if (dragSourceList == targetList) { // ignore if dropping into the same list
                    return false;
                }

                dragSourceSelectionModel.getValue().clearSelection();
                dragSourceList.getValue().removeAll(draggedLayouts);

                if (dragSourceList == chosenListProperty) {     // drag from 'Selected' list
                    // Allows for citations to drop into the list that they belong in from 'Selected'
                    // regardless of which available ListView physically received the drop.
                    for (PreviewLayout layout : draggedLayouts) {
                        ListProperty<PreviewLayout> destination = destinationFor(layout);   // get list based on type
                        if (!destination.getValue().contains(layout)) { // check for duplicate
                            destination.add(layout);
                        }
                        lastRoutedLayoutProperty.setValue(layout);
                    }
                    success = true;

                    cslListProperty.getValue().sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
                    customizedListProperty.getValue().sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
                } else {                                        // drag from 'Available' lists
                    // prevents adding duplicate names
                    List<PreviewLayout> filteredLayouts = draggedLayouts.stream().filter(layout -> !targetList.getValue().contains(layout)).toList();
                    targetList.getValue().addAll(filteredLayouts);
                    success = true;

                    if (targetList == cslListProperty) {
                        targetList.getValue().sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
                    }
                }
            }
        }

        return success;
    }

    /// This is called, when the user drops some PreviewLayouts on another cell in chosenListView to sort them
    ///
    /// @param targetLayout the Layout, the user drops a layout on
    public boolean dragDroppedInChosenCell(PreviewLayout targetLayout, Dragboard dragboard) {
        boolean success = false;

        if (dragboard.hasContent(DragAndDropDataFormats.PREVIEWLAYOUTS)) {
            List<PreviewLayout> draggedSelectedLayouts = new ArrayList<>(localDragboard.getPreviewLayouts());
            if (!draggedSelectedLayouts.isEmpty()) {
                chosenSelectionModelProperty.getValue().clearSelection();
                int targetId = chosenListProperty.getValue().indexOf(targetLayout);

                // An empty cell reference below the last occupied row can resolve to -1 here.
                // Treat it the same as "no specific target".
                // Otherwise, the dragged cell gets consumed, placed in an invalid index, and drops into nothing
                if (targetId < 0) {
                    targetLayout = null;
                }
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
                    //                    targetId = chosenListProperty.getValue().indexOf(targetLayout) + onSelectedDelta;
                    targetId = chosenListProperty.getValue().indexOf(targetLayout);
                    // Guard again: the target may have become stale as a side effect of the removal above.
                    targetId = (targetId < 0) ? chosenListProperty.getValue().size() : targetId + onSelectedDelta;
                } else if (targetId != 0) {
                    targetId = chosenListProperty.getValue().size();
                }

                List<PreviewLayout> filteredLayouts = draggedSelectedLayouts.stream().filter(layout -> !chosenListProperty.getValue().contains(layout)).toList();
                chosenListProperty.getValue().addAll(targetId, filteredLayouts);

                draggedSelectedLayouts.forEach(layout -> chosenSelectionModelProperty.getValue().select(layout));

                success = true;
            }
        }

        return success;
    }

    public BooleanProperty showAsExtraTabProperty() {
        return showAsExtraTabProperty;
    }

    public BooleanProperty showPreviewInEntryTableTooltip() {
        return showPreviewInEntryTableTooltip;
    }

    public ListProperty<PreviewLayout> cslListProperty() {
        return cslListProperty;
    }

    public ListProperty<PreviewLayout> customizedListProperty() {
        return customizedListProperty;
    }

    public ListProperty<PreviewLayout> destinationFor(PreviewLayout layout) {
        return (layout instanceof TextBasedPreviewLayout) ? customizedListProperty : cslListProperty;
    }

    public FilteredList<PreviewLayout> getFilteredCslLayouts() {
        return this.filteredCslLayouts;
    }

    public FilteredList<PreviewLayout> getFilteredCustomizedLayouts() {
        return this.filteredCustomizedLayouts;
    }

    public void setAvailableFilter(String searchTerm) {
        // need to filter on both csl and customized tabs now
        Predicate<PreviewLayout> predicate =
                preview -> searchTerm.isEmpty()
                        || preview.containsCaseIndependent(searchTerm);
        this.filteredCslLayouts.setPredicate(predicate);
        this.filteredCustomizedLayouts.setPredicate(predicate);
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

    public ObjectProperty<PreviewLayout> selectedLayoutProperty() {
        return selectedLayoutProperty;
    }

    public StringProperty sourceTextProperty() {
        return sourceTextProperty;
    }

    public BooleanProperty shouldDownloadCoversProperty() {
        return shouldDownloadCovers;
    }

    public ObjectProperty<PreviewLayout> lastRoutedLayoutProperty() {
        return lastRoutedLayoutProperty;
    }

    public StringProperty styleNameProperty() {
        return styleNameProperty;
    }

    public void addBstStyle(Path bstFile) {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(bstFile);
        bstStylesPaths.add(bstFile);
        cslListProperty().add(bstPreviewLayout);
        chosenListProperty().add(bstPreviewLayout);
    }

    public void removeCustomStyle(PreviewLayout layout) {
        if (layout instanceof BstPreviewLayout bstLayout) {
            cslListProperty.remove(bstLayout);
            chosenListProperty.remove(bstLayout);
            // Remove the path so it doesn't come back on restart
            bstStylesPaths.remove((bstLayout.getFilePath()));
        }
    }

    public void addCustomizedStyle() {
        TextBasedPreviewLayout layout =
                TextBasedPreviewLayout.of(
                        nextCustomStyleDefaultName(),
                        TextBasedPreviewLayout.DEFAULT,
                        layoutFormatterPreferences,
                        abbreviationRepository);

        customizedListProperty.add(layout);

        availableSelectionModelProperty.getValue().clearSelection();
        availableSelectionModelProperty.getValue().select(layout);
        setPreviewLayout(layout);
    }

    public void removeCustomizedStyle() {
        PreviewLayout layout =
                availableSelectionModelProperty.getValue().getSelectedItem();

        // customized citation item are TextBasedPreviewLayout
        // This check prevents original csl citations from being deleted
        if (!(layout instanceof TextBasedPreviewLayout)) {
            return;
        }

        customizedListProperty.remove(layout);
        availableSelectionModelProperty.getValue().clearSelection();
    }

    // Commits an edit made in the style-name field to the currently selected TextBasedPreviewLayout.
    // No-ops for non-customized (CSL/BST) selections. Reverts the field on blank/duplicate input.
    public void renameSelectedStyle(String newName) {
        if (selectedLayoutProperty.getValue() instanceof TextBasedPreviewLayout layout) {
            if (newName != null && !newName.isBlank()) {
                String trimmed = newName.trim();
                if (trimmed.equals(layout.getDisplayName())) {
                    return;
                }
                // check for duplicates in both lists.
                boolean isDupInCustomizedListProperty = customizedListProperty.stream()
                                                                              .filter(existing -> existing != layout)
                                                                              .anyMatch(existing -> existing.getDisplayName().equalsIgnoreCase(trimmed));
                boolean isDupInCslListProperty = cslListProperty.stream()
                                                                .filter(existing -> existing != layout)
                                                                .anyMatch(existing -> existing.getDisplayName().equalsIgnoreCase(trimmed));
                if (isDupInCslListProperty || isDupInCustomizedListProperty) {
                    dialogService.showWarningDialogAndWait(
                            Localization.lang("Error"),
                            Localization.lang("A style with this name already exists."));
                    styleNameProperty.setValue(layout.getDisplayName());
                } else {
                    layout.setName(trimmed);
                    styleNameProperty.setValue(trimmed);
                    customizedListProperty.getValue().sort(Comparator.comparing(PreviewLayout::getDisplayName, String.CASE_INSENSITIVE_ORDER));
                }
            }
        }
    }

    private String nextCustomStyleDefaultName() {
        while (true) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS");
            String candidate = Localization.lang("Customized preview style") + " " + LocalDateTime.now().format(formatter);
            boolean exists = customizedListProperty.stream()
                                                   .map(PreviewLayout::getDisplayName)
                                                   .anyMatch(candidate::equals);
            if (!exists) {
                return candidate;
            }
        }
    }
}
