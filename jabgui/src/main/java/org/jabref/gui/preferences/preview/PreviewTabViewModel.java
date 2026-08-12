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
import java.util.stream.Stream;

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

// This class is Preferences -> Entry Preview tab model
//
// [PreviewTab] is the controller of Entry Preview tab
//
// @see PreviewTab
public class PreviewTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewTabViewModel.class);
    private static final Pattern XML_TAG_PATTERN = Pattern.compile("(?<ELEMENT>(</?\\h*)(\\w+)([^<>]*)(\\h*/?>))"
            + "|(?<COMMENT><!--[^<>]+-->)");
    private static final Pattern XML_ATTRIBUTES_PATTERN = Pattern.compile("(\\w+\\h*)(=)(\\h*\"[^\"]+\")");

    private final BooleanProperty showAsExtraTabProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty showPreviewInEntryTableTooltip = new SimpleBooleanProperty(false);

    private final BooleanProperty shouldDownloadCovers = new SimpleBooleanProperty();

    /*
    TODO:
    ARROW BUTTONS
    [x] duplicates show up on the right side from the customized tab without moving the item
    [x] right arrow only listens to last click -> right arrow should only listen to last clicked on csl/customized tabs
    solution: changed code in to(x)ButtonAction(), mouseClickedChosen(), mouseClickedAvailable()
        to check the last clicked tab (previousTab). availableTabPane listener sets this previousTab

        dialog
                dialogService.showWarningDialogAndWait(
                        Localization.lang("Add custom preview style"),
                        Localization.lang("A custom preview style with this name already exists."));
                return;


    DRAG AND DROP
    [x] issue: you can drag and drop same items in the same listView and it would add the item again
    solution:

    bugs and concerns addressed
    Shouldn't be able to move csl items into customized
        - ignore the operation if not customized.
    [x] a few drag and drop issues:
    1. when I drag and drop an item from a list into the 'customized' tab (under available) or into the chosen List (under selected), duplicates can be added; I would like to prevent this
    2. when I drag and drop an item from a list into itself, it adds another of the same item. This only occurs in the customized tab list and under 'selected' of the chosen list; I also need to prevent this.
    3. when I drag and drop an item into the empty space of the 'selected', I add one item as expected. However, there is an issue where when I drag and drop into cells where items are listed, it adds the item twice. I should only be adding the item once.
    Solution - added checks in dragDroppedInChosenCell and dragDropped to prevent drops if
        src & dest list are same
        item already exists in the list

       [x] Issue J a v a   M e s s a g e : I n d e x :   - 1  message pops up occassionally when i drag
       and drop an item below the list of occupied cells.
       I suspect that the list is not large enough to take on more items beyond what it currently is
       and gives this error message and just makes the item disappear without it being moved anywhere
       on adding more items within the space of populated cells, it's able to add the items,
       then afterwards, adding items into the empty space below the populated cells works.
       I'm having a hard time reproducing the issue everytime, I'm not entirely sure what causes this issue.
       **when I move items from selected to available enough times, and then dragdrop to empty cells
       I think its dropping to an index that's out of bounds, and then eats the style

       solution:
       The existing code only falls back to "append at end" when targetLayout == null.
       But a stale non-null reference that no longer exists in the list also produces indexOf == -1,
       and that path was never checked and then addAll(-1, ...) throws an error,
       and because it throws after the item was already removed from the source list
       (dragSourceList.getValue().removeAll(...) a few lines earlier),
       the item is gone from both lists.
       That's your "eaten" style and the "Index: -1" message.
       The two changes in dragDroppedInChosenCell:
       (1) indexOf < 0 treated as "no target" up front
       (2) the second indexOf lookup after removal is re-guarded the same way instead of trusting it can't go negative.
       Both addAll calls now only ever receive a valid, non-negative index.


       methods: arrows, drag and drop, double click
       [x] should only put customizable items into customize tab (OPTIONAL)
       [x] only put csl items into csl tab
       Can you also make code changes so that when I {drag and drop, double click, arrow button} a citation style from selected to available, it will:
       move the citation style to the correct list (custom citation style moves to the list in customized tab and original non-TextBasedPreviewLayout citation styles moves to the list in csl tab)
       switch and focus to the corresponding tab (csl or customized)

       Used conditional statements to place the citation styles in the correct listProperties
       for (PreviewLayout layout : selected) {
            ListProperty<PreviewLayout> destination = destinationFor(layout);
            if (!destination.getValue().contains(layout)) {
                destination.add(layout);
            }
            lastRoutedLayoutProperty.setValue(layout);
        }

        [x] focus on the tab relevant to the citation style that got moved
        Solution: method keeps track of the last layout that was routed from src to dest
        It then selects the tab that was last referenced.
        focusTabOnLastRoutedLayout()

       [x] right arrow is disabled when selecting items in customized tab.
       solution updateRightButtonBindingFocus() fixes this issue by unbinding and binding to the current availableSelectionModel (i.e. the current tab)

       [x] issue, when clicking customized tab when its empty, and then creating/adding to the list and then clicking an item, we cant see anything in the preview/edit tab.
       solution: creating a style via the + button on the Customized tab will correctly show up in preview/edit (fix #2)

        previewViewer.visibleProperty().bind(viewModel.selectedLayoutProperty().isNotNull());
        ...
        editArea.visibleProperty().bind(viewModel.selectedLayoutProperty().isNotNull());

        selectedLayoutProperty is already kept correctly up to date by every selection-change listener on cslListView, customizedListView, and chosenListView (they all call viewModel.setPreviewLayout(newValue)),
        plus addCustomizedStyle() sets it directly. So this one property is a reliable, always-current signal for "something is selected, show the editor",
        regardless of which tab/list it came from, and it doesn't go stale on tab switches the way a captured MultipleSelectionModel reference does.


       [] PREVIEW default custom style does not persist when it is in customized tab
       in findLayoutByName, it only searches in chosenListProperty and cslListProperty
       but not in customizedListProperty.
               // If user drags original default style from Selected to Available
        // it lands in customized and findLayoutByName(TextBasedPreviewLayout.NAME)
        // cannot find. storeSettings() silently a new PREVIEW Default layout, losing edits instead of finding the real one.
        solution:


    Given the above, "Update storage to support multiple customized styles, including migration and tests" means:

    Replace the single customPreviewLayout: String with a list of customized-style records in PreviewPreferences (mirroring how bstPreviewLayoutPaths already works).
    Give each customized style a stable identity independent of its display name, so renaming doesn't break the save/load round trip.
    Update PreviewTabViewModel.setValues() / storeSettings() to materialize/dematerialize the whole list, not just one entry.
    Whatever reads/writes the actual backing store (Preferences API) needs a new key for the list, and a migration that takes an existing user's old single customPreviewLayout value and wraps it into the new list format the first time they open the new version — otherwise everyone's existing custom style vanishes on upgrade.
    Tests covering: the migration path, and normal save/load with N customized styles.


    [x] refresh button doesnt work on new custom citation styles
    in PreviewTabViewModel.resetDefaultLayout(), conditional was only search for
    layouts with PREVIEW as the text, must be searching for customizedLayouts (i.e. TextBasedPreviewLayout)


    [x] when typing in a duplicate name to rename and hitting the enter button,
        the dialogService.showWarningDialogAndWait returns an error prompt multiple times.
        When closing the window and having a duplicate name in the stylenamefield,
        the error prompt loops
        solution:
        - showWarningDialogAndWait opens a ui dialog window and steals the focus
        which triggers losing focus and calls commitStyleNameEdit() again, with the duplicate name
        still in the styleFieldName, causing multiple pop ups.

        - On window close, the same thing happens: closing the window blurs
        styleNameField →
        focus-lost listener fires →
        tries to open a warning dialog →
        but the window is already tearing down, so JavaFX can't create a platform window for the new dialog →
        RuntimeException: could not create platform window. Since the field is still focused-then-unfocused repeatedly during teardown, you get repeated attempts.

        using a global flag to catch multiple triggers will safeguard this scenario
        if the user clicks out of the styleNameField with a duplicate rename, revert the rename and don't show error for QoL
        Otherwise, if user hit enter, rename is deliberate and error should show

        ****ended up reverting so that only enter key will be responsible for renaming.

        [x] whenever I rename a style, it does not reflect in the 'customized' tab or the
        'selected' view.
        solution: On renaming through PreviewTab.commitStyleNameEdit()
        you must refresh the chosen and customized views
            customizedListView.refresh();   // refresh view to display rename in 'customized'
            chosenListView.refresh();       // refresh view to display rename in 'selected'

        [x] Issue when dragdrop from selected to available, tabs snap to wherever the
        style should belong, but if you swap to another tab from available
        and then dragdrop from available to selected, it snaps back to whatever tab remembered
        last from the selected -> available draganddrop

        solution:
        PreviewTab.dragDropped(...)
        // Only switch tabs when routing OUT of Selected INTO Available
        // dropping in Selected has no need to refocus the Available tabs.
        if (success && targetList != viewModel.chosenListProperty()) {
            focusTabOnLastRoutedLayout();
        }


        [x]trying to type in the edit field when the customized tab is empty and then gets an item
        can only type 1 letter and freezes.
        solution: editing text of a Customized-tab style will no longer get nulled out mid-keystroke
        reapply whatever layoutProperty is currently selected and reset in setPreviewLayout
        public void refreshPreview() {
            PreviewLayout current = selectedLayoutProperty.getValue();
            setPreviewLayout(null);
            setPreviewLayout(current);
        }

    [x] 3 Create/Delete new customized entry (add a + and - button above the list)
        a. add buttons
        b. link buttons
        c. create customized (prevent duplicates, done by localdatetime now())
        d. delete customized

    [x] 4 Rename a customized entry

    [] 5 testing
    [] 6 documentation

     */
    private final ObjectProperty<PreviewLayout> lastRoutedLayoutProperty = new SimpleObjectProperty<>();
    private final ListProperty<PreviewLayout> cslListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<PreviewLayout> customizedListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<MultipleSelectionModel<PreviewLayout>> availableSelectionModelProperty = new SimpleObjectProperty<>(new NoSelectionModel<>());
    //    private final FilteredList<PreviewLayout> filteredAvailableLayouts = new FilteredList<>(this.availableListProperty());
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
        for (CustomizedPreviewStyle stored : previewPreferences.getCustomizedPreviewLayouts()) {
            TextBasedPreviewLayout textBasedPreviewLayout = new TextBasedPreviewLayout(stored.id(), stored.name(), stored.text(), layoutFormatterPreferences, abbreviationRepository);
            if (chosenListProperty.stream().noneMatch(currLayout -> currLayout instanceof TextBasedPreviewLayout textLayout && textLayout.getId().equals(stored.id()))) {
                customizedListProperty.getValue().add(textBasedPreviewLayout);
            }
        }
        // cslListProperty.clear();
        //        if (chosenListProperty.stream().noneMatch(TextBasedPreviewLayout.class::isInstance)) {
        //            cslListProperty.getValue().add(TextBasedPreviewLayout.of(
        //                    previewPreferences.getCustomPreviewLayout(),
        //                    layoutFormatterPreferences,
        //                    abbreviationRepository));
        //        }

        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);

        BackgroundTask.wrap(CSLStyleLoader::getStyles)
                      .onSuccess(styles -> styles.stream()
                                                 .map(style -> new CitationStylePreviewLayout(style, entryTypesManager))
                                                 .filter(style -> chosenListProperty.getValue().filtered(item ->
                                                         item.getName().equals(style.getName())).isEmpty())
                                                 .sorted(Comparator.comparing(PreviewLayout::getName))
                                                 //                                                 .forEach(cslListProperty::add))
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
        //        setPreviewLayout(null);
        //        setPreviewLayout(chosenSelectionModelProperty.getValue().getSelectedItem());
    }

    private TextBasedPreviewLayout findLayoutById(String id) {
        //        List<PreviewLayout> combinedList = new ArrayList<>();
        //        combinedList.addAll(cslListProperty);
        //        combinedList.addAll(customizedListProperty);
        //        combinedList.addAll(chosenListProperty);
        //        List<TextBasedPreviewLayout> filteredTextBasedPreviewLayoutList = new ArrayList<>();
        //        for (PreviewLayout layout : combinedList) {
        //            if (layout instanceof TextBasedPreviewLayout) {
        //                if (((TextBasedPreviewLayout) layout).getId().equals(id)) {
        //                    return (TextBasedPreviewLayout) layout;
        //                }
        //            }
        //        }
        //        return null;
        return Stream.of(cslListProperty, customizedListProperty, chosenListProperty)
                     .flatMap(prop -> prop.getValue().stream())
                     .filter(TextBasedPreviewLayout.class::isInstance)
                     .map(TextBasedPreviewLayout.class::cast)
                     .filter(layout -> layout.getId().equals(id))
                     .findAny()
                     .orElse(null);
    }

    //    private PreviewLayout findLayoutByName(String name) {
    //        // If user drags original default style from Selected to Available
    //        // it lands in customized and findLayoutByName(TextBasedPreviewLayout.NAME)
    //        // cannot find. storeSettings() silently a new PREVIEW Default layout, losing edits instead of finding the real one.
    //        return cslListProperty.getValue().stream().filter(layout -> layout.getName().equals(name))
    //                              .findAny()
    //                              .or(() -> customizedListProperty.getValue().stream().filter(layout -> layout.getName().equals(name)).findAny())
    //                              .or(() -> chosenListProperty.getValue().stream().filter(layout -> layout.getName().equals(name)).findAny())
    //                              .orElse(null);
    //        //        return cslListProperty.getValue().stream().filter(layout -> layout.getName().equals(name))
    //        //                              .findAny()
    //        //                                      .orElse(chosenListProperty.getValue().stream().filter(layout -> layout.getName().equals(name))
    //        //                                                                .findAny()
    //        //                                                                .orElse(null));
    //    }

    /// Store the changes of preference-preview settings.
    @Override
    public void storeSettings() {
        //        if (chosenListProperty.isEmpty()) {
        //            PreviewLayout textBasedPreviewLayout = findLayoutByName(TextBasedPreviewLayout.NAME);
        //            //            PreviewLayout textBasedPreviewLayout = findLayoutById(TextBasedPreviewLayout.NAME);
        //            if (textBasedPreviewLayout != null) {
        //                chosenListProperty.add(textBasedPreviewLayout);
        //            } else {
        //                chosenListProperty.add(TextBasedPreviewLayout.of(
        //                        TextBasedPreviewLayout.DEFAULT,
        //                        layoutFormatterPreferences,
        //                        abbreviationRepository));
        //            }
        //        }
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
        previewPreferences.getCustomizedPreviewLayouts().setAll(toStore);

        //        if (findLayoutByName(TextBasedPreviewLayout.NAME) instanceof TextBasedPreviewLayout customLayout) {
        //            previewPreferences.setCustomPreviewLayout(customLayout.getText());
        //        }

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

    public void addToChosen() {
        List<PreviewLayout> selected = new ArrayList<>(availableSelectionModelProperty.getValue().getSelectedItems());
        availableSelectionModelProperty.getValue().clearSelection();
        cslListProperty.removeAll(selected);
        chosenListProperty.addAll(selected);
    }

    public void addToChosen(ListProperty<PreviewLayout> sourceList) {
        List<PreviewLayout> selected = new ArrayList<>(availableSelectionModelProperty.getValue().getSelectedItems());
        availableSelectionModelProperty.getValue().clearSelection();
        sourceList.removeAll(selected);
        chosenListProperty.addAll(selected);
    }

    //    public void removeFromChosen() {
    //        List<PreviewLayout> selected = new ArrayList<>(chosenSelectionModelProperty.getValue().getSelectedItems());
    //        chosenSelectionModelProperty.getValue().clearSelection();
    //        chosenListProperty.removeAll(selected);
    //        cslListProperty.addAll(selected);
    //        cslListProperty.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
    //    }

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
    //
    //    public void removeFromChosen(ListProperty<PreviewLayout> destList) {
    //        List<PreviewLayout> selected = new ArrayList<>(chosenSelectionModelProperty.getValue().getSelectedItems());
    //        chosenSelectionModelProperty.getValue().clearSelection();
    //        chosenListProperty.removeAll(selected);
    //
    //        // ---
    //        //        List<PreviewLayout> selectedList = availableSelectionModelProperty.getValue().getSelectedItems()
    //        //                                                                          .stream().filter(layout -> layout instanceof TextBasedPreviewLayout).toList();
    //        //        List<PreviewLayout> filtered = new ArrayList<>();
    //        //        for (PreviewLayout layout : selectedList) {
    //        //            if (destList.equals(cslListProperty)) {
    //        //                // only add csl layouts
    //        //                if (!(layout instanceof TextBasedPreviewLayout)) {
    //        //                    filtered.add(layout);
    //        //                }
    //        //            } else if (destList.equals(customizedListProperty)) {
    //        //                // only add textBased layouts
    //        //                if (layout instanceof TextBasedPreviewLayout) {
    //        //                    filtered.add(layout);
    //        //                }
    //        //            }
    //        //        }
    //        //        chosenSelectionModelProperty.getValue().clearSelection();
    //        //        chosenListProperty.removeAll(filtered);
    //        //        destList.addAll(filtered);
    //        // ---
    //
    //        destList.addAll(selected);
    //        destList.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
    //    }

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
        if (selectedLayoutProperty.getValue() instanceof TextBasedPreviewLayout layout) {
            layout.setText(TextBasedPreviewLayout.of(
                    TextBasedPreviewLayout.DEFAULT,
                    layoutFormatterPreferences,
                    abbreviationRepository).getText());
            refreshPreview();
        }
        //        PreviewLayout defaultLayout = findLayoutByName(TextBasedPreviewLayout.NAME);
        //        if (defaultLayout instanceof TextBasedPreviewLayout layout) {
        //            layout.setText(TextBasedPreviewLayout.of(
        //                    TextBasedPreviewLayout.DEFAULT,
        //                    layoutFormatterPreferences,
        //                    abbreviationRepository).getText());
        //        }
        //        refreshPreview();
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

    //    public boolean dragDropped(ListProperty<PreviewLayout> targetList, Dragboard dragboard) {
    //        boolean success = false;
    //
    //        if (dragboard.hasContent(DragAndDropDataFormats.PREVIEWLAYOUTS)) {
    //            List<PreviewLayout> draggedLayouts = localDragboard.getPreviewLayouts();
    //            if (!draggedLayouts.isEmpty()) {
    //                dragSourceSelectionModel.getValue().clearSelection();
    //                dragSourceList.getValue().removeAll(draggedLayouts);
    //                targetList.getValue().addAll(draggedLayouts);
    //                success = true;
    //
    //                if (targetList == cslListProperty) {
    //                    targetList.getValue().sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
    //                }
    //            }
    //        }
    //
    //        return success;
    //    }

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

                // A stale/empty cell reference (e.g. a virtualized filler cell below the last
                // occupied row) resolves to -1 here. Treat it the same as "no specific target".
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

    //    public ListProperty<PreviewLayout> availableListProperty() {
    //        return cslListProperty;
    //    }

    public ListProperty<PreviewLayout> cslListProperty() {
        return cslListProperty;
    }

    public ListProperty<PreviewLayout> customizedListProperty() {
        return customizedListProperty;
    }

    public ListProperty<PreviewLayout> destinationFor(PreviewLayout layout) {
        return (layout instanceof TextBasedPreviewLayout) ? customizedListProperty : cslListProperty;
    }

    //    public FilteredList<PreviewLayout> getFilteredAvailableLayouts() {
    //        return this.filteredAvailableLayouts;
    //    }

    public FilteredList<PreviewLayout> getFilteredCslLayouts() {
        return this.filteredCslLayouts;
    }

    public FilteredList<PreviewLayout> getFilteredCustomizedLayouts() {
        return this.filteredCustomizedLayouts;
    }

    public void setAvailableFilter(String searchTerm) {
        //        this.filteredAvailableLayouts.setPredicate(
        //                preview -> searchTerm.isEmpty()
        //                        || preview.containsCaseIndependent(searchTerm));
        //        this.filteredAvailableLayouts.setPredicate(
        //                preview -> searchTerm.isEmpty()
        //                        || preview.containsCaseIndependent(searchTerm));
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

    /// Commits an edit made in the style-name field to the currently selected TextBasedPreviewLayout.
    /// No-ops for non-customized (CSL/BST) selections. Reverts the field on blank/duplicate input.
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
