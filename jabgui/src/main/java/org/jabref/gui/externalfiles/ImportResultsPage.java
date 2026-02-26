package org.jabref.gui.externalfiles;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;

public class ImportResultsPage extends WizardPane {

    private final UnlinkedFilesDialogViewModel viewModel;

    private ProgressIndicator progressIndicator;
    private Label progressLabel;
    private VBox progressPane;
    private VBox contentPane;

    private TableView<ImportFilesResultItemViewModel> resultsTable;
    private TableColumn<ImportFilesResultItemViewModel, JabRefIcon> colStatus;
    private TableColumn<ImportFilesResultItemViewModel, String> colFile;
    private TableColumn<ImportFilesResultItemViewModel, String> colMessage;

    private Label summaryLabel;
    private Button exportButton;

    public ImportResultsPage(UnlinkedFilesDialogViewModel viewModel) {
        this.viewModel = viewModel;

        setHeaderText(Localization.lang("Import results"));
        setupUI();
        setupBindings();
    }

    private void setupUI() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setMaxWidth(Double.MAX_VALUE);
        mainLayout.setMaxHeight(Double.MAX_VALUE);

        /// Progress pane
        progressPane = new VBox(10);
        progressPane.setStyle("-fx-alignment: center; -fx-padding: 20;");
        progressPane.setMaxWidth(Double.MAX_VALUE);
        progressPane.setMaxHeight(Double.MAX_VALUE);

        progressIndicator = new ProgressIndicator();
        progressIndicator.progressProperty().bind(viewModel.progressValueProperty());

        progressLabel = new Label();
        progressLabel.textProperty().bind(viewModel.progressTextProperty());

        progressPane.getChildren().addAll(progressIndicator, progressLabel);

        /// Content pane with proper sizing
        contentPane = new VBox(10);
        contentPane.setMaxWidth(Double.MAX_VALUE);
        contentPane.setMaxHeight(Double.MAX_VALUE);
        contentPane.setFillWidth(true);
        contentPane.setPrefSize(550, 450);

        summaryLabel = new Label();
        summaryLabel.setStyle("-fx-font-weight: bold;");

        /// TableView with explicit size constraints
        resultsTable = new TableView<>();
        resultsTable.setItems(viewModel.resultTableItems());
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultsTable.setMaxWidth(Double.MAX_VALUE);
        resultsTable.setMaxHeight(Double.MAX_VALUE);
        resultsTable.setPrefHeight(400);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);

        colStatus = new TableColumn<>(Localization.lang("Status"));
        colStatus.setCellValueFactory(cellData -> cellData.getValue().icon());
        colStatus.setCellFactory(new ValueTableCellFactory<ImportFilesResultItemViewModel, JabRefIcon>().withGraphic(JabRefIcon::getGraphicNode));
        colStatus.setResizable(true);
        colStatus.setPrefWidth(60);

        colFile = new TableColumn<>(Localization.lang("File"));
        colFile.setCellValueFactory(cellData -> cellData.getValue().file());
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>().withGraphic(this::createEllipsisLabel).withTooltip(item -> item).install(colFile);
        colFile.setResizable(true);

        colMessage = new TableColumn<>(Localization.lang("Message"));
        colMessage.setCellValueFactory(cellData -> cellData.getValue().message());
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>().withGraphic(this::createEllipsisLabel).withTooltip(item -> item).install(colMessage);
        colMessage.setResizable(true);

        resultsTable.getColumns().addAll(colStatus, colFile, colMessage);

        HBox buttonBar = new HBox();
        exportButton = new Button(Localization.lang("Export results"));
        exportButton.setOnAction(e -> viewModel.startExport());
        buttonBar.getChildren().add(exportButton);

        contentPane.getChildren().addAll(summaryLabel, resultsTable, buttonBar);

        mainLayout.setCenter(progressPane);
        setContent(mainLayout);
    }

    private void setupBindings() {
        progressPane.managedProperty().bind(viewModel.taskActiveProperty());
        progressPane.visibleProperty().bind(viewModel.taskActiveProperty());

        viewModel.resultTableItems().addListener((ListChangeListener<ImportFilesResultItemViewModel>) c -> {
            if (!viewModel.resultTableItems().isEmpty() && !viewModel.taskActiveProperty().get()) {
                Platform.runLater(() -> {
                    ((BorderPane) getContent()).setCenter(contentPane);
                    updateSummary();
                });
            }
        });

        exportButton.disableProperty().bind(Bindings.isEmpty(viewModel.resultTableItems()).or(viewModel.taskActiveProperty()));
    }

    private void updateSummary() {
        int totalCount = viewModel.resultTableItems().size();
        long successCount = viewModel.resultTableItems().stream().filter(ImportFilesResultItemViewModel::isSuccess).count();
        long failCount = totalCount - successCount;

        String summaryText = Localization.lang("Import completed: %0 successful, %1 failed", String.valueOf(successCount), String.valueOf(failCount));

        summaryLabel.setText(summaryText);
    }

    private Label createEllipsisLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        return label;
    }

    @Override
    public void onEnteringPage(Wizard wizard) {
        Platform.runLater(() -> {
            if (viewModel.resultTableItems().isEmpty()) {
                viewModel.startImport();
            }
        });
    }
}
