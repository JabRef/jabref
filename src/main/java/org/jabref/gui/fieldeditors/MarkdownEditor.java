package org.jabref.gui.fieldeditors;

import java.io.File;
import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.scene.control.TextInputControl;

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

    public MarkdownEditor(Field field,
                          SuggestionProvider<?> suggestionProvider,
                          FieldCheckers fieldCheckers,
                          GuiPreferences preferences,
                          UndoManager undoManager,
                          UndoAction undoAction,
                          RedoAction redoAction,
                          BibDatabaseContext databaseContext) {
        super(field, suggestionProvider, fieldCheckers, preferences, true, undoManager, undoAction, redoAction);

        this.databaseContext = databaseContext;

        this.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles()) {
                String mdText = imageToMdText(event.getDragboard().getFiles());

                this.insertTextFromDragInput(event.getX(), event.getY(), mdText);

                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private String imageToMdText(List<File> files) {
        String mdImageTemplate = "![%s](file://%s)\n";
        StringBuilder allImagesText = new StringBuilder();

        // With this you can get the path to the default directory
        if (databaseContext.getMetaData().getDefaultFileDirectory().isPresent()) {
            databaseContext.getMetaData().getDefaultFileDirectory().get();
        }

        for (File file: files) {
            allImagesText.append(mdImageTemplate.formatted(file.getName(), file.getAbsolutePath()));
        }

        return allImagesText.toString();
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
                } else if (ClipBoardManager.hasFiles()) {
                    List<File> files = ClipBoardManager.getFiles();

                    String mdText = imageToMdText(files);

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
}
