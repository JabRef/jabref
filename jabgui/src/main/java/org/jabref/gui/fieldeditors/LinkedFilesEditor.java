package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
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
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.ContextAction;
import org.jabref.gui.fieldeditors.contextmenu.ContextMenuFactory;
import org.jabref.gui.fieldeditors.contextmenu.ContextMenuFactory.MultiContextCommandFactory;
import org.jabref.gui.fieldeditors.contextmenu.ContextMenuFactory.SingleContextCommandFactory;
import org.jabref.gui.fieldeditors.contextmenu.MultiContextAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.importer.GrobidUseDialogHelper;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.linkedfile.DeleteFileAction;
import org.jabref.gui.linkedfile.LinkedFileEditDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.uithreadaware.UiThreadObservableList;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import jakarta.inject.Inject;

public class LinkedFilesEditor extends HBox implements FieldEditorFX {

    @FXML private ListView<LinkedFileViewModel> listView;
    @FXML private JabRefIconView fulltextFetcher;
    @FXML private ProgressIndicator progressIndicator;

    private final Field field;
    private final BibDatabaseContext databaseContext;
    private final SuggestionProvider<?> suggestionProvider;
    private final FieldCheckers fieldCheckers;

    @Inject private DialogService dialogService;
    @Inject private GuiPreferences preferences;
    @Inject private BibEntryTypesManager bibEntryTypesManager;
    @Inject private JournalAbbreviationRepository abbreviationRepository;
    @Inject private TaskExecutor taskExecutor;
    @Inject private UndoManager undoManager;

    private LinkedFilesEditorViewModel viewModel;

    private ObservableOptionalValue<BibEntry> bibEntry = EasyBind.wrapNullable(new SimpleObjectProperty<>());
    private final UiThreadObservableList<LinkedFileViewModel> decoratedModelList;

    private ContextMenu activeContextMenu = null;
    private ContextMenuFactory contextMenuFactory;

    public LinkedFilesEditor(Field field,
                             BibDatabaseContext databaseContext,
                             SuggestionProvider<?> suggestionProvider,
                             FieldCheckers fieldCheckers) {
        this.field = field;
        this.databaseContext = databaseContext;
        this.suggestionProvider = suggestionProvider;
        this.fieldCheckers = fieldCheckers;

        ViewLoader.view(this)
                  .root(this)
                  .load();

        decoratedModelList = new UiThreadObservableList<>(viewModel.filesProperty());
        Bindings.bindContentBidirectional(listView.itemsProperty().get(), decoratedModelList);
    }

    @FXML
    private void initialize() {
        this.viewModel = new LinkedFilesEditorViewModel(
                field,
                suggestionProvider,
                dialogService,
                databaseContext,
                taskExecutor,
                fieldCheckers,
                preferences,
                undoManager);

        new ViewModelListCellFactory<LinkedFileViewModel>()
                .withStringTooltip(LinkedFileViewModel::getDescriptionAndLink)
                .withGraphic(this::createFileDisplay)
                .withOnMouseClickedEvent(this::handleItemMouseClick)
                .setOnDragDetected(this::handleOnDragDetected)
                .setOnDragDropped(this::handleOnDragDropped)
                .setOnDragOver(this::handleOnDragOver)
                .withValidation(LinkedFileViewModel::fileExistsValidationStatus)
                .install(listView);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        fulltextFetcher.visibleProperty().bind(viewModel.fulltextLookupInProgressProperty().not());
        progressIndicator.visibleProperty().bind(viewModel.fulltextLookupInProgressProperty());

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
            LinkedFileViewModel transferredItem = null;
            int draggedIdx = 0;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getFile().equals(linkedFile)) {
                    draggedIdx = i;
                    transferredItem = items.get(i);
                    break;
                }
            }
            int thisIdx = items.indexOf(originalItem);
            items.set(draggedIdx, originalItem);
            items.set(thisIdx, transferredItem);
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

        Label label = new Label();
        label.graphicProperty().bind(linkedFile.typeIconProperty());
        label.textProperty().bind(linkedFile.linkProperty());
        label.getStyleClass().setAll("file-row-text");
        label.textOverrunProperty().setValue(OverrunStyle.LEADING_ELLIPSIS);
        EasyBind.subscribe(linkedFile.isAutomaticallyFoundProperty(), found -> label.pseudoClassStateChanged(opacity, found));

        HBox info = new HBox(8);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setStyle("-fx-padding: 0.5em 0 0.5em 0;"); // To align with buttons below which also have 0.5em padding
        info.getChildren().setAll(label, progressIndicator);

        Button acceptAutoLinkedFile = IconTheme.JabRefIcons.AUTO_LINKED_FILE.asButton();
        acceptAutoLinkedFile.setTooltip(new Tooltip(Localization.lang("This file was found automatically. Do you want to link it to this entry?")));
        acceptAutoLinkedFile.visibleProperty().bind(linkedFile.isAutomaticallyFoundProperty());
        acceptAutoLinkedFile.managedProperty().bind(linkedFile.isAutomaticallyFoundProperty());
        acceptAutoLinkedFile.setOnAction(event -> linkedFile.acceptAsLinked());
        acceptAutoLinkedFile.getStyleClass().setAll("icon-button");

        Button writeMetadataToPdf = IconTheme.JabRefIcons.PDF_METADATA_WRITE.asButton();
        writeMetadataToPdf.setTooltip(new Tooltip(Localization.lang("Write BibTeX to PDF (XMP and embedded)")));
        writeMetadataToPdf.visibleProperty().bind(linkedFile.isOfflinePdfProperty());
        writeMetadataToPdf.getStyleClass().setAll("icon-button");
        WriteMetadataToSinglePdfAction writeMetadataToSinglePdfAction = new WriteMetadataToSinglePdfAction(
                linkedFile.getFile(),
                bibEntry.getValueOrElse(new BibEntry()),
                databaseContext, dialogService, preferences.getFieldPreferences(),
                preferences.getFilePreferences(), preferences.getXmpPreferences(), abbreviationRepository, bibEntryTypesManager,
                taskExecutor
        );
        writeMetadataToPdf.disableProperty().bind(writeMetadataToSinglePdfAction.executableProperty().not());
        writeMetadataToPdf.setOnAction(event -> writeMetadataToSinglePdfAction.execute());

        Button parsePdfMetadata = IconTheme.JabRefIcons.PDF_METADATA_READ.asButton();
        parsePdfMetadata.setTooltip(new Tooltip(Localization.lang("Parse Metadata from PDF.")));
        parsePdfMetadata.visibleProperty().bind(linkedFile.isOfflinePdfProperty());
        parsePdfMetadata.setOnAction(event -> {
            GrobidUseDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferences.getGrobidPreferences());
            linkedFile.parsePdfMetadataAndShowMergeDialog();
        });
        parsePdfMetadata.getStyleClass().setAll("icon-button");

        HBox container = new HBox(2);
        container.setPrefHeight(Double.NEGATIVE_INFINITY);
        container.maxWidthProperty().bind(listView.widthProperty().subtract(20d));
        container.getChildren().addAll(acceptAutoLinkedFile, info, writeMetadataToPdf, parsePdfMetadata);

        return container;
    }

    private void setUpKeyBindings() {
        listView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = preferences.getKeyBindingRepository().mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                switch (keyBinding.get()) {
                    case DELETE_ENTRY:
                        deleteAttachedFilesWithConfirmation();
                        event.consume();
                        break;
                    default:
                        // Pass other keys to children
                }
            }
        });
    }

    private void deleteAttachedFilesWithConfirmation() {
        new DeleteFileAction(dialogService, preferences.getFilePreferences(), databaseContext,
                viewModel, listView.getSelectionModel().getSelectedItems()).execute();
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
        dialogService.showCustomDialogAndWait(new LinkedFileEditDialog()).filter(file -> !file.isEmpty()).ifPresent(newLinkedFile -> viewModel.addNewLinkedFile(newLinkedFile));
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
        if (event.getButton() == MouseButton.PRIMARY && (event.getClickCount() == 2)) {
            linkedFile.open(); // Double-click: open file
        } else if (activeContextMenu != null && event.getButton() == MouseButton.PRIMARY) {
            activeContextMenu.hide(); // Hide context menu if left-click
            activeContextMenu = null;
        } else if (event.getButton() == MouseButton.SECONDARY) {
            if (activeContextMenu != null) {
                activeContextMenu.hide(); // Hide any existing context menu
                activeContextMenu = null;
            }

            SingleContextCommandFactory contextCommandFactory = (action, file) ->
                    new ContextAction(action, file, databaseContext, bibEntry, preferences, viewModel);

            MultiContextCommandFactory multiContextCommandFactory = (action, files) ->
                    new MultiContextAction(action, files, databaseContext, bibEntry, preferences, viewModel);

            contextMenuFactory = new ContextMenuFactory(
                    dialogService,
                    preferences,
                    databaseContext,
                    bibEntry,
                    viewModel,
                    contextCommandFactory,
                    multiContextCommandFactory
            );

            ContextMenu contextMenu = contextMenuFactory.createForSelection(listView.getSelectionModel().getSelectedItems());
            contextMenu.show(listView, event.getScreenX(), event.getScreenY());
            activeContextMenu = contextMenu;
        }
    }

    @Override
    public double getWeight() {
        return 3;
    }
}
