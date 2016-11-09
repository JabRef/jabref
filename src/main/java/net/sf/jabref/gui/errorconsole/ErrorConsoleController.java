package net.sf.jabref.gui.errorconsole;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.util.ViewModelListCellFactory;

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
        allMessages.setCellFactory(new ViewModelListCellFactory<LogEvent>().
                withGraphic( viewModel -> {
                    Level logLevel = viewModel.getLevel();
                    switch (logLevel.getStandardLevel()) {
                        case ERROR:
                            return (IconTheme.JabRefIcon.INTEGRITY_FAIL.getGraphicNode());
                        case WARN:
                            return (IconTheme.JabRefIcon.INTEGRITY_WARN.getGraphicNode());
                        case INFO:
                            return (IconTheme.JabRefIcon.INTEGRITY_INFO.getGraphicNode());
                        default:
                            return null;
                    }
                }).
                withStyleClass( viewModel -> {
                    Level logLevel = viewModel.getLevel();
                    switch (logLevel.getStandardLevel()) {
                        case ERROR:
                            return "exception";
                        case WARN:
                            return "output";
                        case INFO:
                            return "log";
                        default:
                            return null;
                    }
                }).
                withText( viewModel -> viewModel.getMessage().getFormattedMessage())
        );
    }

}
