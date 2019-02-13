package org.jabref.gui.contentselector;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

public class ContentSelectorDialogView extends BaseDialog<Void> {

    @FXML private Button addFieldNameButton;
    @FXML private Button removeFieldNameButton;
    @FXML private Button addKeywordButton;
    @FXML private Button removeKeywordButton;
    @FXML private ListView<String> fieldNamesListView;
    @FXML private ListView<String> keywordsListView;
    @FXML private ButtonType saveButton;

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
    }

    @FXML
    private void addNewFieldName() {
        viewModel.showInputFieldNameDialog();
    }

    @FXML
    private void removeFieldName() {
        String selectedFieldName = fieldNamesListView.getFocusModel().getFocusedItem();
        viewModel.showRemoveFieldNameConfirmationDialog(selectedFieldName);
    }

    private void initFieldNameListView() {
        fieldNamesListView.setItems(viewModel.loadFieldNames());
    }
}
