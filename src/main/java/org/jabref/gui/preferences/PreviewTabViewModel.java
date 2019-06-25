package org.jabref.gui.preferences;

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.citationstyle.PreviewLayout;
import org.jabref.logic.citationstyle.TextBasedPreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreviewPreferences;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewTabViewModel.class);

    private final ListProperty<PreviewLayout> availableListProperty = new SimpleListProperty<>();
    private final ObjectProperty<MultipleSelectionModel<PreviewLayout>> availableSelectionModelProperty = new SimpleObjectProperty<>();
    private final ListProperty<PreviewLayout> chosenListProperty = new SimpleListProperty<>();
    private final ObjectProperty<MultipleSelectionModel<PreviewLayout>> chosenSelectionModelProperty = new SimpleObjectProperty<>();

    private final BooleanProperty selectedIsEditableProperty = new SimpleBooleanProperty(false);

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final PreviewPreferences previewPreferences;
    private final TaskExecutor taskExecutor;

    public PreviewTabViewModel(DialogService dialogService, JabRefPreferences preferences, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        previewPreferences = preferences.getPreviewPreferences();

        setValues();
    }

    public void setValues() {
        final ObservableList<PreviewLayout> availableLayouts = FXCollections.observableArrayList();

        chosenListProperty.setValue(FXCollections.observableArrayList(previewPreferences.getPreviewCycle()));
        if (chosenListProperty.stream().noneMatch(layout -> layout instanceof TextBasedPreviewLayout)) {
            chosenListProperty.add(previewPreferences.getTextBasedPreviewLayout());
        }

        BackgroundTask.wrap(CitationStyle::discoverCitationStyles)
                .onSuccess(value -> value.stream()
                        .map(CitationStylePreviewLayout::new)
                        .sorted(Comparator.comparing(PreviewLayout::getName))
                        .filter(style -> !chosenListProperty.contains(style))
                        .forEach(availableLayouts::add)) // does not accept a property, so this is using availableLayouts
                .onFailure(ex -> {
                    LOGGER.error("Something went wrong while adding the discovered CitationStyles to the list. ", ex);
                    dialogService.showErrorDialogAndWait(Localization.lang("Error adding discovered CitationStyles"), ex);
                })
                .executeWith(taskExecutor);

        availableListProperty.setValue(availableLayouts);
    }

    private PreviewLayout findLayoutByNameOrDefault(String name) {
        return availableListProperty.getValue().stream().filter(layout -> layout.getName().equals(name))
                .findAny()
                .orElse(chosenListProperty.getValue().stream().filter(layout -> layout.getName().equals(name))
                        .findAny()
                        .orElse(previewPreferences.getTextBasedPreviewLayout()));
    }

    public PreviewLayout findLayoutByNameOrNull(String name) {
        return availableListProperty.getValue().stream().filter(layout -> layout.getName().equals(name))
                .findAny()
                .orElse(chosenListProperty.getValue().stream().filter(layout -> layout.getName().equals(name))
                        .findAny()
                        .orElse(null));
    }

    /**
     * XML-Syntax-Highlighting for RichTextFX-Codearea
     * created by (c) Carlos Martins (github: @cemartins)
     * License: BSD-2-Clause
     * see https://github.com/FXMisc/RichTextFX/blob/master/LICENSE
     * and: https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/README.md#xml-editor
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
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
            } else {
                if (matcher.group("ELEMENT") != null) {
                    String attributesText = matcher.group(GROUP_ATTRIBUTES_SECTION);

                    spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_OPEN_BRACKET) - matcher.start(GROUP_OPEN_BRACKET));
                    spansBuilder.add(Collections.singleton("anytag"), matcher.end(GROUP_ELEMENT_NAME) - matcher.end(GROUP_OPEN_BRACKET));

                    if (!attributesText.isEmpty()) {

                        lastKwEnd = 0;

                        Matcher amatcher = ATTRIBUTES.matcher(attributesText);
                        while (amatcher.find()) {
                            spansBuilder.add(Collections.emptyList(), amatcher.start() - lastKwEnd);
                            spansBuilder.add(Collections.singleton("attribute"), amatcher.end(GROUP_ATTRIBUTE_NAME) - amatcher.start(GROUP_ATTRIBUTE_NAME));
                            spansBuilder.add(Collections.singleton("tagmark"), amatcher.end(GROUP_EQUAL_SYMBOL) - amatcher.end(GROUP_ATTRIBUTE_NAME));
                            spansBuilder.add(Collections.singleton("avalue"), amatcher.end(GROUP_ATTRIBUTE_VALUE) - amatcher.end(GROUP_EQUAL_SYMBOL));
                            lastKwEnd = amatcher.end();
                        }
                        if (attributesText.length() > lastKwEnd) {
                            spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
                        }
                    }

                    lastKwEnd = matcher.end(GROUP_ATTRIBUTES_SECTION);

                    spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_CLOSE_BRACKET) - lastKwEnd);
                }
            }
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    @Override
    public void storeSettings() {
        PreviewPreferences previewPreferences = preferences.getPreviewPreferences();

        if (chosenListProperty.isEmpty()) {
            chosenListProperty.add(previewPreferences.getTextBasedPreviewLayout());
        }

        PreviewPreferences newPreviewPreferences = preferences.getPreviewPreferences()
                                                              .getBuilder()
                                                              .withPreviewCycle(chosenListProperty)
                                                              .withPreviewStyle(((TextBasedPreviewLayout) findLayoutByNameOrDefault("Preview")).getText())
                                                              .build();

        if (!chosenSelectionModelProperty.getValue().getSelectedItems().isEmpty()) {
            newPreviewPreferences = newPreviewPreferences.getBuilder().withPreviewCyclePosition(chosenListProperty.getValue().indexOf(chosenSelectionModelProperty.getValue().getSelectedItems().get(0))).build();
        }
        preferences.storePreviewPreferences(newPreviewPreferences);

        for (BasePanel basePanel : JabRefGUI.getMainFrame().getBasePanelList()) {
            // TODO: Find a better way to update preview
            basePanel.closeBottomPane();
            //basePanel.getPreviewPanel().updateLayout(preferences.getPreviewPreferences());
        }
    }

    @Override
    public boolean validateSettings() {
        return !chosenListProperty.getValue().isEmpty();
    }

    public void addToChosen() {
        List<PreviewLayout> selected = new ArrayList<>();
        selected.addAll(availableSelectionModelProperty.getValue().getSelectedItems());
        availableListProperty.removeAll(selected);
        chosenListProperty.addAll(selected);
    }

    public void removeFromChosen() {
        List<PreviewLayout> selected = new ArrayList<>();
        selected.addAll(chosenSelectionModelProperty.getValue().getSelectedItems());
        chosenListProperty.removeAll(selected);
        availableListProperty.addAll(selected);
        availableListProperty.sort((a,b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public List<Integer> selectedInChosenUp(List<Integer> oldIndices) {
        List<Integer> selected = new ArrayList<>();
        List<Integer> newIndices = new ArrayList<>();
        selected.addAll(oldIndices); // oldIndices needs to be copied, because remove(oldIndex) alters oldIndices

        for (int oldIndex : selected) {
            boolean alreadyTaken = newIndices.contains(oldIndex - 1);
            int newIndex = ((oldIndex > 0) && !alreadyTaken) ? oldIndex - 1 : oldIndex;
            chosenListProperty.add(newIndex, chosenListProperty.remove(oldIndex));
            newIndices.add(newIndex);
        }
        return newIndices;
    }

    public List<Integer> selectedInChosenDown(List<Integer> oldIndices) {
        List<Integer> selected = new ArrayList<>();
        List<Integer> newIndices = new ArrayList<>();
        selected.addAll(oldIndices);

        for (int i = selected.size() - 1; i >= 0; i--) {
            int oldIndex = selected.get(i);
            boolean alreadyTaken = newIndices.contains(oldIndex + 1);
            int newIndex = ((oldIndex < (chosenListProperty.size() - 1)) && !alreadyTaken) ? oldIndex + 1 : oldIndex;
            chosenListProperty.add(newIndex, chosenListProperty.remove(oldIndex));
            newIndices.add(newIndex);
        }
        return newIndices;
    }

    public PreviewLayout getCurrentLayout() {
        if (!chosenSelectionModelProperty.getValue().getSelectedItems().isEmpty()) {
            return chosenSelectionModelProperty.getValue().getSelectedItems().get(0);
        }

        if (!chosenListProperty.getValue().isEmpty()) {
            return chosenListProperty.getValue().get(0);
        }

        return findLayoutByNameOrDefault("Preview");
    }

    public void resetDefaultStyle() {
        ((TextBasedPreviewLayout) findLayoutByNameOrDefault("Preview"))
                .setText(preferences.getPreviewPreferences().getDefaultPreviewStyle());
    }

    public ListProperty<PreviewLayout> availableListProperty() { return availableListProperty; }

    public ObjectProperty<MultipleSelectionModel<PreviewLayout>> availableSelectionModelProperty() { return availableSelectionModelProperty; }

    public ListProperty<PreviewLayout> chosenListProperty() { return chosenListProperty; }

    public ObjectProperty<MultipleSelectionModel<PreviewLayout>> chosenSelectionModelProperty() { return chosenSelectionModelProperty; }

    public BooleanProperty selectedIsEditableProperty() { return selectedIsEditableProperty; }
}
