package org.jabref.gui.fieldeditors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

public class MarkdownEditor extends SimpleEditor {
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
            if (event.getGestureSource() != textArea && event.getDragboard().hasFiles()) {
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
                        success = insertToDbAndAppend(file, textArea);
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean insertToDbAndAppend(File file, EditorTextArea textArea) {
        Optional<Path> fileDir = databaseContext.getFirstExistingFileDir(filePreferences);
        if (fileDir.isEmpty()) {
            return false;
        }
        Path destination = fileDir.get().resolve(file.getName());
        try {
            Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            String relativePath = FileUtil.relativize(destination, databaseContext.getFileDirectories(filePreferences)).toString();
            String markdownText = "![" + file.getName() + "](" + relativePath + ")\n";
            textArea.appendText(markdownText);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
