package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.citationstyle.PreviewLayout;
import org.jabref.logic.citationstyle.TextBasedPreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreviewPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPreferencesTab implements PrefsTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewPreferencesTab.class);

    private final ObservableList<PreviewLayout> availableModel = FXCollections.observableArrayList();
    private final ObservableList<PreviewLayout> chosenModel = FXCollections.observableArrayList();

    private final ListView<PreviewLayout> available = new ListView<>(availableModel);
    private final ListView<PreviewLayout> chosen = new ListView<>(chosenModel);

    private final Button btnRight = new Button("»");
    private final Button btnLeft = new Button("«");
    private final Button btnUp = new Button(Localization.lang("Up"));
    private final Button btnDown = new Button(Localization.lang("Down"));
    private final GridPane gridPane = new GridPane();
    private final TextArea previewTextArea = new TextArea();
    private final Button btnTest = new Button(Localization.lang("Test"));
    private final Button btnDefault = new Button(Localization.lang("Default"));
    private final ScrollPane scrollPane = new ScrollPane(previewTextArea);
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public PreviewPreferencesTab(DialogService dialogService, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        setupLogic();
        setupGui();
    }

    private void setupLogic() {
        BooleanBinding nothingSelectedFromChosen = Bindings.isEmpty(chosen.getSelectionModel().getSelectedItems());

        btnLeft.disableProperty().bind(nothingSelectedFromChosen);
        btnDown.disableProperty().bind(nothingSelectedFromChosen);
        btnUp.disableProperty().bind(nothingSelectedFromChosen);
        btnRight.disableProperty().bind(Bindings.isEmpty(available.getSelectionModel().getSelectedItems()));

        btnRight.setOnAction(event -> {
            for (PreviewLayout layout : available.getSelectionModel().getSelectedItems()) {
                availableModel.remove(layout);
                chosenModel.add(layout);
            }
        });

        btnLeft.setOnAction(event -> {
            for (PreviewLayout layout : chosen.getSelectionModel().getSelectedItems()) {
                availableModel.add(layout);
                chosenModel.remove(layout);
            }
        });

        btnUp.setOnAction(event -> {
            List<Integer> newSelectedIndices = new ArrayList<>();
            for (int oldIndex : chosen.getSelectionModel().getSelectedIndices()) {
                boolean alreadyTaken = newSelectedIndices.contains(oldIndex - 1);
                int newIndex = ((oldIndex > 0) && !alreadyTaken) ? oldIndex - 1 : oldIndex;
                chosenModel.add(newIndex, chosenModel.remove(oldIndex));
                chosen.getSelectionModel().select(newIndex);
            }
        });

        btnDown.setOnAction(event -> {
            List<Integer> newSelectedIndices = new ArrayList<>();
            ObservableList<Integer> selectedIndices = chosen.getSelectionModel().getSelectedIndices();
            for (int i = selectedIndices.size() - 1; i >= 0; i--) {
                int oldIndex = selectedIndices.get(i);
                boolean alreadyTaken = newSelectedIndices.contains(oldIndex + 1);
                int newIndex = ((oldIndex < (chosenModel.size() - 1)) && !alreadyTaken) ? oldIndex + 1 : oldIndex;
                chosenModel.add(newIndex, chosenModel.remove(oldIndex));
                chosen.getSelectionModel().select(newIndex);
            }
        });

        btnDefault.setOnAction(event -> previewTextArea.setText(Globals.prefs.getPreviewPreferences()
                                                                             .getDefaultPreviewStyle()
                                                                             .replace("__NEWLINE__", "\n")));

        btnTest.setOnAction(event -> {
            try {
                PreviewViewer testPane = new PreviewViewer(new BibDatabaseContext(), dialogService);
                testPane.setEntry(TestEntry.getTestEntry());

                PreviewLayout layout = chosen.getSelectionModel().getSelectedItem();
                testPane.setLayout(layout);

                DialogPane pane = new DialogPane();
                pane.setContent(testPane);
                dialogService.showCustomDialogAndWait(Localization.lang("Preview"), pane, ButtonType.OK);

            } catch (StringIndexOutOfBoundsException exception) {
                LOGGER.warn("Parsing error.", exception);
                dialogService.showErrorDialogAndWait(Localization.lang("Parsing error"), Localization.lang("Parsing error") + ": " + Localization.lang("illegal backslash expression"), exception);
            }
        });
    }

    private void setupGui() {
        VBox vBox = new VBox();
        btnRight.setPrefSize(80, 20);
        btnLeft.setPrefSize(80, 20);
        btnUp.setPrefSize(80, 20);
        btnDown.setPrefSize(80, 20);
        vBox.getChildren().addAll(new Label(""), new Label(""), new Label(""), new Label(""), new Label(""),
                new Label(""), new Label(""), btnRight, btnLeft, new Label(""), new Label(""), new Label(""),
                btnUp, btnDown);
        Label currentPreview = new Label(Localization.lang("Current Preview"));
        currentPreview.getStyleClass().add("sectionHeader");
        gridPane.add(currentPreview, 1, 1);
        gridPane.add(available, 1, 2);
        gridPane.add(vBox, 2, 2);
        gridPane.add(chosen, 3, 2);
        gridPane.add(btnTest, 2, 6);
        gridPane.add(btnDefault, 3, 6);
        previewTextArea.setPrefSize(600, 300);
        gridPane.add(scrollPane, 1, 9);

        btnTest.disableProperty().bind(Bindings.isEmpty(chosen.getSelectionModel().getSelectedItems()));

        new ViewModelListCellFactory<PreviewLayout>().withText(PreviewLayout::getName).install(chosen);
        new ViewModelListCellFactory<PreviewLayout>().withText(PreviewLayout::getName).install(available);
    }

    @Override
    public Node getBuilder() {
        return gridPane;
    }

    @Override
    public void setValues() {
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();

        chosenModel.clear();
        chosenModel.addAll(previewPreferences.getPreviewCycle());

        availableModel.clear();
        if (chosenModel.stream().noneMatch(layout -> layout instanceof TextBasedPreviewLayout)) {
            availableModel.add(previewPreferences.getTextBasedPreviewLayout());
        }

        BackgroundTask.wrap(CitationStyle::discoverCitationStyles)
                      .onSuccess(value -> value.stream()
                                               .map(CitationStylePreviewLayout::new)
                                               .filter(style -> !chosenModel.contains(style))
                                               .sorted(Comparator.comparing(PreviewLayout::getName))
                                               .forEach(availableModel::add))
                      .onFailure(ex -> {
                          LOGGER.error("something went wrong while adding the discovered CitationStyles to the list ", ex);
                          dialogService.showErrorDialogAndWait(Localization.lang("Error adding discovered CitationStyles"), ex);
                      })
                      .executeWith(taskExecutor);

        previewTextArea.setText(previewPreferences.getPreviewStyle().replace("__NEWLINE__", "\n"));
    }

    @Override
    public void storeSettings() {
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();

        if (chosenModel.isEmpty()) {
            chosenModel.add(previewPreferences.getTextBasedPreviewLayout());
        }

        PreviewPreferences newPreviewPreferences = Globals.prefs.getPreviewPreferences()
                                                                .getBuilder()
                                                                .withPreviewCycle(chosenModel)
                                                                .withPreviewStyle(previewTextArea.getText().replace("\n", "__NEWLINE__"))
                                                                .build();
        if (!chosen.getSelectionModel().isEmpty()) {
            newPreviewPreferences = newPreviewPreferences.getBuilder().withPreviewCyclePosition(chosen.getSelectionModel().getSelectedIndex()).build();
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
        return !chosenModel.isEmpty();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry preview");
    }

}
