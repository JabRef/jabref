package org.jabref.gui.libraryproperties.contentselectors;

import java.util.Optional;
import java.util.function.Supplier;

import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;

import org.jabref.gui.DialogService;
import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;

public class ContentSelectorView extends AbstractPropertiesTabView<ContentSelectorViewModel> {

    @FXML private Button removeFieldNameButton;
    @FXML private Button addKeywordButton;
    @FXML private Button removeKeywordButton;
    @FXML private ListView<Field> fieldsListView;
    @FXML private ListView<String> keywordsListView;

    @Inject private DialogService dialogService;

    private final BibDatabaseContext databaseContext;

    public ContentSelectorView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Content selectors");
    }

    @FXML
    public void initialize() {
        this.viewModel = new ContentSelectorViewModel(databaseContext, dialogService);

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
}
