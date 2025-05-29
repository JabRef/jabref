package org.jabref.http.server.cayw.gui;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.jabref.logic.l10n.Localization;

public class SearchDialog<T> {

    private final ObservableList<CAYWEntry<T>> selectedItems = FXCollections.observableArrayList();
    private Stage dialogStage;

    public SearchDialog() {
    }

    public List<T> show(Function<String, List<T>> searchFunction, List<CAYWEntry<T>> entries) {
        FilteredList<CAYWEntry<T>> searchResults = new FilteredList<>(FXCollections.observableArrayList(entries));
        selectedItems.clear();

        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setTitle(Localization.lang("Search..."));
        dialogStage.setResizable(false);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double dialogWidth = screenBounds.getWidth() * 0.5;
        double dialogHeight = screenBounds.getHeight() * 0.4;

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        SearchField<T> searchField = new SearchField<>(searchResults, searchFunction);
        searchField.setMaxWidth(Double.MAX_VALUE);

        SearchResultContainer<T> resultContainer = new SearchResultContainer<>(searchResults, selectedItems);
        resultContainer.setPrefHeight(150);

        ScrollPane scrollPane = new ScrollPane(resultContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        SelectedItemsContainer<T> selectedContainer = new SelectedItemsContainer<>(selectedItems);

        Button finishButton = new Button(Localization.lang("Finish Search"));
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

        dialogStage.setX((screenBounds.getWidth() - dialogWidth) / 2);
        dialogStage.setY((screenBounds.getHeight() - dialogHeight) / 2);

        dialogStage.showAndWait();

        return selectedItems.stream().map(CAYWEntry::getValue).collect(Collectors.toList());
    }

    public void close() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
