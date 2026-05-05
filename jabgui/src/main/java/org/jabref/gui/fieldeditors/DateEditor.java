package org.jabref.gui.fieldeditors;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.component.TemporalAccessorPicker;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class DateEditor extends HBox implements FieldEditorFX {

    @FXML private DateEditorViewModel viewModel;
    @FXML private EditorTextField textField;
    @FXML private TemporalAccessorPicker datePicker;

    @Inject private UndoManager undoManager;
    @Inject private GuiPreferences preferences;
    @Inject private KeyBindingRepository keyBindingRepository;

    private boolean synchronizingPicker;

    public DateEditor(Field field,
                      DateTimeFormatter dateFormatter,
                      SuggestionProvider<?> suggestionProvider,
                      FieldCheckers fieldCheckers,
                      UndoAction undoAction,
                      RedoAction redoAction) {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new DateEditorViewModel(field, suggestionProvider, dateFormatter, fieldCheckers, undoManager);
        textField.setId(field.getName());
        datePicker.setStringConverter(viewModel.getDateToStringConverter());
        viewModel.textProperty().addListener((_, _, newValue) -> UiTaskExecutor.runInJavaFXThread(() -> {
            if (!textField.isFocused()) {
                syncPickerWithText(normalizeText(newValue));
            }
        }));
        datePicker.temporalAccessorValueProperty().addListener((_, _, newValue) -> {
            if (!synchronizingPicker && (newValue != null)) {
                acceptCommittedText(formatDate(newValue));
            }
        });
        textField.setOnAction(event -> commitTextFieldValue());
        textField.focusedProperty().addListener((_, _, newValue) -> {
            if (!newValue) {
                commitTextFieldValue();
            }
        });
        establishBinding(textField, viewModel.textProperty(), keyBindingRepository, undoAction, redoAction);
        textField.initContextMenu(new DefaultMenu(textField), keyBindingRepository);
        new EditorValidator(preferences).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textField);
    }

    public DateEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
        syncPickerWithText(viewModel.textProperty().getValueSafe());
    }

    @Override
    public Parent getNode() {
        return this;
    }

    private void commitTextFieldValue() {
        String currentText = normalizeText(textField.getText());

        if (currentText.isEmpty()) {
            syncPickerWithValue(null);
            acceptCommittedText(currentText);
            return;
        }

        Optional<TemporalAccessor> pickerCompatibleDate = getLosslessPickerDate(currentText);
        if (pickerCompatibleDate.isPresent()) {
            pickerCompatibleDate.ifPresent(datePicker::setTemporalAccessorValue);
            return;
        }

        if (Date.parse(currentText).isPresent()) {
            syncPickerWithValue(null);
            acceptCommittedText(currentText);
            return;
        }

        syncPickerWithValue(null);
    }

    private Optional<TemporalAccessor> getLosslessPickerDate(String text) {
        TemporalAccessor parsedDate = viewModel.getDateToStringConverter().fromString(text);
        if (parsedDate == null) {
            return Optional.empty();
        }

        String formattedDate = formatDate(parsedDate);
        if (Objects.equals(formattedDate, text)) {
            return Optional.of(parsedDate);
        }

        return Optional.empty();
    }

    private String formatDate(TemporalAccessor date) {
        return normalizeText(viewModel.getDateToStringConverter().toString(date));
    }

    private void acceptCommittedText(String text) {
        String normalizedText = normalizeText(text);
        if (!Objects.equals(textField.getText(), normalizedText)) {
            textField.setText(normalizedText);
        }
    }

    private void syncPickerWithText(String text) {
        Optional<TemporalAccessor> pickerCompatibleDate = getLosslessPickerDate(text);
        syncPickerWithValue(pickerCompatibleDate.orElse(null));
    }

    private void syncPickerWithValue(TemporalAccessor value) {
        synchronizingPicker = true;
        try {
            datePicker.setTemporalAccessorValue(value);
        } finally {
            synchronizingPicker = false;
        }
    }

    private String normalizeText(String text) {
        return text == null ? "" : text;
    }
}
