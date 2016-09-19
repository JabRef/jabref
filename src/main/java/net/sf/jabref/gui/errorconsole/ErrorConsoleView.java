/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.errorconsole;

import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.util.Callback;

import net.sf.jabref.gui.FXAlert;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

public class ErrorConsoleView extends FXMLView {

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

    public ErrorConsoleView() {
        super();
        bundle = Localization.getMessages();
    }

    public void show() {
        FXAlert errorConsole = new FXAlert(AlertType.ERROR, Localization.lang("Event log"), false);
        DialogPane pane = (DialogPane) this.getView();
        errorConsole.setDialogPane(pane);
        errorConsole.setResizable(true);
        errorConsole.show();
    }

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
