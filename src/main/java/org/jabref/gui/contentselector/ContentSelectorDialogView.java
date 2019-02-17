package org.jabref.gui.contentselector;

import java.util.Optional;
import java.util.function.Supplier;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.metadata.MetaData;

import com.airhacks.afterburner.views.ViewLoader;

public class ContentSelectorDialogView extends BaseDialog<Void> {

    private static final int FIRST_ELEMENT = 0;

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

    private ContentSelectorDialogViewModel viewModel;

    private final BasePanel basePanel;
    private final DialogService dialogService;
    private final MetaData metaData;

    public ContentSelectorDialogView(JabRefFrame jabRefFrame) {
        this.setTitle(Localization.lang("Manage content selectors"));
        this.getDialogPane().setPrefSize(375, 475);

        this.basePanel = jabRefFrame.getCurrentBasePanel();
        this.dialogService = jabRefFrame.getDialogService();
        this.metaData = basePanel.getBibDatabaseContext().getMetaData();

        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> saveChangesAndClose());
    }

    @FXML
    public void initialize() {
        viewModel = new ContentSelectorDialogViewModel(metaData, basePanel, dialogService);

        initListView(fieldNamesListView, viewModel::getFieldNamesBackingList, (observable, oldValue, newValue) -> onFieldNameSelected(newValue));
        initListView(keywordsListView, viewModel::getKeywordsBackingList, (observable, oldValue, newValue) -> onKeywordSelected());
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

    private void initListView(ListView<String> listViewToInit, Supplier<ObservableList<String>> backingList, ChangeListener<String> onSelectedListener) {
        listViewToInit.setItems(backingList.get());
        listViewToInit.getSelectionModel().selectedItemProperty().addListener(onSelectedListener);
        listViewToInit.getSelectionModel().select(FIRST_ELEMENT);
    }

    private void onFieldNameSelected(String newValue) {
        removeKeywordButton.setDisable(!getSelectedKeyword().isPresent());
        removeFieldNameButton.setDisable(!getSelectedFieldName().isPresent());
        addKeywordButton.setDisable(newValue == null);
        viewModel.populateKeywordsFor((newValue));
    }

    private void onKeywordSelected() {
        removeKeywordButton.setDisable(!getSelectedKeyword().isPresent());
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
