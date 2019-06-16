package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

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

    private final StringProperty previewTextProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final TaskExecutor taskExecutor;

    public PreviewTabViewModel(DialogService dialogService, JabRefPreferences preferences, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;

        setValues();
    }

    public void setValues() {
        PreviewPreferences previewPreferences = preferences.getPreviewPreferences();
        ObservableList<PreviewLayout> availableLayouts = FXCollections.observableArrayList();

        chosenListProperty.setValue(FXCollections.observableArrayList(previewPreferences.getPreviewCycle()));

        availableLayouts.clear();
        if (chosenListProperty.stream().noneMatch(layout -> layout instanceof TextBasedPreviewLayout)) {
            availableLayouts.add(previewPreferences.getTextBasedPreviewLayout());
        }

        BackgroundTask.wrap(CitationStyle::discoverCitationStyles)
                      .onSuccess(value -> value.stream()
                                               .map(CitationStylePreviewLayout::new)
                                               .filter(style -> !chosenListProperty.contains(style))
                                               .sorted(Comparator.comparing(PreviewLayout::getName))
                                               .forEach(availableLayouts::add))
                      .onFailure(ex -> {
                          LOGGER.error("something went wrong while adding the discovered CitationStyles to the list ", ex);
                          dialogService.showErrorDialogAndWait(Localization.lang("Error adding discovered CitationStyles"), ex);
                      })
                      .executeWith(taskExecutor);

        availableListProperty.setValue(availableLayouts);

        previewTextProperty.setValue(previewPreferences.getPreviewStyle().replace("__NEWLINE__", "\n"));
    }

    @Override
    public void storeSettings() {
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();

        if (chosenListProperty.isEmpty()) {
            chosenListProperty.add(previewPreferences.getTextBasedPreviewLayout());
        }

        PreviewPreferences newPreviewPreferences = Globals.prefs.getPreviewPreferences()
                                                                .getBuilder()
                                                                .withPreviewCycle(chosenListProperty)
                                                                .withPreviewStyle(previewTextProperty.getValue().replace("\n", "__NEWLINE__"))
                                                                .build();
        if (!selectedChosenItemsProperty.getValue().isEmpty()) {
            newPreviewPreferences = newPreviewPreferences.getBuilder().withPreviewCyclePosition(chosenListProperty.getValue().indexOf(selectedChosenItemsProperty.get(0))).build();
        }
        Globals.prefs.storePreviewPreferences(newPreviewPreferences);

        for (BasePanel basePanel : JabRefGUI.getMainFrame().getBasePanelList()) {
            // TODO: Find a better way to update preview
            basePanel.closeBottomPane();
            //basePanel.getPreviewPanel().updateLayout(Globals.prefs.getPreviewPreferences());
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
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

    public void testChosen() {
        try {
            PreviewViewer testPane = new PreviewViewer(new BibDatabaseContext(), dialogService, Globals.stateManager);
            testPane.setEntry(TestEntry.getTestEntry());

            PreviewLayout layout = chosenListProperty.getValue().get(0);
            testPane.setLayout(layout);

            DialogPane pane = new DialogPane();
            pane.setContent(testPane);
            dialogService.showCustomDialogAndWait(Localization.lang("Preview"), pane, ButtonType.OK);

        } catch (StringIndexOutOfBoundsException exception) {
            LOGGER.warn("Parsing error.", exception);
            dialogService.showErrorDialogAndWait(Localization.lang("Parsing error"), Localization.lang("Parsing error") + ": " + Localization.lang("illegal backslash expression"), exception);
        }
    }

    public void setChosenDefault() {
        previewTextProperty.setValue(Globals.prefs.getPreviewPreferences()
                                                  .getDefaultPreviewStyle()
                                                  .replace("__NEWLINE__", "\n"));
    }

    public ListProperty<PreviewLayout> availableListProperty() { return availableListProperty; }

    public ListProperty<PreviewLayout> selectedAvailableItemsProperty() { return selectedAvailableItemsProperty; }

    public ListProperty<PreviewLayout> chosenListProperty() { return chosenListProperty; }

    public ListProperty<PreviewLayout> selectedChosenItemsProperty() { return selectedChosenItemsProperty; }

    public StringProperty previewTextProperty() { return previewTextProperty; }
}
