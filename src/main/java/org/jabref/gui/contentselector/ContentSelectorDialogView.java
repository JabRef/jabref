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

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import org.fxmisc.easybind.EasyBind;

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
    private ListView<String> fieldNamesListView;
    @FXML
    private ListView<String> keywordsListView;
    @FXML
    private ButtonType saveButton;

    @Inject
    private DialogService dialogService;
    private final BasePanel basePanel;
    private ContentSelectorDialogViewModel viewModel;

    public ContentSelectorDialogView(BasePanel basePanel) {
        this.setTitle(Localization.lang("Manage content selectors"));
        this.getDialogPane().setPrefSize(375, 475);

        this.basePanel = basePanel;

        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> saveChangesAndClose());
    }

    @FXML
    public void initialize() {
        viewModel = new ContentSelectorDialogViewModel(basePanel, dialogService);

        initFieldNameComponents();
        initKeywordsComponents();
    }

    private void initFieldNameComponents() {
        initListView(fieldNamesListView, viewModel::getFieldNamesBackingList);
        viewModel.selectedFieldNameProperty().bind(fieldNamesListView.getSelectionModel().selectedItemProperty());
        removeFieldNameButton.disableProperty().bind(viewModel.isNoFieldNameSelected());
        EasyBind.subscribe(viewModel.selectedFieldNameProperty(), viewModel::populateKeywords);
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
        getSelectedFieldName().ifPresent(viewModel::showRemoveFieldNameConfirmationDialog);
    }

    @FXML
    private void addNewKeyword() {
        getSelectedFieldName().ifPresent(viewModel::showInputKeywordDialog);
    }

    @FXML
    private void removeKeyword() {
        Optional<String> fieldName = getSelectedFieldName();
        Optional<String> keywordToRemove = getSelectedKeyword();
        if (fieldName.isPresent() && keywordToRemove.isPresent()) {
            viewModel.showRemoveKeywordConfirmationDialog(fieldName.get(), keywordToRemove.get());
        }
    }

    private void initListView(ListView<String> listViewToInit, Supplier<ListProperty<String>> backingList) {
        listViewToInit.itemsProperty().bind(backingList.get());
        listViewToInit.getSelectionModel().selectFirst();
    }

    private Optional<String> getSelectedFieldName() {
        return Optional.of(fieldNamesListView.getSelectionModel()).map(SelectionModel::getSelectedItem);
    }

    private Optional<String> getSelectedKeyword() {
        return Optional.of(keywordsListView.getSelectionModel()).map(SelectionModel::getSelectedItem);
    }

    private void saveChangesAndClose() {
        viewModel.saveChanges();
        close();
    }
}
