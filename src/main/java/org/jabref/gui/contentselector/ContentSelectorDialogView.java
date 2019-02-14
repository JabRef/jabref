package org.jabref.gui.contentselector;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import java.util.Optional;

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

    private final JabRefFrame jabRefFrame;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final MetaData metaData;

    public ContentSelectorDialogView(JabRefFrame jabRefFrame) {
        this.setTitle(Localization.lang("Manage content selectors"));
        this.getDialogPane().setPrefSize(375, 475);

        this.jabRefFrame = jabRefFrame;
        this.dialogService = jabRefFrame.getDialogService();
        this.stateManager = Globals.stateManager;
        this.metaData = stateManager.getActiveDatabase().map(BibDatabaseContext::getMetaData).orElseGet(MetaData::new);

        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> System.out.println("test"));
    }

    @FXML
    public void initialize() {
        viewModel = new ContentSelectorDialogViewModel(metaData, dialogService);
        initFieldNameListView();
        initKeywordsListView();
    }

    private void initKeywordsListView() {
        keywordsListView.setItems(viewModel.getKeywordsBackingList());
        keywordsListView.getSelectionModel().select(FIRST_ELEMENT);
        keywordsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                removeKeywordButton.setDisable(!getSelectedKeyword().isPresent())
        );
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

    private Optional<String> getSelectedFieldName() {
        return Optional.of(fieldNamesListView).map(ListView::getSelectionModel).map(SelectionModel::getSelectedItem);
    }

    private Optional<String> getSelectedKeyword() {
        return Optional.of(keywordsListView).map(ListView::getSelectionModel).map(SelectionModel::getSelectedItem);
    }

    private void initFieldNameListView() {
        fieldNamesListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.populateKeywordsFor(newValue);
            removeKeywordButton.setDisable(!getSelectedKeyword().isPresent());
        });
        fieldNamesListView.setItems(viewModel.getFieldNamesBackingList());
        fieldNamesListView.getSelectionModel().select(FIRST_ELEMENT);
    }
}
