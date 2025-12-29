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
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefDialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.DragDrop;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.Nullable;

/// Displays the entry preview
///
/// The instance is re-used at each tab. The code ensures that the panel is moved across tabs when the user switches the tab.
public class PreviewPanel extends VBox implements PreviewControls {

    private final ExternalFilesEntryLinker fileLinker;
    private final KeyBindingRepository keyBindingRepository;
    private final PreviewViewer previewView;
    private final PreviewPreferences previewPreferences;
    private final DialogService dialogService;
    private final StateManager stateManager;

    private BibEntry entry;

    public PreviewPanel(DialogService dialogService,
                        KeyBindingRepository keyBindingRepository,
                        GuiPreferences preferences,
                        ThemeManager themeManager,
                        TaskExecutor taskExecutor,
                        StateManager stateManager) {
        this.keyBindingRepository = keyBindingRepository;
        this.dialogService = dialogService;
        this.previewPreferences = preferences.getPreviewPreferences();
        this.fileLinker = new ExternalFilesEntryLinker(preferences.getExternalApplicationsPreferences(), preferences.getFilePreferences(), dialogService, stateManager);
        this.stateManager = stateManager;

        PreviewPreferences previewPreferences = preferences.getPreviewPreferences();
        previewView = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor, stateManager.searchQueryProperty());
        previewView.setLayout(previewPreferences.getSelectedPreviewLayout());
        previewView.setContextMenu(createPopupMenu());
        previewView.setOnDragDetected(this::onDragDetected);
        previewView.setOnDragOver(PreviewPanel::onDragOver);
        previewView.setOnDragDropped(this::onDragDropped);

        this.getChildren().add(previewView);

        createKeyBindings();
        previewView.setLayout(previewPreferences.getSelectedPreviewLayout());
    }

    private void onDragDetected(MouseEvent event) {
        previewView.startFullDrag();

        Dragboard dragboard = previewView.startDragAndDrop(TransferMode.COPY);
        ClipboardContent content = new ClipboardContent();
        content.putHtml(previewView.getSelectionHtmlContent());
        dragboard.setContent(content);

        event.consume();
    }

    private static void onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE, TransferMode.LINK);
        }
        event.consume();
    }

    private void onDragDropped(DragEvent event) {
        boolean success = false;
        if (event.getDragboard().hasContent(DataFormat.FILES)) {
            TransferMode transferMode = event.getTransferMode();
            List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).collect(Collectors.toList());
            DragDrop.handleDropOfFiles(files, transferMode, fileLinker, entry);
            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    private void createKeyBindings() {
        previewView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                if (keyBinding.get() == KeyBinding.COPY_PREVIEW) {
                    previewView.copyPreviewHtmlToClipBoard();
                    event.consume();
                }
            }
        });
    }

    private ContextMenu createPopupMenu() {
        MenuItem copyCitationHtml = new MenuItem(Localization.lang("Copy citation (html)"), IconTheme.JabRefIcons.COPY.getGraphicNode());
        keyBindingRepository.getKeyCombination(KeyBinding.COPY_PREVIEW).ifPresent(copyCitationHtml::setAccelerator);
        copyCitationHtml.setOnAction(_ -> previewView.copyPreviewHtmlToClipBoard());
        MenuItem copyCitationText = new MenuItem(Localization.lang("Copy citation (text)"));
        copyCitationText.setOnAction(_ -> previewView.copyPreviewPlainTextToClipBoard());
        MenuItem exportToClipboard = new MenuItem(Localization.lang("Export to clipboard"));
        exportToClipboard.setOnAction(_ -> previewView.exportToClipBoard(stateManager));
        MenuItem copySelection = new MenuItem(Localization.lang("Copy selection"));
        copySelection.setOnAction(_ -> previewView.copySelectionToClipBoard());
        MenuItem printEntryPreview = new MenuItem(Localization.lang("Print entry preview"), IconTheme.JabRefIcons.PRINTED.getGraphicNode());
        printEntryPreview.setOnAction(_ -> previewView.print());
        MenuItem previousPreviewLayout = new MenuItem(Localization.lang("Previous preview layout"));
        keyBindingRepository.getKeyCombination(KeyBinding.PREVIOUS_PREVIEW_LAYOUT).ifPresent(previousPreviewLayout::setAccelerator);
        previousPreviewLayout.setOnAction(_ -> this.previousPreviewStyle());
        MenuItem nextPreviewLayout = new MenuItem(Localization.lang("Next preview layout"));
        keyBindingRepository.getKeyCombination(KeyBinding.NEXT_PREVIEW_LAYOUT).ifPresent(nextPreviewLayout::setAccelerator);
        nextPreviewLayout.setOnAction(_ -> this.nextPreviewStyle());

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(copyCitationHtml);
        menu.getItems().add(copyCitationText);
        menu.getItems().add(copySelection);
        menu.getItems().add(printEntryPreview);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(nextPreviewLayout);
        menu.getItems().add(previousPreviewLayout);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(exportToClipboard);
        return menu;
    }

    public void setEntry(BibEntry entry) {
        this.entry = entry;
        previewView.setEntry(entry);
        previewView.setLayout(previewPreferences.getSelectedPreviewLayout());
    }

    public void setDatabase(@Nullable BibDatabaseContext databaseContext) {
        previewView.setDatabaseContext(databaseContext);
    }

    public void print() {
        previewView.print();
    }

    @Override
    public void nextPreviewStyle() {
        cyclePreview(previewPreferences.getLayoutCyclePosition() + 1);
    }

    @Override
    public void previousPreviewStyle() {
        cyclePreview(previewPreferences.getLayoutCyclePosition() - 1);
    }

    private void cyclePreview(int newPosition) {
        previewPreferences.setLayoutCyclePosition(newPosition);

        PreviewLayout layout = previewPreferences.getSelectedPreviewLayout();
        previewView.setLayout(layout);
        dialogService.notify(new JabRefDialogService.PreviewNotification("Preview style", Localization.lang("Preview style changed to: %0", layout.getDisplayName())));
    }
}
