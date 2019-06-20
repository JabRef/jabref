package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ScrollPane;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.citationstyle.PreviewLayout;
import org.jabref.logic.citationstyle.TextBasedPreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreviewPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewTabViewModel.class);

    private final ListProperty<PreviewLayout> availableListProperty = new SimpleListProperty<>();
    private final ListProperty<PreviewLayout> selectedAvailableItemsProperty = new SimpleListProperty<>();
    private final ListProperty<PreviewLayout> chosenListProperty = new SimpleListProperty<>();
    private final ListProperty<PreviewLayout> selectedChosenItemsProperty = new SimpleListProperty<>();

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

    @Override
    public void storeSettings() {
        PreviewPreferences previewPreferences = preferences.getPreviewPreferences();

        if (chosenListProperty.isEmpty()) {
            chosenListProperty.add(previewPreferences.getTextBasedPreviewLayout());
        }

        PreviewPreferences newPreviewPreferences = preferences.getPreviewPreferences()
                                                              .getBuilder()
                                                              .withPreviewCycle(chosenListProperty)
                                                              .withPreviewStyle(((TextBasedPreviewLayout) findLayoutByNameOrDefault("Preview")).getLayoutText())
                                                              .build();

        if (!selectedChosenItemsProperty.getValue().isEmpty()) {
            newPreviewPreferences = newPreviewPreferences.getBuilder().withPreviewCyclePosition(chosenListProperty.getValue().indexOf(selectedChosenItemsProperty.get(0))).build();
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
        for (PreviewLayout layout : selectedAvailableItemsProperty.getValue()) {
            availableListProperty.remove(layout);
            chosenListProperty.add(layout);
        }
    }

    public void removeFromChosen() {
        for (PreviewLayout layout : selectedChosenItemsProperty.getValue()) {
            availableListProperty.add(layout);
            availableListProperty.sort((a,b) -> { return a.getName().compareToIgnoreCase(b.getName()); } );
            chosenListProperty.remove(layout);
        }
    }

    public List<Integer> selectedInChosenUp(List<Integer> oldindices) {
        List<Integer> newIndices = new ArrayList<>();
        for (int oldIndex : oldindices) {
            boolean alreadyTaken = newIndices.contains(oldIndex - 1);
            int newIndex = ((oldIndex > 0) && !alreadyTaken) ? oldIndex - 1 : oldIndex;
            chosenListProperty.add(newIndex, chosenListProperty.remove(oldIndex));
            newIndices.add(newIndex);
        }
        return newIndices;
    }

    public List<Integer> selectedInChosenDown(List<Integer> oldIndices) {
        List<Integer> newSelectedIndices = new ArrayList<>();
        for (int i = oldIndices.size() - 1; i >= 0; i--) {
            int oldIndex = oldIndices.get(i);
            boolean alreadyTaken = newSelectedIndices.contains(oldIndex + 1);
            int newIndex = ((oldIndex < (chosenListProperty.size() - 1)) && !alreadyTaken) ? oldIndex + 1 : oldIndex;
            chosenListProperty.add(newIndex, chosenListProperty.remove(oldIndex));
            newSelectedIndices.add(newIndex);
        }
        return newSelectedIndices;
    }

    public PreviewViewer getPreviewViewer() {
        PreviewLayout layout = (selectedChosenItemsProperty.getValue().isEmpty()) ?
                chosenListProperty.getValue().get(0) :
                selectedChosenItemsProperty.getValue().get(0);

        try {
            PreviewViewer testPane = new PreviewViewer(new BibDatabaseContext(), dialogService, Globals.stateManager);
            testPane.setEntry(TestEntry.getTestEntry());
            testPane.setLayout(layout);
            return testPane;
        } catch (StringIndexOutOfBoundsException exception) {
            LOGGER.warn("Parsing error.", exception);
            dialogService.showErrorDialogAndWait(Localization.lang("Parsing error"), Localization.lang("Parsing error") + ": " + Localization.lang("illegal backslash expression"), exception);
            return ((PreviewViewer) new ScrollPane());
        }
    }

    public void resetDefaultStyle() {
        ((TextBasedPreviewLayout) findLayoutByNameOrDefault("Preview")) // There are issues with caching the testentries
                .setLayoutText(preferences.getPreviewPreferences().getDefaultPreviewStyle());
    }

    public ListProperty<PreviewLayout> availableListProperty() { return availableListProperty; }

    public ListProperty<PreviewLayout> selectedAvailableItemsProperty() { return selectedAvailableItemsProperty; }

    public ListProperty<PreviewLayout> chosenListProperty() { return chosenListProperty; }

    public ListProperty<PreviewLayout> selectedChosenItemsProperty() { return selectedChosenItemsProperty; }
}
