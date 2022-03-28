package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
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
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.copyfiles.CopySingleFileAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.GrobidOptInDialogHelper;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.uithreadaware.UiThreadObservableList;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;

public class LinkedFilesEditor extends HBox implements FieldEditorFX {

    @FXML private final LinkedFilesEditorViewModel viewModel;
    @FXML private ListView<LinkedFileViewModel> listView;

    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final UiThreadObservableList<LinkedFileViewModel> decoratedModelList;
    private final PreferencesService preferencesService;

    private ObservableOptionalValue<BibEntry> bibEntry = EasyBind.wrapNullable(new SimpleObjectProperty<>());

    public LinkedFilesEditor(Field field,
                             DialogService dialogService,
                             BibDatabaseContext databaseContext,
                             TaskExecutor taskExecutor,
                             SuggestionProvider<?> suggestionProvider,
                             FieldCheckers fieldCheckers,
                             PreferencesService preferences) {
        this.viewModel = new LinkedFilesEditorViewModel(field, suggestionProvider, dialogService, databaseContext, taskExecutor, fieldCheckers, preferences);
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.preferencesService = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();

        ViewModelListCellFactory<LinkedFileViewModel> cellFactory = new ViewModelListCellFactory<LinkedFileViewModel>()
                .withStringTooltip(LinkedFileViewModel::getDescription)
                .withGraphic(this::createFileDisplay)
                .withContextMenu(this::createContextMenuForFile)
                .withOnMouseClickedEvent(this::handleItemMouseClick)
                .setOnDragDetected(this::handleOnDragDetected)
                .setOnDragDropped(this::handleOnDragDropped)
                .setOnDragOver(this::handleOnDragOver)
                .withValidation(LinkedFileViewModel::fileExistsValidationStatus);

        listView.setCellFactory(cellFactory);

        decoratedModelList = new UiThreadObservableList<>(viewModel.filesProperty());
        Bindings.bindContentBidirectional(listView.itemsProperty().get(), decoratedModelList);
        setUpKeyBindings();
    }

    private void handleOnDragOver(LinkedFileViewModel originalItem, DragEvent event) {
        if ((event.getGestureSource() != originalItem) && event.getDragboard().hasContent(DragAndDropDataFormats.LINKED_FILE)) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
    }

    private void handleOnDragDetected(@SuppressWarnings("unused") LinkedFileViewModel linkedFile, MouseEvent event) {
        LinkedFile selectedItem = listView.getSelectionModel().getSelectedItem().getFile();
        if (selectedItem != null) {
            ClipboardContent content = new ClipboardContent();
            Dragboard dragboard = listView.startDragAndDrop(TransferMode.MOVE);
            // We have to use the model class here, as the content of the dragboard must be serializable
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

        event.setDropCompleted(success);
        event.consume();
    }

    private Node createFileDisplay(LinkedFileViewModel linkedFile) {
        PseudoClass opacity = PseudoClass.getPseudoClass("opacity");

        Node icon = linkedFile.getTypeIcon().getGraphicNode();
        icon.setOnMouseClicked(event -> linkedFile.open());

        Text link = new Text();
        link.textProperty().bind(linkedFile.linkProperty());
        link.getStyleClass().setAll("file-row-text");
        EasyBind.subscribe(linkedFile.isAutomaticallyFoundProperty(), found -> link.pseudoClassStateChanged(opacity, found));

        Text desc = new Text();
        desc.textProperty().bind(linkedFile.descriptionProperty());
        desc.getStyleClass().setAll("file-row-text");

        ProgressBar progressIndicator = new ProgressBar();
        progressIndicator.progressProperty().bind(linkedFile.downloadProgressProperty());
        progressIndicator.visibleProperty().bind(linkedFile.downloadOngoingProperty());

        HBox info = new HBox(8);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setStyle("-fx-padding: 0.5em 0 0.5em 0;"); // To align with buttons below which also have 0.5em padding
        info.getChildren().setAll(icon, link, desc, progressIndicator);

        Button acceptAutoLinkedFile = IconTheme.JabRefIcons.AUTO_LINKED_FILE.asButton();
        acceptAutoLinkedFile.setTooltip(new Tooltip(Localization.lang("This file was found automatically. Do you want to link it to this entry?")));
        acceptAutoLinkedFile.visibleProperty().bind(linkedFile.isAutomaticallyFoundProperty());
        acceptAutoLinkedFile.managedProperty().bind(linkedFile.isAutomaticallyFoundProperty());
        acceptAutoLinkedFile.setOnAction(event -> linkedFile.acceptAsLinked());
        acceptAutoLinkedFile.getStyleClass().setAll("icon-button");

        Button writeMetadataToPdf = IconTheme.JabRefIcons.IMPORT.asButton();
        writeMetadataToPdf.setTooltip(new Tooltip(Localization.lang("Write BibTeXEntry metadata to PDF.")));
        writeMetadataToPdf.visibleProperty().bind(linkedFile.isOfflinePdfProperty());
        writeMetadataToPdf.setOnAction(event -> linkedFile.writeMetadataToPdf());
        writeMetadataToPdf.getStyleClass().setAll("icon-button");

        Button parsePdfMetadata = IconTheme.JabRefIcons.FILE_SEARCH.asButton();
        parsePdfMetadata.setTooltip(new Tooltip(Localization.lang("Parse Metadata from PDF.")));
        parsePdfMetadata.visibleProperty().bind(linkedFile.isOfflinePdfProperty());
        parsePdfMetadata.setOnAction(event -> {
            GrobidOptInDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferencesService.getImporterPreferences());
            linkedFile.parsePdfMetadataAndShowMergeDialog();
        });
        parsePdfMetadata.getStyleClass().setAll("icon-button");

        HBox container = new HBox(10);
        container.setPrefHeight(Double.NEGATIVE_INFINITY);

        container.getChildren().addAll(acceptAutoLinkedFile, info, writeMetadataToPdf, parsePdfMetadata);

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
        bibEntry = EasyBind.wrapNullable(new SimpleObjectProperty<>(entry));
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @FXML
    private void addNewFile() {
        viewModel.addNewFile();
    }

    @FXML
    private void fetchFulltext() {
        viewModel.fetchFulltext();
    }

    @FXML
    private void addFromURL() {
        viewModel.addFromURL();
    }

    private void handleItemMouseClick(LinkedFileViewModel linkedFile, MouseEvent event) {

        if (event.getButton().equals(MouseButton.PRIMARY) && (event.getClickCount() == 2)) {
            // Double click -> open
            linkedFile.open();
        }
    }

    @Override
    public double getWeight() {
        return 2;
    }

    private ContextMenu createContextMenuForFile(LinkedFileViewModel linkedFile) {
        ContextMenu menu = new ContextMenu();
        ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

        menu.getItems().addAll(
                factory.createMenuItem(StandardActions.EDIT_FILE_LINK, new ContextAction(StandardActions.EDIT_FILE_LINK, linkedFile, preferencesService)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.OPEN_FILE, new ContextAction(StandardActions.OPEN_FILE, linkedFile, preferencesService)),
                factory.createMenuItem(StandardActions.OPEN_FOLDER, new ContextAction(StandardActions.OPEN_FOLDER, linkedFile, preferencesService)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.DOWNLOAD_FILE, new ContextAction(StandardActions.DOWNLOAD_FILE, linkedFile, preferencesService)),
                factory.createMenuItem(StandardActions.RENAME_FILE_TO_PATTERN, new ContextAction(StandardActions.RENAME_FILE_TO_PATTERN, linkedFile, preferencesService)),
                factory.createMenuItem(StandardActions.RENAME_FILE_TO_NAME, new ContextAction(StandardActions.RENAME_FILE_TO_NAME, linkedFile, preferencesService)),
                factory.createMenuItem(StandardActions.MOVE_FILE_TO_FOLDER, new ContextAction(StandardActions.MOVE_FILE_TO_FOLDER, linkedFile, preferencesService)),
                factory.createMenuItem(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, new ContextAction(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, linkedFile, preferencesService)),
                factory.createMenuItem(StandardActions.COPY_FILE_TO_FOLDER, new CopySingleFileAction(linkedFile.getFile(), dialogService, databaseContext, preferencesService.getFilePreferences())),
                factory.createMenuItem(StandardActions.REMOVE_LINK, new ContextAction(StandardActions.REMOVE_LINK, linkedFile, preferencesService)),
                factory.createMenuItem(StandardActions.DELETE_FILE, new ContextAction(StandardActions.DELETE_FILE, linkedFile, preferencesService))
        );

        return menu;
    }

    private class ContextAction extends SimpleCommand {

        private final StandardActions command;
        private final LinkedFileViewModel linkedFile;

        public ContextAction(StandardActions command, LinkedFileViewModel linkedFile, PreferencesService preferencesService) {
            this.command = command;
            this.linkedFile = linkedFile;

            this.executable.bind(
                    switch (command) {
                        case RENAME_FILE_TO_PATTERN -> Bindings.createBooleanBinding(
                                () -> !linkedFile.getFile().isOnlineLink()
                                        && linkedFile.getFile().findIn(databaseContext, preferencesService.getFilePreferences()).isPresent()
                                        && !linkedFile.isGeneratedNameSameAsOriginal(),
                                linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                        case MOVE_FILE_TO_FOLDER, MOVE_FILE_TO_FOLDER_AND_RENAME -> Bindings.createBooleanBinding(
                                () -> !linkedFile.getFile().isOnlineLink()
                                        && linkedFile.getFile().findIn(databaseContext, preferencesService.getFilePreferences()).isPresent()
                                        && !linkedFile.isGeneratedPathSameAsOriginal(),
                                linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                        case DOWNLOAD_FILE -> Bindings.createBooleanBinding(
                                () -> linkedFile.getFile().isOnlineLink(),
                                linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                        case OPEN_FILE, OPEN_FOLDER, RENAME_FILE_TO_NAME, DELETE_FILE -> Bindings.createBooleanBinding(
                                () -> !linkedFile.getFile().isOnlineLink()
                                        && linkedFile.getFile().findIn(databaseContext, preferencesService.getFilePreferences()).isPresent(),
                                linkedFile.getFile().linkProperty(), bibEntry.getValue().map(BibEntry::getFieldsObservable).orElse(null));
                        default -> BindingsHelper.constantOf(true);
                    });
        }

        @Override
        public void execute() {
            switch (command) {
                case EDIT_FILE_LINK -> linkedFile.edit();
                case OPEN_FILE -> linkedFile.open();
                case OPEN_FOLDER -> linkedFile.openFolder();
                case DOWNLOAD_FILE -> linkedFile.download();
                case RENAME_FILE_TO_PATTERN -> linkedFile.renameToSuggestion();
                case RENAME_FILE_TO_NAME -> linkedFile.askForNameAndRename();
                case MOVE_FILE_TO_FOLDER -> linkedFile.moveToDefaultDirectory();
                case MOVE_FILE_TO_FOLDER_AND_RENAME -> linkedFile.moveToDefaultDirectoryAndRename();
                case DELETE_FILE -> viewModel.deleteFile(linkedFile);
                case REMOVE_LINK -> viewModel.removeFileLink(linkedFile);
            }
        }
    }
}
