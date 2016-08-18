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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.util.Callback;

import net.sf.jabref.gui.FXAlert;
import net.sf.jabref.logic.error.MessagePriority;
import net.sf.jabref.logic.error.ObservableMessageWithPriority;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.logging.ObservableMessages;

import com.airhacks.afterburner.views.FXMLView;

public class ErrorConsoleView extends FXMLView {

    private final ErrorConsoleViewModel errorViewModel = new ErrorConsoleViewModel();
    private BooleanProperty isDeveloperButtonEnable = new SimpleBooleanProperty();

    @FXML
    private Button closeButton;
    @FXML
    private Button copyLogButton;
    @FXML
    private Button createIssueButton;
    @FXML
    private ListView<ObservableMessageWithPriority> allMessage;

    public ErrorConsoleView() {
        super();
        bundle = Localization.getMessages();
    }

    public void show() {
        FXAlert errorConsole = new FXAlert(AlertType.ERROR, Localization.lang("Developer information"), false);

        // create extra Error-Icon for Dialogpane in JavaFX
        Label img = new Label();
        img.getStyleClass().addAll("alert", "error", "dialog-pane");
        img.setScaleX(1.5);
        img.setScaleY(1.5);
        DialogPane pane = (DialogPane) this.getView();
        pane.setGraphic(img);
        errorConsole.setDialogPane(pane);
        errorConsole.setResizable(true);
        errorConsole.show();
    }

    @FXML
    private void initialize() {
        ButtonBar.setButtonData(copyLogButton, ButtonBar.ButtonData.LEFT);
        ButtonBar.setButtonData(createIssueButton, ButtonBar.ButtonData.LEFT);

        ObservableList<ObservableMessageWithPriority> masterData = ObservableMessages.INSTANCE.messagesPropety();
        listViewStyle();
        allMessage.setItems(masterData);
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

    private void listViewStyle(){
        // handler for listCell appearance (example for exception Cell)
        allMessage.setCellFactory(new Callback<ListView<ObservableMessageWithPriority>, ListCell<ObservableMessageWithPriority>>() {
            @Override
            public ListCell<ObservableMessageWithPriority> call(ListView<ObservableMessageWithPriority> listView) {
                return new ListCell<ObservableMessageWithPriority>() {
                    @Override
                    public void updateItem(ObservableMessageWithPriority omp, boolean empty) {
                        super.updateItem(omp, empty);
                        if (omp != null) {
                            setText(omp.getMessage());
                            getStyleClass().clear();
                            if (omp.getPriority() == MessagePriority.HIGH) {
                                    getStyleClass().add("exception");
                            } else if (omp.getPriority() == MessagePriority.MEDIUM) {
                                    getStyleClass().add("output");
                            } else {
                                getStyleClass().add("log");
                            }
                        } else {
                            setText("");
                        }
                    }
                };
            }
        });
    }
}
