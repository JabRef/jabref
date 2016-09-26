package net.sf.jabref.gui.errorconsole;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.util.Callback;

import net.sf.jabref.gui.IconTheme;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Controller for the error console view model having access to ui elements as well as to the view model.
 */
public class ErrorConsoleController {

    private final ErrorConsoleViewModel errorViewModel = new ErrorConsoleViewModel();

    @FXML
    private Button closeButton;
    @FXML
    private Button copyLogButton;
    @FXML
    private Button createIssueButton;
    @FXML
    private ListView<LogEvent> allMessages;
    @FXML
    private Label descriptionLabel;

    @FXML
    private void initialize() {
        listViewStyle();
        allMessages.itemsProperty().bind(errorViewModel.allMessagesDataproperty());
        descriptionLabel.setGraphic(IconTheme.JabRefIcon.CONSOLE.getGraphicNode());
    }

    @FXML
    private void copyLogButton() {
        errorViewModel.copyLog();
    }

    @FXML
    private void createIssueButton() {
        errorViewModel.reportIssue();
    }

    @FXML
    private void closeErrorDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Style the list view with icon and message color
     */
    private void listViewStyle() {
        // Handler for listCell appearance (example for exception Cell)
        allMessages.setCellFactory(
                new Callback<ListView<LogEvent>, ListCell<LogEvent>>() {
                    @Override
                    public ListCell<LogEvent> call(
                            ListView<LogEvent> listView) {
                        return new ListCell<LogEvent>() {

                            @Override
                            public void updateItem(LogEvent logMessage, boolean empty) {
                                super.updateItem(logMessage, empty);
                                if (logMessage != null) {
                                    setText(logMessage.getMessage().toString());

                                    Level logLevel = logMessage.getLevel();
                                    switch (logLevel.getStandardLevel()) {
                                        case ERROR:
                                            getStyleClass().add("exception");
                                            setGraphic(IconTheme.JabRefIcon.INTEGRITY_FAIL.getGraphicNode());
                                            break;
                                        case WARN:
                                            getStyleClass().add("output");
                                            setGraphic(IconTheme.JabRefIcon.INTEGRITY_WARN.getGraphicNode());
                                            break;
                                        case INFO:
                                            getStyleClass().add("log");
                                            setGraphic(IconTheme.JabRefIcon.INTEGRITY_INFO.getGraphicNode());
                                            break;
                                        default:
                                            setText(null);
                                            setGraphic(null);
                                            break;
                                    }
                                }
                            }
                        };
                    }
                });
    }

}
