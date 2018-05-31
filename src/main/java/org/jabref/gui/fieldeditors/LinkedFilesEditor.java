package org.jabref.gui.fieldeditors;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.JabRefPreferences;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;

public class LinkedFilesEditor extends HBox implements FieldEditorFX {

    @FXML private final LinkedFilesEditorViewModel viewModel;
    @FXML private ListView<LinkedFileViewModel> listView;

    public LinkedFilesEditor(String fieldName, DialogService dialogService, BibDatabaseContext databaseContext, TaskExecutor taskExecutor, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, JabRefPreferences preferences) {
        this.viewModel = new LinkedFilesEditorViewModel(fieldName, suggestionProvider, dialogService, databaseContext, taskExecutor, fieldCheckers, preferences);

        ControlHelper.loadFXMLForControl(this);

        ViewModelListCellFactory<LinkedFileViewModel> cellFactory = new ViewModelListCellFactory<LinkedFileViewModel>()
                .withTooltip(LinkedFileViewModel::getDescription)
                .withGraphic(LinkedFilesEditor::createFileDisplay)
                .withContextMenu(this::createContextMenuForFile)
                .withOnMouseClickedEvent(this::handleItemMouseClick)
                .setOnDragDetected(this::handleOnDragDetected)
                .setOnDragDropped(this::handleOnDragDropped)
                .setOnDragOver(this::handleOnDragOver);

        listView.setCellFactory(cellFactory);

        setUpFilesDragAndDrop();
        Bindings.bindContentBidirectional(listView.itemsProperty().get(), viewModel.filesProperty());
        setUpKeyBindings();
    }

    private void setUpFilesDragAndDrop() {
        listView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY, TransferMode.LINK);
            }
        });

        listView.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            ObservableList<LinkedFileViewModel> items = listView.itemsProperty().get();

            if (dragboard.hasFiles()) {
                List<LinkedFileViewModel> linkedFiles = dragboard.getFiles().stream().map(File::toPath).map(viewModel::fromFile).collect(Collectors.toList());
                items.addAll(linkedFiles);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

    }

    private void handleOnDragOver(LinkedFileViewModel originalItem, DragEvent event) {
        if ((event.getGestureSource() != originalItem) && event.getDragboard().hasContent(DragAndDropDataFormats.LINKED_FILE)) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY, TransferMode.LINK);
        }
    }

    private void handleOnDragDetected(@SuppressWarnings("unused") LinkedFileViewModel linkedFile, MouseEvent event) {
        LinkedFile selectedItem = listView.getSelectionModel().getSelectedItem().getFile();
        if (selectedItem != null) {
            ClipboardContent content = new ClipboardContent();
            Dragboard dragboard = listView.startDragAndDrop(TransferMode.MOVE);
            //We have to use the model class here, as the content of the dragboard must be serializable
            content.put(DragAndDropDataFormats.LINKED_FILE, selectedItem);
            dragboard.setContent(content);
        }
        event.consume();
    }

    private void handleOnDragDropped(LinkedFileViewModel originalItem, DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;

        ObservableList<LinkedFileViewModel> items = listView.itemsProperty().get();

        if (dragboard.hasContent(DragAndDropDataFormats.LINKED_FILE)) {

            LinkedFile linkedFile = (LinkedFile) dragboard.getContent(DragAndDropDataFormats.LINKED_FILE);
            LinkedFileViewModel transferedItem = null;
            int draggedIdx = 0;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getFile().equals(linkedFile)) {
                    draggedIdx = i;
                    transferedItem = items.get(i);
                    break;
                }
            }
            int thisIdx = items.indexOf(originalItem);
            items.set(draggedIdx, originalItem);
            items.set(thisIdx, transferedItem);
            success = true;
        }
        if (dragboard.hasFiles()) {
            List<LinkedFileViewModel> linkedFiles = dragboard.getFiles().stream().map(File::toPath).map(viewModel::fromFile).collect(Collectors.toList());
            items.addAll(linkedFiles);
        }
        event.setDropCompleted(success);
        event.consume();

    }

    private static Node createFileDisplay(LinkedFileViewModel linkedFile) {
        Text icon = MaterialDesignIconFactory.get().createIcon(linkedFile.getTypeIcon());
        icon.setOnMouseClicked(event -> linkedFile.open());
        Text link = new Text();
        link.textProperty().bind(linkedFile.linkProperty());
        Text desc = new Text();
        desc.textProperty().bind(linkedFile.descriptionProperty());

        ProgressBar progressIndicator = new ProgressBar();
        progressIndicator.progressProperty().bind(linkedFile.downloadProgressProperty());
        progressIndicator.visibleProperty().bind(linkedFile.downloadOngoingProperty());

        Button acceptAutoLinkedFile = MaterialDesignIconFactory.get().createIconButton(MaterialDesignIcon.BRIEFCASE_CHECK);
        acceptAutoLinkedFile.setTooltip(new Tooltip(Localization.lang("This file was found automatically. Do you want to link it to this entry?")));
        acceptAutoLinkedFile.visibleProperty().bind(linkedFile.isAutomaticallyFoundProperty());
        acceptAutoLinkedFile.setOnAction(event -> linkedFile.acceptAsLinked());
        acceptAutoLinkedFile.getStyleClass().setAll("flatButton");

        Button writeXMPMetadata = MaterialDesignIconFactory.get().createIconButton(MaterialDesignIcon.IMPORT);
        writeXMPMetadata.setTooltip(new Tooltip(Localization.lang("Write BibTeXEntry as XMP-metadata to PDF.")));
        writeXMPMetadata.visibleProperty().bind(linkedFile.canWriteXMPMetadataProperty());
        writeXMPMetadata.setOnAction(event -> linkedFile.writeXMPMetadata());
        writeXMPMetadata.getStyleClass().setAll("flatButton");

        HBox container = new HBox(10);
        container.setPrefHeight(Double.NEGATIVE_INFINITY);

        if (desc.getText().isEmpty()) {
            container.getChildren().addAll(icon, link, progressIndicator, acceptAutoLinkedFile, writeXMPMetadata);
        } else {
            container.getChildren().addAll(icon, desc, link, progressIndicator, acceptAutoLinkedFile, writeXMPMetadata);
        }

        return container;
    }

    private void setUpKeyBindings() {
        listView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = Globals.getKeyPrefs().mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                switch (keyBinding.get()) {
                    case DELETE_ENTRY:
                        LinkedFileViewModel selectedItem = listView.getSelectionModel().getSelectedItem();
                        if (selectedItem != null) {
                            viewModel.deleteFile(selectedItem);
                        }
                        event.consume();
                        break;
                    default:
                        // Pass other keys to children
                }
            }
        });
    }

    public LinkedFilesEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @FXML
    private void addNewFile(ActionEvent event) {
        viewModel.addNewFile();
    }

    @FXML
    private void fetchFulltext(ActionEvent event) {
        viewModel.fetchFulltext();
    }

    @FXML
    private void addFromURL(ActionEvent event) {
        viewModel.addFromURL();
    }

    private ContextMenu createContextMenuForFile(LinkedFileViewModel linkedFile) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-font-size: " + Globals.prefs.getFontSizeFX() + "pt;");

        MenuItem edit = new MenuItem(Localization.lang("Edit"));
        edit.setOnAction(event -> linkedFile.edit());

        MenuItem openFile = new MenuItem(Localization.lang("Open"));
        openFile.setOnAction(event -> linkedFile.open());

        MenuItem openFolder = new MenuItem(Localization.lang("Open folder"));
        openFolder.setOnAction(event -> linkedFile.openFolder());

        MenuItem download = new MenuItem(Localization.lang("Download file"));
        download.setOnAction(event -> linkedFile.download());

        MenuItem renameFile = new MenuItem(Localization.lang("Rename file"));
        renameFile.setOnAction(event -> linkedFile.rename());
        renameFile.setDisable(linkedFile.getFile().isOnlineLink());

        MenuItem moveFile = new MenuItem(Localization.lang("Move file to file directory"));
        moveFile.setOnAction(event -> linkedFile.moveToDefaultDirectory());
        moveFile.setDisable(linkedFile.getFile().isOnlineLink());

        MenuItem deleteFile = new MenuItem(Localization.lang("Permanently delete local file"));
        deleteFile.setOnAction(event -> viewModel.deleteFile(linkedFile));
        deleteFile.setDisable(linkedFile.getFile().isOnlineLink());

        MenuItem deleteLink = new MenuItem(Localization.lang("Remove link"));
        deleteLink.setOnAction(event -> viewModel.removeFileLink(linkedFile));

        menu.getItems().add(edit);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().addAll(openFile, openFolder);
        menu.getItems().add(new SeparatorMenuItem());
        if (linkedFile.getFile().isOnlineLink()) {
            menu.getItems().add(download);
        }
        menu.getItems().addAll(renameFile, moveFile, deleteLink, deleteFile);

        return menu;
    }

    private void handleItemMouseClick(LinkedFileViewModel linkedFile, MouseEvent event) {

        if (event.getButton().equals(MouseButton.PRIMARY) && (event.getClickCount() == 2)) {
            // Double click -> edit
            linkedFile.edit();
        }
    }

    @Override
    public double getWeight() {
        return 2;
    }
}
