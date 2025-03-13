package org.jabref.gui.integrity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.integrity.IntegrityIssue;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;

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

    private final double FIX_BUTTON_HEIGHT = 20.0;

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

    private void handleRowClick(IntegrityMessage message, MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            libraryTab.editEntryAndFocusField(message.entry(), message.field());
            if (event.getClickCount() == 2) {
                this.close();
            }
        }
    }

    public IntegrityCheckDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    private void initialize() {
        viewModel = new IntegrityCheckDialogViewModel(messages);

        new ViewModelTableRowFactory<IntegrityMessage>()
                .withOnMouseClickedEvent(this::handleRowClick)
                .install(messagesTable);
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

            /**
             * Configures the action for the button based on the provided {@link IntegrityMessage}.
             * If a fix is available for the message, the button is configured to apply the fix when clicked.
             *
             * @param message the {@link IntegrityMessage} to check for available fixes
             */
            private void configureAction(IntegrityMessage message) {
                Optional<IntegrityIssue> issue = IntegrityIssue.fromMessage(message);
                if (issue.isEmpty()) {
                    return;
                }
                if (issue.get().getFix().isEmpty()) {
                    setGraphic(new Label(Localization.lang("No fix available")));
                    return;
                }
                configureButton(issue.get().getFix().toString(), () -> {
                    viewModel.fix(issue.get(), message);
                    viewModel.removeFromEntryTypes(message.field().getDisplayName());
                    Platform.runLater(() -> viewModel.columnsListProperty().getValue().removeIf(column -> Objects.equals(column.message(), message.message())));
                });
                setGraphic(button);
            }

            private void configureButton(String text, Runnable action) {
                button.setText(text);
                button.setPrefHeight(FIX_BUTTON_HEIGHT);
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

       messagesTable.itemsProperty().bind(viewModel.columnsListProperty());
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

    private void updateEntryTypeCombo() {
        Set<String> entryTypes = viewModel.getEntryTypes();
        Set<String> uniqueTexts = new HashSet<>();
        entryTypeCombo.getItems().clear();

        Arrays.stream(IntegrityIssue.values())
              .filter(issue -> issue.getFix().isPresent())
              .filter(issue -> entryTypes.contains(issue.getText()))
              .filter(issue -> uniqueTexts.add(issue.getText()))
              .forEach(issue -> entryTypeCombo.getItems().add(issue.getText()));

        if (entryTypeCombo.getItems().isEmpty()) {
            entryTypeCombo.getItems().add(Localization.lang("No fix available"));
        }
        entryTypeCombo.getSelectionModel().selectFirst();
    }

    public void fix(IntegrityIssue issue, IntegrityMessage message) {
        viewModel.fix(issue, message);
        viewModel.removeFromEntryTypes(message.field().getDisplayName());
        Platform.runLater(() -> viewModel.columnsListProperty().getValue().removeIf(column -> Objects.equals(column.message(), message.message())));
    }

    /**
     * Checks if a given {@link IntegrityMessage} has a fix available.
     *
     * @param message the {@link IntegrityMessage} to check
     * @return {@code true} if a fix is available, {@code false} otherwise
     */
    private boolean hasFix(IntegrityMessage message) {
        return message != null && message.field() != null && IntegrityIssue.fromMessage(message)
                                                                           .map(issue -> issue.getFix().isPresent())
                                                                           .orElse(false);
    }

    /**
     * Attempts to fix all {@link IntegrityMessage} objects of the selected type.
     * If fixes are available, they are applied, and the fixed messages are removed.
     * A notification is shown to indicate success or failure.
     */
    @FXML
    private void fixByType() {
        AtomicBoolean fixed = new AtomicBoolean(false);

        String selectedType = entryTypeCombo.getSelectionModel().getSelectedItem();
        Optional<IntegrityIssue> selectedIssue = Arrays.stream(IntegrityIssue.values())
                                                       .filter(issue -> issue.getText().equals(selectedType))
                                                       .findFirst();

        selectedIssue.ifPresent(issue -> {
            messagesTable.getItems().stream()
                    .filter(message -> message.message().equals(issue.getText()) && hasFix(message)) // Filter messages matching the selected issue type and have a fix
                    .forEach(message -> {
                        fix(issue, message);
                        fixed.set(true);
                    });
        });

        updateEntryTypeCombo();

        if (fixed.get()) {
            dialogService.notify(Localization.lang("Fixed successfully."));
        } else {
            dialogService.notify(Localization.lang("No fixes available."));
        }
    }

    /**
     * Attempts to fix all {@link IntegrityMessage} objects that have a fix available.
     * Messages with applicable fixes are processed, and the corresponding UI elements are updated.
     */
    @FXML
    private void fixAll() {
        messagesTable.getItems().stream()
                .filter(this::hasFix)   // Filter all messages that have a fix
                .forEach(message -> IntegrityIssue.fromMessage(message).ifPresent(issue -> fix(issue, message)));

        updateEntryTypeCombo();
    }
}
