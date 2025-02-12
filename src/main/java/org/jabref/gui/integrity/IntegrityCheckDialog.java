package org.jabref.gui.integrity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.citationkeypattern.GenerateCitationKeyAction;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;
import org.controlsfx.control.table.TableFilter;

public class IntegrityCheckDialog extends BaseDialog<Void> {

    @FXML private TableView<IntegrityMessage> messagesTable;
    @FXML private TableColumn<IntegrityMessage, String> keyColumn;
    @FXML private TableColumn<IntegrityMessage, String> fieldColumn;
    @FXML private TableColumn<IntegrityMessage, String> messageColumn;
    @FXML private TableColumn<IntegrityMessage, String> fixesColumn;
    @FXML private MenuButton keyFilterButton;
    @FXML private MenuButton fieldFilterButton;
    @FXML private MenuButton messageFilterButton;
    @FXML private ComboBox<String> entryTypeCombo;

    @Inject private ThemeManager themeManager;

    private final List<IntegrityMessage> messages;
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final CliPreferences preferences;
    private final UndoManager undoManager;

    private IntegrityCheckDialogViewModel viewModel;
    private TableFilter<IntegrityMessage> tableFilter;

    public IntegrityCheckDialog(List<IntegrityMessage> messages,
                                Supplier<LibraryTab> tabSupplier,
                                DialogService dialogService,
                                StateManager stateManager,
                                TaskExecutor taskExecutor,
                                CliPreferences preferences,
                                UndoManager undoManager) {
        this.messages = messages;
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.undoManager = undoManager;

        this.setTitle(Localization.lang("Check integrity"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        themeManager.updateFontStyle(getDialogPane().getScene());
    }

    private void onSelectionChanged(ListChangeListener.Change<? extends IntegrityMessage> change) {
        if (change.next()) {
            change.getAddedSubList().stream().findFirst().ifPresent(message ->
                    tabSupplier.get().editEntryAndFocusField(message.entry(), message.field()));
        }
    }

    public IntegrityCheckDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    private void initialize() {
        viewModel = new IntegrityCheckDialogViewModel(messages, dialogService);

        messagesTable.getSelectionModel().getSelectedItems().addListener(this::onSelectionChanged);
        messagesTable.setItems(viewModel.getMessages());
        entryTypeCombo.getItems().addAll(viewModel.getEntryTypes());
        entryTypeCombo.getSelectionModel().selectFirst();
        keyColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().entry().getCitationKey().orElse("")));
        fieldColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().field().getDisplayName()));
        messageColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().message()));

        fixesColumn.setCellFactory(row -> new TableCell<>() {
            private final Button button = new Button();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    IntegrityMessage rowData = getTableRow().getItem();
                    switch (rowData.field()) {
                        case StandardField.TITLE:
                            button.setText("Fix");
                            button.setOnAction(event -> {
                                viewModel.fixTitle(rowData);
                                dialogService.notify(Localization.lang("Masked capital letters using curly brackets {}"));
                                removeRowFromTable(getTableRow().getItem());
                            });
                            setGraphic(button);
                            break;
                        case InternalField.KEY_FIELD:
                            button.setText(Localization.lang("Generate Key"));
                            button.setOnAction(event -> new GenerateCitationKeyAction(tabSupplier, dialogService, stateManager, taskExecutor, preferences, undoManager).execute());
                            removeRowFromTable(getTableRow().getItem());
                            setGraphic(button);
                            break;
                        default:
                            setGraphic(null);
                            break;
                    }
                }
            }
        });

        new ValueTableCellFactory<IntegrityMessage, String>()
                .withText(Function.identity())
                .withTooltip(Function.identity())
                .install(messageColumn);

        tableFilter = TableFilter.forTableView(messagesTable)
                                 .apply();

        addMessageColumnFilter(keyColumn, keyFilterButton);
        addMessageColumnFilter(fieldColumn, fieldFilterButton);
        addMessageColumnFilter(messageColumn, messageFilterButton);
    }

    private void addMessageColumnFilter(TableColumn<IntegrityMessage, String> messageColumn, MenuButton messageFilterButton) {
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

    private void removeRowFromTable(IntegrityMessage message) {
        ObservableList<IntegrityMessage> mutableMessages = FXCollections.observableArrayList(messagesTable.getItems());
        mutableMessages.remove(message);
        messagesTable.setItems(mutableMessages);
    }

    @FXML
    private void fixByType() {
        String selectedType = entryTypeCombo.getSelectionModel().getSelectedItem();
        if (selectedType.equals(StandardField.TITLE.getDisplayName())) {
            for (IntegrityMessage message : messagesTable.getItems()) {
                if (message.field().getDisplayName().equals(selectedType)) {
                    viewModel.fixTitle(message);
                    removeRowFromTable(message);
                    viewModel.removeFromEntryTypes(message.field().getDisplayName());
                    entryTypeCombo.getItems().setAll(viewModel.getEntryTypes());
                    entryTypeCombo.getSelectionModel().selectFirst();
                    dialogService.notify(Localization.lang("Fixed successfully!"));
                }
            }
        } else {
            dialogService.notify(Localization.lang("No fixes available!"));
        }
    }
}
