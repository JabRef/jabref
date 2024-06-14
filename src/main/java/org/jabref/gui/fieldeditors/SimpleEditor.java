package org.jabref.gui.fieldeditors;

import java.util.Arrays;
import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.ContentSelectorSuggestionProvider;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleEditor extends HBox implements FieldEditorFX {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleEditor.class);

    private final SimpleEditorViewModel viewModel;
    private final TextInputControl textInput;
    private final boolean isMultiLine;

    public SimpleEditor(final Field field,
                        final SuggestionProvider<?> suggestionProvider,
                        final FieldCheckers fieldCheckers,
                        final PreferencesService preferences,
                        final boolean isMultiLine,
                        final UndoManager undoManager) {
        this.viewModel = new SimpleEditorViewModel(field, suggestionProvider, fieldCheckers, undoManager);
        this.isMultiLine = isMultiLine;

        textInput = createTextInputControl();
        HBox.setHgrow(textInput, Priority.ALWAYS);

        preventCursorJumping();

        ((ContextMenuAddable) textInput).initContextMenu(new DefaultMenu(textInput));
        this.getChildren().add(textInput);

        if (!isMultiLine) {
            AutoCompletionTextInputBinding<?> autoCompleter = AutoCompletionTextInputBinding.autoComplete(textInput, viewModel::complete, viewModel.getAutoCompletionStrategy());
            if (suggestionProvider instanceof ContentSelectorSuggestionProvider) {
                // If content selector values are present, then we want to show the auto complete suggestions immediately on focus
                autoCompleter.setShowOnFocus(true);
            }
        }

        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textInput);
    }

    private void preventCursorJumping() {
        // We need some more sophisticated handling to avoid cursor jumping
        // https://github.com/JabRef/jabref/issues/5904

        EasyBind.subscribe(viewModel.textProperty(), newText -> {
            int lastCaretPosition = textInput.getCaretPosition();
            LOGGER.trace("Caret at position {}", lastCaretPosition);
            String oldText = textInput.getText();
            textInput.setText(newText);
            LOGGER.trace("listener triggered: '{}' -> '{}'", oldText, newText);
            if (oldText == null) {
                LOGGER.trace("Empty field");
                return;
            }
            if (newText == null) {
                LOGGER.trace("Field cleared");
                return;
            }
            if (oldText.equals(newText)) {
                LOGGER.trace("No change, returned.");
                return;
            }
            LOGGER.trace("Trying to adapt...");
            // This is a special case when the text is set to a new value
            // In this case, we want to adjust the caret position
            List<String> oldValueCharacters = Arrays.asList(oldText.split(""));
            List<String> newValueCharacters = Arrays.asList(newText.split(""));
            List<AbstractDelta<String>> deltaList = DiffUtils.diff(oldValueCharacters, newValueCharacters).getDeltas();
            LOGGER.trace("Deltas: {}", deltaList);
            AbstractDelta<String> lastDelta = null;
            for (AbstractDelta<String> delta : deltaList) {
                if (delta.getSource().getPosition() > lastCaretPosition) {
                    break;
                }
                lastDelta = delta;
            }
            if (lastDelta != null) {
                LOGGER.trace("Last Delta: {}", lastDelta);
                LOGGER.trace("Last Delta source: {}", lastDelta.getSource());
                LOGGER.trace("Last Delta target: {}", lastDelta.getTarget());
                int offset = lastDelta.getTarget().getPosition() - lastDelta.getSource().getPosition();
                LOGGER.trace("Offset before patching: {}", offset);

                switch (lastDelta.getType()) {
                    case DELETE:
                        offset -= lastDelta.getSource().size();
                        break;
                    case INSERT:
                        offset += lastDelta.getTarget().size();
                        break;
                    case CHANGE:
                        offset += lastDelta.getTarget().size() - lastDelta.getSource().size();
                        break;
                    default:
                        break;
                }
                LOGGER.trace("Offset after patching: {}", offset);

                int newCaretPosition = lastCaretPosition + offset;
                textInput.positionCaret(newCaretPosition);
                LOGGER.trace("newCaretPosition: {}", newCaretPosition);
            }
        });
        EasyBind.subscribe(textInput.textProperty(), newText -> {
            viewModel.textProperty().set(newText);
        });
    }

    protected TextInputControl createTextInputControl() {
        return isMultiLine ? new EditorTextArea() : new EditorTextField();
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @Override
    public void requestFocus() {
        textInput.requestFocus();
    }
}
