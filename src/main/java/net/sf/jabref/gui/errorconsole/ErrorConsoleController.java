package net.sf.jabref.gui.errorconsole;

import javax.inject.Inject;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import net.sf.jabref.gui.AbstractController;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.DialogService;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.keyboard.KeyBindingPreferences;
import net.sf.jabref.gui.util.ViewModelListCellFactory;
import net.sf.jabref.logic.util.BuildInfo;

public class ErrorConsoleController extends AbstractController<ErrorConsoleViewModel> {

    @FXML private Button closeButton;
    @FXML private Button copyLogButton;
    @FXML private Button createIssueButton;
    @FXML private ListView<LogEventViewModel> messagesListView;
    @FXML private Label descriptionLabel;

    @Inject private DialogService dialogService;
    @Inject private ClipBoardManager clipBoardManager;
    @Inject private BuildInfo buildInfo;
    @Inject private KeyBindingPreferences keyBindingPreferences;

    @FXML
    private void initialize() {
        viewModel = new ErrorConsoleViewModel(dialogService, clipBoardManager, buildInfo);

        messagesListView.setCellFactory(new ViewModelListCellFactory<LogEventViewModel>()
                .withGraphic(viewModel1 -> viewModel1.getIcon().getGraphicNode())
                .withStyleClass(LogEventViewModel::getStyleClass)
                .withText(LogEventViewModel::getDisplayText)
        );
        messagesListView.itemsProperty().bind(viewModel.allMessagesDataProperty());
        messagesListView.scrollTo(viewModel.allMessagesDataProperty().getSize() - 1);
        messagesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        viewModel.allMessagesDataProperty().addListener((ListChangeListener<LogEventViewModel>) (change -> {
            int size = viewModel.allMessagesDataProperty().size();
            if (size > 0) {
                messagesListView.scrollTo(size - 1);
            }
        }));
        descriptionLabel.setGraphic(IconTheme.JabRefIcon.CONSOLE.getGraphicNode());
    }

    @FXML
    private void copySelectedLogEntries(KeyEvent event) {
        if (keyBindingPreferences.checkKeyCombinationEquality(KeyBinding.COPY, event)) {
            ObservableList<LogEventViewModel> selectedEntries = messagesListView.getSelectionModel().getSelectedItems();
            viewModel.copyLog(selectedEntries);
        }
    }

    @FXML
    private void copyLog() {
        viewModel.copyLog();
    }

    @FXML
    private void createIssue() {
        viewModel.reportIssue();
    }

    @FXML
    private void closeErrorDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
