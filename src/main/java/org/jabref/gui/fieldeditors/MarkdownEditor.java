package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javafx.scene.control.TextInputControl;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

public class MarkdownEditor extends SimpleEditor {
    private final FlexmarkHtmlConverter flexmarkHtmlConverter = FlexmarkHtmlConverter.builder().build();
    private final BibDatabaseContext databaseContext;
    private final GuiPreferences preferences;

    public MarkdownEditor(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, GuiPreferences preferences, BibDatabaseContext databaseContext, UndoManager undoManager, UndoAction undoAction, RedoAction redoAction) {
        super(field, suggestionProvider, fieldCheckers, preferences, true, undoManager, undoAction, redoAction);
        this.databaseContext = databaseContext;
        this.preferences = preferences;
        setupDragAndDrop();
    }

    @Override
    protected TextInputControl createTextInputControl() {
        return new EditorTextArea() {
            @Override
            public void paste() {
                if (ClipBoardManager.hasHtml()) {
                    String htmlText = ClipBoardManager.getHtmlContents();
                    String mdText = flexmarkHtmlConverter.convert(htmlText);
                    super.replaceSelection(mdText);
                } else {
                    super.paste();
                }
            }
        };
    }

    private void setupDragAndDrop() {
        getTextInput().setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() || event.getDragboard().hasUrl()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        getTextInput().setOnDragDropped(this::handleImageDrop);
    }

    private void handleImageDrop(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (dragboard.hasFiles()) {
            for (File file : dragboard.getFiles()) {
                if (isImageFile(file)) {
                    File copiedFile = copyFileToImagesFolder(file);
                    if (copiedFile != null) {
                        insertMarkdownImage(copiedFile);
                    }
                }
            }
            event.setDropCompleted(true);
        } else if (dragboard.hasUrl()) {
            String url = dragboard.getUrl();
            File downloadedFile = downloadImageFromUrl(url);
            if (downloadedFile != null) {
                insertMarkdownImage(downloadedFile);
            }
            event.setDropCompleted(true);
        } else {
            event.setDropCompleted(false);
        }
        event.consume();
    }

    private boolean isImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".gif");
    }

    private File copyFileToImagesFolder(File file) {
        Path imageFolder = databaseContext.getFirstExistingFileDir(preferences.getFilePreferences()).orElse(Path.of("images"));
        if (!Files.exists(imageFolder)) {
            try {
                Files.createDirectories(imageFolder);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        Path destinationFile = imageFolder.resolve(file.getName());
        try {
            Files.copy(file.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return destinationFile.toFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File downloadImageFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String fileName = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
            Path imageFolder = databaseContext.getFirstExistingFileDir(preferences.getFilePreferences()).orElse(Path.of("images"));
            if (!Files.exists(imageFolder)) {
                Files.createDirectories(imageFolder);
            }
            Path destinationFile = imageFolder.resolve(fileName);
            Files.copy(url.openStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return destinationFile.toFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void insertMarkdownImage(File imageFile) {
        Path relativePath = databaseContext.getDatabasePath().map(dbPath -> dbPath.getParent().relativize(imageFile.toPath())).orElse(imageFile.toPath());
        String markdownImage = String.format("\n![Image](%s)\n", relativePath.toString().replace("\\", "/"));
        getTextInput().replaceSelection(markdownImage);
    }

    public void setEditable(boolean isEditable) {
        getTextInput().setEditable(isEditable);
    }
}
