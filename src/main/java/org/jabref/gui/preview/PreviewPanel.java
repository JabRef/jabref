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

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.citationstyle.PreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreviewPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPanel extends VBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewPanel.class);

    private final ExternalFilesEntryLinker fileLinker;
    private final KeyBindingRepository keyBindingRepository;
    private final PreviewViewer previewView;
    private BibEntry entry;
    private BasePanel basePanel;
    private DialogService dialogService;

    public PreviewPanel(BibDatabaseContext database, BasePanel basePanel, DialogService dialogService, ExternalFileTypes externalFileTypes, KeyBindingRepository keyBindingRepository, PreviewPreferences preferences) {
        this.basePanel = basePanel;
        this.keyBindingRepository = keyBindingRepository;
        this.dialogService = dialogService;
        fileLinker = new ExternalFilesEntryLinker(externalFileTypes, Globals.prefs.getFilePreferences(), database);

        previewView = new PreviewViewer(database, dialogService, Globals.stateManager);
        previewView.setLayout(preferences.getCurrentPreviewStyle());
        previewView.setContextMenu(createPopupMenu());
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
                    LOGGER.debug("Mode MOVE"); //shift on win or no modifier
                    fileLinker.moveFilesToFileDirAndAddToEntry(entry, files);
                }
                if (event.getTransferMode() == TransferMode.LINK) {
                    LOGGER.debug("Node LINK"); //alt on win
                    fileLinker.addFilesToEntry(entry, files);
                }
                if (event.getTransferMode() == TransferMode.COPY) {
                    LOGGER.debug("Mode Copy"); //ctrl on win, no modifier on Xubuntu
                    fileLinker.copyFilesToFileDirAndAddToEntry(entry, files);
                }
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });
        this.getChildren().add(previewView);

        createKeyBindings();
        updateLayout(preferences, true);
    }

    public void close() {
        basePanel.closeBottomPane();
    }

    public void updateLayout(PreviewPreferences previewPreferences) {
        updateLayout(previewPreferences, false);
    }

    private void updateLayout(PreviewPreferences previewPreferences, boolean init) {
        PreviewLayout currentPreviewStyle = previewPreferences.getCurrentPreviewStyle();
        previewView.setLayout(currentPreviewStyle);
        if (!init) {
            dialogService.notify(Localization.lang("Preview style changed to: %0", currentPreviewStyle.getName()));
        }
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
                    case CLOSE:
                        close();
                        event.consume();
                        break;
                    default:
                }
            }
        });
    }

    private ContextMenu createPopupMenu() {
        MenuItem copyPreview = new MenuItem(Localization.lang("Copy preview"), IconTheme.JabRefIcons.COPY.getGraphicNode());
        copyPreview.setAccelerator(keyBindingRepository.getKeyCombination(KeyBinding.COPY_PREVIEW));
        copyPreview.setOnAction(event -> previewView.copyPreviewToClipBoard());
        MenuItem printEntryPreview = new MenuItem(Localization.lang("Print entry preview"), IconTheme.JabRefIcons.PRINTED.getGraphicNode());
        printEntryPreview.setOnAction(event -> previewView.print());
        /* Deleted since it does not work anymore. Needs refactoring.
        MenuItem previousPreviewLayout = new MenuItem(Localization.lang("Previous preview layout"));
        previousPreviewLayout.setAccelerator(keyBindingRepository.getKeyCombination(KeyBinding.PREVIOUS_PREVIEW_LAYOUT));
        previousPreviewLayout.setOnAction(event -> basePanel.previousPreviewStyle());
        MenuItem nextPreviewLayout = new MenuItem(Localization.lang("Next preview layout"));
        nextPreviewLayout.setAccelerator(keyBindingRepository.getKeyCombination(KeyBinding.NEXT_PREVIEW_LAYOUT));
        nextPreviewLayout.setOnAction(event -> basePanel.nextPreviewStyle());
        */

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(copyPreview);
        menu.getItems().add(printEntryPreview);
        menu.getItems().add(new SeparatorMenuItem());
        // menu.getItems().add(nextPreviewLayout);
        // menu.getItems().add(previousPreviewLayout);
        return menu;
    }

    public void setEntry(BibEntry entry) {
        this.entry = entry;
        previewView.setEntry(entry);
    }

    public void print() {
        previewView.print();
    }
}
