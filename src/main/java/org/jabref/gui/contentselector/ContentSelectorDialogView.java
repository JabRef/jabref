package org.jabref.gui.contentselector;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class ContentSelectorDialogView extends BaseDialog<Void> {

    @FXML
    private Button addFieldNameButton;
    @FXML
    private Button removeFieldNameButton;
    @FXML
    private Button addKeywordButton;
    @FXML
    private Button removeKeywordButton;
    @FXML
    private ListView<Field> fieldsListView;
    @FXML
    private ListView<String> keywordsListView;
    @FXML
    private ButtonType saveButton;

    @Inject
    private DialogService dialogService;
    private final LibraryTab libraryTab;
    private ContentSelectorDialogViewModel viewModel;

    public ContentSelectorDialogView(LibraryTab libraryTab) {
        this.setTitle(Localization.lang("Manage content selectors"));
        this.getDialogPane().setPrefSize(375, 475);

        this.libraryTab = libraryTab;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> saveChangesAndClose());
    }

    @FXML
    public void initialize() {
        viewModel = new ContentSelectorDialogViewModel(libraryTab, dialogService);

        initFieldNameComponents();
        initKeywordsComponents();
    }

    private void initFieldNameComponents() {
        initListView(fieldsListView, viewModel::getFieldNamesBackingList);
        viewModel.selectedFieldProperty().bind(fieldsListView.getSelectionModel().selectedItemProperty());
        new ViewModelListCellFactory<Field>()
                .withText(Field::getDisplayName)
                .install(fieldsListView);
        removeFieldNameButton.disableProperty().bind(viewModel.isNoFieldNameSelected());
        EasyBind.subscribe(viewModel.selectedFieldProperty(), viewModel::populateKeywords);
    }

    private void initKeywordsComponents() {
        initListView(keywordsListView, viewModel::getKeywordsBackingList);
        viewModel.selectedKeywordProperty().bind(keywordsListView.getSelectionModel().selectedItemProperty());
        addKeywordButton.disableProperty().bind(viewModel.isFieldNameListEmpty());
        removeKeywordButton.disableProperty().bind(viewModel.isNoKeywordSelected());
    }

    @FXML
    private void addNewFieldName() {
        viewModel.showInputFieldNameDialog();
    }

    @FXML
    private void removeFieldName() {
        getSelectedField().ifPresent(viewModel::showRemoveFieldNameConfirmationDialog);
    }

    @FXML
    private void addNewKeyword() {
        getSelectedField().ifPresent(viewModel::showInputKeywordDialog);
    }

    @FXML
    private void removeKeyword() {
        Optional<Field> fieldName = getSelectedField();
        Optional<String> keywordToRemove = getSelectedKeyword();
        if (fieldName.isPresent() && keywordToRemove.isPresent()) {
            viewModel.showRemoveKeywordConfirmationDialog(fieldName.get(), keywordToRemove.get());
        }
    }

    private <T> void initListView(ListView<T> listViewToInit, Supplier<ListProperty<T>> backingList) {
        listViewToInit.itemsProperty().bind(backingList.get());
        listViewToInit.getSelectionModel().selectFirst();
    }

    private Optional<Field> getSelectedField() {
        return Optional.of(fieldsListView.getSelectionModel()).map(SelectionModel::getSelectedItem);
    }

    private Optional<String> getSelectedKeyword() {
        return Optional.of(keywordsListView.getSelectionModel()).map(SelectionModel::getSelectedItem);
    }

    private void saveChangesAndClose() {
        viewModel.saveChanges();
        close();
    }
}
