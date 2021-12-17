package org.jabref.gui.preview;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PreviewPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPanel extends VBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewPanel.class);

    private final ExternalFilesEntryLinker fileLinker;
    private final KeyBindingRepository keyBindingRepository;
    private final PreviewViewer previewView;
    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final IndexingTaskManager indexingTaskManager;
    private BibEntry entry;

    public PreviewPanel(BibDatabaseContext database,
                        DialogService dialogService,
                        ExternalFileTypes externalFileTypes,
                        KeyBindingRepository keyBindingRepository,
                        PreferencesService preferences,
                        StateManager stateManager, IndexingTaskManager indexingTaskManager) {
        this.keyBindingRepository = keyBindingRepository;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.indexingTaskManager = indexingTaskManager;
        fileLinker = new ExternalFilesEntryLinker(externalFileTypes, preferences.getFilePreferences(), database);

        PreviewPreferences previewPreferences = preferences.getPreviewPreferences();
        previewView = new PreviewViewer(database, dialogService, stateManager);
        previewView.setLayout(previewPreferences.getCurrentPreviewStyle());
        previewView.setContextMenu(createPopupMenu());
        previewView.setTheme(this.preferences.getTheme());
        previewView.setOnDragDetected(event -> {
            previewView.startFullDrag();

            Dragboard dragboard = previewView.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putHtml(previewView.getSelectionHtmlContent());
            dragboard.setContent(content);

            event.consume();
        });

        previewView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE, TransferMode.LINK);
            }
            event.consume();
        });

        previewView.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasContent(DataFormat.FILES)) {
                List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).collect(Collectors.toList());

                if (event.getTransferMode() == TransferMode.MOVE) {
                    LOGGER.debug("Mode MOVE"); // shift on win or no modifier
                    fileLinker.moveFilesToFileDirAndAddToEntry(entry, files, indexingTaskManager);
                }
                if (event.getTransferMode() == TransferMode.LINK) {
                    LOGGER.debug("Node LINK"); // alt on win
                    fileLinker.addFilesToEntry(entry, files);
                }
                if (event.getTransferMode() == TransferMode.COPY) {
                    LOGGER.debug("Mode Copy"); // ctrl on win, no modifier on Xubuntu
                    fileLinker.copyFilesToFileDirAndAddToEntry(entry, files, indexingTaskManager);
                }
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });
        this.getChildren().add(previewView);

        createKeyBindings();
        updateLayout(previewPreferences, true);
    }

    public void updateLayout(PreviewPreferences previewPreferences) {
        updateLayout(previewPreferences, false);
    }

    private void updateLayout(PreviewPreferences previewPreferences, boolean init) {
        PreviewLayout currentPreviewStyle = previewPreferences.getCurrentPreviewStyle();
        previewView.setLayout(currentPreviewStyle);
        preferences.storePreviewPreferences(previewPreferences);
        if (!init) {
            dialogService.notify(Localization.lang("Preview style changed to: %0", currentPreviewStyle.getDisplayName()));
        }
    }

    private void updateLayoutByPreferences(PreferencesService preferences) {
        previewView.setLayout(preferences.getPreviewPreferences().getCurrentPreviewStyle());
    }

    private void createKeyBindings() {
        previewView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                switch (keyBinding.get()) {
                    case COPY_PREVIEW:
                        previewView.copyPreviewToClipBoard();
                        event.consume();
                        break;
                    default:
                }
            }
        });
    }

    private ContextMenu createPopupMenu() {
        MenuItem copyPreview = new MenuItem(Localization.lang("Copy preview"), IconTheme.JabRefIcons.COPY.getGraphicNode());
        keyBindingRepository.getKeyCombination(KeyBinding.COPY_PREVIEW).ifPresent(copyPreview::setAccelerator);
        copyPreview.setOnAction(event -> previewView.copyPreviewToClipBoard());
        MenuItem copySelection = new MenuItem(Localization.lang("Copy selection"));
        copySelection.setOnAction(event -> previewView.copySelectionToClipBoard());
        MenuItem printEntryPreview = new MenuItem(Localization.lang("Print entry preview"), IconTheme.JabRefIcons.PRINTED.getGraphicNode());
        printEntryPreview.setOnAction(event -> previewView.print());
        MenuItem previousPreviewLayout = new MenuItem(Localization.lang("Previous preview layout"));
        keyBindingRepository.getKeyCombination(KeyBinding.PREVIOUS_PREVIEW_LAYOUT).ifPresent(previousPreviewLayout::setAccelerator);
        previousPreviewLayout.setOnAction(event -> this.previousPreviewStyle());
        MenuItem nextPreviewLayout = new MenuItem(Localization.lang("Next preview layout"));
        keyBindingRepository.getKeyCombination(KeyBinding.NEXT_PREVIEW_LAYOUT).ifPresent(nextPreviewLayout::setAccelerator);
        nextPreviewLayout.setOnAction(event -> this.nextPreviewStyle());

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(copyPreview);
        menu.getItems().add(copySelection);
        menu.getItems().add(printEntryPreview);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(nextPreviewLayout);
        menu.getItems().add(previousPreviewLayout);
        return menu;
    }

    public void setEntry(BibEntry entry) {
        updateLayoutByPreferences(preferences);
        this.entry = entry;
        previewView.setEntry(entry);
    }

    public void print() {
        previewView.print();
    }

    public void nextPreviewStyle() {
        cyclePreview(preferences.getPreviewPreferences().getPreviewCyclePosition() + 1);
    }

    public void previousPreviewStyle() {
        cyclePreview(preferences.getPreviewPreferences().getPreviewCyclePosition() - 1);
    }

    private void cyclePreview(int newPosition) {
        PreviewPreferences previewPreferences = preferences
                .getPreviewPreferences()
                .getBuilder()
                .withPreviewCyclePosition(newPosition)
                .build();
        updateLayout(previewPreferences);
    }
}
