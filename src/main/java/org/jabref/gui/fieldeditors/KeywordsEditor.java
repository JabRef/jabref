package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.contextmenu.DefaultMenu;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class KeywordsEditor extends HBox implements FieldEditorFX {

    @FXML private final KeywordsEditorViewModel viewModel;
    @FXML private EditorTextField textField;
    @FXML private ComboBox<Keyword> keywordsComboBox;

    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;

    public KeywordsEditor(Field field,
                          SuggestionProvider<?> suggestionProvider,
                          FieldCheckers fieldCheckers,
                          BibDatabaseContext databaseContext) {

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new KeywordsEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                preferencesService,
                databaseContext,
                undoManager,
                dialogService);

        textField.textProperty().bindBidirectional(viewModel.textProperty());
        textField.initContextMenu(new DefaultMenu(textField));

        new ViewModelListCellFactory<Keyword>().withText(Keyword::toString).install(keywordsComboBox);

        AutoCompletionTextInputBinding<?> autoCompleter = AutoCompletionTextInputBinding.autoComplete(textField, viewModel::complete, viewModel.getAutoCompletionStrategy());
        autoCompleter.setShowOnFocus(true);

        new EditorValidator(preferencesService).configureValidation(viewModel.getFieldValidator().getValidationStatus(), textField);

        keywordsComboBox.setItems(viewModel.getFilteredKeywords());
    }

    public KeywordsEditorViewModel getViewModel() {
        return viewModel;
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
        textField.requestFocus();
    }

    @Override
    public double getWeight() {
        return 2;
    }

    @FXML
    public void addKeyword() {
        viewModel.addKeyword(keywordsComboBox.getSelectionModel().getSelectedItem());
    }
}
