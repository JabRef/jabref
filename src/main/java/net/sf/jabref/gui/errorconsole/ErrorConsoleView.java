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

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

import net.sf.jabref.gui.FXAlert;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.error.LogMessageWithPriority;
import net.sf.jabref.logic.error.MessagePriority;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.logging.LogMessage;

import com.airhacks.afterburner.views.FXMLView;

public class ErrorConsoleView extends FXMLView {

    private final ErrorConsoleViewModel errorViewModel = new ErrorConsoleViewModel();

    @FXML
    private Button closeButton;
    @FXML
    private Button copyLogButton;
    @FXML
    private Button createIssueButton;
    @FXML
    private ListView<LogMessageWithPriority> allMessage;
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
        ButtonBar.setButtonData(copyLogButton, ButtonBar.ButtonData.LEFT);
        ButtonBar.setButtonData(createIssueButton, ButtonBar.ButtonData.LEFT);

        ObservableList<LogMessageWithPriority> masterData = LogMessage.getInstance().messagesProperty();
        listViewStyle();
        allMessage.setItems(masterData);
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
        allMessage.setCellFactory(
                new Callback<ListView<LogMessageWithPriority>, ListCell<LogMessageWithPriority>>() {
                    @Override
                    public ListCell<LogMessageWithPriority> call(
                            ListView<LogMessageWithPriority> listView) {
                        return new ListCell<LogMessageWithPriority>() {

                            @Override
                            public void updateItem(LogMessageWithPriority logMessageWithPriority, boolean empty) {
                                super.updateItem(logMessageWithPriority, empty);
                                if (logMessageWithPriority != null) {
                                    setText(logMessageWithPriority.getMessage());
                                    Text graphic = new Text();
                                    graphic.getStyleClass().add("icon");

                                    MessagePriority prio = logMessageWithPriority.getPriority();
                                    switch (prio) {
                                        case HIGH:
                                            getStyleClass().add("exception");
                                            graphic.setText(IconTheme.JabRefIcon.INTEGRITY_FAIL.getCode());
                                            break;
                                        case MEDIUM:
                                            getStyleClass().add("output");
                                            graphic.setText(IconTheme.JabRefIcon.INTEGRITY_WARN.getCode());
                                            break;
                                        case LOW:
                                            getStyleClass().add("log");
                                            graphic.setText(IconTheme.JabRefIcon.INTEGRITY_INFO.getCode());
                                            break;
                                        default:
                                            break;
                                    }
                                    setGraphic(graphic);
                                } else {
                                    setText(null);
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
    }
}
