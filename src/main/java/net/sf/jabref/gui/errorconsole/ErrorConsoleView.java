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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
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
        DialogPane pane = (DialogPane) this.getView();
        pane.setHeader(createDialogPaneHeader());
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

    /*
     * create a grid pane with two columns to insert one image on left side and one text on right side.
     * This will be late set in the header of the dialog pane
     * @return the generate grid pane
     */
    private GridPane createDialogPaneHeader() {
        GridPane headerGrid = new GridPane();
        ColumnConstraints graphicColumn = new ColumnConstraints();
        graphicColumn.setFillWidth(false);
        graphicColumn.setHgrow(Priority.NEVER);
        ColumnConstraints textColumn = new ColumnConstraints();
        textColumn.setFillWidth(true);
        textColumn.setHgrow(Priority.ALWAYS);
        headerGrid.getColumnConstraints().setAll(graphicColumn, textColumn);
        headerGrid.setPadding(new Insets(10));

        Image image = new Image("https://cdn2.iconfinder.com/data/icons/windows-8-metro-style/512/console.png");
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(48);
        imageView.setFitHeight(48);
        StackPane stackPane = new StackPane(imageView);
        stackPane.setAlignment(Pos.CENTER);
        headerGrid.add(stackPane, 0, 0);

        Label headerLabel = new Label(Localization.lang("We now give you an insight into the inner workings of JabRef's brain. ") +
                Localization.lang("This might help to diagonalize the root of problems. ") + System.lineSeparator() +
                Localization.lang("Please use the button below to inform the developers about an issue."));
        headerLabel.setWrapText(true);
        headerLabel.setPadding(new Insets(10));
        headerLabel.setAlignment(Pos.CENTER_LEFT);
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setMaxHeight(Double.MAX_VALUE);
        headerGrid.add(headerLabel, 1, 0);

        return headerGrid;
    }

    //style the list view with icon and message color
    private void listViewStyle() {
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
                                setGraphic(listImageResize("http://www.iconsdb.com/icons/preview/red/info-xxl.png"));
                                getStyleClass().add("exception");
                            } else if (omp.getPriority() == MessagePriority.MEDIUM) {
                                setGraphic(listImageResize("http://www.iconsdb.com/icons/preview/black/info-xxl.png"));
                                getStyleClass().add("output");
                            } else {
                                setGraphic(listImageResize("http://www.iconsdb.com/icons/preview/royal-blue/info-xxl.png"));
                                getStyleClass().add("log");
                            }
                        } else {
                            setText(null);
                            setGraphic(null);
                        }
                    }
                };
            }
        });
    }

    //resize the url image in icon size 16x16
    private ImageView listImageResize(String imageURL) {
        Image image = new Image(imageURL);
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
        return imageView;
    }
}
