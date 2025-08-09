package org.jabref.http.server.cayw.gui;

import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchDialog {

    public static final Logger LOGGER = LoggerFactory.getLogger(SearchDialog.class);

    private static final double DIALOG_WIDTH_RATIO = 0.5;
    private static final double DIALOG_HEIGHT_RATIO = 0.4;
    private static final int PREF_HEIGHT = 150;

    private final ObservableList<CAYWEntry> selectedItems = FXCollections.observableArrayList();

    private Stage dialogStage;

    public List<CAYWEntry> show(Function<String, List<CAYWEntry>> searchFunction, List<CAYWEntry> entries) {
        FilteredList<CAYWEntry> searchResults = new FilteredList<>(FXCollections.observableArrayList(entries));
        selectedItems.clear();

        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setTitle(Localization.lang("%0 | Cite As You Write", "JabRef"));
        dialogStage.setResizable(true);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double dialogWidth = screenBounds.getWidth() * DIALOG_WIDTH_RATIO;
        double dialogHeight = screenBounds.getHeight() * DIALOG_HEIGHT_RATIO;

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        SearchField searchField = new SearchField(searchResults, searchFunction);
        searchField.setMaxWidth(Double.MAX_VALUE);

        SearchResultContainer resultContainer = new SearchResultContainer(searchResults, selectedItems);
        resultContainer.setPrefHeight(PREF_HEIGHT);

        ScrollPane scrollPane = new ScrollPane(resultContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        SelectedItemsContainer selectedContainer = new SelectedItemsContainer(selectedItems);

        Button finishButton = new Button(Localization.lang("Cite"));
        finishButton.setOnAction(event -> {
            dialogStage.close();
        });

        mainLayout.getChildren().addAll(
                searchField,
                selectedContainer,
                scrollPane,
                finishButton
        );

        Scene scene = new Scene(mainLayout, dialogWidth, dialogHeight);

        scene.getStylesheets().add("cayw.css");
        mainLayout.getStyleClass().add("search-dialog");
        scrollPane.getStyleClass().add("scroll-pane");

        dialogStage.setScene(scene);

        try (InputStream inputStream = getClass().getResourceAsStream("/JabRef-icon-64.png")) {
            if (inputStream == null) {
                LOGGER.warn("Error loading icon for SearchDialog");
            } else {
                Image icon = new Image(inputStream);
                dialogStage.getIcons().add(icon);
            }
        } catch (Exception e) {
            LOGGER.warn("Error loading icon for SearchDialog", e);
        }

        dialogStage.setX((screenBounds.getWidth() - dialogWidth) / 2);
        dialogStage.setY((screenBounds.getHeight() - dialogHeight) / 2);

        dialogStage.setAlwaysOnTop(true);
        dialogStage.showAndWait();

        return selectedItems;
    }

    public void close() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
