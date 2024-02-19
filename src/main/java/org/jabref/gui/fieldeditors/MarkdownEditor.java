package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.TextInputControl;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

public class MarkdownEditor extends SimpleEditor {

    private final FlexmarkHtmlConverter flexmarkHtmlConverter = FlexmarkHtmlConverter.builder().build();

    public MarkdownEditor(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, PreferencesService preferences, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, preferences, true, undoManager);
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
}
