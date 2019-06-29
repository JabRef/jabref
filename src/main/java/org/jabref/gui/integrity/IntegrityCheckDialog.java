package org.jabref.gui.integrity;

import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;

import org.jabref.gui.BasePanel;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.table.TableFilter;

public class IntegrityCheckDialog extends BaseDialog<Void> {

    private final List<IntegrityMessage> messages;
    private final BasePanel basePanel;
    @FXML private TableView<IntegrityMessage> messagesTable;
    @FXML private TableColumn<IntegrityMessage, String> keyColumn;
    @FXML private TableColumn<IntegrityMessage, String> fieldColumn;
    @FXML private TableColumn<IntegrityMessage, String> messageColumn;
    private IntegrityCheckDialogViewModel viewModel;

    public IntegrityCheckDialog(List<IntegrityMessage> messages, BasePanel basePanel) {
        this.messages = messages;
        this.basePanel = basePanel;
        this.setTitle(Localization.lang("Check integrity"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    private void onSelectionChanged(ListChangeListener.Change<? extends IntegrityMessage> change) {
        if (change.next()) {
            change.getAddedSubList().stream().findFirst().ifPresent(message ->
                    basePanel.editEntryAndFocusField(message.getEntry(), message.getField()));
        }
    }

    public IntegrityCheckDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    private void initialize() {
        viewModel = new IntegrityCheckDialogViewModel(messages);

        messagesTable.getSelectionModel().getSelectedItems().addListener(this::onSelectionChanged);
        messagesTable.setItems(viewModel.getMessages());
        keyColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getEntry().getCiteKeyOptional().orElse("")));
        fieldColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getField().getDisplayName()));
        messageColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getMessage()));

        TableFilter.forTableView(messagesTable)
                   .apply();
    }
}
