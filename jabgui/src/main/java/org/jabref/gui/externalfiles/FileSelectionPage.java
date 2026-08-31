package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.documentviewer.PdfDocumentViewer;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSelectionPage extends WizardPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSelectionPage.class);

    private static final int PREVIEW_REFRESH_DELAY = 300;

    private static final List<Field> METADATA_PRIORITIZED_FIELDS = List.of(
            StandardField.AUTHOR,
            StandardField.TITLE,
            StandardField.YEAR,
            StandardField.JOURNAL,
            StandardField.JOURNALTITLE,
            StandardField.BOOKTITLE,
            StandardField.DOI,
            StandardField.URL);

    private final UnlinkedFilesDialogViewModel viewModel;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final ImportFormatPreferences importFormatPreferences;
    private final BooleanProperty invalidProperty = new SimpleBooleanProperty(false);
    private final DelayTaskThrottler previewThrottler;

    private @Nullable BackgroundTask<?> currentMetadataTask;

    private CheckTreeView<FileNodeViewModel> unlinkedFilesList;
    private Label fileCountLabel;
    private VBox progressPane;
    private VBox contentPane;
    private CheckBox enablePreviewCheckBox;
    private TextArea metadataPreview;
    private PdfDocumentViewer pdfPreview;
    private SplitPane splitPane;
    private TitledPane previewPane;

    private Button selectAllButton;
    private Button unselectAllButton;
    private Button expandAllButton;
    private Button collapseAllButton;
    private Button showPreviewButton;
    private boolean nextButtonBound = false;

    public FileSelectionPage(StateManager stateManager,
                             UnlinkedFilesDialogViewModel viewModel,
                             ImportFormatPreferences importFormatPreferences,
                             TaskExecutor taskExecutor) {
        this.viewModel = viewModel;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.importFormatPreferences = importFormatPreferences;
        this.previewThrottler = taskExecutor.createThrottler(PREVIEW_REFRESH_DELAY);

        setHeaderText(Localization.lang("Select files to import"));
        setGraphic(null);
        setupUI();
        setupBindings();
    }

    public BooleanProperty invalidProperty() {
        return invalidProperty;
    }

    private void setupUI() {
        BorderPane mainLayout = new BorderPane();

        progressPane = new VBox(10);
        progressPane.getStyleClass().addAll("align-center", "padding-20");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.progressProperty().bind(viewModel.progressValueProperty());

        Label progressLabel = new Label();
        progressLabel.textProperty().bind(viewModel.progressTextProperty());

        progressPane.getChildren().addAll(progressIndicator, progressLabel);

        contentPane = new VBox(10);

        fileCountLabel = new Label();
        fileCountLabel.getStyleClass().add("bold");

        unlinkedFilesList = new CheckTreeView<>();
        unlinkedFilesList.setCellFactory(_ -> new UnlinkedFilesCellFactory(stateManager, viewModel));

        unlinkedFilesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        unlinkedFilesList.setContextMenu(createContextMenu());
        VBox.setVgrow(unlinkedFilesList, Priority.ALWAYS);

        VBox treePane = new VBox(unlinkedFilesList);

        enablePreviewCheckBox = new CheckBox(Localization.lang("Enable preview"));
        enablePreviewCheckBox.setSelected(false);

        pdfPreview = new PdfDocumentViewer();
        VBox.setVgrow(pdfPreview, Priority.ALWAYS);

        Label metadataLabel = new Label(Localization.lang("Extracted metadata"));
        metadataPreview = new TextArea();
        metadataPreview.setEditable(false);
        metadataPreview.setWrapText(true);
        metadataPreview.setPrefRowCount(8);
        metadataPreview.setText(Localization.lang("Preview disabled"));

        Button closePreviewButton = new Button();
        closePreviewButton.setGraphic(IconTheme.JabRefIcons.CLOSE.getGraphicNode());
        closePreviewButton.getStyleClass().add("icon-button");
        closePreviewButton.setTooltip(new Tooltip(Localization.lang("Close PDF preview")));
        closePreviewButton.setOnAction(_ -> hidePreviewPane());

        HBox previewControls = new HBox(8, enablePreviewCheckBox, closePreviewButton);
        HBox.setHgrow(enablePreviewCheckBox, Priority.ALWAYS);

        VBox previewContent = new VBox(8, previewControls, pdfPreview, metadataLabel, metadataPreview);
        previewContent.setPadding(new Insets(8));
        previewPane = new TitledPane(Localization.lang("PDF preview"), previewContent);
        previewPane.setExpanded(true);
        previewPane.setCollapsible(false);

        splitPane = new SplitPane(treePane, previewPane);
        splitPane.setDividerPositions(0.58);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        HBox buttonBar = new HBox(5);
        selectAllButton = new Button(Localization.lang("Select all"));
        selectAllButton.setOnAction(e -> unlinkedFilesList.getCheckModel().checkAll());

        unselectAllButton = new Button(Localization.lang("Unselect all"));
        unselectAllButton.setOnAction(e -> unlinkedFilesList.getCheckModel().clearChecks());

        expandAllButton = new Button(Localization.lang("Expand all"));
        expandAllButton.setOnAction(e -> expandTree(unlinkedFilesList.getRoot(), true));

        collapseAllButton = new Button(Localization.lang("Collapse all"));
        collapseAllButton.setOnAction(e -> expandTree(unlinkedFilesList.getRoot(), false));

        showPreviewButton = new Button(Localization.lang("Show PDF preview"));
        showPreviewButton.setManaged(false);
        showPreviewButton.setVisible(false);
        showPreviewButton.setOnAction(_ -> showPreviewPane());

        buttonBar.getChildren().addAll(selectAllButton, unselectAllButton, expandAllButton, collapseAllButton, showPreviewButton);

        contentPane.getChildren().addAll(fileCountLabel, splitPane, buttonBar);

        mainLayout.setCenter(progressPane);
        setContent(mainLayout);
    }

    private void setupBindings() {
        progressPane.managedProperty().bind(viewModel.taskActiveProperty());
        progressPane.visibleProperty().bind(viewModel.taskActiveProperty());

        unlinkedFilesList.rootProperty().bind(EasyBind.map(viewModel.treeRootProperty(), fileNode -> fileNode.map(fileNodeViewModel -> new RecursiveTreeItem<>(fileNodeViewModel, FileNodeViewModel::getChildren)).orElse(null)));

        EasyBind.subscribe(unlinkedFilesList.rootProperty(), root -> {
            if (root != null) {
                ((CheckBoxTreeItem<FileNodeViewModel>) root).setSelected(true);
                root.setExpanded(true);

                EasyBind.bindContent(viewModel.checkedFileListProperty(), unlinkedFilesList.getCheckModel().getCheckedItems());

                updateFileCount(root);
                refreshPreviewForCurrentSelection();

                ((BorderPane) getContent()).setCenter(contentPane);
            } else {
                EasyBind.bindContent(viewModel.checkedFileListProperty(), FXCollections.observableArrayList());
                showPreviewDisabledState(Localization.lang("Select a PDF file to preview"));
                ((BorderPane) getContent()).setCenter(progressPane);
            }
        });

        unlinkedFilesList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                previewThrottler.schedule(() -> UiTaskExecutor.runNowOrInJavaFXThread(this::refreshPreviewForCurrentSelection)));
        enablePreviewCheckBox.selectedProperty().addListener((observable, oldValue, enabled) -> refreshPreviewForCurrentSelection());

        invalidProperty().bind(Bindings.isEmpty(viewModel.checkedFileListProperty()).or(viewModel.taskActiveProperty()));

        selectAllButton.disableProperty().bind(viewModel.taskActiveProperty());
        unselectAllButton.disableProperty().bind(viewModel.taskActiveProperty());
        expandAllButton.disableProperty().bind(viewModel.taskActiveProperty());
        collapseAllButton.disableProperty().bind(viewModel.taskActiveProperty());
    }

    private void refreshPreviewForCurrentSelection() {
        if (!isPreviewPaneVisible()) {
            return;
        }

        if (!isPreviewActive()) {
            showPreviewDisabledState(Localization.lang("Preview disabled"));
            return;
        }

        TreeItem<FileNodeViewModel> selectedItem = unlinkedFilesList.getSelectionModel().getSelectedItem();
        if ((selectedItem == null) || (selectedItem.getValue() == null)) {
            showPreviewDisabledState(Localization.lang("Select a PDF file to preview"));
            return;
        }

        Path selectedPath = selectedItem.getValue().getPath();
        if (!Files.isRegularFile(selectedPath) || !FileUtil.isPDFFile(selectedPath)) {
            showPreviewDisabledState(Localization.lang("Select a PDF file to preview"));
            return;
        }

        pdfPreview.show(selectedPath);

        cancelCurrentMetadataTask();
        metadataPreview.setText(Localization.lang("Loading metadata..."));

        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> new PdfMergeMetadataImporter(importFormatPreferences).importDatabase(selectedPath));
        currentMetadataTask = task;
        task.onSuccess(result -> {
            if (currentMetadataTask != task) {
                return;
            }
            metadataPreview.setText(formatParserResult(result));
        });
        // importDatabase converts expected exceptions into a ParserResult; this only catches unexpected runtime errors
        task.onFailure(exception -> {
            if (currentMetadataTask != task) {
                return;
            }
            LOGGER.warn("Could not extract PDF metadata for {}", selectedPath, exception);
            metadataPreview.setText(Localization.lang("Could not extract Metadata from: %0", selectedPath.getFileName().toString()));
        });
        task.executeWith(taskExecutor);
    }

    private boolean isPreviewActive() {
        return enablePreviewCheckBox.isSelected();
    }

    private boolean isPreviewPaneVisible() {
        return splitPane.getItems().contains(previewPane);
    }

    /// [impl->req~jabgui.externalfiles.unlinked-files.preview.close~1]
    private void hidePreviewPane() {
        if (!splitPane.getItems().remove(previewPane)) {
            return;
        }

        showPreviewButton.setManaged(true);
        showPreviewButton.setVisible(true);
        showPreviewDisabledState(Localization.lang("Preview disabled"));
    }

    private void showPreviewPane() {
        if (isPreviewPaneVisible()) {
            return;
        }

        splitPane.getItems().add(previewPane);
        splitPane.setDividerPositions(0.58);
        showPreviewButton.setManaged(false);
        showPreviewButton.setVisible(false);
        refreshPreviewForCurrentSelection();
    }

    private void showPreviewDisabledState(String metadataText) {
        previewThrottler.cancel();
        cancelCurrentMetadataTask();
        pdfPreview.show(null);
        metadataPreview.setText(metadataText);
    }

    private void updateFileCount(TreeItem<FileNodeViewModel> root) {
        if (root != null && root.getValue() != null) {
            int fileCount = root.getValue().getFileCount();
            fileCountLabel.setText(Localization.lang("Found %0 file(s)", String.valueOf(fileCount)));
        }
    }

    private void expandTree(TreeItem<?> item, boolean expand) {
        if ((item != null) && !item.isLeaf()) {
            item.setExpanded(expand);
            for (TreeItem<?> child : item.getChildren()) {
                expandTree(child, expand);
            }
        }
    }

    private ContextMenu createContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory();

        contextMenu.getItems().add(factory.createMenuItem(StandardActions.SELECT_ALL, new TreeContextAction(StandardActions.SELECT_ALL)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.UNSELECT_ALL, new TreeContextAction(StandardActions.UNSELECT_ALL)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.EXPAND_ALL, new TreeContextAction(StandardActions.EXPAND_ALL)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.COLLAPSE_ALL, new TreeContextAction(StandardActions.COLLAPSE_ALL)));

        return contextMenu;
    }

    private void cancelCurrentMetadataTask() {
        if (currentMetadataTask != null) {
            currentMetadataTask.cancel();
            currentMetadataTask = null;
        }
    }

    /// Cancels any in-flight preview work.
    public void cancelPreviewTasks() {
        showPreviewDisabledState(Localization.lang("Preview disabled"));
    }

    /// Cancels in-flight preview work and stops the preview throttler.
    public void shutdown() {
        cancelPreviewTasks();
        previewThrottler.shutdown();
    }

    private String formatParserResult(ParserResult result) {
        if (result.getDatabase().hasEntries()) {
            return formatBibEntry(result.getDatabase().getEntries().getFirst());
        }
        if (result.isInvalid()) {
            return result.getErrorMessage();
        }
        return Localization.lang("No extracted metadata available.");
    }

    private static int priorityIndex(Field field) {
        int index = METADATA_PRIORITIZED_FIELDS.indexOf(field);
        return index >= 0 ? index : METADATA_PRIORITIZED_FIELDS.size();
    }

    private String formatBibEntry(BibEntry entry) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add(Localization.lang("Type: %0", entry.getType().getDisplayName()));
        Comparator<Field> byDisplayOrder = Comparator.comparingInt(FileSelectionPage::priorityIndex)
                                                     .thenComparing(Field::getName);
        entry.getFieldMap().entrySet().stream()
             .filter(field -> !field.getKey().equals(StandardField.FILE))
             .sorted(Map.Entry.comparingByKey(byDisplayOrder))
             .forEach(field -> {
                 String value = field.getValue();
                 if (value != null && !value.isBlank()) {
                     joiner.add(field.getKey().getName() + ": " + value);
                 }
             });
        return joiner.toString();
    }

    @Override
    public void onEnteringPage(Wizard wizard) {
        // Start search if not already done
        if (viewModel.treeRootProperty().get().isEmpty()) {
            ((BorderPane) getContent()).setCenter(progressPane);
            viewModel.startSearch();
        }

        // Bind Next button only once
        if (!nextButtonBound) {
            Platform.runLater(() -> {
                Node nextButton = this.lookupButton(ButtonType.NEXT);
                if (nextButton != null) {
                    nextButton.disableProperty().bind(invalidProperty());
                    nextButtonBound = true;
                }
            });
        }
    }

    @Override
    public void onExitingPage(Wizard wizard) {
        cancelPreviewTasks();
    }

    private class TreeContextAction extends SimpleCommand {
        private final StandardActions command;

        public TreeContextAction(StandardActions command) {
            this.command = command;
            this.executable.bind(unlinkedFilesList.rootProperty().isNotNull());
        }

        @Override
        public void execute() {
            switch (command) {
                case SELECT_ALL ->
                        unlinkedFilesList.getCheckModel().checkAll();
                case UNSELECT_ALL ->
                        unlinkedFilesList.getCheckModel().clearChecks();
                case EXPAND_ALL ->
                        expandTree(unlinkedFilesList.getRoot(), true);
                case COLLAPSE_ALL ->
                        expandTree(unlinkedFilesList.getRoot(), false);
            }
        }
    }
}
