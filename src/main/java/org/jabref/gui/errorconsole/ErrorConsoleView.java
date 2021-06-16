package org.jabref.gui.errorconsole;

import javax.inject.Inject;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.Callback;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;

import com.airhacks.afterburner.views.ViewLoader;

public class ErrorConsoleView extends BaseDialog<Void> {

    private ErrorConsoleViewModel viewModel;

    @FXML private ButtonType copyLogButton;
    @FXML private ButtonType clearLogButton;
    @FXML private ButtonType createIssueButton;
    @FXML private ListView<LogEventViewModel> messagesListView;
    @FXML private Label descriptionLabel;

    @Inject private DialogService dialogService;
    @Inject private ClipBoardManager clipBoardManager;
    @Inject private BuildInfo buildInfo;
    @Inject private KeyBindingRepository keyBindingRepository;

    public ErrorConsoleView() {
        this.setTitle(Localization.lang("Event log"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(copyLogButton, getDialogPane(), event -> copyLog());
        ControlHelper.setAction(clearLogButton, getDialogPane(), event -> clearLog());
        ControlHelper.setAction(createIssueButton, getDialogPane(), event -> createIssue());
    }

    @FXML
    private void initialize() {
        viewModel = new ErrorConsoleViewModel(dialogService, clipBoardManager, buildInfo);
        messagesListView.setCellFactory(createCellFactory());
        messagesListView.itemsProperty().bind(viewModel.allMessagesDataProperty());
        messagesListView.scrollTo(viewModel.allMessagesDataProperty().getSize() - 1);
        messagesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        viewModel.allMessagesDataProperty().addListener((ListChangeListener<LogEventViewModel>) (change -> {
            int size = viewModel.allMessagesDataProperty().size();
            if (size > 0) {
                messagesListView.scrollTo(size - 1);
            }
        }));
        descriptionLabel.setGraphic(IconTheme.JabRefIcons.CONSOLE.getGraphicNode());
    }

    private Callback<ListView<LogEventViewModel>, ListCell<LogEventViewModel>> createCellFactory() {
        return cell -> new ListCell<LogEventViewModel>() {

            private HBox graphic;
            private Node icon;
            private VBox message;
            private Label heading;
            private Label stacktrace;

            {
                graphic = new HBox(10);
                heading = new Label();
                stacktrace = new Label();
                message = new VBox();
                message.getChildren().setAll(heading, stacktrace);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            public void updateItem(LogEventViewModel event, boolean empty) {
                super.updateItem(event, empty);

                if ((event == null) || empty) {
                    setGraphic(null);
                } else {
                    icon = event.getIcon().getGraphicNode();
                    heading.setText(event.getDisplayText());
                    heading.getStyleClass().setAll(event.getStyleClass());
                    stacktrace.setText(event.getStackTrace().orElse(""));
                    graphic.getStyleClass().setAll(event.getStyleClass());
                    graphic.getChildren().setAll(icon, message);
                    setGraphic(graphic);
                }
            }
        };
    }

    @FXML
    private void copySelectedLogEntries(KeyEvent event) {
        if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.COPY, event)) {
            ObservableList<LogEventViewModel> selectedEntries = messagesListView.getSelectionModel().getSelectedItems();
            viewModel.copyLog(selectedEntries);
        }
    }

    @FXML
    private void copyLog() {
        viewModel.copyLog();
    }

    @FXML
    private void clearLog() {
        viewModel.clearLog();
    }

    @FXML
    private void createIssue() {
        viewModel.reportIssue();
    }
}
