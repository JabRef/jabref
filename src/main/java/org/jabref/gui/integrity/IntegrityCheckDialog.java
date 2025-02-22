package org.jabref.gui.integrity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

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
    private final LibraryTab libraryTab;
    private final DialogService dialogService;

    private IntegrityCheckDialogViewModel viewModel;
    private TableFilter<IntegrityMessage> tableFilter;

    public IntegrityCheckDialog(List<IntegrityMessage> messages,
                                LibraryTab libraryTab,
                                DialogService dialogService) {
        this.messages = messages;
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;

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
                    libraryTab.editEntryAndFocusField(message.entry(), message.field()));
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
        updateEntryTypeCombo();
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
                    return;
                }
                IntegrityMessage rowData = getTableRow().getItem();
                configureAction(rowData);
            }

            private void configureAction(IntegrityMessage message) {
                Optional<IntegrityIssue> issue = IntegrityIssue.fromField(message.field());
                if (issue.isPresent() && issue.get().getFix() != null) {
                    configureButton(issue.get().getFix(), () -> {
                        viewModel.fix(issue.get(), message);
                        removeRowFromTable(message);
                    });
                    setGraphic(button);
                    return;
                }
                setGraphic(null);
            }

            private void configureButton(String text, Runnable action) {
                button.setText(text);
                button.setPrefHeight(20.0);
                button.setOnAction(event -> action.run());
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

    private void updateEntryTypeCombo() {
        Set<Field> entryTypes = viewModel.getEntryTypes();
        Set<String> uniqueTexts = new HashSet<>();
        entryTypeCombo.getItems().clear();

        Arrays.stream(IntegrityIssue.values())
              .filter(issue -> entryTypes.contains(issue.getField()))
              .filter(issue -> uniqueTexts.add(issue.getText()))
              .forEach(issue -> entryTypeCombo.getItems().add(issue.getText()));

        if (!entryTypeCombo.getItems().isEmpty()) {
            entryTypeCombo.getSelectionModel().selectFirst();
        }
    }

    private boolean hasFix(IntegrityMessage message) {
        return message != null && message.field() != null && IntegrityIssue.fromField(message.field())
                                                                           .map(issue -> issue.getFix() != null)
                                                                           .orElse(false);
    }

    @FXML
    private void fixByType() {
        AtomicBoolean fixed = new AtomicBoolean(false);

        String selectedType = entryTypeCombo.getSelectionModel().getSelectedItem();
        Optional<IntegrityIssue> selectedIssue = Arrays.stream(IntegrityIssue.values())
                                                       .filter(issue -> issue.getText().equals(selectedType))
                                                       .findFirst();

        selectedIssue.ifPresent(issue -> {
            messagesTable.getItems().stream()
                    .filter(message -> message.field().equals(issue.getField()) && hasFix(message))
                    .forEach(message -> {
                        viewModel.fix(issue, message);
                        removeRowFromTable(message);
                        viewModel.removeFromEntryTypes(message.field().getDisplayName());
                        fixed.set(true);
                    });
        });

        updateEntryTypeCombo();

        if (fixed.get()) {
            dialogService.notify(Localization.lang("Fixed successfully!"));
        } else {
            dialogService.notify(Localization.lang("No fixes available!"));
        }
    }

    @FXML
    private void fixAll() {
        messagesTable.getItems().stream()
                .filter(this::hasFix)
                .forEach(message -> IntegrityIssue.fromField(message.field()).ifPresent(issue -> {
                    viewModel.fix(issue, message);
                    viewModel.removeFromEntryTypes(message.field().getDisplayName());
                    removeRowFromTable(message);
                }));

        updateEntryTypeCombo();
    }
}
