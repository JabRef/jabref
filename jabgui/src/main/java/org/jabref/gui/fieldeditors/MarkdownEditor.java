package org.jabref.gui.fieldeditors;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.control.TextInputControl;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkdownEditor extends SimpleEditor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarkdownEditor.class);
    private final FlexmarkHtmlConverter flexmarkHtmlConverter = FlexmarkHtmlConverter.builder().build();
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public MarkdownEditor(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, GuiPreferences preferences, UndoManager undoManager, UndoAction undoAction, RedoAction redoAction, BibDatabaseContext bibDatabaseContext) {
        super(field, suggestionProvider, fieldCheckers, preferences, true, undoManager, undoAction, redoAction);
        this.databaseContext = bibDatabaseContext;
        this.filePreferences = preferences.getFilePreferences();
        setUpDragDrop();
    }

    @Override
    protected TextInputControl createTextInputControl(@SuppressWarnings("unused") Field field) {
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

    public void setEditable(boolean isEditable) {
        getTextInput().setEditable(isEditable);
    }

    private void setUpDragDrop() {
        EditorTextArea textArea = (EditorTextArea) getTextInput();
        enableDragOver(textArea);
        enableDragDrop(textArea);
    }

    private void enableDragOver(EditorTextArea textArea) {
        textArea.setOnDragOver(event -> {
            if (event.getGestureSource() != textArea && (event.getDragboard().hasImage() || event.getDragboard().hasFiles())) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
    }

    private void enableDragDrop(EditorTextArea textArea) {
        textArea.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                for (File file : dragboard.getFiles()) {
                    if (FileUtil.isImage(file.toPath())) {
                        success |= insertFileToLibraryAndAppend(file, textArea);
                    }
                }
            } else if (dragboard.hasImage()) {
                success |= insertImageToDbAndAppend(dragboard.getUrl(), textArea);
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Inserts Markdown Link for a file dropped into the editor, copying file to the database file directory if necessary
     *
     * @param file is the file that is dropped into the editor
     * @param textArea is the Editor text area
     * @return true if the Markdown Text was inserted, false otherwise
     */
    private boolean insertFileToLibraryAndAppend(File file, EditorTextArea textArea) {
        Optional<Path> fileDir = databaseContext.getFirstExistingFileDir(filePreferences);
        if (fileDir.isEmpty()) {
            LOGGER.warn("No file directory found, cannot drop file");
            return false;
        }
        List<Path> fileDirList = databaseContext.getFileDirectories(filePreferences);
        Path relativePath = FileUtil.relativize(file.toPath(), fileDirList);

        // If the path can be reached from the configured directories somehow, just insert the Markdown text
        if (!relativePath.isAbsolute()) {
            insertMarkdownText(relativePath.toString(), textArea);
            return true;
        }
        Path destination = fileDir.get().resolve(file.getName());
        String relativePathString = FileUtil.relativize(destination, fileDirList).toString();
        try {
            Files.copy(file.toPath(), destination);
            insertMarkdownText(relativePathString, textArea);
            return true;
        } catch (FileAlreadyExistsException e) {
            LOGGER.warn("Dropped file already exists: {} ", destination, e);
            insertMarkdownText(relativePathString, textArea);
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not copy file: {} ", destination, e);
            return false;
        }
    }

    private boolean insertImageToDbAndAppend(String imageUrl, EditorTextArea textArea) {
        try {
            Path tempFile = new URLDownload(imageUrl).toTemporaryFile();
            return insertFileToLibraryAndAppend(tempFile.toFile(), textArea);
        } catch (MalformedURLException | FetcherException e) {
            LOGGER.error("Failed to download image from URL: {}", imageUrl, e);
            return false;
        }
    }

    private void insertMarkdownText(String relativePath, EditorTextArea textArea) {
        String markdownText = "![](" + relativePath + ")\n";
        int caretPosition = textArea.getCaretPosition();
        textArea.insertText(caretPosition, markdownText);
    }
}
