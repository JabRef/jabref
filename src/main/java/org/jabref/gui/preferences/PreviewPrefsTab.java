package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreviewPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPrefsTab implements PrefsTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewPrefsTab.class);

    private final ObservableList<Object> availableModel = FXCollections.observableArrayList();
    private final ObservableList<Object> chosenModel = FXCollections.observableArrayList();

    private final ListView<Object> available = new ListView<>(availableModel);
    private final ListView<Object> chosen = new ListView<>(chosenModel);

    private final Button btnRight = new Button("»");
    private final Button btnLeft = new Button("«");
    private final Button btnUp = new Button(Localization.lang("Up"));
    private final Button btnDown = new Button(Localization.lang("Down"));
    private final GridPane gridPane = new GridPane();
    private final TextArea layout = new TextArea();
    private final Button btnTest = new Button(Localization.lang("Test"));
    private final Button btnDefault = new Button(Localization.lang("Default"));
    private final ScrollPane scrollPane = new ScrollPane(layout);
    private final DialogService dialogService;
    private final ExternalFileTypes externalFileTypes;
    private final TaskExecutor taskExecutor;

    public PreviewPrefsTab(DialogService dialogService, ExternalFileTypes externalFileTypes, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.externalFileTypes = externalFileTypes;
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
            for (Object object : available.getSelectionModel().getSelectedItems()) {
                availableModel.remove(object);
                chosenModel.add(object);
            }
            storeSettings();
        });

        btnLeft.setOnAction(event -> {
            for (Object object : chosen.getSelectionModel().getSelectedItems()) {
                availableModel.add(object);
                chosenModel.remove(object);
            }
            storeSettings();
        });

        btnUp.setOnAction(event -> {
            List<Integer> newSelectedIndices = new ArrayList<>();
            for (int oldIndex : chosen.getSelectionModel().getSelectedIndices()) {
                boolean alreadyTaken = newSelectedIndices.contains(oldIndex - 1);
                int newIndex = ((oldIndex > 0) && !alreadyTaken) ? oldIndex - 1 : oldIndex;
                chosenModel.add(newIndex, chosenModel.remove(oldIndex));
                chosen.getSelectionModel().select(newIndex);
            }
            storeSettings();
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
            storeSettings();
        });

        btnDefault.setOnAction(event -> layout.setText(Globals.prefs.getPreviewPreferences()
                                                                    .getPreviewStyleDefault()
                                                                    .replace("__NEWLINE__", "\n")));

        btnTest.setOnAction(event -> {
            try {

                PreviewPanel testPane = new PreviewPanel(null, new BibDatabaseContext(), Globals.getKeyPrefs(), Globals.prefs.getPreviewPreferences(), dialogService, externalFileTypes);
                if (chosen.getSelectionModel().getSelectedItems().isEmpty()) {
                    testPane.setFixedLayout(layout.getText());
                    testPane.setEntry(TestEntry.getTestEntry());
                } else {
                    int indexStyle = chosen.getSelectionModel().getSelectedIndex();
                    PreviewPreferences preferences = Globals.prefs.getPreviewPreferences();
                    preferences = new PreviewPreferences(preferences.getPreviewCycle(), indexStyle, preferences.getPreviewPanelDividerPosition(), preferences.isPreviewPanelEnabled(), preferences.getPreviewStyle(), preferences.getPreviewStyleDefault());

                    testPane = new PreviewPanel(JabRefGUI.getMainFrame().getCurrentBasePanel(), new BibDatabaseContext(), Globals.getKeyPrefs(), preferences, dialogService, externalFileTypes);
                    testPane.setEntry(TestEntry.getTestEntry());
                    testPane.updateLayout(preferences);
                }

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
        layout.setPrefSize(600, 300);
        gridPane.add(scrollPane, 1, 9);
    }

    @Override
    public Node getBuilder() {
        return gridPane;
    }

    @Override
    public void setValues() {
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();

        chosenModel.clear();
        boolean isPreviewChosen = false;
        for (String style : previewPreferences.getPreviewCycle()) {
            // in case the style is not a valid citation style file, an empty Optional is returned
            Optional<CitationStyle> citationStyle = CitationStyle.createCitationStyleFromFile(style);
            if (citationStyle.isPresent()) {
                chosenModel.add(citationStyle.get());
            } else {
                if (isPreviewChosen) {
                    LOGGER.error("Preview is already in the list, something went wrong");
                    continue;
                }
                isPreviewChosen = true;
                chosenModel.add(Localization.lang("Preview"));
            }
        }

        availableModel.clear();
        if (!isPreviewChosen) {
            availableModel.add(Localization.lang("Preview"));
        }

        BackgroundTask.wrap(() -> CitationStyle.discoverCitationStyles())
                      .onSuccess(value -> {
                          value.stream()
                               .filter(style -> !previewPreferences.getPreviewCycle().contains(style.getFilePath()))
                               .sorted(Comparator.comparing(CitationStyle::getTitle))
                               .forEach(availableModel::add);
                      })
                      .onFailure(ex -> {
                          LOGGER.error("something went wrong while adding the discovered CitationStyles to the list ", ex);
                          dialogService.showErrorDialogAndWait(Localization.lang("Error adding discovered CitationStyles"), ex);
                      }).executeWith(taskExecutor);

        layout.setText(Globals.prefs.getPreviewPreferences().getPreviewStyle().replace("__NEWLINE__", "\n"));
    }

    @Override
    public void storeSettings() {
        List<String> styles = new ArrayList<>();

        if (chosenModel.isEmpty()) {
            chosenModel.add(Localization.lang("Preview"));
        }
        for (Object obj : chosenModel) {
            if (obj instanceof CitationStyle) {
                styles.add(((CitationStyle) obj).getFilePath());
            } else if (obj instanceof String) {
                styles.add("Preview");
            }
        }
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences()
                                                             .getBuilder()
                                                             .withPreviewCycle(styles)
                                                             .withPreviewStyle(layout.getText().replace("\n", "__NEWLINE__"))
                                                             .build();
        if (!chosen.getSelectionModel().isEmpty()) {
            previewPreferences = previewPreferences.getBuilder().withPreviewCyclePosition(chosen.getSelectionModel().getSelectedIndex()).build();
        }
        Globals.prefs.storePreviewPreferences(previewPreferences);

        // update preview
        for (BasePanel basePanel : JabRefGUI.getMainFrame().getBasePanelList()) {
            basePanel.getPreviewPanel().updateLayout(Globals.prefs.getPreviewPreferences());
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
