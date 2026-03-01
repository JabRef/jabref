package org.jabref.gui.externalfiles;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.logic.l10n.Localization;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;

public class FileSelectionPage extends WizardPane {

    private final UnlinkedFilesDialogViewModel viewModel;
    private final StateManager stateManager;
    private final BooleanProperty invalidProperty = new SimpleBooleanProperty(false);

    private CheckTreeView<FileNodeViewModel> unlinkedFilesList;
    private ProgressIndicator progressIndicator;
    private Label progressLabel;
    private Label fileCountLabel;
    private VBox progressPane;
    private VBox contentPane;

    private Button selectAllButton;
    private Button unselectAllButton;
    private Button expandAllButton;
    private Button collapseAllButton;
    private boolean nextButtonBound = false;

    public FileSelectionPage(StateManager stateManager, UnlinkedFilesDialogViewModel viewModel) {
        this.viewModel = viewModel;
        this.stateManager = stateManager;

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
        progressPane.setStyle("-fx-alignment: center; -fx-padding: 20;");

        progressIndicator = new ProgressIndicator();
        progressIndicator.progressProperty().bind(viewModel.progressValueProperty());

        progressLabel = new Label();
        progressLabel.textProperty().bind(viewModel.progressTextProperty());

        progressPane.getChildren().addAll(progressIndicator, progressLabel);

        contentPane = new VBox(10);

        fileCountLabel = new Label();
        fileCountLabel.setStyle("-fx-font-weight: bold;");

        unlinkedFilesList = new CheckTreeView<>();
        unlinkedFilesList.setCellFactory(_ -> new UnlinkedFilesCellFactory(stateManager, viewModel));

        unlinkedFilesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        unlinkedFilesList.setContextMenu(createContextMenu());
        VBox.setVgrow(unlinkedFilesList, Priority.ALWAYS);

        HBox buttonBar = new HBox(5);
        selectAllButton = new Button(Localization.lang("Select all"));
        selectAllButton.setOnAction(e -> unlinkedFilesList.getCheckModel().checkAll());

        unselectAllButton = new Button(Localization.lang("Unselect all"));
        unselectAllButton.setOnAction(e -> unlinkedFilesList.getCheckModel().clearChecks());

        expandAllButton = new Button(Localization.lang("Expand all"));
        expandAllButton.setOnAction(e -> expandTree(unlinkedFilesList.getRoot(), true));

        collapseAllButton = new Button(Localization.lang("Collapse all"));
        collapseAllButton.setOnAction(e -> expandTree(unlinkedFilesList.getRoot(), false));

        buttonBar.getChildren().addAll(selectAllButton, unselectAllButton, expandAllButton, collapseAllButton);

        contentPane.getChildren().addAll(fileCountLabel, unlinkedFilesList, buttonBar);

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

                ((BorderPane) getContent()).setCenter(contentPane);
            } else {
                EasyBind.bindContent(viewModel.checkedFileListProperty(), FXCollections.observableArrayList());
                ((BorderPane) getContent()).setCenter(progressPane);
            }
        });

        invalidProperty().bind(Bindings.isEmpty(viewModel.checkedFileListProperty()).or(viewModel.taskActiveProperty()));

        selectAllButton.disableProperty().bind(viewModel.taskActiveProperty());
        unselectAllButton.disableProperty().bind(viewModel.taskActiveProperty());
        expandAllButton.disableProperty().bind(viewModel.taskActiveProperty());
        collapseAllButton.disableProperty().bind(viewModel.taskActiveProperty());
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
