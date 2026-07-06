package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.copyfiles.CopyLinkedFilesAction;
import org.jabref.gui.fieldeditors.contextmenu.ContextAction;
import org.jabref.gui.fieldeditors.contextmenu.ContextMenuFactory;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.importer.GrobidUseDialogHelper;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.linkedfile.DeleteFileAction;
import org.jabref.gui.linkedfile.LinkedFileEditDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import jakarta.inject.Inject;

public class LinkedFilesEditor extends HBox implements FieldEditorFX {

    // Upper bound on how many rows the ListView grows to before it starts scrolling internally,
    // so entries with many linked files cannot expand the entry editor layout indefinitely.
    private static final int MAX_VISIBLE_ROWS = 5;

    @FXML
    private ListView<LinkedFileViewModel> listView;
    @FXML
    private JabRefIconView fulltextFetcher;
    @FXML
    private ProgressIndicator progressIndicator;

    private final Field field;
    private final BibDatabaseContext databaseContext;
    private final SuggestionProvider<?> suggestionProvider;
    private final FieldCheckers fieldCheckers;

    @Inject
    private DialogService dialogService;
    @Inject
    private GuiPreferences preferences;
    @Inject
    private BibEntryTypesManager bibEntryTypesManager;
    @Inject
    private JournalAbbreviationRepository abbreviationRepository;
    @Inject
    private TaskExecutor taskExecutor;
    @Inject
    private UndoManager undoManager;
    @Inject
    private FileUpdateMonitor fileUpdateMonitor;
    @Inject
    private StateManager stateManager;

    private LinkedFilesEditorViewModel viewModel;

    private ObservableOptionalValue<BibEntry> bibEntry = EasyBind.wrapNullable(new SimpleObjectProperty<>());

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

        // Bind directly to the view model's own list (not a wrapper): JavaFX's bindContentBidirectional
        // dispatches change events by identity of the lists it was given, so wrapping one side (e.g. in
        // UiThreadObservableList) makes every Change#getList() report the wrapped delegate instead of the
        // list JavaFX is tracking, silently breaking the live sync (a Change#getList() identity mismatch).
        // filesProperty() is only ever mutated on the FX Application Thread, so no extra marshaling is needed.
        Bindings.bindContentBidirectional(listView.itemsProperty().get(), viewModel.filesProperty());
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
                undoManager
        );

        this.contextMenuFactory = new ContextMenuFactory(
                dialogService,
                preferences,
                databaseContext,
                bibEntry,
                viewModel,
                taskExecutor,
                fileUpdateMonitor,
                undoManager,
                stateManager
        );

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

        // Size the control to (number of files + 1) rows, so it ends right after the content instead of leaving blank
        // space, but cap it at MAX_VISIBLE_ROWS so large file lists scroll internally rather than growing the layout.
        // The row height comes from the CSS-driven fixed cell size, so theming and font scaling adjust it naturally.
        listView.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> Math.min(listView.getItems().size() + 1, MAX_VISIBLE_ROWS) * listView.getFixedCellSize(),
                listView.getItems(),
                listView.fixedCellSizeProperty()));
        listView.maxHeightProperty().bind(listView.fixedCellSizeProperty().multiply(MAX_VISIBLE_ROWS));

        fulltextFetcher.visibleProperty().bind(viewModel.fulltextLookupInProgressProperty().not());
        progressIndicator.visibleProperty().bind(viewModel.fulltextLookupInProgressProperty());

        setUpKeyBindings();

        // Double-clicking the empty row below the files (the "+1" row) adds a new file, same as the add button.
        listView.setOnMouseClicked(event -> {
            if ((event.getButton() == MouseButton.PRIMARY) && (event.getClickCount() == 2) && isEmptyRow(event.getTarget())) {
                addNewFile();
            }
        });
    }

    private static boolean isEmptyRow(EventTarget target) {
        Optional<ListCell<?>> enclosingCell = target instanceof Node node
                                              ? findEnclosingListCell(node)
                                              : Optional.empty();
        return enclosingCell.map(ListCell::isEmpty).orElse(false);
    }

    private static Optional<ListCell<?>> findEnclosingListCell(Node node) {
        if (node instanceof ListCell<?> cell) {
            return Optional.of(cell);
        }
        return Optional.ofNullable(node.getParent()).flatMap(LinkedFilesEditor::findEnclosingListCell);
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
        icon.setOnMouseClicked(_ -> linkedFile.open());

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
        info.getStyleClass().add("linked-files-info"); // To align with buttons below which also have 0.5em padding
        info.getChildren().setAll(label, progressIndicator);

        Button acceptAutoLinkedFile = ControlHelper.iconButton(IconTheme.JabRefIcons.AUTO_LINKED_FILE);
        acceptAutoLinkedFile.setTooltip(new Tooltip(Localization.lang("This file was found automatically. Do you want to link it to this entry?")));
        acceptAutoLinkedFile.visibleProperty().bind(linkedFile.isAutomaticallyFoundProperty());
        acceptAutoLinkedFile.managedProperty().bind(linkedFile.isAutomaticallyFoundProperty());
        acceptAutoLinkedFile.setOnAction(_ -> linkedFile.acceptAsLinked());
        acceptAutoLinkedFile.getStyleClass().setAll("icon-button");

        Button writeMetadataToPdf = ControlHelper.iconButton(IconTheme.JabRefIcons.PDF_METADATA_WRITE);
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
        writeMetadataToPdf.setOnAction(_ -> writeMetadataToSinglePdfAction.execute());

        Button parsePdfMetadata = ControlHelper.iconButton(IconTheme.JabRefIcons.PDF_METADATA_READ);
        parsePdfMetadata.setTooltip(new Tooltip(Localization.lang("Parse Metadata from PDF.")));
        parsePdfMetadata.visibleProperty().bind(linkedFile.isOfflinePdfProperty());
        parsePdfMetadata.setOnAction(_ -> {
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
                    case DELETE_ENTRY -> {
                        deleteAttachedFilesWithConfirmation();
                        event.consume();
                    }
                    case RENAME_FILE_TO_NAME -> {
                        LinkedFileViewModel selectedFile = listView.getSelectionModel().getSelectedItem();
                        if (selectedFile != null) {
                            new ContextAction(StandardActions.RENAME_FILE_TO_NAME, selectedFile, databaseContext, bibEntry, preferences, viewModel).execute();
                            event.consume();
                        }
                    }
                    case OPEN_FILE -> {
                        LinkedFileViewModel selectedFile = listView.getSelectionModel().getSelectedItem();
                        if (selectedFile != null) {
                            new ContextAction(StandardActions.OPEN_FILE, selectedFile, databaseContext, bibEntry, preferences, viewModel).execute();
                            event.consume();
                        }
                    }
                    case OPEN_FOLDER -> {
                        LinkedFileViewModel selectedFile = listView.getSelectionModel().getSelectedItem();
                        if (selectedFile != null) {
                            new ContextAction(StandardActions.OPEN_FOLDER, selectedFile, databaseContext, bibEntry, preferences, viewModel).execute();
                            event.consume();
                        }
                    }
                    case OPEN_CLOSE_ENTRY_EDITOR -> {
                        LinkedFileViewModel selectedFile = listView.getSelectionModel().getSelectedItem();
                        if (selectedFile != null) {
                            new ContextAction(StandardActions.EDIT_FILE_LINK, selectedFile, databaseContext, bibEntry, preferences, viewModel).execute();
                            event.consume();
                        }
                    }
                    case COPY -> {
                        LinkedFileViewModel selectedFile = listView.getSelectionModel().getSelectedItem();
                        if (selectedFile != null) {
                            new CopyLinkedFilesAction(selectedFile.getFile(), dialogService, databaseContext, preferences.getFilePreferences()).execute();
                            event.consume();
                        }
                    }
                    default -> {
                        // Pass other keys to children
                    }
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
        if (event.getButton() == MouseButton.PRIMARY) {
            if (event.getClickCount() == 2) {
                linkedFile.open();
                event.consume();
                return;
            }
            if (activeContextMenu != null) {
                activeContextMenu.hide();
                activeContextMenu = null;
            }
        } else if (event.getButton() == MouseButton.SECONDARY) {
            if (activeContextMenu != null) {
                activeContextMenu.hide();
                activeContextMenu = null;
            }

            ContextMenu menu = contextMenuFactory.createMenuForSelection(
                    listView.getSelectionModel().getSelectedItems());

            menu.setOnHidden(_ -> activeContextMenu = null);

            menu.show(listView, event.getScreenX(), event.getScreenY());
            activeContextMenu = menu;

            event.consume();
        }
    }

    @Override
    public double getWeight() {
        return 3;
    }
}

