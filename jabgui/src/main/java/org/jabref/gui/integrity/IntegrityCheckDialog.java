package org.jabref.gui.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.JabRefException;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.FieldTextMapper;

import com.airhacks.afterburner.views.ViewLoader;
import com.airhacks.afterburner.views.ViewLoaderResult;
import jakarta.inject.Inject;
import org.controlsfx.control.table.TableFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrityCheckDialog extends BaseDialog<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrityCheckDialog.class);

    @FXML private TableView<IntegrityMessage> messagesTable;
    @FXML private TableColumn<IntegrityMessage, String> keyColumn;
    @FXML private TableColumn<IntegrityMessage, String> fieldColumn;
    @FXML private TableColumn<IntegrityMessage, String> messageColumn;
    @FXML private MenuButton keyFilterButton;
    @FXML private MenuButton fieldFilterButton;
    @FXML private MenuButton messageFilterButton;
    @FXML private VBox dialogVBox;

    @Inject private EntryEditor entryEditor;
    @Inject private StateManager stateManager;
    private final List<IntegrityMessage> messages;
    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private IntegrityCheckDialogViewModel viewModel;
    private TableFilter<IntegrityMessage> tableFilter;
    private BibLogSettingsPane bibLogSettingsPane;
    private final List<IntegrityMessage> blgWarnings = new ArrayList<>();

    public IntegrityCheckDialog(List<IntegrityMessage> messages, LibraryTab libraryTab, DialogService dialogService) {
        this.messages = messages;
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;

        this.setTitle(Localization.lang("Check integrity"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    private void handleRowClick(IntegrityMessage message, MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            libraryTab.clearAndSelect(message.entry());

            stateManager.getEditorShowing().setValue(true);

            // Focus field async to give entry editor time to load
            Platform.runLater(() -> entryEditor.setFocusToField(message.field()));
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
        keyColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().entry().getCitationKey().orElse("")));
        fieldColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(FieldTextMapper.getDisplayName(row.getValue().field())));
        messageColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().message()));

        new ValueTableCellFactory<IntegrityMessage, String>()
                .withText(Function.identity())
                .withTooltip(Function.identity())
                .install(messageColumn);

        tableFilter = TableFilter.forTableView(messagesTable)
                                 .apply();

        addMessageColumnFilter(keyColumn, keyFilterButton);
        addMessageColumnFilter(fieldColumn, fieldFilterButton);
        addMessageColumnFilter(messageColumn, messageFilterButton);

        loadBibLogSettingsPane();
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

    /**
     * Loads the BibLogSettingsPane.fxml view
     * and initializes its controller.
     */
    private void loadBibLogSettingsPane() {
        try {
            ViewLoaderResult result = ViewLoader.view(BibLogSettingsPane.class).load();

            Node settingsNode = result.getView();
            bibLogSettingsPane = (BibLogSettingsPane) result.getController();

            if (bibLogSettingsPane == null) {
                LOGGER.error("Failed to get controller from ViewLoader");
                return;
            }
            bibLogSettingsPane.initializeViewModel(
                    libraryTab.getBibDatabaseContext(),
                    this::reloadBlgWarnings
            );
            dialogVBox.getChildren().add(1, settingsNode);
            reloadBlgWarnings();
        } catch (JabRefException e) {
            dialogService.notify(e.getLocalizedMessage());
            LOGGER.error("Failed to initialize BibLogSettingsPane", e);
        }
    }

    /**
     * Called on:
     * (1) Dialog initialization (default load)
     * (2) User triggers Browse or Reset in BibLogSettingsPane
     * <p>
     * This reloads .blg warnings and merges them into the main message list.
     */
    private void reloadBlgWarnings() {
        try {
            bibLogSettingsPane.refreshWarnings(libraryTab.getBibDatabaseContext());
        } catch (JabRefException e) {
            dialogService.notify(e.getLocalizedMessage());
            LOGGER.warn("Failed to load .blg warnings", e);
        }
        List<IntegrityMessage> newWarnings = new ArrayList<>(bibLogSettingsPane.getBlgWarnings());

        if (newWarnings.isEmpty() && bibLogSettingsPane.wasBlgFileManuallySelected()) {
            dialogService.notify(Localization.lang("No warnings found. Please check if the .blg file matches the current library."));
        }

        messages.removeAll(blgWarnings);
        blgWarnings.clear();
        blgWarnings.addAll(newWarnings);
        messages.addAll(newWarnings);

        viewModel.getMessages().setAll(messages);
    }
}
