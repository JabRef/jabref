package org.jabref.gui.integrity;

import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.table.TableFilter;

public class IntegrityCheckDialog extends BaseDialog<Void> {

    private final List<IntegrityMessage> messages;
    private final LibraryTab libraryTab;
    @FXML private TableView<IntegrityMessage> messagesTable;
    @FXML private TableColumn<IntegrityMessage, String> keyColumn;
    @FXML private TableColumn<IntegrityMessage, String> fieldColumn;
    @FXML private TableColumn<IntegrityMessage, String> messageColumn;
    @FXML private MenuButton keyFilterButton;
    @FXML private MenuButton fieldFilterButton;
    @FXML private MenuButton messageFilterButton;
    private IntegrityCheckDialogViewModel viewModel;
    private TableFilter<IntegrityMessage> tableFilter;

    public IntegrityCheckDialog(List<IntegrityMessage> messages, LibraryTab libraryTab) {
        this.messages = messages;
        this.libraryTab = libraryTab;
        this.setTitle(Localization.lang("Check integrity"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    private void onSelectionChanged(ListChangeListener.Change<? extends IntegrityMessage> change) {
        if (change.next()) {
            change.getAddedSubList().stream().findFirst().ifPresent(message ->
                    libraryTab.editEntryAndFocusField(message.getEntry(), message.getField()));
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
        keyColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getEntry().getCitationKey().orElse("")));
        fieldColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getField().getDisplayName()));
        messageColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().getMessage()));

        tableFilter = TableFilter.forTableView(messagesTable)
                                 .apply();

        tableFilter.getColumnFilter(keyColumn).ifPresent(columnFilter -> {
            ContextMenu keyContextMenu = keyColumn.getContextMenu();
            if (keyContextMenu != null) {
                keyFilterButton.setContextMenu(keyContextMenu);
                keyFilterButton.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (keyContextMenu.isShowing()) {
                            keyContextMenu.setX(event.getScreenX());
                            keyContextMenu.setY(event.getScreenY());
                        } else {
                            keyContextMenu.show(keyFilterButton, event.getScreenX(), event.getScreenY());
                        }
                    }
                });
            }
        });

        tableFilter.getColumnFilter(fieldColumn).ifPresent(columnFilter -> {
            ContextMenu fieldContextMenu = fieldColumn.getContextMenu();
            if (fieldContextMenu != null) {
                fieldFilterButton.setContextMenu(fieldContextMenu);
                fieldFilterButton.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (fieldContextMenu.isShowing()) {
                            fieldContextMenu.setX(event.getScreenX());
                            fieldContextMenu.setY(event.getScreenY());
                        } else {
                            fieldContextMenu.show(fieldFilterButton, event.getScreenX(), event.getScreenY());
                        }
                    }
                });
            }
        });

        tableFilter.getColumnFilter(messageColumn).ifPresent(columnFilter -> {
            ContextMenu messageContextMenu = messageColumn.getContextMenu();
            if (messageContextMenu != null) {
                messageFilterButton.setContextMenu(messageContextMenu);
                messageFilterButton.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (messageContextMenu.isShowing()) {
                            messageContextMenu.setX(event.getScreenX());
                            messageContextMenu.setY(event.getScreenY());
                        } else {
                            messageContextMenu.show(messageFilterButton, event.getScreenX(), event.getScreenY());
                        }
                    }
                });
            }
        });
    }

    public void clearFilters() {
        if (tableFilter != null) {
            tableFilter.resetFilter();
            messagesTable.getColumns().forEach(column -> {
                tableFilter.selectAllValues(column);
                column.setGraphic(null);
            });
        }
    }
}
