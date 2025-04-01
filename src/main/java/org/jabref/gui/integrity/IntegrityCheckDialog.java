package org.jabref.gui.integrity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
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

    @Inject private ThemeManager themeManager;
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
        keyColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().entry().getCitationKey().orElse("")));
        fieldColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().field().getDisplayName()));
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
            FXMLLoader loader = new FXMLLoader();
            try (var fxmlStream = getClass().getResourceAsStream("BibLogSettingsPane.fxml")) {
                if (fxmlStream == null) {
                    LOGGER.error("Could not find BibLogSettingsPane.fxml");
                    return;
                }

                Node settingsNode = loader.load(fxmlStream);
                bibLogSettingsPane = loader.getController();
                bibLogSettingsPane.initialize(
                        libraryTab.getBibDatabaseContext(),
                        dialogService,
                        this::reloadBlgWarnings
                );
                dialogVBox.getChildren().add(1, settingsNode);
                reloadBlgWarnings();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load BibLogSettingsPane", e);
        }
    }

    /**
     * Called on:
     * (1) Dialog initialization (default load)
     * (2) User triggers Browse or Reset in BibLogSettingsPane
     *
     * Steps:
     * 1. Get the resolved .blg path and check if it has changed.
     * 2. Fetch new .blg warnings from ViewModel.
     * 3. Replace old warnings and update the table.
     */
    private void reloadBlgWarnings() {
        bibLogSettingsPane.getViewModel().getResolvedBlgPath().ifPresent(newBlgPath -> {
            Optional<Path> lastPath = bibLogSettingsPane.getViewModel().getLastResolvedBlgPath();
            if (lastPath.isPresent() && newBlgPath.equals(lastPath)) {
                return;
            }
            List<IntegrityMessage> newWarnings = bibLogSettingsPane.getViewModel().getBlgWarnings(libraryTab.getBibDatabaseContext());
            messages.removeAll(blgWarnings);
            blgWarnings.clear();
            blgWarnings.addAll(newWarnings);
            messages.addAll(blgWarnings);

            viewModel = new IntegrityCheckDialogViewModel(messages);
            messagesTable.setItems(viewModel.getMessages());
        });
    }
}
